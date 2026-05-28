interface Logger {
    fun info(msg: String)
    fun error(msg: String)
}

class ConsoleLogger : Logger {
    override fun info(msg: String) {
        println("[INFO] $msg")
    }

    override fun error(msg: String) {
        println("[ERROR] $msg")
    }
}
