class HandRank(
    val category: Combination,
    val cards: List<Card>,
) : Comparable<HandRank> {
    override fun compareTo(other: HandRank): Int {
        val categoryCmp = category.compareTo(other.category)
        if (categoryCmp != 0) return categoryCmp

        // Тейкбрейк в зависимости от комбинации
        val thisKey = tiebreakKey()
        val otherKey = other.tiebreakKey()

        for (i in thisKey.indices) {
            val cmp = thisKey[i].compareTo(otherKey[i])
            if (cmp != 0) return cmp
        }

        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is HandRank) return false
        return category.value == other.category.value && compareTo(other) == 0
    }

    override fun hashCode(): Int {
        var result = category.value
        result = 31 * result + cards.hashCode()
        return result
    }

    private fun tiebreakKey(): List<Int> {
        val ranks = cards.map { it.rank.value }
        val counts = ranks.groupingBy { it }.eachCount()

        return when (category.value) {
            Combination.ROYAL_FLUSH.value, Combination.STRAIGHT_FLUSH.value, Combination.STRAIGHT.value -> {
                // Для стритов и стрит-флашей - самая высокая карта стрита
                // A-2-3-4-5 имеет высокую карту 5, остальные - высокую карту
                val unique = ranks.distinct().sorted()
                // Проверяем на A-2-3-4-5
                if (unique.contains(14) && unique.contains(2) && unique.contains(3) &&
                    unique.contains(4) && unique.contains(5)
                ) {
                    listOf(5) // A-2-3-4-5 считается как стрит с высокой 5
                } else {
                    listOf(unique.maxOrNull() ?: 0)
                }
            }
            Combination.FOUR_OF_A_KIND.value -> {
                // Кварды, затем кикер
                val quadRank = counts.entries.first { it.value == 4 }.key
                val kicker = ranks.filter { it != quadRank }.maxOrNull() ?: 0
                listOf(quadRank, kicker)
            }
            Combination.FULL_HOUSE.value -> {
                // Тройка, затем пара
                val tripRank = counts.entries.first { it.value == 3 }.key
                val pairRank = counts.entries.first { it.value == 2 }.key
                listOf(tripRank, pairRank)
            }
            Combination.FLUSH.value -> {
                // Все карты по убыванию
                ranks.sortedDescending()
            }
            Combination.THREE_OF_A_KIND.value -> {
                // Тройка, затем кикеры
                val tripRank = counts.entries.first { it.value == 3 }.key
                val kickers = ranks.filter { it != tripRank }.sortedDescending()
                listOf(tripRank) + kickers
            }
            Combination.TWO_PAIR.value -> {
                // Старшая пара, младшая пара, кикер
                val pairs = counts.entries.filter { it.value == 2 }.sortedByDescending { it.key }
                val highPair = pairs[0].key
                val lowPair = pairs[1].key
                val kicker = ranks.filter { it != highPair && it != lowPair }.maxOrNull() ?: 0
                listOf(highPair, lowPair, kicker)
            }
            Combination.ONE_PAIR.value -> {
                // Пара, затем кикеры
                val pairRank = counts.entries.first { it.value == 2 }.key
                val kickers = ranks.filter { it != pairRank }.sortedDescending()
                listOf(pairRank) + kickers
            }
            else -> {
                // High card - все карты по убыванию
                ranks.sortedDescending()
            }
        }
    }
}
