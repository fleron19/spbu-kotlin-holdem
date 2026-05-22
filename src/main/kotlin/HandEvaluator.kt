class HandEvaluator {
    fun compare(hand1: HandRank, hand2: HandRank): Int = hand1.compareTo(hand2)

    fun bestHand(player: Player, community: List<Card>): HandRank {
        val allCards = player.getHole() + community
        require(allCards.size == 7) { "Expected exactly 7 cards (2 hole + 5 community)" }

        var bestRank: HandRank? = null

        // Перебор всех комбинаций по 5 карт из 7 (C(7,5) = 21 комбинация)
        for (i in 0 until allCards.size) {
            for (j in i + 1 until allCards.size) {
                for (k in j + 1 until allCards.size) {
                    for (l in k + 1 until allCards.size) {
                        for (m in l + 1 until allCards.size) {
                            val fiveCards = listOf(
                                allCards[i],
                                allCards[j],
                                allCards[k],
                                allCards[l],
                                allCards[m],
                            )
                            val rank = HandRank(detectCombination(fiveCards).value, fiveCards.sortedByDescending { it.rank.value })
                            if (bestRank == null || rank > bestRank) {
                                bestRank = rank
                            }
                        }
                    }
                }
            }
        }

        return bestRank!!
    }

    private fun detectCombination(cards: List<Card>): Combination {
        if (cards.size != 5) throw IllegalArgumentException("Exactly 5 cards required")

        val rankCounts = cards.groupingBy { it.rank.value }.eachCount()
        val isFlush = cards.all { it.suit == cards[0].suit }
        val rankValues = cards.map { it.rank.value }.sorted()

        val isStraight = if (rankValues.toSet().size == 5) {
            rankValues.last() - rankValues.first() == 4 ||
                (rankValues == listOf(2, 3, 4, 5, 14)) // A-5 straight
        } else {
            false
        }

        return when {
            isFlush && isStraight && rankValues.contains(14) && rankValues.contains(13) -> Combination.ROYAL_FLUSH
            isFlush && isStraight -> Combination.STRAIGHT_FLUSH
            rankCounts.containsValue(4) -> Combination.FOUR_OF_A_KIND
            rankCounts.containsValue(3) && rankCounts.containsValue(2) -> Combination.FULL_HOUSE
            isFlush -> Combination.FLUSH
            isStraight -> Combination.STRAIGHT
            rankCounts.containsValue(3) -> Combination.THREE_OF_A_KIND
            rankCounts.values.count { it == 2 } == 2 -> Combination.TWO_PAIR
            rankCounts.containsValue(2) -> Combination.ONE_PAIR
            else -> Combination.HIGH_CARD
        }
    }
}
