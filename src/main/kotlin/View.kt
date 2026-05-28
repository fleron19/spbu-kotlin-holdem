interface View {
    // Game setup
    fun printWelcome()
    fun printGameInfo(game: Game)
    fun printBlindsConfigured(smallBlind: Int, bigBlind: Int)
    fun printPlayerSetupHeader(playerNumber: Int)
    fun printError(msg: String)
    fun printMessage(msg: String)

    // Player management
    fun printPlayerOrder(players: List<Player>, dealerIndex: Int, sbIndex: Int, bbIndex: Int)
    fun printAllPlayersStatus(players: List<Player>)

    // Hand info
    fun printHandInfo(hand: Hand)
    fun dealFlop()
    fun dealTurn()
    fun dealRiver()

    // Betting round
    fun printBettingRoundStart(startingPlayer: Player)
    fun printBettingRoundComplete()
    fun printPlayerTurn(player: Player, isDealer: Boolean)
    fun printAvailableActions(hand: Hand, player: Player)
    fun printToCall(amount: Int)
    fun printInvalidAction()
    fun printOnlyOnePlayerLeft()
    fun printAllPlayersFolded()

    // Actions input
    fun readAction(hand: Hand, player: Player): Action
    fun readBetAmount(prompt: String, minRaise: Int): Int
    fun readPositiveInt(prompt: String, default: Int): Int
    fun readBlindAmount(blindType: String, default: Int): Int
    fun readPlayerName(): String
    fun askAddPlayer(): Boolean
    fun askNewHand(): Boolean

    // Showdown
    fun printShowdownStart()
    fun printCommunityCards(cards: List<Card>)
    fun printHandRank(player: Player, rank: HandRank)
    fun printWinners(winners: List<Player>)

    // Game state
    fun printGameSaved()
    fun printGameLoaded()
    fun printGameEnded()
    fun printGameAlreadyEnded()

    // Helper (optional, for CLI)
    fun printPlayerStatus(player: Player)
}
