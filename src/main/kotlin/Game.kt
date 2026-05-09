import java.util.UUID

class Game(
    private val id: UUID,
    private val players: MutableList<Player> = mutableListOf(),
    private var currentHand: Hand? = null
) {
    fun addPlayer(p: Player) {
        TODO()
    }

    fun startHand(): Hand {
        TODO()
    }
}
