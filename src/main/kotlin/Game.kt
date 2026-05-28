import java.util.UUID

class Game(
    private val id: UUID = UUID.randomUUID(),
    private val _players: MutableList<Player> = mutableListOf(),
    private var currentHand: Hand? = null,
    private var currentBet: Int = 0,
    private var dealer: Int = 0,
) {

    val players: List<Player> get() = _players

    fun getId(): UUID = id

    fun getCurrentHand(): Hand? = currentHand

    fun getCurrentBet(): Int = currentBet

    fun setCurrentBet(bet: Int) {
        currentBet = bet
    }

    fun addPlayer(p: Player) {
        if (players.none { it.getId() == p.getId() }) {
            _players.add(p)
        }
    }

    fun removePlayer(p: Player) {
        _players.removeIf { it.getId() == p.getId() }
    }

    fun startHand(): Hand {
        dealer = (dealer + 1) % players.size
        val hand = Hand(UUID.randomUUID(), players, dealerIndex = dealer)
        hand.dealHole()
        currentHand = hand
        currentBet = 0
        return hand
    }

    fun getCurrentHandOrThrow(): Hand {
        return currentHand ?: throw IllegalStateException("No hand has been started")
    }

    fun isHandInProgress(): Boolean = currentHand != null

    fun clearHand() {
        currentHand = null
    }
}
