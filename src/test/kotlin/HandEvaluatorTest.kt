import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class HandEvaluatorTest {
    private val evaluator = HandEvaluator()

    private fun makePlayer(cards: List<Card>): Player {
        return Player(
            UUID.randomUUID(),
            "Test",
            1000,
            cards.toMutableList(),
        )
    }

    @Test
    fun `bestHand returns HIGH_CARD for no combination`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.DIAMONDS, Rank.NINE),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.SEVEN),
            Card(Suit.SPADES, Rank.FIVE),
            Card(Suit.HEARTS, Rank.TWO),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.HIGH_CARD.value, result.category)
    }

    @Test
    fun `bestHand returns ONE_PAIR for single pair`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.SEVEN),
                Card(Suit.DIAMONDS, Rank.TWO),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.SEVEN),
            Card(Suit.SPADES, Rank.FIVE),
            Card(Suit.HEARTS, Rank.KING),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.ONE_PAIR.value, result.category)
    }

    @Test
    fun `bestHand returns TWO_PAIR for two pairs`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.ACE),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.KING),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.HEARTS, Rank.QUEEN),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.TWO_PAIR.value, result.category)
    }

    @Test
    fun `bestHand returns THREE_OF_A_KIND for three of a kind`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.SEVEN),
                Card(Suit.DIAMONDS, Rank.TWO),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.SEVEN),
            Card(Suit.SPADES, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.KING),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.THREE_OF_A_KIND.value, result.category)
    }

    @Test
    fun `bestHand returns STRAIGHT for consecutive ranks`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.EIGHT),
                Card(Suit.DIAMONDS, Rank.NINE),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.FIVE),
            Card(Suit.SPADES, Rank.SIX),
            Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.STRAIGHT.value, result.category)
    }

    @Test
    fun `bestHand returns ACE_LOW_STRAIGHT for A-2-3-4-5`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.DIAMONDS, Rank.FIVE),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.TWO),
            Card(Suit.SPADES, Rank.THREE),
            Card(Suit.HEARTS, Rank.FOUR),
            Card(Suit.DIAMONDS, Rank.SIX),
            Card(Suit.CLUBS, Rank.SEVEN),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.STRAIGHT.value, result.category)
    }

    @Test
    fun `bestHand returns FLUSH for five same suit`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.KING),
                Card(Suit.HEARTS, Rank.TWO),
            ),
        )
        val community = listOf(
            Card(Suit.HEARTS, Rank.FIVE),
            Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.NINE),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.FLUSH.value, result.category)
    }

    @Test
    fun `bestHand returns FULL_HOUSE for three plus pair`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.SEVEN),
                Card(Suit.DIAMONDS, Rank.FIVE),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.SEVEN),
            Card(Suit.SPADES, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.FIVE),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.FULL_HOUSE.value, result.category)
    }

    @Test
    fun `bestHand returns FOUR_OF_A_KIND for four same rank`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.SEVEN),
                Card(Suit.DIAMONDS, Rank.KING),
            ),
        )
        val community = listOf(
            Card(Suit.CLUBS, Rank.SEVEN),
            Card(Suit.SPADES, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.FOUR_OF_A_KIND.value, result.category)
    }

    @Test
    fun `bestHand returns STRAIGHT_FLUSH for straight and flush`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.NINE),
                Card(Suit.HEARTS, Rank.FIVE),
            ),
        )
        val community = listOf(
            Card(Suit.HEARTS, Rank.SIX),
            Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.EIGHT),
            Card(Suit.DIAMONDS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.STRAIGHT_FLUSH.value, result.category)
    }

    @Test
    fun `bestHand returns ROYAL_FLUSH for 10-J-Q-K-A same suit`() {
        val player = makePlayer(
            listOf(
                Card(Suit.HEARTS, Rank.ACE),
                Card(Suit.HEARTS, Rank.KING),
            ),
        )
        val community = listOf(
            Card(Suit.HEARTS, Rank.TEN),
            Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.HEARTS, Rank.QUEEN),
            Card(Suit.DIAMONDS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
        )
        val result = evaluator.bestHand(player, community)
        assertEquals(Combination.ROYAL_FLUSH.value, result.category)
    }

    @Test
    fun `compare returns positive when first hand is better`() {
        val highCard = HandRank(Combination.HIGH_CARD.value, listOf(Card(Suit.HEARTS, Rank.ACE)))
        val onePair = HandRank(Combination.ONE_PAIR.value, listOf(Card(Suit.HEARTS, Rank.KING)))
        assertTrue(evaluator.compare(onePair, highCard) > 0)
    }

    @Test
    fun `compare returns negative when second hand is better`() {
        val highCard = HandRank(Combination.HIGH_CARD.value, listOf(Card(Suit.HEARTS, Rank.ACE)))
        val onePair = HandRank(Combination.ONE_PAIR.value, listOf(Card(Suit.HEARTS, Rank.KING)))
        assertTrue(evaluator.compare(highCard, onePair) < 0)
    }

    @Test
    fun `compare returns zero for identical hands`() {
        val hand1 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.KING)),
        )
        val hand2 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.CLUBS, Rank.KING), Card(Suit.SPADES, Rank.KING)),
        )
        assertEquals(0, evaluator.compare(hand1, hand2))
    }

    @Test
    fun `compare uses kicker to break ties in same category`() {
        val hand1 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.KING), Card(Suit.CLUBS, Rank.ACE)),
        )
        val hand2 = HandRank(
            Combination.ONE_PAIR.value,
            listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.KING), Card(Suit.CLUBS, Rank.QUEEN)),
        )
        assertTrue(evaluator.compare(hand1, hand2) > 0)
    }
}
