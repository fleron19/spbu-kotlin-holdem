class Pot {
    private var total: Int = 0
    private val contributions: MutableMap<Player, Int> = mutableMapOf()
    private val sidePots: MutableList<Pot> = mutableListOf()

    fun add(player: Player, amount: Int) {
        val actualBet = minOf(amount, player.getStack())
        if (actualBet > 0) {
            contributions[player] = (contributions[player] ?: 0) + actualBet
            total += actualBet
        }
    }

    fun getTotal(): Int = total

    fun getContributions(): Map<Player, Int> = contributions

    fun buildSidePots() {
        // Упрощённая версия - пока без side pots для простоты
        // В полной версии нужно сортировать игроков по стеку и создавать side pots
        sidePots.clear()
        val sortedPlayers = contributions.entries.sortedBy { it.value }
        if (sortedPlayers.size < 2) return

        var previousAmount = 0
        for ((i, entry) in sortedPlayers.withIndex()) {
            if (entry.value > previousAmount) {
                val sidePot = Pot()
                for ((p, amt) in contributions) {
                    val sideAmount = minOf(amt - previousAmount, entry.value - previousAmount)
                    if (sideAmount > 0) {
                        sidePot.add(p, sideAmount)
                    }
                }
                sidePots.add(sidePot)
                previousAmount = entry.value
            }
        }
    }

    fun distributeWinners(winners: List<Player>): Map<Player, Int> {
        if (winners.isEmpty()) return emptyMap()
        if (sidePots.isEmpty()) {
            val share = total / winners.size
            return winners.associateWith { share }
        }

        // Распределение по side pots
        val result = mutableMapOf<Player, Int>()
        var previousAmount = 0
        for ((idx, sidePot) in sidePots.withIndex()) {
            val activeWinners = winners.filter { contributions[it] ?: 0 > previousAmount }
            if (activeWinners.isNotEmpty()) {
                val share = sidePot.total / activeWinners.size
                for (w in activeWinners) {
                    result[w] = (result[w] ?: 0) + share
                }
            }
            previousAmount = sidePot.contributions.values.minOrNull() ?: previousAmount
        }
        return result
    }

    fun reset() {
        total = 0
        contributions.clear()
        sidePots.clear()
    }
}
