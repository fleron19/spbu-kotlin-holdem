import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardTest {

    @Test
    fun `toString returns correct format for number cards`() {
        assertEquals("2♥", Card(Suit.HEARTS, Rank.TWO).toString())
        assertEquals("7♠", Card(Suit.SPADES, Rank.SEVEN).toString())
        assertEquals("9♦", Card(Suit.DIAMONDS, Rank.NINE).toString())
    }

    @Test
    fun `toString returns T for ten`() {
        assertEquals("T♣", Card(Suit.CLUBS, Rank.TEN).toString())
    }

    @Test
    fun `toString returns face card letters`() {
        assertEquals("J♥", Card(Suit.HEARTS, Rank.JACK).toString())
        assertEquals("Q♠", Card(Suit.SPADES, Rank.QUEEN).toString())
        assertEquals("K♦", Card(Suit.DIAMONDS, Rank.KING).toString())
    }

    @Test
    fun `toString returns A for ace`() {
        assertEquals("A♣", Card(Suit.CLUBS, Rank.ACE).toString())
    }

    @Test
    fun `toString returns correct suit symbols`() {
        assertEquals("2♥", Card(Suit.HEARTS, Rank.TWO).toString())
        assertEquals("2♦", Card(Suit.DIAMONDS, Rank.TWO).toString())
        assertEquals("2♣", Card(Suit.CLUBS, Rank.TWO).toString())
        assertEquals("2♠", Card(Suit.SPADES, Rank.TWO).toString())
    }

    @Test
    fun `toString returns full rank for non-face cards`() {
        assertEquals("2♠", Card(Suit.SPADES, Rank.TWO).toString())
        assertEquals("5♠", Card(Suit.SPADES, Rank.FIVE).toString())
        assertEquals("8♠", Card(Suit.SPADES, Rank.EIGHT).toString())
    }
}
