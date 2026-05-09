class Deck {
    private var cards: MutableList<Card> = mutableListOf()

    init {
        reset()
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun draw(): Card {
        require(cards.isNotEmpty()) { "Deck is empty" }
        return cards.removeAt(cards.lastIndex)
    }

    fun reset() {
        cards = mutableListOf<Card>().apply {
            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    add(Card(suit, rank))
                }
            }
        }
    }
}
