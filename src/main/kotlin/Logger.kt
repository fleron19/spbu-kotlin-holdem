class Logger {
    fun log(level: String, msg: String) {
        println("[$level] $msg")
    }

    fun info(msg: String) {
        log("INFO", msg)
    }

    fun error(msg: String) {
        log("ERROR", msg)
    }
}
