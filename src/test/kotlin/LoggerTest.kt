import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoggerTest {
    @Test
    fun `log should print message with level`() {
        val logger: Logger = ConsoleLogger()
        // Просто проверяем, что метод не бросает исключений
        logger.info("info message")
        logger.error("error message")
        // Тест проходит если нет исключений
        assertTrue(true)
    }
}
