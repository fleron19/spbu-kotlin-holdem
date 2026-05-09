import java.util.UUID

interface Storage {
    fun saveGame(g: Game)
    fun loadGame(id: UUID): Game
}
