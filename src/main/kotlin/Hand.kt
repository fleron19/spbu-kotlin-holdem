import java.util.UUID

class Hand(
    private val id: UUID,
    var phase: GamePhase = GamePhase.WAITING,
    val deck: Deck = Deck(),
    private val players: MutableList<Player> = mutableListOf(),
    private val pot: Pot = Pot(),
    private val community: MutableList<Card> = mutableListOf(),
    private val actions: MutableList<Action> = mutableListOf(),
) {
    fun getPlayers(): List<Player> = players
    fun getCommunity(): List<Card> = community
    fun addCommunityCard(card: Card) { community.add(card) }
    fun getPot(): Pot = pot

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun dealHole() {
        deck.reset()
        deck.shuffle()
        for (player in players) {
            repeat(2) {
                player.receiveCard(deck.draw())
            }
        }
    }

    fun dealCommunity(n: Int) {
        repeat(n) {
            community.add(deck.draw())
        }
    }

    fun showdown(): List<Player> {
        val evaluator = HandEvaluator()
        val rankings = mutableMapOf<Player, HandRank>()

        for (player in players) {
            if (player.getStatus() != PlayerStatus.FOLDED) {
                rankings[player] = evaluator.bestHand(player, community)
            }
        }

        if (rankings.isEmpty()) return emptyList()

        val bestRank = rankings.values.maxByOrNull { it }!!
        return rankings.filter { it.value == bestRank }.keys.toList()
    }
}