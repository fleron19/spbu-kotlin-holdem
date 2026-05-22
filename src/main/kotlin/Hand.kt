import java.util.UUID

class Hand(
    private val id: UUID,
    private val players: MutableList<Player>,
    private val pot: Pot = Pot(),
    private val deck: Deck = Deck(),
) {
    var phase: GamePhase = GamePhase.WAITING
        private set

    private val community: MutableList<Card> = mutableListOf()
    private var currentBet: Int = 0
    private var dealerIndex: Int = 0
    private var currentPlayerIndex: Int = 0
    private var endedEarly: Boolean = false

    fun getId(): UUID = id
    fun getPlayers(): List<Player> = players
    fun getCommunity(): List<Card> = community.toList()
    fun getPot(): Pot = pot
    fun getCurrentBet(): Int = currentBet
    fun setCurrentBet(bet: Int) {
        currentBet = bet
    }
    fun getDealerIndex(): Int = dealerIndex
    fun getCurrentPlayerIndex(): Int = currentPlayerIndex
    fun setCurrentPlayerIndex(index: Int) {
        currentPlayerIndex = index
    }
    fun isEndedEarly(): Boolean = endedEarly

    fun dealHole() {
        deck.reset()
        deck.shuffle()
        community.clear()
        currentBet = 0
        phase = GamePhase.PREFLOP

        for (player in players) {
            player.setStatus(PlayerStatus.ACTIVE)
            player.clearHole()
        }

        for (player in players) {
            repeat(2) { player.receiveCard(deck.draw()) }
        }
    }

    fun dealFlop() {
        require(phase == GamePhase.PREFLOP) { "Can't deal flop in $phase" }
        deck.draw()
        repeat(3) { community.add(deck.draw()) }
        phase = GamePhase.FLOP
    }

    fun dealTurn() {
        require(phase == GamePhase.FLOP) { "Can't deal turn in $phase" }
        deck.draw()
        community.add(deck.draw())
        phase = GamePhase.TURN
    }

    fun dealRiver() {
        require(phase == GamePhase.TURN) { "Can't deal river in $phase" }
        deck.draw()
        community.add(deck.draw())
        phase = GamePhase.RIVER
    }

    fun addCommunityCard(card: Card) {
        community.add(card)
    }

    fun showdown(): List<Player> {
        phase = GamePhase.SHOWDOWN
        val evaluator = HandEvaluator()
        val rankings = mutableMapOf<Player, HandRank>()

        for (player in players) {
            if (player.getStatus() != PlayerStatus.FOLDED) {
                rankings[player] = evaluator.bestHand(player, community)
            }
        }

        if (rankings.isEmpty()) return emptyList()

        val bestRank = rankings.values.maxByOrNull { it }!!
        val winners = rankings.filter { it.value == bestRank }.keys.toList()

        // Строим side pots перед распределением
        pot.buildSidePots(players)
        
        val winnings = pot.distributeWinners(winners)
        for ((player, amount) in winnings) {
            player.setStack(player.getStack() + amount)
        }

        pot.reset()
        return winners
    }

    fun endHandEarly(winner: Player?) {
        phase = GamePhase.SHOWDOWN
        endedEarly = true
        if (winner != null) {
            // Победитель получает весь банк
            val totalPot = pot.getTotal()
            winner.setStack(winner.getStack() + totalPot)
            pot.reset()
        }
    }

    fun reset() {
        phase = GamePhase.WAITING
        community.clear()
        currentBet = 0
        pot.reset()
        for (player in players) {
            player.clearHole()
            player.setStatus(PlayerStatus.ACTIVE)
        }
    }
}
