import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PotTest {
    @Test
    fun `add should add amount to pot`() {
        val pot = Pot()
        val player = Player(UUID.randomUUID(), "Test", 1000)

        pot.add(player, 100)

        assertEquals(100, pot.getTotal())
        assertEquals(100, pot.getContributions()[player])
    }

    @Test
    fun `add should add requested amount (caller must limit by stack)`() {
        val pot = Pot()
        val player = Player(UUID.randomUUID(), "Test", 50)

        // caller должен сам ограничить сумму стеком игрока
        val actualBet = minOf(100, player.getStack())
        pot.add(player, actualBet)

        assertEquals(50, pot.getTotal())
    }

    @Test
    fun `add multiple players`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)

        pot.add(player1, 100)
        pot.add(player2, 150)

        assertEquals(250, pot.getTotal())
        assertEquals(100, pot.getContributions()[player1])
        assertEquals(150, pot.getContributions()[player2])
    }

    @Test
    fun `distributeWinners should split pot equally`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)

        pot.add(player1, 100)
        pot.add(player2, 100)

        val winners = listOf(player1, player2)
        val distribution = pot.distributeWinners(winners)

        assertEquals(100, distribution[player1])
        assertEquals(100, distribution[player2])
    }

    @Test
    fun `reset should clear pot`() {
        val pot = Pot()
        val player = Player(UUID.randomUUID(), "Test", 1000)

        pot.add(player, 100)
        pot.reset()

        assertEquals(0, pot.getTotal())
        assertTrue(pot.getContributions().isEmpty())
    }
}
