import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID

interface Storage {
    fun saveGame(g: Game)
    fun loadGame(id: UUID): Game?
}

class FileStorage(
    private val basePath: String = "./saves",
    private val logger: Logger,
) : Storage {
    init {
        File(basePath).mkdirs()
    }

    override fun saveGame(g: Game) {
        val file = File("$basePath/${g.getId()}.dat")
        ObjectOutputStream(file.outputStream()).use { out ->
            out.writeObject(g)
        }
        logger.info("Game saved to ${file.absolutePath}")
    }

    override fun loadGame(id: UUID): Game? {
        val file = File("$basePath/$id.dat")
        if (!file.exists()) {
            logger.error("Game file not found: ${file.absolutePath}")
            return null
        }
        return ObjectInputStream(file.inputStream()).use { inp ->
            inp.readObject() as Game
        }
    }
}
