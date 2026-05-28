enum class Suit(val code: Int) {
    HEARTS(0),
    DIAMONDS(1),
    CLUBS(2),
    SPADES(3),
    ;

    override fun toString(): String = when (this) {
        HEARTS -> "♥"
        DIAMONDS -> "♦"
        CLUBS -> "♣"
        SPADES -> "♠"
    }
}

enum class Rank(val value: Int) {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(11),
    QUEEN(12),
    KING(13),
    ACE(14),
    ;

    override fun toString(): String = when (this) {
        TWO -> "2"
        THREE -> "3"
        FOUR -> "4"
        FIVE -> "5"
        SIX -> "6"
        SEVEN -> "7"
        EIGHT -> "8"
        NINE -> "9"
        TEN -> "T"
        JACK -> "J"
        QUEEN -> "Q"
        KING -> "K"
        ACE -> "A"
    }
}
enum class Combination(val value: Int) {
    HIGH_CARD(1),
    ONE_PAIR(2),
    TWO_PAIR(3),
    THREE_OF_A_KIND(4),
    STRAIGHT(5),
    FLUSH(6),
    FULL_HOUSE(7),
    FOUR_OF_A_KIND(8),
    STRAIGHT_FLUSH(9),
    ROYAL_FLUSH(10),
}

enum class PlayerStatus {
    ACTIVE,
    FOLDED,
    ALL_IN,
    OUT,
}

enum class GamePhase {
    WAITING,
    PREFLOP,
    FLOP,
    TURN,
    RIVER,
    SHOWDOWN,
}

enum class Action {
    FOLD,
    CHECK,
    CALL,
    BET,
    RAISE,
    ALL_IN,
}
