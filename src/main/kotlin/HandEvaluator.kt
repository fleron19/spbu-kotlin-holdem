class HandEvaluator {
    fun bestHand(player: Player, community: List<Card>): HandRank {
        val allCards = player.getHole() + community
        require(allCards.size == 7) { "Expected exactly 7 cards (2 hole + 5 community)" }

        var bestRank: HandRank? = null

        for (i in allCards.indices) {
            for (j in i + 1 until allCards.size) {
                val fiveCards = allCards.filterIndexed { index, _ -> index != i && index != j }
                val rank =
                    HandRank(detectCombination(fiveCards).value, fiveCards.sortedByDescending { it.getRank().value })
                if (bestRank == null || rank > bestRank) {
                    bestRank = rank
                }
            }
        }

        return bestRank!!
    }

    fun compare(a: HandRank, b: HandRank): Int = a.compareTo(b)

    private fun detectCombination(cards: List<Card>): Combination {
        val rankCounts = cards.groupingBy { it.getRank().value }.eachCount()
        val isFlush = cards.all { it.getSuit() == cards[0].getSuit() }
        val isStraight = isStraight(cards.map { it.getRank().value }.toSet())

        val counts = rankCounts.values.toList().sortedDescending()

        return when {
            isFlush && isStraight && cards.any { it.getRank().value == 14 } && cards.any { it.getRank().value == 13 } ->
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

    private fun isStraight(ranks: Set<Int>): Boolean {
        if (ranks.size < 5) return false
        val sorted = ranks.sorted()

        for (i in 0..sorted.size - 5) {
            var consecutive = true
            for (j in i until i + 4) {
                if (sorted[j + 1] - sorted[j] != 1) {
                    consecutive = false
                    break
                }
            }
            if (consecutive) return true
        }

        if (ranks.contains(14) && ranks.contains(2) && ranks.contains(3) &&
            ranks.contains(4) && ranks.contains(5)
        ) {
            return true
        }

        return false
    }
}
