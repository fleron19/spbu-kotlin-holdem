import java.util.UUID

class Hand(
    private val id: UUID,
    var phase: GamePhase = GamePhase.WAITING,
    val deck: Deck = Deck(),
    private val players: MutableList<Player> = mutableListOf(),
    private val pot: Pot = Pot(),
    val community: List<Card> = listOf(),
    private val actions: MutableList<Action> = mutableListOf()
) {
    fun dealHole() {
        TODO()
    }

    fun dealCommunity(n: Int) {
        TODO()
    }

    fun showdown(): List<Player> {
        TODO()
    }
}
