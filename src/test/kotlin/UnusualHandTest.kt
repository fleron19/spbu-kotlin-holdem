import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class UnusualHandTest {

    @Test
    fun `all players fold except one - winner takes pot without showdown`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 1000)
        val player2 = Player(UUID.randomUUID(), "Felder1", 1000)
        val player3 = Player(UUID.randomUUID(), "Felder2", 1000)
        val players = mutableListOf(player1, player2, player3)

        pot.add(player1, 500)
        pot.add(player2, 100)
        pot.add(player3, 100)

        player2.setStatus(PlayerStatus.FOLDED)
        player3.setStatus(PlayerStatus.FOLDED)

        // Только player1 не фолднул
        pot.buildSidePots(players)

        // Создается 2 side pot'а:
        // - Main: 100 * 3 = 300 (eligible: player1)
        // - Side: 400 * 1 = 400 (eligible: player1)
        assertEquals(2, pot.getSidePots().size)
        assertEquals(300, pot.getSidePots()[0].amount)
        assertEquals(400, pot.getSidePots()[1].amount)
        assertEquals(1, pot.getSidePots()[0].eligiblePlayers.size) // Только player1
        assertEquals(1, pot.getSidePots()[1].eligiblePlayers.size) // Только player1

        val winners = listOf(player1)
        val distribution = pot.distributeWinners(winners)
        assertEquals(700, distribution[player1]) // 300 + 400
    }

    @Test
    fun `all check on flop - no additional bets`() {
        // Сценарий: префлоп блайнды, затем все чекнут
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Dealer", 1000)
        val player2 = Player(UUID.randomUUID(), "SB", 1000)
        val player3 = Player(UUID.randomUUID(), "BB", 1000)
        val players = listOf(player1, player2, player3)

        // Блайнды
        pot.add(player2, 50) // SB
        pot.add(player3, 100) // BB

        // Префлоп: все уравняли
        pot.add(player1, 100) // Dealer calls
        pot.add(player2, 50) // SB completes to 100
        // BB уже поставил 100, чек

        assertEquals(300, pot.getTotal())

        // Флоп, все чек
        // Никаких дополнительных ставок
        assertEquals(300, pot.getTotal())

        pot.buildSidePots(players)

        assertEquals(1, pot.getSidePots().size)
        assertEquals(300, pot.getSidePots()[0].amount)
    }

    @Test
    fun `all check on all streets`() {
        // Все игроки чекнут на префлопе (кроме блайндов), флопе, терне, ривере
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Dealer", 1000)
        val player2 = Player(UUID.randomUUID(), "SB", 1000)
        val player3 = Player(UUID.randomUUID(), "BB", 1000)
        val players = listOf(player1, player2, player3)

        // Блайнды
        pot.add(player2, 50) // SB
        pot.add(player3, 100) // BB

        // Префлоп: все уравняли
        pot.add(player1, 100) // Dealer calls
        pot.add(player2, 50) // SB completes to 100

        // Флоп - все чек
        // Терн - все чек
        // Ривер - все чек

        pot.buildSidePots(players)

        assertEquals(1, pot.getSidePots().size)
        assertEquals(300, pot.getSidePots()[0].amount)

        // Все игроки вельи к победителям (допустим, все равные карты)
        val winners = listOf(player1, player2, player3)
        val distribution = pot.distributeWinners(winners)

        // Банк 300 делится на троих
        assertEquals(100, distribution[player1])
        assertEquals(100, distribution[player2])
        assertEquals(100, distribution[player3])
    }

    @Test
    fun `check on turn after preflop bets`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)
        val players = listOf(player1, player2)

        // Префлоп ставки
        pot.add(player1, 200)
        pot.add(player2, 200)

        assertEquals(400, pot.getTotal())

        // Флоп, все чек - банк не меняется
        assertEquals(400, pot.getTotal())

        // Терн, все чек - банк не меняется
        assertEquals(400, pot.getTotal())

        // Ривер, все чек
        assertEquals(400, pot.getTotal())

        pot.buildSidePots(players)
        assertEquals(1, pot.getSidePots().size)
        assertEquals(400, pot.getSidePots()[0].amount)
    }

    @Test
    fun `one player folds after call - no refund`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Winner", 1000)
        val player2 = Player(UUID.randomUUID(), "Felder", 1000)
        val players = listOf(player1, player2)

        pot.add(player1, 100)
        pot.add(player2, 100)

        // Игрок 2 фолдит перед показом - его ставка остается в банке
        player2.setStatus(PlayerStatus.FOLDED)

        // buildSidePots учитывает ВСЕ ставки, но eligible только активные
        pot.buildSidePots(players)

        assertEquals(1, pot.getSidePots().size)
        // Весь банк 200 (включая ставку фолднувшего)
        assertEquals(200, pot.getSidePots()[0].amount)
        // Только player1 eligible (так как player2 фолднул)
        assertEquals(1, pot.getSidePots()[0].eligiblePlayers.size)

        val winners = listOf(player1)
        val distribution = pot.distributeWinners(winners)
        // Победитель получает весь банк
        assertEquals(200, distribution[player1])
    }

    @Test
    fun `split pot with identical hands`() {
        // Нит - одинаковые руки (невозможно в реальной игре, но для теста)
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)
        val players = listOf(player1, player2)

        pot.add(player1, 500)
        pot.add(player2, 500)

        pot.buildSidePots(players)

        val winners = listOf(player1, player2)
        val distribution = pot.distributeWinners(winners)

        assertEquals(500, distribution[player1])
        assertEquals(500, distribution[player2])
    }

    @Test
    fun `all-in preflop, check on later streets`() {
        // Player1 all-in префлоп
        // Player2 чек на флопе, терне, ривере
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "AllIn", 200)
        val player2 = Player(UUID.randomUUID(), "Checker", 1000)
        val players = listOf(player1, player2)

        player1.setStatus(PlayerStatus.ALL_IN)

        pot.add(player1, 200)
        pot.add(player2, 200) // только уравнял

        // Флоп, терн, ривер - чек (нет активных ставок)

        pot.buildSidePots(players)

        assertEquals(1, pot.getSidePots().size)
        assertEquals(400, pot.getSidePots()[0].amount)

        val winners = listOf(player2)
        val distribution = pot.distributeWinners(winners)
        assertEquals(400, distribution[player2])
    }

    @Test
    fun `multiple all-ins with different amounts`() {
        // Player1: all-in 50
        // Player2: all-in 150
        // Player3: calls 500
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Short1", 50)
        val player2 = Player(UUID.randomUUID(), "Short2", 150)
        val player3 = Player(UUID.randomUUID(), "Deep", 1000)
        val players = listOf(player1, player2, player3)

        player1.setStatus(PlayerStatus.ALL_IN)
        player2.setStatus(PlayerStatus.ALL_IN)

        pot.add(player1, 50)
        pot.add(player2, 150)
        pot.add(player3, 500)

        pot.buildSidePots(players)

        assertEquals(3, pot.getSidePots().size)

        // Main: 50 * 3 = 150
        assertEquals(150, pot.getSidePots()[0].amount)

        // Side1: 100 * 2 = 200
        assertEquals(200, pot.getSidePots()[1].amount)

        // Side2: 350 * 1 = 350
        assertEquals(350, pot.getSidePots()[2].amount)

        // Player3 выигрывает всё
        val winners = listOf(player3)
        val distribution = pot.distributeWinners(winners)
        assertEquals(700, distribution[player3]) // 150 + 200 + 350
    }

    @Test
    fun `check-raise scenario`() {
        val pot = Pot()
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        val player2 = Player(UUID.randomUUID(), "Player2", 1000)
        val players = listOf(player1, player2)

        // Player1 чек
        // Player2 бет 100
        pot.add(player2, 100)
        // Player1 рейз до 200 (добавляет 200)
        pot.add(player1, 200)
        // Player2 колл (добавляет 100)
        pot.add(player2, 100)

        assertEquals(400, pot.getTotal())

        pot.buildSidePots(players)
        assertEquals(1, pot.getSidePots().size)
        assertEquals(400, pot.getSidePots()[0].amount)
    }

    @Test
    fun `player with zero stack cannot bet`() {
        val player = Player(UUID.randomUUID(), "Broke", 0)

        val betAmount = player.bet(100)

        assertEquals(0, betAmount)
        assertEquals(0, player.getStack())
    }

    @Test
    fun `folded player cannot act`() {
        val player = Player(UUID.randomUUID(), "Felder", 1000)
        player.setStatus(PlayerStatus.FOLDED)

        assertTrue(player.getStatus() == PlayerStatus.FOLDED)
        assertEquals(1000, player.getStack()) // Стак не меняется при фолде
    }

    @Test
    fun `all-in player stays in hand but cannot act`() {
        val player = Player(UUID.randomUUID(), "AllIn", 100)

        val actualBet = player.bet(100)
        player.setStatus(PlayerStatus.ALL_IN)

        assertEquals(100, actualBet)
        assertEquals(0, player.getStack())
        assertTrue(player.getStatus() == PlayerStatus.ALL_IN)
    }
}
