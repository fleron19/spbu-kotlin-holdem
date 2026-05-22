class View(private val logger: Logger) {
    fun printWelcome() {
        println("========================================")
        println("    Texas Hold'em Poker - Console Game")
        println("========================================")
    }

    fun printGameInfo(game: Game) {
        println("\n--- Game ${game.getId()} ---")
        println("Players (${game.getPlayers().size}):")
        for (player in game.getPlayers()) {
            println("  - ${player.name}: ${player.getStack()} chips [${player.getStatus()}]")
        }
    }

    fun printHandInfo(hand: Hand) {
        println("\n========== ${hand.phase} ==========")
        println("Hand ID: ${hand.getId()}")
        println("Dealer (Button): Player at index ${hand.getDealerIndex()}")
        val community = hand.getCommunity()
        if (community.isNotEmpty()) {
            println("Community cards: ${community.joinToString(" ")}")
        } else {
            println("Community cards: (none)")
        }
        println("Current bet: ${hand.getCurrentBet()}")
        println("Pot: ${hand.getPot().getTotal()}")
        println("===================================")
    }

    fun printPlayerHand(player: Player, isDealer: Boolean = false) {
        val dealerTag = if (isDealer) " [DEALER]" else ""
        println("Your cards:$dealerTag ${player.getHole().joinToString(" ")}")
        println("Your stack: ${player.getStack()}")
    }

    fun printAvailableActions(hand: Hand, player: Player) {
        val playerContribution = hand.getPot().getContributions()[player] ?: 0
        val currentBet = hand.getCurrentBet()
        val toCall = currentBet - playerContribution
        val canCheck = toCall <= 0

        if (player.getStatus() != PlayerStatus.FOLDED) {
            println("1) Fold")
            if (canCheck) {
                println("2) Check")
                println("3) Bet")
            } else {
                println("2) Call $toCall")
                println("3) Raise (min ${toCall * 2})")
            }
            println("4) All-in")
        } else {
            println("You have folded this hand.")
        }
    }

    fun printPlayerOrder(players: List<Player>, dealerIndex: Int, sbIndex: Int, bbIndex: Int) {
        println("\n--- Player Order (Clockwise) ---")
        val order = mutableListOf<String>()
        for ((i, player) in players.withIndex()) {
            val tag = when (i) {
                dealerIndex -> " [DEALER]"
                sbIndex -> " [SB]"
                bbIndex -> " [BB]"
                else -> ""
            }
            order.add("${player.name}$tag")
        }
        println(order.joinToString(" -> "))
        println("--------------------------------")
    }

    fun readAction(hand: Hand, player: Player): Action {
        val playerContribution = hand.getPot().getContributions()[player] ?: 0
        val canCheck = playerContribution >= hand.getCurrentBet()

        print("Choose action (1-4): ")
        val input = readLine()?.trim()
        return when (input) {
            "1" -> Action.FOLD
            "2" -> if (canCheck) Action.CHECK else Action.CALL
            "3" -> if (canCheck) Action.BET else Action.RAISE
            "4" -> Action.ALL_IN
            else -> Action.FOLD
        }
    }

    fun readBetAmount(minRaise: Int = 0): Int {
        print("Enter bet amount (min $minRaise): ")
        val amount = readLine()?.trim()?.toIntOrNull() ?: 0
        if (minRaise > 0 && amount > 0 && amount < minRaise) {
            println("Minimum raise is $minRaise")
            return readBetAmount(minRaise)
        }
        return amount
    }

    fun printWinner(winners: List<Player>, amountPerWinner: Int) {
        println("\n========================================")
        println("Winners: ${winners.joinToString(", ") { it.name }}")
        println("Winning amount per player: $amountPerWinner")
        println("========================================")
    }

    fun printHandRank(player: Player, rank: HandRank) {
        val rankNames: Map<Combination, String> = mapOf(
            Combination.HIGH_CARD to "High Card",
            Combination.ONE_PAIR to "Pair",
            Combination.TWO_PAIR to "Two Pair",
            Combination.THREE_OF_A_KIND to "Three of a Kind",
            Combination.STRAIGHT to "Straight",
            Combination.FLUSH to "Flush",
            Combination.FULL_HOUSE to "Full House",
            Combination.FOUR_OF_A_KIND to "Four of a Kind",
            Combination.STRAIGHT_FLUSH to "Straight Flush",
            Combination.ROYAL_FLUSH to "Royal Flush",
        )
        val combination = Combination.values().firstOrNull { it.value == rank.category }
        val rankName = combination?.let { rankNames[it] } ?: "Unknown"
        println("${player.name}: ${player.getHole().joinToString(" ")} - $rankName")
    }

    fun printMessage(msg: String) {
        println(msg)
    }

    fun printError(msg: String) {
        println("ERROR: $msg")
    }

    fun printPlayerStatus(player: Player) {
        println("${player.name}: ${player.getStack()} chips, Status: ${player.getStatus()}")
    }

    fun printAllPlayersStatus(players: List<Player>) {
        println("\n--- Player Status ---")
        for (player in players) {
            printPlayerStatus(player)
        }
    }

    fun askNewHand(): Boolean {
        print("Start new hand? (y/n): ")
        return readLine()?.trim()?.lowercase() == "y"
    }

    fun askAddPlayer(): Boolean {
        print("Add another player? (y/n): ")
        return readLine()?.trim()?.lowercase() == "y"
    }

    fun readPlayerName(): String {
        print("Enter player name: ")
        return readLine()?.trim() ?: "Player"
    }

    fun readPlayerStack(): Int {
        print("Enter starting stack: ")
        return readLine()?.trim()?.toIntOrNull() ?: 1000
    }
}
