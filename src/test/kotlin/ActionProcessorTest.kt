import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ActionProcessorTest {
    private val processor = ActionProcessor()

    @Test
    fun `FOLD should be valid for active player`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        assertTrue(processor.validate(player, Action.FOLD, 0, hand))
    }

    @Test
    fun `FOLD should be invalid for folded player`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        player.setStatus(PlayerStatus.FOLDED)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)

        assertFalse(processor.validate(player, Action.FOLD, 0, hand))
    }

    @Test
    fun `CHECK should be valid when player has matched bet`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()
        hand.setCurrentBet(0)

        assertTrue(processor.validate(player, Action.CHECK, 0, hand))
    }

    @Test
    fun `BET should be valid with positive amount`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        assertTrue(processor.validate(player, Action.BET, 100, hand))
    }

    @Test
    fun `BET should be invalid with zero amount`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        assertFalse(processor.validate(player, Action.BET, 0, hand))
    }

    @Test
    fun `BET should be invalid exceeding player stack`() {
        val player = Player(UUID.randomUUID(), "Test", 100)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        assertFalse(processor.validate(player, Action.BET, 200, hand))
    }

    @Test
    fun `ALL_IN should be valid when player has chips`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        assertTrue(processor.validate(player, Action.ALL_IN, 0, hand))
    }

    @Test
    fun `execute FOLD should set player to FOLDED`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        processor.execute(hand, player, Action.FOLD, 0)

        assertEquals(PlayerStatus.FOLDED, player.getStatus())
    }

    @Test
    fun `execute BET should deduct from player stack`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        val initialStack = player.getStack()
        processor.execute(hand, player, Action.BET, 100)

        assertEquals(initialStack - 100, player.getStack())
        assertEquals(100, hand.getPot().getTotal())
    }

    @Test
    fun `execute ALL_IN should set player to ALL_IN`() {
        val player = Player(UUID.randomUUID(), "Test", 1000)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        processor.execute(hand, player, Action.ALL_IN, 0)

        assertEquals(PlayerStatus.ALL_IN, player.getStatus())
    }

    @Test
    fun `execute ALL_IN should bet all remaining chips`() {
        val player = Player(UUID.randomUUID(), "Test", 250)
        val players = mutableListOf(player)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        processor.execute(hand, player, Action.ALL_IN, 0)

        assertEquals(0, player.getStack())
        assertEquals(250, hand.getPot().getTotal())
        assertEquals(250, hand.getPot().getContributions()[player])
        assertEquals(PlayerStatus.ALL_IN, player.getStatus())
    }

    @Test
    fun `ALL_IN should update currentBet`() {
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 500)
        val players = mutableListOf(player1, player2)
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        // Player1 ставит 100
        processor.execute(hand, player1, Action.BET, 100)
        assertEquals(100, hand.getCurrentBet())

        // Player2 идет all-in со 150 (меньше чем currentBet)
        player2.setStack(150)
        processor.execute(hand, player2, Action.ALL_IN, 0)
        
        // currentBet должен быть обновлен до 150 (вклад player2)
        assertEquals(150, hand.getCurrentBet())
        assertEquals(PlayerStatus.ALL_IN, player2.getStatus())
    }
}
