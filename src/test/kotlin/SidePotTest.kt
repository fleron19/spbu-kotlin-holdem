import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class SidePotTest {

    @Test
    fun `side pot created when one player all-in with less than others`() {
        // Игрок 1: all-in 100
        // Игрок 2: ставит 200
        // Игрок 3: ставит 200
        // Main pot: 100 * 3 = 300
        // Side pot: 100 * 2 = 200 (только игроки 2 и 3)

        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 100)
        val player2 = Player(UUID.randomUUID(), "Regular1", 1000)
        val player3 = Player(UUID.randomUUID(), "Regular2", 1000)
        val players = listOf(player1, player2, player3)

        pot.add(player1, 100) // all-in
        pot.add(player2, 200)
        pot.add(player3, 200)

        pot.buildSidePots(players)

        assertEquals(2, pot.getSidePots().size, "Должно быть 2 side pot'а")

        // Main pot: 100 * 3 = 300
        assertEquals(300, pot.getSidePots()[0].amount)
        assertEquals(3, pot.getSidePots()[0].eligiblePlayers.size)

        // Side pot: 100 * 2 = 200
        assertEquals(200, pot.getSidePots()[1].amount)
        assertEquals(2, pot.getSidePots()[1].eligiblePlayers.size)
        assertTrue(!pot.getSidePots()[1].eligiblePlayers.contains(player1))
    }

    @Test
    fun `all-in player only eligible for main pot`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 50)
        val player2 = Player(UUID.randomUUID(), "Regular", 1000)
        val players = listOf(player1, player2)

        pot.add(player1, 50) // all-in
        pot.add(player2, 500) // много ставит

        pot.buildSidePots(players)

        assertEquals(2, pot.getSidePots().size)

        // Main pot: 50 * 2 = 100
        assertEquals(100, pot.getSidePots()[0].amount)
        assertTrue(pot.getSidePots()[0].eligiblePlayers.contains(player1))
        assertTrue(pot.getSidePots()[0].eligiblePlayers.contains(player2))

        // Side pot: 450 * 1 = 450 (только player2)
        assertEquals(450, pot.getSidePots()[1].amount)
        assertTrue(!pot.getSidePots()[1].eligiblePlayers.contains(player1))
        assertTrue(pot.getSidePots()[1].eligiblePlayers.contains(player2))
    }

    @Test
    fun `distributeWinners gives side pot to correct winner`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 100)
        val player2 = Player(UUID.randomUUID(), "Winner", 1000)
        val player3 = Player(UUID.randomUUID(), "Loser", 1000)
        val players = listOf(player1, player2, player3)

        pot.add(player1, 100)
        pot.add(player2, 200)
        pot.add(player3, 200)

        pot.buildSidePots(players)

        // Player2 выигрывает (лучшая рука)
        val winners = listOf(player2)
        val distribution = pot.distributeWinners(winners)

        assertEquals(500, distribution[player2]) // 300 main + 200 side
    }

    @Test
    fun `distributeWinners splits side pot between multiple winners`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 100)
        val player2 = Player(UUID.randomUUID(), "Winner1", 1000)
        val player3 = Player(UUID.randomUUID(), "Winner2", 1000)
        val players = listOf(player1, player2, player3)

        pot.add(player1, 100)
        pot.add(player2, 200)
        pot.add(player3, 200)

        pot.buildSidePots(players)

        // Оба player2 и player3 выигрывают (нит)
        val winners = listOf(player2, player3)
        val distribution = pot.distributeWinners(winners)

        // Каждый получает: 150 main + 100 side = 250
        assertEquals(250, distribution[player2])
        assertEquals(250, distribution[player3])
        // player1 получил 0 (проиграл)
    }

    @Test
    fun `folded player gets nothing`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Winner", 1000)
        val player2 = Player(UUID.randomUUID(), "Felder", 1000)
        val players = listOf(player1, player2)

        pot.add(player1, 100)
        pot.add(player2, 100)

        pot.buildSidePots(players)

        player2.setStatus(PlayerStatus.FOLDED)
        val winners = listOf(player1)
        val distribution = pot.distributeWinners(winners)

        assertEquals(200, distribution[player1])
        assertTrue(!distribution.containsKey(player2))
    }

    @Test
    fun `three players with different stacks creates two side pots`() {
        // Player1: all-in 50
        // Player2: all-in 150
        // Player3: calls 500
        // Main pot: 50 * 3 = 150 (все)
        // Side pot 1: 100 * 2 = 200 (player2 + player3)
        // Side pot 2: 350 * 1 = 350 (только player3)

        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn50", 50)
        val player2 = Player(UUID.randomUUID(), "AllIn150", 150)
        val player3 = Player(UUID.randomUUID(), "Deep", 1000)
        val players = listOf(player1, player2, player3)

        pot.add(player1, 50)
        pot.add(player2, 150)
        pot.add(player3, 500)

        pot.buildSidePots(players)

        assertEquals(3, pot.getSidePots().size)

        assertEquals(150, pot.getSidePots()[0].amount) // main: 50 * 3
        assertEquals(3, pot.getSidePots()[0].eligiblePlayers.size)

        assertEquals(200, pot.getSidePots()[1].amount) // side1: 100 * 2
        assertEquals(2, pot.getSidePots()[1].eligiblePlayers.size)
        assertTrue(!pot.getSidePots()[1].eligiblePlayers.contains(player1))

        assertEquals(350, pot.getSidePots()[2].amount) // side2: 350 * 1
        assertEquals(1, pot.getSidePots()[2].eligiblePlayers.size)
        assertTrue(pot.getSidePots()[2].eligiblePlayers.contains(player3))
    }

    @Test
    fun `all-in with equal stacks no side pot`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Player1", 500)
        val player2 = Player(UUID.randomUUID(), "Player2", 500)
        val players = listOf(player1, player2)

        pot.add(player1, 500)
        pot.add(player2, 500)

        pot.buildSidePots(players)

        assertEquals(1, pot.getSidePots().size)
        assertEquals(1000, pot.getSidePots()[0].amount)

        val winners = listOf(player1)
        val distribution = pot.distributeWinners(winners)
        assertEquals(1000, distribution[player1])
    }

    @Test
    fun `caller can only win main pot against all-in`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 100)
        val player2 = Player(UUID.randomUUID(), "Caller", 1000)
        val players = listOf(player1, player2)

        pot.add(player1, 100)
        pot.add(player2, 100) // caller только уравнял

        pot.buildSidePots(players)

        assertEquals(1, pot.getSidePots().size)
        assertEquals(200, pot.getSidePots()[0].amount)

        // Player2 выигрывает
        val winners = listOf(player2)
        val distribution = pot.distributeWinners(winners)
        assertEquals(200, distribution[player2])
    }
}
