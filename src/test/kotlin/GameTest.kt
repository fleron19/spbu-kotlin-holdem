import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class GameTest {
    @Test
    fun `addPlayer should add player to game`() {
        val game = Game()
        val player = Player(UUID.randomUUID(), "Test", 1000)

        game.addPlayer(player)

        assertEquals(1, game.players.size)
        assertTrue(game.players.contains(player))
    }

    @Test
    fun `addPlayer should not add duplicate player`() {
        val game = Game()
        val player = Player(UUID.randomUUID(), "Test", 1000)

        game.addPlayer(player)
        game.addPlayer(player)

        assertEquals(1, game.players.size)
    }

    @Test
    fun `startHand should create new hand and deal cards`() {
        val game = Game()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)

        game.addPlayer(player1)
        game.addPlayer(player2)

        val hand = game.startHand()

        assertNotNull(hand)
        assertEquals(2, hand.getPlayers().size)
        assertEquals(2, player1.getHole().size)
        assertEquals(2, player2.getHole().size)
        assertEquals(GamePhase.PREFLOP, hand.phase)
    }

    @Test
    fun `startHand should deal different cards to players`() {
        val game = Game()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)

        game.addPlayer(player1)
        game.addPlayer(player2)

        val hand = game.startHand()
        val player1Cards = player1.getHole()
        val player2Cards = player2.getHole()

        // Убедимся что карты разные
        for (c1 in player1Cards) {
            for (c2 in player2Cards) {
                assertEquals(false, c1 == c2)
            }
        }
    }

    @Test
    fun `getCurrentHand should return null before start`() {
        val game = Game()
        assertNull(game.getCurrentHand())
    }

    @Test
    fun `isHandInProgress should return correct value`() {
        val game = Game()
        assertFalse(game.isHandInProgress())

        val player = Player(UUID.randomUUID(), "Test", 1000)
        game.addPlayer(player)
        game.startHand()

        assertTrue(game.isHandInProgress())
    }
}
