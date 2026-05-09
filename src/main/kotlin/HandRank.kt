class HandRank(
    val category: Int,
    val cards: List<Card>,
) : Comparable<HandRank> {
    override fun compareTo(other: HandRank): Int {
        val categoryCmp = category.compareTo(other.category)
        if (categoryCmp != 0) return categoryCmp

        val thisRanks = tiebreakRanks()
        val otherRanks = other.tiebreakRanks()

        for (i in thisRanks.indices) {
            val cmp = thisRanks[i].compareTo(otherRanks[i])
            if (cmp != 0) return cmp
        }

        return 0
    }

    private fun tiebreakRanks(): List<Int> {
        val ranks = cards.map { it.getRank().value }
        val counts = ranks.groupingBy { it }.eachCount()

        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
            .map { it.key }
    }
}
