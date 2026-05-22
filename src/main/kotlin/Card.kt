data class Card(
    val suit: Suit,
    val rank: Rank,
) {
    override fun toString(): String {
        val rankChar = when (rank) {
            Rank.TEN -> "T"
            Rank.JACK -> "J"
            Rank.QUEEN -> "Q"
            Rank.KING -> "K"
            Rank.ACE -> "A"
            else -> rank.value.toString()
        }
        val suitChar = when (suit) {
            Suit.SPADES -> "♠"
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
        }
        return "$rankChar$suitChar"
    }
}
