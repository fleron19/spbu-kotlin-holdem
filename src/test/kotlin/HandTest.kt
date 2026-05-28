import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class HandTest {
    @Test
    fun `dealHole should deal 2 cards to each player`() {
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        hand.dealHole()

        assertEquals(2, players[0].getHole().size)
        assertEquals(2, players[1].getHole().size)
        assertEquals(GamePhase.PREFLOP, hand.phase)
    }

    @Test
    fun `dealFlop should deal 3 community cards`() {
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        hand.dealHole()
        hand.dealFlop()

        assertEquals(3, hand.getCommunity().size)
        assertEquals(GamePhase.FLOP, hand.phase)
    }

    @Test
    fun `dealTurn should deal 1 more community card`() {
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        hand.dealHole()
        hand.dealFlop()
        hand.dealTurn()

        assertEquals(4, hand.getCommunity().size)
        assertEquals(GamePhase.TURN, hand.phase)
    }

    @Test
    fun `dealRiver should deal 1 more community card`() {
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        hand.dealHole()
        hand.dealFlop()
        hand.dealTurn()
        hand.dealRiver()

        assertEquals(5, hand.getCommunity().size)
        assertEquals(GamePhase.RIVER, hand.phase)
    }

    @Test
    fun `showdown should determine winner`() {
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        hand.dealHole()
        // Даем игрокам разные руки для тестирования
        // Player1: полный дом (тузы и десятки)
        // Player2: стрит (слабее полного дома)

        // Добавляем карты вручную для предсказуемого результата
        players[0].clearHole()
        players[1].clearHole()

        // Полный дом для Player1 (AA + TTT на столе)
        players[0].receiveCard(Card(Suit.SPADES, Rank.ACE))
        players[0].receiveCard(Card(Suit.HEARTS, Rank.ACE))

        // Слабые карты для Player2
        players[1].receiveCard(Card(Suit.DIAMONDS, Rank.KING))
        players[1].receiveCard(Card(Suit.CLUBS, Rank.QUEEN))

        // Добавляем community карты - три десятки и две другие
        hand.addCommunityCard(Card(Suit.SPADES, Rank.TEN))
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.TEN))
        hand.addCommunityCard(Card(Suit.DIAMONDS, Rank.TEN))
        hand.addCommunityCard(Card(Suit.CLUBS, Rank.JACK))
        hand.addCommunityCard(Card(Suit.SPADES, Rank.NINE))

        // Делаем ставки чтобы был банк
        hand.getPot().add(players[0], 100)
        hand.getPot().add(players[1], 100)

        val winners = hand.showdown()

        assertEquals(1, winners.size)
        assertEquals("Player1", winners[0].name)
        assertEquals(GamePhase.SHOWDOWN, hand.phase)
        // Player1 должен выиграть банк (1000 начальных - 100 ставка + 200 выигрыш = 1100)
        // Но так как Player2 тоже поставил 100, общий банк 200, и Player1 получает 1000 + 100 (свой возврат) + 100 (выигрыш) = 1200
        assertEquals(1200, players[0].getStack())
    }

    @Test
    fun `reset should clear hand state`() {
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        hand.dealHole()
        hand.dealFlop()

        hand.reset()

        assertEquals(GamePhase.WAITING, hand.phase)
        assertTrue(hand.getCommunity().isEmpty())
        assertEquals(0, players[0].getHole().size)
    }
}
