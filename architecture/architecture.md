# Class diagram

```mermaid
classDiagram
    %% Enums
    class Suit {
        <<enum>>
        HEARTS
        DIAMONDS
        CLUBS
        SPADES
    }

    class Rank {
        <<enum>>
        TWO
        THREE
        FOUR
        FIVE
        SIX
        SEVEN
        EIGHT
        NINE
        TEN
        JACK
        QUEEN
        KING
        ACE
    }

    class PlayerStatus {
        <<enum>>
        ACTIVE
        FOLDED
        ALL_IN
    }

    class Action {
        <<enum>>
        FOLD
        CHECK
        CALL
        BET
        RAISE
        ALL_IN
    }

    class GamePhase {
        <<enum>>
        WAITING
        PREFLOP
        FLOP
        TURN
        RIVER
        SHOWDOWN
    }

    class Combination {
        <<enum>>
        HIGH_CARD
        ONE_PAIR
        TWO_PAIR
        THREE_OF_A_KIND
        STRAIGHT
        FLUSH
        FULL_HOUSE
        FOUR_OF_A_KIND
        STRAIGHT_FLUSH
        ROYAL_FLUSH
    }

    %% Core classes
    class Card {
        - suit: Suit
        - rank: Rank
        + toString() String
    }

    class Deck {
        - cards: List~Card~
        + shuffle() Unit
        + draw() Card
        + reset() Unit
    }

    class Player {
        - id: UUID
        + name: String
        - stack: Int
        - hole: MutableList~Card~
        - status: PlayerStatus
        + receiveCard(c: Card) Unit
        + bet(amount: Int) Int
    }

    class Pot {
        - total: Int
        - contributions: Map~Player, Int~
        - sidePots: List~Pot~
        + add(player: Player, amount: Int) Unit
        + buildSidePots() Unit
        + distributeWinners(winners: List~Player~) Map~Player, Int~
    }

    class Hand {
        - id: UUID
        + phase: GamePhase
        - deck: Deck
        - players: MutableList~Player~
        - pot: Pot
        + community: List~Card~
        - actions: MutableList~Action~
        + dealHole() Unit
        + dealCommunity(n: Int) Unit
        + addCommunityCard(card: Card) Unit
        + showdown() List~Player~
    }

    class Game {
        - id: UUID
        - players: List~Player~
        - currentHand: Hand?
        + addPlayer(p: Player) Unit
        + startHand() Hand
    }

    class ActionProcessor {
        + validate(player: Player, action: Action, amount: Int) Boolean
        + execute(hand: Hand, player: Player, action: Action, amount: Int) Boolean
    }

    class HandEvaluator {
        + bestHand(player: Player, community: List~Card~) HandRank
        + compare(a: HandRank, b: HandRank) Int
    }

    class HandRank {
        + category: Int
        + cards: List~Card~
        + compareTo(other: HandRank) Int
    }

    class Logger {
        + log(level: String, msg: String) Unit
        + info(msg: String) Unit
        + error(msg: String) Unit
    }

    %% Infrastructure
    class Storage {
        <<interface>>
        + saveGame(g: Game) Unit
        + loadGame(id: UUID) Game
    }

    class Controller {
        - storage: Storage
        - logger: Logger
        + startGame() Unit
        + playerAction(player: Player, action: Action, amount: Int) Unit
    }

    %% Relationships
    Game o-- Player : contains
    Game --> Hand : creates
    Game o-- Pot : manages
    Hand o-- Deck : has
    Hand o-- Pot : tracks
    Hand ..> HandEvaluator : evaluates
    Controller o-- Game : controls
    Controller --> Storage : uses
    Controller --> Logger : uses
    Controller --> ActionProcessor : uses
    ActionProcessor --> Hand : modifies
    ActionProcessor --> Pot : updates
    ActionProcessor --> Logger : logs
    ActionProcessor ..> HandEvaluator : uses
