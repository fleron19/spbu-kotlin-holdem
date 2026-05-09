data class Card(
    private val suit: Suit,
    private val rank: Rank
) {
    fun getSuit(): Suit = suit
    fun getRank(): Rank = rank

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