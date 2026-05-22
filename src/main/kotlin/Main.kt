fun main() {
    val logger = Logger()
    val storage = FileStorage()
    val view = View(logger)
    val controller = Controller(storage, logger, view)

    controller.startGame()

    while (true) {
        controller.startHand()

        if (!view.askNewHand()) {
            view.printMessage("Thanks for playing!")
            break
        }
    }
}
