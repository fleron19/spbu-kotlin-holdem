import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HandRankTest {

    @Test
    fun `compareTo returns 0 for identical hands`() {
        val hand1 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.KING))
        )
        val hand2 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.CLUBS, Rank.KING), Card(Suit.SPADES, Rank.KING))
        )
        assertEquals(0, hand1.compareTo(hand2))
    }

    @Test
    fun `compareTo returns positive when first hand has higher category`() {
        val highCard = HandRank(Combination.HIGH_CARD.value, listOf(Card(Suit.HEARTS, Rank.ACE)))
        val onePair = HandRank(Combination.ONE_PAIR.value, listOf(Card(Suit.HEARTS, Rank.TWO)))
        assertTrue(onePair.compareTo(highCard) > 0)
    }

    @Test
    fun `compareTo returns negative when second hand has higher category`() {
        val highCard = HandRank(Combination.HIGH_CARD.value, listOf(Card(Suit.HEARTS, Rank.ACE)))
        val onePair = HandRank(Combination.ONE_PAIR.value, listOf(Card(Suit.HEARTS, Rank.TWO)))
        assertTrue(highCard.compareTo(onePair) < 0)
    }

    @Test
    fun `compareTo compares pair rank as tiebreaker`() {
        val hand1 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.KING), Card(Suit.CLUBS, Rank.ACE))
        )
        val hand2 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.DIAMONDS, Rank.QUEEN), Card(Suit.CLUBS, Rank.ACE))
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares full house trips rank first`() {
        val hand1 = HandRank(
            Combination.FULL_HOUSE.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.TWO),
                Card(Suit.HEARTS, Rank.TWO)
            )
        )
        val hand2 = HandRank(
            Combination.FULL_HOUSE.value,
            listOf(
                Card(Suit.HEARTS, Rank.QUEEN),
                Card(Suit.DIAMONDS, Rank.QUEEN),
                Card(Suit.CLUBS, Rank.QUEEN),
                Card(Suit.SPADES, Rank.ACE),
                Card(Suit.HEARTS, Rank.ACE)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares full house pair rank when trips are equal`() {
        val hand1 = HandRank(
            Combination.FULL_HOUSE.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.ACE),
                Card(Suit.HEARTS, Rank.ACE)
            )
        )
        val hand2 = HandRank(
            Combination.FULL_HOUSE.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.QUEEN),
                Card(Suit.HEARTS, Rank.QUEEN)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares two pair by higher pair first`() {
        val hand1 = HandRank(
            Combination.TWO_PAIR.value,
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.ACE),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.KING),
                Card(Suit.HEARTS, Rank.TWO)
            )
        )
        val hand2 = HandRank(
            Combination.TWO_PAIR.value,
            listOf(
                Card(Suit.HEARTS, Rank.QUEEN),
                Card(Suit.DIAMONDS, Rank.QUEEN),
                Card(Suit.CLUBS, Rank.JACK),
                Card(Suit.SPADES, Rank.JACK),
                Card(Suit.HEARTS, Rank.ACE)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares two pair by lower pair when higher pair is equal`() {
        val hand1 = HandRank(
            Combination.TWO_PAIR.value,
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.ACE),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.KING),
                Card(Suit.HEARTS, Rank.TWO)
            )
        )
        val hand2 = HandRank(
            Combination.TWO_PAIR.value,
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.ACE),
                Card(Suit.CLUBS, Rank.QUEEN),
                Card(Suit.SPADES, Rank.QUEEN),
                Card(Suit.HEARTS, Rank.TWO)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares two pair by kicker when both pairs are equal`() {
        val hand1 = HandRank(
            Combination.TWO_PAIR.value,
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.ACE),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.KING),
                Card(Suit.HEARTS, Rank.QUEEN)
            )
        )
        val hand2 = HandRank(
            Combination.TWO_PAIR.value,
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.ACE),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.KING),
                Card(Suit.HEARTS, Rank.JACK)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares high card hands by highest card first`() {
        val hand1 = HandRank(
            Combination.HIGH_CARD.value,
            listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN), Card(Suit.CLUBS, Rank.JACK))
        )
        val hand2 = HandRank(
            Combination.HIGH_CARD.value,
            listOf(Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.DIAMONDS, Rank.JACK), Card(Suit.CLUBS, Rank.TEN))
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares four of a kind by quads rank then kicker`() {
        val hand1 = HandRank(
            Combination.FOUR_OF_A_KIND.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.KING),
                Card(Suit.HEARTS, Rank.ACE)
            )
        )
        val hand2 = HandRank(
            Combination.FOUR_OF_A_KIND.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.KING),
                Card(Suit.HEARTS, Rank.QUEEN)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }

    @Test
    fun `compareTo compares three of a kind by trips rank then kickers`() {
        val hand1 = HandRank(
            Combination.THREE_OF_A_KIND.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.ACE),
                Card(Suit.HEARTS, Rank.QUEEN)
            )
        )
        val hand2 = HandRank(
            Combination.THREE_OF_A_KIND.value,
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.KING),
                Card(Suit.CLUBS, Rank.KING),
                Card(Suit.SPADES, Rank.QUEEN),
                Card(Suit.HEARTS, Rank.JACK)
            )
        )
        assertTrue(hand1.compareTo(hand2) > 0)
    }
}
