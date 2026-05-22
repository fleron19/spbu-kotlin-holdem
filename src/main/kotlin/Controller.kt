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

        println("\nConfigure blinds:")
        print("Enter small blind amount: ")
        smallBlind = readLine()?.trim()?.toIntOrNull() ?: 50
        print("Enter big blind amount: ")
        bigBlind = readLine()?.trim()?.toIntOrNull() ?: 100

        game = Game()
        var playerCount = 0

        while (true) {
            playerCount++
            view.printMessage("\nPlayer $playerCount:")
            val name = view.readPlayerName()
            val stack = view.readPlayerStack()
            val player = Player(UUID.randomUUID(), name, stack)
            game?.addPlayer(player)
            logger.info("Added player: ${player.name} with ${player.getStack()} chips")

            if (!view.askAddPlayer()) break
        }

        view.printGameInfo(game!!)
        view.printMessage("\nGame configured! Blinds: $smallBlind/$bigBlind")
    }

    fun startHand() {
        if (game == null || game?.getPlayers()?.isEmpty() == true) {
            view.printError("Game not initialized")
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
        runBettingRound(hand, preFlopStartIndex, isPreFlop = true)

        // Постфлоп: Флоп, Терн, Ривер
        while (hand.phase != GamePhase.SHOWDOWN) {
            when (hand.phase) {
                GamePhase.PREFLOP -> {
                    println("\nDealing Flop...")
                    hand.dealFlop()
                }
                GamePhase.FLOP -> {
                    println("\nDealing Turn...")
                    hand.dealTurn()
                }
                GamePhase.TURN -> {
                    println("\nDealing River...")
                    hand.dealRiver()
                }
                GamePhase.RIVER -> break
                else -> break
            }

            view.printHandInfo(hand)

            // Постфлоп: первый активный слева от дилера
            val startIndexCandidate = (hand.getDealerIndex() + 1) % players.size
            val postFlopStartIndex = findFirstActive(hand, startIndexCandidate)
            if (postFlopStartIndex == -1) break
            runBettingRound(hand, postFlopStartIndex, isPreFlop = false)
        }

        handleShowdown(hand)
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
            if (p.getStatus() != PlayerStatus.FOLDED && p.getStatus() != PlayerStatus.OUT) {
                return index
            }
            index = (index + 1) % players.size
            iterations++
        }
        return -1
    }

    private fun runBettingRound(hand: Hand, startingPlayerIndex: Int, isPreFlop: Boolean) {
        val players = hand.getPlayers()
        hand.setCurrentPlayerIndex(startingPlayerIndex)

        view.printMessage("\n--- Betting round (starts with " + players[startingPlayerIndex].name + ") ---")

        // На префлопе BB уже поставил big blind, поэтому он считается агрессором
        val bbIndex = (hand.getDealerIndex() + 2) % players.size
        var lastAggressorIndex = if (isPreFlop) bbIndex else -1
        var lastRaiseAmount = bigBlind // размер последнего рейза (для минимального рейза)
        val playersWhoActedSinceLastRaise = mutableSetOf<Int>()

        var roundComplete = false

        while (!roundComplete) {
            val currentIndex = hand.getCurrentPlayerIndex()
            val currentPlayer = players[currentIndex]

            if (currentPlayer.getStatus() == PlayerStatus.FOLDED || currentPlayer.getStatus() == PlayerStatus.OUT) {
                hand.setCurrentPlayerIndex((currentIndex + 1) % players.size)
                if (findFirstActive(hand, hand.getCurrentPlayerIndex()) == -1) {
                    roundComplete = true
                    break
                }
                continue
            }

            view.printMessage("\n" + currentPlayer.name + "'s turn")
            val isDealer = hand.getDealerIndex() == currentIndex
            view.printPlayerHand(currentPlayer, isDealer)
            view.printAvailableActions(hand, currentPlayer)

            val playerContribution = hand.getPot().getContributions()[currentPlayer] ?: 0
            val currentBet = hand.getCurrentBet()
            val toCall = currentBet - playerContribution

            if (toCall > 0) {
                view.printMessage("To call: $toCall")
            }

            val action = view.readAction(hand, currentPlayer)
            var amount = 0

            when (action) {
                Action.BET, Action.RAISE -> {
                    // Минимальный рейз: toCall + max(lastRaiseAmount, bigBlind)
                    val minRaise = maxOf(lastRaiseAmount, bigBlind)
                    val minBetAmount = toCall + minRaise
                    amount = view.readBetAmount(minBetAmount)
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
                view.printError("Invalid action, try again")
                continue
            }

            // Обновляем текущую ставку
            updateCurrentBet(hand)
            val newCurrentBet = hand.getCurrentBet()
            val newContribution = hand.getPot().getContributions()[currentPlayer] ?: 0

            // Проверяем, был ли это рейз (игрок увеличил ставку выше текущей БЫВШЕЙ ставки)
            if (newContribution > currentBet) {
                // Новый рейз
                val raiseAmount = newContribution - currentBet
                lastAggressorIndex = currentIndex
                lastRaiseAmount = raiseAmount
                playersWhoActedSinceLastRaise.clear()
            }

            playersWhoActedSinceLastRaise.add(currentIndex)

            // Проверяем завершение раунда
            roundComplete = checkRoundComplete(
                hand,
                startingPlayerIndex,
                lastAggressorIndex,
                playersWhoActedSinceLastRaise,
                isPreFlop,
            )

            if (roundComplete) break

            // Переходим к следующему игроку
            hand.setCurrentPlayerIndex((currentIndex + 1) % players.size)

            if (findFirstActive(hand, hand.getCurrentPlayerIndex()) == -1) {
                roundComplete = true
                break
            }
        }

        view.printMessage("\n--- Betting round complete ---")
    }

    private fun checkRoundComplete(
        hand: Hand,
        startingPlayerIndex: Int,
        lastAggressorIndex: Int,
        playersWhoActedSinceLastRaise: Set<Int>,
        isPreFlop: Boolean,
    ): Boolean {
        val players = hand.getPlayers()
        val currentBet = hand.getCurrentBet()
        val currentIndex = hand.getCurrentPlayerIndex()

        // Считаем активных игроков (не folded, не out, не all-in)
        val activeIndexes = mutableListOf<Int>()
        var unmatchedCount = 0

        for ((i, player) in players.withIndex()) {
            if (player.getStatus() == PlayerStatus.FOLDED || player.getStatus() == PlayerStatus.OUT) continue
            if (player.getStatus() == PlayerStatus.ALL_IN) continue

            activeIndexes.add(i)
            val contrib = hand.getPot().getContributions()[player] ?: 0
            if (contrib < currentBet) unmatchedCount++
        }

        // Если есть игроки, не уравнявшие ставку — раунд не завершён
        if (unmatchedCount > 0) return false

        // Все уравняли. Проверяем, все ли активные сыграли
        if (activeIndexes.isEmpty()) return true
        if (!activeIndexes.all { playersWhoActedSinceLastRaise.contains(it) }) return false

        // Определяем, какой игрок должен сделать последний ход в раунде
        val endPlayerIndex = if (isPreFlop) {
            // На префлопе раунд завершается после хода BB
            (hand.getDealerIndex() + 2) % players.size
        } else {
            // На постфлопе: если никто не повышал — начинали со startingPlayerIndex,
            // значит раунд завершается после хода startingPlayerIndex
            // Если был рейз — завершается после хода lastAggressorIndex
            if (lastAggressorIndex == -1) startingPlayerIndex else lastAggressorIndex
        }

        // Раунд завершается, когда ход перешёл к игроку ПОСЛЕ endPlayerIndex
        val nextPlayerAfterEnd = (endPlayerIndex + 1) % players.size
        return currentIndex == nextPlayerAfterEnd
    }

    private fun updateCurrentBet(hand: Hand) {
        val players = hand.getPlayers()
        var maxBet = 0
        for (player in players) {
            val contribution = hand.getPot().getContributions()[player] ?: 0
            if (contribution > maxBet) {
                maxBet = contribution
            }
        }
        hand.setCurrentBet(maxBet)
    }

    private fun handleShowdown(hand: Hand) {
        view.printMessage("\n=== SHOWDOWN ===")
        val players = hand.getPlayers()
        val community = hand.getCommunity()
        val evaluator = HandEvaluator()

        view.printMessage("\nCommunity cards: " + community.joinToString(" "))

        for (player in players) {
            if (player.getStatus() != PlayerStatus.FOLDED) {
                val rank = evaluator.bestHand(player, community)
                view.printHandRank(player, rank)
            }
        }

        val winners = hand.showdown()
        val totalWin = hand.getPot().getTotal()
        view.printWinner(winners, totalWin / winners.size)

        view.printAllPlayersStatus(players)
    }

    fun playerAction(player: Player, action: Action, amount: Int) {
        val hand = game?.getCurrentHand()
        if (hand == null) {
            view.printError("No hand in progress")
            return
        }

        actionProcessor.execute(hand, player, action, amount)
    }

    fun saveGame() {
        game?.let { g ->
            storage.saveGame(g)
            view.printMessage("Game saved!")
        }
    }

    fun loadGame(id: UUID) {
        val loaded = storage.loadGame(id)
        if (loaded != null) {
            game = loaded
            view.printMessage("Game loaded!")
            view.printGameInfo(game!!)
        } else {
            view.printError("Game not found")
        }
    }

    fun getGame(): Game? = game
}
