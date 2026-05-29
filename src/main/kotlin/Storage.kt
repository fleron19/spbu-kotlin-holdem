import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.sql.DriverManager
import java.util.UUID

interface Storage {
    fun saveGame(g: Game)
    fun loadGame(id: UUID): Game?

    fun saveGameSetup(name: String, playerNames: List<String>, playerStacks: List<Int>, dealerIndex: Int, sb: Int, bb: Int) {
        throw UnsupportedOperationException("saveGameSetup not supported by this storage")
    }

    fun loadGameSetup(name: String): GameSetupData? {
        throw UnsupportedOperationException("loadGameSetup not supported by this storage")
    }
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

data class GameSetupData(
    val gameName: String,
    val playerNames: List<String>,
    val playerStacks: List<Int>,
    val dealerIndex: Int,
    val sb: Int,
    val bb: Int,
)

class SqliteStorage(private val dbPath: String = "./saves/game_setups.db") : Storage {
    private val connection by lazy {
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:$dbPath").apply {
            createStatement().executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS game_setups (
                    game_name TEXT PRIMARY KEY,
                    player1_name TEXT DEFAULT '',
                    player2_name TEXT DEFAULT '',
                    player3_name TEXT DEFAULT '',
                    player4_name TEXT DEFAULT '',
                    player5_name TEXT DEFAULT '',
                    player6_name TEXT DEFAULT '',
                    player7_name TEXT DEFAULT '',
                    player8_name TEXT DEFAULT '',
                    player9_name TEXT DEFAULT '',
                    player10_name TEXT DEFAULT '',
                    player11_name TEXT DEFAULT '',
                    player1_stack INTEGER DEFAULT -1,
                    player2_stack INTEGER DEFAULT -1,
                    player3_stack INTEGER DEFAULT -1,
                    player4_stack INTEGER DEFAULT -1,
                    player5_stack INTEGER DEFAULT -1,
                    player6_stack INTEGER DEFAULT -1,
                    player7_stack INTEGER DEFAULT -1,
                    player8_stack INTEGER DEFAULT -1,
                    player9_stack INTEGER DEFAULT -1,
                    player10_stack INTEGER DEFAULT -1,
                    player11_stack INTEGER DEFAULT -1,
                    dealer_index INTEGER DEFAULT -1,
                    sb INTEGER,
                    bb INTEGER
                )
                """.trimIndent(),
            )
        }
    }

    override fun saveGameSetup(name: String, playerNames: List<String>, playerStacks: List<Int>, dealerIndex: Int, sb: Int, bb: Int) {
        val stmt = connection.prepareStatement(
            """
            INSERT OR REPLACE INTO game_setups 
            (game_name, 
             player1_name, player2_name, player3_name, player4_name, player5_name,
             player6_name, player7_name, player8_name, player9_name, player10_name, player11_name,
             player1_stack, player2_stack, player3_stack, player4_stack, player5_stack,
             player6_stack, player7_stack, player8_stack, player9_stack, player10_stack, player11_stack,
             dealer_index, sb, bb)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        )
        stmt.setString(1, name)
        for (i in 0 until 11) {
            if (i < playerNames.size) {
                stmt.setString(2 + i, playerNames[i])
                stmt.setInt(13 + i, playerStacks[i])
            } else {
                stmt.setString(2 + i, "")
                stmt.setInt(13 + i, -1)
            }
        }
        stmt.setInt(24, dealerIndex)
        stmt.setInt(25, sb)
        stmt.setInt(26, bb)
        stmt.executeUpdate()
    }

    override fun loadGameSetup(name: String): GameSetupData? {
        val stmt = connection.prepareStatement("SELECT * FROM game_setups WHERE game_name = ?")
        stmt.setString(1, name)
        val rs = stmt.executeQuery()
        if (!rs.next()) return null

        val playerNames = mutableListOf<String>()
        val playerStacks = mutableListOf<Int>()
        for (i in 0 until 11) {
            val pn = rs.getString(2 + i) ?: ""
            val ps = rs.getInt(13 + i)
            if (pn.isNotEmpty() && ps >= 0) {
                playerNames.add(pn)
                playerStacks.add(ps)
            }
        }
        val dealerIndex = rs.getInt(24)
        val sb = rs.getInt(25)
        val bb = rs.getInt(26)
        return GameSetupData(name, playerNames, playerStacks, dealerIndex, sb, bb)
    }

    override fun saveGame(g: Game) {
        throw UnsupportedOperationException("saveGame not supported by SqliteStorage")
    }

    override fun loadGame(id: UUID): Game? {
        throw UnsupportedOperationException("loadGame not supported by SqliteStorage")
    }
}
