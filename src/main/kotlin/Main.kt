fun main() {
    val logger: Logger = ConsoleLogger()
    val storage: Storage = FileStorage(basePath = "./saves", logger = logger)
    val view: View = ViewCli(logger)
    val controller: Controller = Controller(storage, logger, view)

    controller.startGame()

    while (true) {
        controller.startHand()

        if (!view.askNewHand()) {
            view.printMessage("Thanks for playing!")
            break
        }
    }
}