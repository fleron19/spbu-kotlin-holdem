class Pot {
    private var total: Int = 0
    private val contributions: MutableMap<Player, Int> = mutableMapOf()
    private val sidePots: MutableList<Pot> = mutableListOf()

    fun add(player: Player, amount: Int) {
        TODO()
    }

    fun buildSidePots() {
        TODO()
    }

    fun distributeWinners(winners: List<Player>): Map<Player, Int> {
        TODO()
    }
}
