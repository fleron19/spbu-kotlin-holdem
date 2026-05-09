import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class HandTest {

    private fun makePlayer(name: String = "Test", stack: Int = 1000): Player {
        return Player(UUID.randomUUID(), name, stack)
    }

    private fun makeHand(vararg playerNames: String): Hand {
        val hand = Hand(UUID.randomUUID())
        for (name in playerNames) {
            hand.addPlayer(makePlayer(name))
        }
        return hand
    }

    @Test
    fun `addPlayer adds player to hand`() {
        val hand = makeHand()
        val player = makePlayer("Alice")
        hand.addPlayer(player)
        assertEquals(1, hand.getPlayers().size)
        assertEquals("Alice", hand.getPlayers()[0].name)
    }

    @Test
    fun `dealHole deals 2 cards to each player`() {
        val hand = makeHand("Alice", "Bob", "Charlie")
        hand.dealHole()
        for (player in hand.getPlayers()) {
            assertEquals(2, player.getHole().size)
        }
    }

    @Test
    fun `dealCommunity adds cards to community`() {
        val hand = makeHand("Alice")
        hand.dealHole()
        hand.dealCommunity(3)
        assertEquals(3, hand.getCommunity().size)
    }

    @Test
    fun `dealCommunity can be called multiple times`() {
        val hand = makeHand("Alice")
        hand.dealHole()
        hand.dealCommunity(3)
        hand.dealCommunity(1)
        hand.dealCommunity(1)
        assertEquals(5, hand.getCommunity().size)
    }

    @Test
    fun `showdown excludes folded players`() {
        val hand = Hand(UUID.randomUUID())
        val alice = Player(UUID.randomUUID(), "Alice", 1000, mutableListOf(Card(Suit.HEARTS, Rank.TWO), Card(Suit.DIAMONDS, Rank.THREE)))
        val bob = Player(UUID.randomUUID(), "Bob", 1000, mutableListOf(Card(Suit.HEARTS, Rank.FOUR), Card(Suit.DIAMONDS, Rank.FIVE)))
        hand.addPlayer(alice)
        hand.addPlayer(bob)
        hand.dealCommunity(5)
        alice.setStatus(PlayerStatus.FOLDED)
        val winners = hand.showdown()
        assertEquals(1, winners.size)
        assertEquals("Bob", winners[0].name)
    }

    @Test
    fun `showdown returns best hand`() {
        val alice = Player(UUID.randomUUID(), "Alice", 1000, mutableListOf(
            Card(Suit.HEARTS, Rank.ACE),
            Card(Suit.HEARTS, Rank.KING)
        ))
        val bob = Player(UUID.randomUUID(), "Bob", 1000, mutableListOf(
            Card(Suit.CLUBS, Rank.TWO),
            Card(Suit.DIAMONDS, Rank.THREE)
        ))
        val hand = Hand(UUID.randomUUID())
        hand.addPlayer(alice)
        hand.addPlayer(bob)
// Use deck to ensure consistent cards for Alice's pair of Aces
        hand.addCommunityCard(Card(Suit.SPADES, Rank.ACE))
        hand.addCommunityCard(Card(Suit.DIAMONDS, Rank.FIVE))
        hand.addCommunityCard(Card(Suit.CLUBS, Rank.SEVEN))
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.NINE))
        hand.addCommunityCard(Card(Suit.CLUBS, Rank.TWO))
        val winners = hand.showdown()
        assertEquals(1, winners.size)
        assertEquals("Alice", winners[0].name)
    }

    @Test
    fun `showdown returns both players on tie`() {
        val alice = Player(UUID.randomUUID(), "Alice", 1000, mutableListOf(
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.HEARTS, Rank.KING)
        ))
        val bob = Player(UUID.randomUUID(), "Bob", 1000, mutableListOf(
            Card(Suit.DIAMONDS, Rank.KING),
            Card(Suit.CLUBS, Rank.KING)
        ))
        val hand = Hand(UUID.randomUUID())
        hand.addPlayer(alice)
        hand.addPlayer(bob)
        hand.dealCommunity(5)
        val winners = hand.showdown()
        assertEquals(2, winners.size)
        assert(winners.contains(alice))
        assert(winners.contains(bob))
    }

    @Test
    fun `getPot returns pot instance`() {
        val hand = makeHand("Alice")
        assertEquals(hand.getPot(), hand.getPot())
    }
}