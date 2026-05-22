import java.util.Scanner

class ViewCli(private val logger: Logger) : View {
    private val scanner = Scanner(System.`in`)

    override fun printWelcome() {
        println("========================================")
        println("    Texas Hold'em Poker - Console Game")
        println("========================================")
    }

    override fun printGameInfo(game: Game) {
        println("\n--- Game ${game.getId()} ---")
        println("Players (${game.getPlayers().size}):")
        for (player in game.getPlayers()) {
            println("  - ${player.name}: ${player.getStack()} chips [${player.getStatus()}]")
        }
    }

    override fun printHandInfo(hand: Hand) {
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

    override fun printPlayerOrder(players: List<Player>, dealerIndex: Int, sbIndex: Int, bbIndex: Int) {
        println("\n--- Player Order (Clockwise) ---")
        val order = mutableListOf<String>()
        for ((i, player) in players.withIndex()) {
            val tags = mutableListOf<String>()
            if (i == bbIndex) tags.add("[BB]")
            if (i == dealerIndex) tags.add("[DEALER]")
            if (i == sbIndex) tags.add("[SB]")
            val tag = if (tags.isNotEmpty()) " ${tags.joinToString(" ")}" else ""
            order.add("${player.name}$tag")
        }
        println(order.joinToString(" -> "))
        println("--------------------------------")
    }

    override fun printPlayerSetupHeader(playerNumber: Int) {
        println("\nPlayer $playerNumber:")
    }

    override fun printBlindsConfigured(smallBlind: Int, bigBlind: Int) {
        println("\nGame configured! Blinds: $smallBlind/$bigBlind")
    }

    override fun printOnlyOnePlayerLeft() {
        println("\n--- Only one player left, hand over ---")
    }

    override fun printBettingRoundStart(startingPlayer: Player) {
        println("\n--- Betting round (starts with ${startingPlayer.name}) ---")
    }

    override fun printBettingRoundComplete() {
        println("\n--- Betting round complete ---")
    }

    override fun printPlayerTurn(player: Player, isDealer: Boolean) {
        val dealerTag = if (isDealer) " [DEALER]" else ""
        println("\n${player.name}'s turn$dealerTag")
        println("Your cards:${if (isDealer) " [DEALER]" else ""} ${player.getHole().joinToString(" ")}")
        println("Your stack: ${player.getStack()}")
    }

    override fun printAvailableActions(hand: Hand, player: Player) {
        val playerContribution = hand.getPot().getContributions()[player] ?: 0
        val currentBet = hand.getCurrentBet()
        val toCall = currentBet - playerContribution
        val canCheck = toCall <= 0

        if (player.getStatus() == PlayerStatus.FOLDED) {
            println("You have folded this hand.")
            return
        }

        if (player.getStatus() == PlayerStatus.ALL_IN) {
            println("You are all-in! Waiting for other players...")
            return
        }

        println("1) Fold")
        if (canCheck) {
            println("2) Check")
            println("3) Bet")
        } else {
            println("2) Call $toCall")
            println("3) Raise (min ${toCall * 2})")
        }
        println("4) All-in")
    }

    override fun printToCall(amount: Int) {
        println("To call: $amount")
    }

    override fun printInvalidAction() {
        println("Invalid action, try again")
    }

    override fun printAllPlayersFolded() {
        println("\n--- All players folded, winner decided ---")
    }

    override fun printShowdownStart() {
        println("\n=== SHOWDOWN ===")
    }

    override fun printCommunityCards(cards: List<Card>) {
        println("\nCommunity cards: " + cards.joinToString(" "))
    }

    override fun readAction(hand: Hand, player: Player): Action {
        val playerContribution = hand.getPot().getContributions()[player] ?: 0
        val canCheck = playerContribution >= hand.getCurrentBet()

        print("Choose action (1-4): ")
        val input = scanner.nextLine().trim()
        return when (input) {
            "1" -> Action.FOLD
            "2" -> if (canCheck) Action.CHECK else Action.CALL
            "3" -> if (canCheck) Action.BET else Action.RAISE
            "4" -> Action.ALL_IN
            else -> Action.FOLD
        }
    }

    override fun readBetAmount(prompt: String, minRaise: Int): Int {
        print("$prompt (min $minRaise): ")
        val amount = scanner.nextLine().trim().toIntOrNull() ?: 0
        if (minRaise > 0 && amount > 0 && amount < minRaise) {
            println("Minimum raise is $minRaise")
            return readBetAmount(prompt, minRaise)
        }
        return maxOf(amount, 0)
    }

    override fun readPositiveInt(prompt: String, default: Int): Int {
        print("$prompt (min 1, default $default): ")
        val input = scanner.nextLine().trim()
        if (input.isNullOrEmpty()) return default

        val value = input.toIntOrNull()
        if (value == null || value < 1) {
            println("Please enter a positive integer!")
            return readPositiveInt(prompt, default)
        }
        return value
    }

    override fun readBlindAmount(blindType: String, default: Int): Int {
        print("Enter $blindType amount (default $default): ")
        val input = scanner.nextLine().trim()
        if (input.isNullOrEmpty()) return default

        val value = input.toIntOrNull()
        if (value == null || value < 1) {
            println("Please enter a positive integer!")
            return readBlindAmount(blindType, default)
        }
        return value
    }

    override fun printWinners(winners: List<Player>) {
        println("\n========================================")
        println("Winners: ${winners.joinToString(", ") { it.name }}")
        println("========================================")
    }

    override fun printMessage(msg: String) {
        println(msg)
    }

    override fun printError(msg: String) {
        println("ERROR: $msg")
    }

    override fun printPlayerStatus(player: Player) {
        println("${player.name}: ${player.getStack()} chips, Status: ${player.getStatus()}")
    }

    override fun printAllPlayersStatus(players: List<Player>) {
        println("\n--- Player Status ---")
        for (player in players) {
            printPlayerStatus(player)
        }
    }

    override fun askNewHand(): Boolean {
        while (true) {
            print("Start new hand? (y/n): ")
            val input = scanner.nextLine().trim().lowercase()
            if (input == "y" || input == "yes") return true
            if (input == "n" || input == "no") return false
            println("Please enter 'y' or 'n'!")
        }
    }

    override fun askAddPlayer(): Boolean {
        while (true) {
            print("Add another player? (y/n): ")
            val input = scanner.nextLine().trim().lowercase()
            if (input == "y" || input == "yes") return true
            if (input == "n" || input == "no") return false
            println("Please enter 'y' or 'n'!")
        }
    }

    override fun readPlayerName(): String {
        print("Enter player name: ")
        val name = scanner.nextLine().trim()
        if (name.isNullOrEmpty()) {
            println("Name cannot be empty!")
            return readPlayerName()
        }
        return name
    }

    override fun printGameSaved() {
        println("Game saved!")
    }

    override fun printGameLoaded() {
        println("Game loaded!")
    }

    override fun dealFlop() {
        println("\nDealing Flop...")
    }

    override fun dealTurn() {
        println("\nDealing Turn...")
    }

    override fun dealRiver() {
        println("\nDealing River...")
    }

    override fun printGameEnded() {
        println("\n========================================")
        println("GAME OVER! Only one player has chips left.")
        println("Congratulations to the winner!")
        println("========================================")
    }

    override fun printGameAlreadyEnded() {
        println("\nGame is already over! Only one player has chips left.")
    }

    override fun printHandRank(player: Player, rank: HandRank) {
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
}
