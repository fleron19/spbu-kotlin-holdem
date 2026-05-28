class Pot {
    private var total: Int = 0
    private val contributions: MutableMap<Player, Int> = mutableMapOf()
    private val sidePots: MutableList<SidePot> = mutableListOf()

    data class SidePot(
        val eligiblePlayers: MutableSet<Player> = mutableSetOf(),
        var amount: Int = 0,
    )

    fun add(player: Player, amount: Int) {
        if (amount > 0) {
            contributions[player] = (contributions[player] ?: 0) + amount
            total += amount
        }
    }

    fun getTotal(): Int = total

    fun getContributions(): Map<Player, Int> = contributions

    fun getSidePots(): List<SidePot> = sidePots.toList()

    fun buildSidePots(players: List<Player>) {
        sidePots.clear()

        // Сортируем игроков по вкладу в банк (все игроки, включая фолднувших)
        val sortedByContribution = contributions.entries
            .filter { it.value > 0 }
            .sortedBy { it.value }

        if (sortedByContribution.isEmpty()) return

        // Находим все уникальные уровни вкладов
        val uniqueLevels = sortedByContribution.map { it.value }.distinct().sorted()

        var previousAmount = 0
        for (level in uniqueLevels) {
            val sidePot = SidePot()
            val levelAmount = level - previousAmount

            // Игроки на этом уровне и выше
            val playersAtOrAbove = sortedByContribution.filter { it.value >= level }
            val playersEligible = playersAtOrAbove.filter {
                it.key.getStatus() != PlayerStatus.FOLDED
            }.map { it.key }

            // Сумма: разница уровня * количество ВСЕХ игроков на этом уровне и выше
            val totalAmount = levelAmount * playersAtOrAbove.size

            for (p in playersEligible) {
                sidePot.eligiblePlayers.add(p)
            }
            sidePot.amount = totalAmount

            sidePots.add(sidePot)
            previousAmount = level
        }
    }

    fun distributeWinners(winners: List<Player>): Map<Player, Int> {
        if (winners.isEmpty()) return emptyMap()
        if (sidePots.isEmpty()) {
            val share = total / winners.size
            return winners.associateWith { share }
        }

        val result = mutableMapOf<Player, Int>()

        // Распределяем каждый side pot среди победителей, которые в нём участвовали
        for (sidePot in sidePots) {
            val eligibleWinners = winners.filter { sidePot.eligiblePlayers.contains(it) }
            if (eligibleWinners.isNotEmpty()) {
                val share = sidePot.amount / eligibleWinners.size
                for (w in eligibleWinners) {
                    result[w] = (result[w] ?: 0) + share
                }
            }
        }

        return result
    }

    fun reset() {
        total = 0
        contributions.clear()
        sidePots.clear()
    }
}
