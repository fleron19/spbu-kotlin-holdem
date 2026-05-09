import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PlayerTest {

    private fun makePlayer(stack: Int = 1000, hole: MutableList<Card> = mutableListOf()): Player {
        return Player(UUID.randomUUID(), "Test", stack, hole)
    }

    @Test
    fun `receiveCard adds card to hole`() {
        val player = makePlayer()
        val card = Card(Suit.HEARTS, Rank.ACE)
        player.receiveCard(card)
        assertEquals(1, player.getHole().size)
        assertEquals(card, player.getHole()[0])
    }

    @Test
    fun `receiveCard adds multiple cards to hole`() {
        val player = makePlayer()
        player.receiveCard(Card(Suit.HEARTS, Rank.ACE))
        player.receiveCard(Card(Suit.DIAMONDS, Rank.KING))
        assertEquals(2, player.getHole().size)
    }

    @Test
    fun `bet reduces stack by bet amount`() {
        val player = makePlayer(stack = 100)
        val actualBet = player.bet(50)
        assertEquals(50, actualBet)
        assertEquals(50, player.getStack())
    }

    @Test
    fun `bet caps at remaining stack`() {
        val player = makePlayer(stack = 100)
        val actualBet = player.bet(150)
        assertEquals(100, actualBet)
        assertEquals(0, player.getStack())
    }

    @Test
    fun `setStack updates stack`() {
        val player = makePlayer(stack = 100)
        player.setStack(200)
        assertEquals(200, player.getStack())
    }

    @Test
    fun `setStatus updates status`() {
        val player = makePlayer()
        player.setStatus(PlayerStatus.FOLDED)
        assertEquals(PlayerStatus.FOLDED, player.getStatus())
    }

    @Test
    fun `getId returns correct UUID`() {
        val id = UUID.randomUUID()
        val player = Player(id, "Test", 100)
        assertEquals(id, player.getId())
    }

    @Test
    fun `getHole returns empty list initially`() {
        val player = makePlayer()
        assertEquals(0, player.getHole().size)
    }
}