class HandEvaluator {
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
                            val rank = HandRank(detectCombination(fiveCards).value, fiveCards.sortedByDescending { it.getRank().value })
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

    fun compare(a: HandRank, b: HandRank): Int = a.compareTo(b)

    private fun detectCombination(cards: List<Card>): Combination {
        val rankCounts = cards.groupingBy { it.getRank().value }.eachCount()
        val isFlush = cards.all { it.getSuit() == cards[0].getSuit() }
        val rankValues = cards.map { it.getRank().value }.sorted()
        val isStraight = isStraight(rankValues)

        val counts = rankCounts.values.sortedDescending()

        return when {
            isFlush && isStraight && rankValues.contains(14) && rankValues.contains(10) ->
                Combination.ROYAL_FLUSH

            isFlush && isStraight ->
                Combination.STRAIGHT_FLUSH

            counts.contains(4) ->
                Combination.FOUR_OF_A_KIND

            counts.contains(3) && counts.contains(2) ->
                Combination.FULL_HOUSE

            isFlush ->
                Combination.FLUSH

            isStraight ->
                Combination.STRAIGHT

            counts.contains(3) ->
                Combination.THREE_OF_A_KIND

            counts.count { it == 2 } == 2 ->
                Combination.TWO_PAIR

            counts.contains(2) ->
                Combination.ONE_PAIR

            else ->
                Combination.HIGH_CARD
        }
    }

    private fun isStraight(ranks: List<Int>): Boolean {
        if (ranks.size < 5) return false
        val unique = ranks.distinct()

        if (unique.size < 5) return false

        // Проверка на обычный стрит
        for (i in 0..unique.size - 5) {
            if (unique[i + 4] - unique[i] == 4) return true
        }

        // Проверка на стрит A-2-3-4-5 (A как 14, но может быть как 1)
        if (unique.contains(14) && unique.contains(2) && unique.contains(3) &&
            unique.contains(4) && unique.contains(5)
        ) {
            return true
        }

        return false
    }
}
