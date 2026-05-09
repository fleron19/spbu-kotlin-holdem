import java.util.UUID

class Player(
    private val id: UUID,
    val name: String,
    private var stack: Int,
    private val hole: MutableList<Card> = mutableListOf(),
    private var status: PlayerStatus = PlayerStatus.ACTIVE,
) {
    fun getId(): UUID = id
    fun getStack(): Int = stack
    fun getStatus(): PlayerStatus = status
    fun getHole(): List<Card> = hole

    fun setStack(value: Int) {
        stack = value
    }

    fun setStatus(value: PlayerStatus) {
        status = value
    }

    fun receiveCard(c: Card) {
        hole.add(c)
    }

    fun bet(amount: Int): Int {
        val actualBet = minOf(amount, stack)
        stack -= actualBet
        return actualBet
    }
}
