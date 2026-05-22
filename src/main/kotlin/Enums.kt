enum class Suit(val code: Int, val symbol: String) {
    HEARTS(0, "♥"),
    DIAMONDS(1, "♦"),
    CLUBS(2, "♣"),
    SPADES(3, "♠"),
}

enum class Rank(val value: Int, val symbol: String) {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "T"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A"),
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
