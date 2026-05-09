import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeckTest {

    @Test
    fun `reset creates 52 cards`() {
        val deck = Deck()
        var count = 0
        while (count == 0 || true) {
            try {
                deck.draw()
                count++
            } catch (e: Exception) {
                break
            }
        }
        assertEquals(52, count)
    }

    @Test
    fun `reset creates all unique cards`() {
        val deck = Deck()
        val drawnCards = mutableSetOf<String>()
        try {
            while (true) {
                val card = deck.draw()
                assertTrue(drawnCards.add(card.toString()), "Duplicate card found: $card")
            }
        } catch (e: Exception) {
            // deck empty
        }
        assertEquals(52, drawnCards.size)
    }

    @Test
    fun `shuffle changes card order`() {
        val deck1 = Deck()
        deck1.shuffle()
        val order1 = mutableListOf<String>()
        try {
            while (true) {
                order1.add(deck1.draw().toString())
            }
        } catch (e: Exception) {
            // deck empty
        }

        val deck2 = Deck()
        deck2.shuffle()
        val order2 = mutableListOf<String>()
        try {
            while (true) {
                order2.add(deck2.draw().toString())
            }
        } catch (e: Exception) {
            // deck empty
        }

        assertNotEquals(order1, order2)
    }

    @Test
    fun `draw removes card from deck`() {
        val deck = Deck()
        val card = deck.draw()
        var remaining = 0
        try {
            while (true) {
                deck.draw()
                remaining++
            }
        } catch (e: Exception) {
            // deck empty
        }
        assertEquals(51, remaining)
    }

    @Test
    fun `reset restores full deck`() {
        val deck = Deck()
        deck.draw()
        deck.draw()
        deck.reset()
        var count = 0
        try {
            while (true) {
                deck.draw()
                count++
            }
        } catch (e: Exception) {
            // deck empty
        }
        assertEquals(52, count)
    }

    @Test
    fun `draw throws when deck is empty`() {
        val deck = Deck()
        repeat(52) { deck.draw() }
        var exceptionThrown = false
        try {
            deck.draw()
        } catch (e: Exception) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }
}