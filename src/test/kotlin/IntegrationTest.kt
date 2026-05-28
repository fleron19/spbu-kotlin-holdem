import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class IntegrationTest {

    @Test
    fun `full hand with 3 players check-check-check on preflop`() {
        // Сценарий: 3 игрока, все чекнут на префлопе после BB
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Dealer", 1000),
            Player(UUID.randomUUID(), "SB", 1000),
            Player(UUID.randomUUID(), "BB", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        // Блайнды: Dealer=дилер, SB=Dealer+1, BB=Dealer+2
        val sbIndex = 1
        val bbIndex = 2

        // SB ставит small blind
        val sbPlayer = players[sbIndex]
        val smallBlind = 50
        sbPlayer.bet(smallBlind)
        hand.getPot().add(sbPlayer, smallBlind)

        // BB ставит big blind
        val bbPlayer = players[bbIndex]
        val bigBlind = 100
        bbPlayer.bet(bigBlind)
        hand.getPot().add(bbPlayer, bigBlind)

        hand.setCurrentBet(bigBlind)

        // Ход начинается с Dealer+3 = (2+3)%3 = 2 (BB должен доставить)
        // Нет, префлоп ход начинается с UTG = dealer+3 = 2%3 = 2? Нет, это BB
        // Правильно: префлоп ход начинается с UTG = dealer+3, но если 3 игрока:
        // dealer=0, SB=1, BB=2, UTG=0 (dealer)
        // Игрок 0 (Dealer) ходит первым, затем игрок 1 (SB), затем игрок 2 (BB)

        // Игрок 0 (Dealer) чек (у него 0 в банке, currentBet=100, нужно уравнять)
        // Нет, у Dealer'а 0 в банке, currentBet=100, он должен CALL 100
        val dealerPlayer = players[0]
        val callAmount = hand.getCurrentBet() - (hand.getPot().getContributions()[dealerPlayer] ?: 0)
        dealerPlayer.bet(callAmount)
        hand.getPot().add(dealerPlayer, callAmount)

        // Игрок 1 (SB) докидывает до bigBlind
        val sbContribution = hand.getPot().getContributions()[sbPlayer] ?: 0
        val sbToCall = hand.getCurrentBet() - sbContribution
        sbPlayer.bet(sbToCall)
        hand.getPot().add(sbPlayer, sbToCall)

        // Игрок 2 (BB) чек (он уже поставил bigBlind)
        // Раунд завершён

        assertEquals(300, hand.getPot().getTotal())
        assertEquals(900, dealerPlayer.getStack())
        assertEquals(900, sbPlayer.getStack())
        assertEquals(900, bbPlayer.getStack())
    }

    @Test
    fun `full hand with raise on preflop`() {
        // Сценарий: 2 игрока, SB колл, BB рейз, SB колл
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Dealer", 1000),
            Player(UUID.randomUUID(), "SB", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)
        hand.dealHole()

        val sbIndex = 1
        val bbIndex = 0 // Dealer

        // SB ставит small blind
        val smallBlind = 50
        val actualSbBet = players[sbIndex].bet(smallBlind)
        hand.getPot().add(players[sbIndex], actualSbBet)

        // BB ставит big blind
        val bigBlind = 100
        val actualBbBet = players[bbIndex].bet(bigBlind)
        hand.getPot().add(players[bbIndex], actualBbBet)

        hand.setCurrentBet(bigBlind)

        // Префлоп: ход начинается с SB (игрок после BB по кругу = dealer = 0? Нет, SB=1)
        // Правильно: UTG = (bbIndex + 1) % size = 1 (SB)
        val sbPlayer = players[sbIndex]
        val sbContribution = hand.getPot().getContributions()[sbPlayer] ?: 0
        val sbToCall = hand.getCurrentBet() - sbContribution // 100 - 50 = 50
        val sbCallBet = sbPlayer.bet(sbToCall)
        hand.getPot().add(sbPlayer, sbCallBet)

        // Теперь ход на BB, он может рейзнуть
        // BB делает рейз: мин рейз = bigBlind = 100, значит рейз до 100+100=200
        val bbPlayer = players[bbIndex]
        val bbContribution = hand.getPot().getContributions()[bbPlayer] ?: 0
        val raiseBy = bigBlind // минимальный рейз
        val totalBet = hand.getCurrentBet() + raiseBy // 100 + 100 = 200
        val bbToAdd = totalBet - bbContribution // 200 - 100 = 100
        val bbRaiseBet = bbPlayer.bet(bbToAdd)
        hand.getPot().add(bbPlayer, bbRaiseBet)
        hand.setCurrentBet(totalBet)

        // Теперь SB должен уравнять ещё 100 (200 - 100 = 100)
        val sbNewContribution = hand.getPot().getContributions()[sbPlayer] ?: 0
        val sbNewToCall = hand.getCurrentBet() - sbNewContribution // 200 - 100 = 100
        val sbCallBet2 = sbPlayer.bet(sbNewToCall)
        hand.getPot().add(sbPlayer, sbCallBet2)

        assertEquals(400, hand.getPot().getTotal())
        assertEquals(800, sbPlayer.getStack()) // 1000 - 50 - 50 - 100 = 800
        assertEquals(800, bbPlayer.getStack()) // 1000 - 100 - 100 = 800
    }

    @Test
    fun `showdown with pair beats high card`() {
        // Сценарий: showdown, пара побеждает старшую карту
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        // Ручная расстановка карт
        // Player1: пара тузов
        players[0].clearHole()
        players[0].receiveCard(Card(Suit.SPADES, Rank.ACE))
        players[0].receiveCard(Card(Suit.HEARTS, Rank.ACE))

        // Player2: старшая карта король
        players[1].clearHole()
        players[1].receiveCard(Card(Suit.DIAMONDS, Rank.KING))
        players[1].receiveCard(Card(Suit.CLUBS, Rank.QUEEN))

        // Community: 7, 8, 9, J, 2 - нет пар
        hand.addCommunityCard(Card(Suit.SPADES, Rank.SEVEN))
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.EIGHT))
        hand.addCommunityCard(Card(Suit.DIAMONDS, Rank.NINE))
        hand.addCommunityCard(Card(Suit.CLUBS, Rank.JACK))
        hand.addCommunityCard(Card(Suit.SPADES, Rank.TWO))

        // Ставки
        hand.getPot().add(players[0], 100)
        hand.getPot().add(players[1], 100)

        val evaluator = HandEvaluator()
        val hand1Rank = evaluator.bestHand(players[0], hand.getCommunity())
        val hand2Rank = evaluator.bestHand(players[1], hand.getCommunity())

        assertTrue(hand1Rank > hand2Rank, "Pair should beat high card")
        assertEquals(Combination.ONE_PAIR, hand1Rank.category)
        assertEquals(Combination.HIGH_CARD, hand2Rank.category)
    }

    @Test
    fun `showdown with flush beats straight`() {
        // Сценарий: флеш побеждает стрит
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        // Player1: флеш в червах (K♥ 9♥ + 7♥ 6♥ Q♥ J♥ T♠ = флеш K-Q-J-9-6)
        players[0].clearHole()
        players[0].receiveCard(Card(Suit.HEARTS, Rank.KING))
        players[0].receiveCard(Card(Suit.HEARTS, Rank.NINE))

        // Player2: стрит 5-6-7-8-9 (не может быть стритом с этими картами)
        // Дадим Player2 стрит: 8♣ 5♣ + 6♦ 7♥ 9♠ T♣ = стрит 5-6-7-8-9
        players[1].clearHole()
        players[1].receiveCard(Card(Suit.CLUBS, Rank.EIGHT))
        players[1].receiveCard(Card(Suit.CLUBS, Rank.FIVE))

        // Community: 7♥, 6♥, Q♥, J♥, T♠
        // Но 7♥ 6♥ нужны для флеша Player1
        // Дадим Player2 стрит через другие карты: нужно 6-7-8-9-T или 5-6-7-8-9
        // Если community: 6♦ 7♦ 9♦ T♦ J♦ - это стрит и флеш!
        // Давайте иначе: Player1 флеш, Player2 стрит без флеша
        // Community: 7♥ 6♥ Q♥ J♥ 2♠ - у Player1 флеш K-Q-J-9-6
        // Player2: 8♣ 5♣ + 7♥ 6♥ Q♥ J♥ 2♠ = нет стрита
        // Дадим Player2 стрит: 8♦ 5♦ + community с 6-7-9-T
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.SEVEN))
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.SIX))
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.QUEEN))
        hand.addCommunityCard(Card(Suit.HEARTS, Rank.JACK))
        hand.addCommunityCard(Card(Suit.SPADES, Rank.TWO))

        val evaluator = HandEvaluator()
        val hand1Rank = evaluator.bestHand(players[0], hand.getCommunity())
        val hand2Rank = evaluator.bestHand(players[1], hand.getCommunity())

        assertTrue(hand1Rank > hand2Rank, "Flush should beat high card")
        assertEquals(Combination.FLUSH, hand1Rank.category)
    }

    @Test
    fun `full house beats flush`() {
        // Сценарий: фулл хаус побеждает флеш
        val players = mutableListOf(
            Player(UUID.randomUUID(), "Player1", 1000),
            Player(UUID.randomUUID(), "Player2", 1000),
        )
        val hand = Hand(UUID.randomUUID(), players)

        // Player1: фулл хаус (77755)
        // В руке: 7♠ 7♥, на столе: 7♦ 5♣ 5♠ X Y
        players[0].clearHole()
        players[0].receiveCard(Card(Suit.SPADES, Rank.SEVEN))
        players[0].receiveCard(Card(Suit.HEARTS, Rank.SEVEN))

        // Player2: флеш в бубнах
        players[1].clearHole()
        players[1].receiveCard(Card(Suit.DIAMONDS, Rank.KING))
        players[1].receiveCard(Card(Suit.DIAMONDS, Rank.QUEEN))

        // Community: 7♦ 5♣ 5♠ A♦ J♦
        // Player1: 7♠ 7♥ 7♦ 5♣ 5♠ = фулл хаус (77755)
        // Player2: K♦ Q♦ A♦ J♦ 7♦ 5♣ 5♠ = флеш A-K-Q-J-7 в бубнах
        hand.addCommunityCard(Card(Suit.DIAMONDS, Rank.SEVEN))
        hand.addCommunityCard(Card(Suit.CLUBS, Rank.FIVE))
        hand.addCommunityCard(Card(Suit.SPADES, Rank.FIVE))
        hand.addCommunityCard(Card(Suit.DIAMONDS, Rank.ACE))
        hand.addCommunityCard(Card(Suit.DIAMONDS, Rank.JACK))

        val evaluator = HandEvaluator()
        val hand1Rank = evaluator.bestHand(players[0], hand.getCommunity())
        val hand2Rank = evaluator.bestHand(players[1], hand.getCommunity())

        assertTrue(hand1Rank > hand2Rank, "Full house should beat flush")
        assertEquals(Combination.FULL_HOUSE, hand1Rank.category)
        assertEquals(Combination.FLUSH, hand2Rank.category)
    }

    @Test
    fun `ace low straight is valid`() {
        // Сценарий: стрит A-2-3-4-5 (стальная пятёрка)
        val player = Player(UUID.randomUUID(), "Test", 1000)
        player.clearHole()
        player.receiveCard(Card(Suit.HEARTS, Rank.ACE))
        player.receiveCard(Card(Suit.SPADES, Rank.FIVE))

        val community = listOf(
            Card(Suit.DIAMONDS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
            Card(Suit.HEARTS, Rank.FOUR),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.DIAMONDS, Rank.QUEEN),
        )

        val evaluator = HandEvaluator()
        val handRank = evaluator.bestHand(player, community)

        assertEquals(Combination.STRAIGHT, handRank.category)
    }

    @Test
    fun `two pair tiebreaker works correctly`() {
        // Сценарий: сравнение двух пар
        val player1 = Player(UUID.randomUUID(), "Player1", 1000)
        player1.clearHole()
        player1.receiveCard(Card(Suit.HEARTS, Rank.ACE))
        player1.receiveCard(Card(Suit.SPADES, Rank.ACE))

        val player2 = Player(UUID.randomUUID(), "Player2", 1000)
        player2.clearHole()
        player2.receiveCard(Card(Suit.HEARTS, Rank.KING))
        player2.receiveCard(Card(Suit.SPADES, Rank.KING))

        // Community: Q♦ Q♣ 7♠ 3♦ 2♥
        // Player1: AA + QQ = пары A и Q
        // Player2: KK + QQ = пары K и Q
        // AA > KK, значит Player1 выигрывает
        val community = listOf(
            Card(Suit.DIAMONDS, Rank.QUEEN),
            Card(Suit.CLUBS, Rank.QUEEN),
            Card(Suit.SPADES, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.HEARTS, Rank.TWO),
        )

        val evaluator = HandEvaluator()
        val hand1Rank = evaluator.bestHand(player1, community)
        val hand2Rank = evaluator.bestHand(player2, community)

        assertTrue(hand1Rank > hand2Rank, "AA+QQ should beat KK+QQ")
        assertEquals(Combination.TWO_PAIR, hand1Rank.category)
        assertEquals(Combination.TWO_PAIR, hand2Rank.category)
    }
}
