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

    class GamePhase {
        <<enum>>
        WAITING
        PREFLOP
        FLOP
        TURN
        RIVER
        SHOWDOWN
    }

    %% Core classes
    class Card {
        + suit: Suit
        + rank: Rank
        + toString() String
    }

    class Deck {
        - cards: List~Card~
        + shuffle() Unit
        + draw() Card
        + reset() Unit
    }

    class Player {
        + id: UUID
        + name: String
        + stack: Int
        + hole: List~Card~
        + status: PlayerStatus
        + receiveCard(c: Card) Unit
        + bet(amount: Int) Int
    }

    class Pot {
        + total: Int
        + contributions: Map~UUID, Int~
        + sidePots: List~Pot~
        + add(playerId: UUID, amount: Int) Unit
        + buildSidePots() Unit
        + distributeWinners(winners: List~UUID~) Map~UUID, Int~
    }

    class Hand {
        + id: UUID
        + phase: GamePhase
        + deck: Deck
        + players: List~Player~
        + pot: Pot
        + community: List~Card~
        + actions: List~String~
        + dealHole() Unit
        + dealCommunity(n: Int) Unit
        + showdown() List~UUID~
    }

    class Game {
        + id: UUID
        + players: List~Player~
        + currentHand: Hand?
        + addPlayer(p: Player) Unit
        + startHand() Hand
    }

    class ActionProcessor {
        + validate(playerId: UUID, action: String, amount: Int) Boolean
        + execute(playerId: UUID, action: String, amount: Int) Unit
        + handleAllIn(playerId: UUID) Unit
    }

    class HandEvaluator {
        + bestHand(player: Player, community: List~Card~) HandRank
        + compare(a: HandRank, b: HandRank) Int
    }

    class HandRank {
        + category: String
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
        + storage: Storage
        + logger: Logger
        + startGame() Unit
        + playerAction(playerId: UUID, action: String, amount: Int) Unit
    }

    %% Relationships
    Game o-- Player : contains
    Game --> Hand : currentHand
    Hand o-- Deck : has
    Hand o-- Pot : has
    Hand o-- Player : players
    Hand ..> HandEvaluator : uses
    Controller --> Storage : uses
    Controller --> Logger : uses
    Controller --> ActionProcessor : uses
    ActionProcessor --> Hand : accesses
    ActionProcessor --> Pot : accesses
    ActionProcessor --> Logger : uses
    ActionProcessor ..> HandEvaluator : uses
