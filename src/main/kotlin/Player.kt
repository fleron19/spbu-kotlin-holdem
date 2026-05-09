import java.util.UUID

class Player(
    private val id: UUID,
    val name: String,
    private var stack: Int,
    private val hole: List<Card> = listOf(),
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
        TODO()
    }

    fun bet(amount: Int): Int {
        TODO()
    }
}
