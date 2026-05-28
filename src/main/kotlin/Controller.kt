import java.util.UUID

class Controller(
    private val storage: Storage,
    private val logger: Logger,
    private val view: View,
    private val actionProcessor: ActionProcessor = ActionProcessor(),
) {
    private var game: Game? = null
    private var bigBlind: Int = 100
    private var smallBlind: Int = 50

    fun startGame() {
        view.printWelcome()

        smallBlind = view.readBlindAmount("small blind", 50)
        bigBlind = view.readBlindAmount("big blind", 100)

        game = Game()
        var playerCount = 0

        do {
            if (playerCount >= 9) {
                view.printMessage("Maximum 10 players reached!") // В Техасском холдеме не больше 10 игроков
                break
            }

            playerCount++
            view.printPlayerSetupHeader(playerCount)
            val name = view.readPlayerName()
            val stack = view.readPositiveInt("Enter starting stack", 1000)
            val player = Player(UUID.randomUUID(), name, stack)
            game?.addPlayer(player)
            logger.info("Added player: ${player.name} with ${player.getStack()} chips")
        } while (view.askAddPlayer())

        while (game?.players?.size ?: 0 < 2) {
            view.printError("Minimum 2 players required!")
            playerCount++
            view.printPlayerSetupHeader(playerCount)
            val name = view.readPlayerName()
            val stack = view.readPositiveInt("Enter starting stack", 1000)
            val player = Player(UUID.randomUUID(), name, stack)
            game?.addPlayer(player)
            logger.info("Added player: ${player.name} with ${player.getStack()} chips")
        }

        view.printGameInfo(requireNotNull(game))
        view.printBlindsConfigured(smallBlind, bigBlind)
    }

    fun startHand() {
        logger.info("Controller.startHand() called, game=${game != null}, players=${game?.players?.size}")
        if (game?.players?.isEmpty() ?: true) {
            logger.error("Controller.startHand(): Game not initialized")
            view.printError("Game not initialized")
            return
        }

        // Проверка: игра уже окончена
        if (isGameEnded()) {
            view.printGameAlreadyEnded()
            return
        }

        val hand = game?.startHand() ?: return
        val players = hand.getPlayers()

        val sbIndex = (hand.getDealerIndex() + 1) % players.size
        val bbIndex = (hand.getDealerIndex() + 2) % players.size

        placeBlind(players[sbIndex], smallBlind, hand)
        placeBlind(players[bbIndex], bigBlind, hand)

        hand.setCurrentBet(bigBlind)

        view.printPlayerOrder(players, hand.getDealerIndex(), sbIndex, bbIndex)
        view.printHandInfo(hand)

        // Префлоп: первый ход после BB
        val preFlopStartIndex = (bbIndex + 1) % players.size
        val preFlopOver = runBettingRound(hand, preFlopStartIndex, isPreFlop = true)

        // Если все фолднули кроме одного, переходим к showdown
        if (preFlopOver && hand.phase == GamePhase.SHOWDOWN) {
            handleShowdown(hand)
            if (isGameEnded()) {
                view.printGameEnded()
            }
            return
        }

        // Постфлоп: Флоп, Терн, Ривер
        while (hand.phase != GamePhase.SHOWDOWN) {
            when (hand.phase) {
                GamePhase.PREFLOP -> {
                    view.dealFlop()
                    hand.dealFlop()
                }
                GamePhase.FLOP -> {
                    view.dealTurn()
                    hand.dealTurn()
                }
                GamePhase.TURN -> {
                    view.dealRiver()
                    hand.dealRiver()
                }
                else -> break
            }

            view.printHandInfo(hand)

            // Постфлоп: первый активный слева от дилера
            val startIndexCandidate = (hand.getDealerIndex() + 1) % players.size
            val postFlopStartIndex = findFirstActive(hand, startIndexCandidate)
            if (postFlopStartIndex == -1) break

            val postFlopOver = runBettingRound(hand, postFlopStartIndex, isPreFlop = false)
            if (postFlopOver) {
                // Раунд завершён - либо все фолднули, либо все уравняли
                if (hand.phase == GamePhase.SHOWDOWN) {
                    // Все фолднули, winner decided
                    handleShowdown(hand)
                    if (isGameEnded()) {
                        view.printGameEnded()
                    }
                    return
                }
                // Все уравняли, продолжаем к следующей улице
            }
        }

        // Обычный showdown после ривера
        handleShowdown(hand)

        // Проверка: игра окончена, если остался только один игрок с фишками
        if (isGameEnded()) {
            view.printGameEnded()
            return
        }
    }

    private fun isGameEnded(): Boolean {
        val playersWithChips = game?.players?.filter { it.getStack() > 0 } ?: emptyList()
        return playersWithChips.size <= 1
    }

    private fun placeBlind(player: Player, amount: Int, hand: Hand) {
        val actualBet = minOf(amount, player.getStack())
        player.bet(actualBet)
        hand.getPot().add(player, actualBet)
        val kind = if (amount == smallBlind) "small blind" else "big blind"
        logger.info("${player.name} posts $kind: $actualBet")
    }

    private fun findFirstActive(hand: Hand, startIndex: Int): Int {
        val players = hand.getPlayers()
        var index = startIndex
        var iterations = 0
        while (iterations < players.size) {
            val p = players[index]
            // Пропускаем фолднувших, OUT и игроков с 0 фишек
            if (p.getStatus() != PlayerStatus.FOLDED &&
                p.getStatus() != PlayerStatus.OUT &&
                p.getStatus() != PlayerStatus.ALL_IN &&
                p.getStack() > 0
            ) {
                return index
            }
            index = (index + 1) % players.size
            iterations++
        }
        return -1
    }

    private fun runBettingRound(hand: Hand, startingPlayerIndex: Int, isPreFlop: Boolean): Boolean {
        val players = hand.getPlayers()
        hand.setCurrentPlayerIndex(startingPlayerIndex)

        // Проверка: если только один игрок имеет фишки, раунд завершается
        val playersWithChips = players.filter {
            it.getStatus() != PlayerStatus.FOLDED &&
                it.getStatus() != PlayerStatus.OUT &&
                it.getStack() > 0
        }
        if (playersWithChips.size <= 1) {
            view.printOnlyOnePlayerLeft()
            return true
        }

        view.printBettingRoundStart(players[startingPlayerIndex])

        // На префлопе BB уже поставил big blind
        val bbIndex = (hand.getDealerIndex() + 2) % players.size
        var lastRaiseAmount = bigBlind
        val playersWhoActed = mutableSetOf<Int>()

        var roundComplete = false
        var firstPass = true

        while (!roundComplete) {
            val currentIndex = hand.getCurrentPlayerIndex()
            val currentPlayer = players[currentIndex]

            // Пропускаем фолднувших, OUT и игроков с 0 фишек (если не all-in)
            if (currentPlayer.getStatus() == PlayerStatus.FOLDED ||
                currentPlayer.getStatus() == PlayerStatus.OUT ||
                (currentPlayer.getStack() == 0 && currentPlayer.getStatus() != PlayerStatus.ALL_IN)
            ) {
                hand.setCurrentPlayerIndex((currentIndex + 1) % players.size)
                if (findFirstActive(hand, hand.getCurrentPlayerIndex()) == -1) {
                    break
                }
                continue
            }

            // Проверка: сколько игроков осталось в игре (не фолднули, не OUT и есть фишки ИЛИ all-in)
            val playersInHand = players.filter {
                it.getStatus() != PlayerStatus.FOLDED &&
                    it.getStatus() != PlayerStatus.OUT &&
                    (it.getStack() > 0 || it.getStatus() == PlayerStatus.ALL_IN)
            }

            // Если остался один игрок в игре - все остальные фолднули или остались без фишек
            if (playersInHand.size <= 1) {
                view.printAllPlayersFolded()
                hand.endHandEarly(playersInHand.firstOrNull())
                return true
            }

            val isDealer = hand.getDealerIndex() == currentIndex
            view.printPlayerTurn(currentPlayer, isDealer)
            view.printAvailableActions(hand, currentPlayer)

            val playerContribution = hand.getPot().getContributions()[currentPlayer] ?: 0
            val currentBet = hand.getCurrentBet()
            val toCall = currentBet - playerContribution

            if (toCall > 0) {
                view.printToCall(toCall)
            }

            val action = view.readAction(hand, currentPlayer)
            var amount = 0

            when (action) {
                Action.BET, Action.RAISE -> {
                    val minRaise = maxOf(lastRaiseAmount, bigBlind)
                    val minBetAmount = toCall + minRaise
                    amount = view.readBetAmount("Enter bet amount", minBetAmount)
                    if (amount < minBetAmount) amount = minBetAmount
                }
                Action.ALL_IN -> {
                    amount = currentPlayer.getStack()
                }
                else -> {}
            }

            val actualAction = if (action == Action.CALL && playerContribution >= currentBet) Action.CHECK else action

            val success = actionProcessor.execute(hand, currentPlayer, actualAction, amount)

            if (!success) {
                view.printInvalidAction()
                continue
            }

            // Обновляем currentBet
            var maxBet = 0
            for (p in players) {
                val contrib = hand.getPot().getContributions()[p] ?: 0
                if (contrib > maxBet) maxBet = contrib
            }
            hand.setCurrentBet(maxBet)

            val newCurrentBet = hand.getCurrentBet()
            val newContribution = hand.getPot().getContributions()[currentPlayer] ?: 0

            // Если был рейз
            if (newContribution > currentBet) {
                lastRaiseAmount = newContribution - currentBet
                playersWhoActed.clear()
            }

            playersWhoActed.add(currentIndex)
            if (newContribution == newCurrentBet || currentPlayer.getStatus() == PlayerStatus.ALL_IN) {
                // Игрок уравнял или all-in
            }

            // Проверка завершения раунда
            val activePlayers = players.filter {
                it.getStatus() != PlayerStatus.FOLDED && it.getStatus() != PlayerStatus.OUT
            }
            val allMatched = activePlayers.all {
                it.getStatus() == PlayerStatus.ALL_IN ||
                    (hand.getPot().getContributions()[it] ?: 0) == newCurrentBet
            }

            // Раунд завершён если:
            // 1. Все активные игроки уравняли ставку (или all-in)
            // 2. Все активные игроки сделали хотя бы один ход (кроме первого круга на префлопе где BB уже поставил)
            val everyoneActed = if (isPreFlop && firstPass) {
                // На префлопе первый круг: все кроме BB должны сыграть, затем BB
                val allButLastActed = activePlayers.all { p ->
                    val pIdx = players.indexOf(p)
                    if (pIdx == bbIndex) true else playersWhoActed.contains(pIdx)
                }
                allButLastActed && playersWhoActed.contains(bbIndex)
            } else {
                activePlayers.all { playersWhoActed.contains(players.indexOf(it)) }
            }

            if (allMatched && everyoneActed && !firstPass) {
                roundComplete = true
            }

            firstPass = false

            if (!roundComplete) {
                hand.setCurrentPlayerIndex((currentIndex + 1) % players.size)
                if (findFirstActive(hand, hand.getCurrentPlayerIndex()) == -1) {
                    break
                }
            }
        }

        view.printBettingRoundComplete()
        return false
    }

    private fun handleShowdown(hand: Hand) {
        val players = hand.getPlayers()
        val community = hand.getCommunity()

        // Если рука завершена досрочно (все фолднули или all-in), просто показываем победителя
        if (hand.isEndedEarly()) {
            val winners = players.filter { it.getStatus() != PlayerStatus.FOLDED }
            if (winners.isNotEmpty()) {
                view.printWinners(winners)
            }
            view.printAllPlayersStatus(players)
            return
        }

        // Если нет community cards (все фолдят на префлопе), просто показываем победителя
        if (community.isEmpty()) {
            val winners = players.filter { it.getStatus() != PlayerStatus.FOLDED }
            if (winners.isNotEmpty()) {
                view.printWinners(winners)
            }
            view.printAllPlayersStatus(players)
            return
        }

        view.printShowdownStart()
        view.printCommunityCards(community)

        for (player in players) {
            if (player.getStatus() != PlayerStatus.FOLDED) {
                val rank = HandEvaluator().bestHand(player, community)
                view.printHandRank(player, rank)
            }
        }

        val winners = hand.showdown()
        view.printWinners(winners)

        view.printAllPlayersStatus(players)
    }

    fun getGame(): Game? = game
    fun setGame(g: Game) {
        game = g
    }

    fun playerAction(player: Player, action: Action, amount: Int) {
        val hand = game?.getCurrentHand() ?: return
        if (actionProcessor.validate(player, action, amount, hand)) {
            actionProcessor.execute(hand, player, action, amount)
        } else {
            logger.error("Invalid action: $action for player ${player.name}")
        }
    }

    fun saveGame() {
        game?.let { g ->
            storage.saveGame(g)
            view.printGameSaved()
        }
    }

    fun loadGame(id: UUID) {
        val loaded = storage.loadGame(id)
        if (loaded != null) {
            game = loaded
            view.printGameInfo(loaded)
        } else {
            view.printError("Game not found")
        }
    }
}
