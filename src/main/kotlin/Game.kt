import java.util.UUID

class Game(
    private val id: UUID = UUID.randomUUID(),
    private val players: MutableList<Player> = mutableListOf(),
    private var currentHand: Hand? = null,
    private var currentBet: Int = 0,
) {
    fun getId(): UUID = id

    fun getPlayers(): List<Player> = players

    fun getCurrentHand(): Hand? = currentHand

    fun getCurrentBet(): Int = currentBet

    fun setCurrentBet(bet: Int) {
        currentBet = bet
    }

    fun addPlayer(p: Player) {
        if (players.none { it.getId() == p.getId() }) {
            players.add(p)
        }
    }

    fun removePlayer(p: Player) {
        players.removeIf { it.getId() == p.getId() }
    }

    fun startHand(): Hand {
        currentHand = Hand(UUID.randomUUID(), players.toMutableList())
        currentHand?.dealHole()
        currentBet = 0
        return currentHand!!
    }

    fun getCurrentHandOrThrow(): Hand {
        return currentHand ?: throw IllegalStateException("No hand has been started")
    }

    fun isHandInProgress(): Boolean = currentHand != null

    fun clearHand() {
        currentHand = null
    }
}
