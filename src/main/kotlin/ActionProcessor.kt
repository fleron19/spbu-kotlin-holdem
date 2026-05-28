class ActionProcessor(private val logger: Logger = ConsoleLogger()) {

    fun validate(player: Player, action: Action, amount: Int, hand: Hand): Boolean {
        when (action) {
            Action.FOLD -> return player.getStatus() != PlayerStatus.FOLDED
            Action.CHECK -> {
                val playerContribution = hand.getPot().getContributions()[player] ?: 0
                return player.getStatus() != PlayerStatus.FOLDED &&
                    playerContribution >= hand.getCurrentBet()
            }
            Action.CALL -> {
                val playerContribution = hand.getPot().getContributions()[player] ?: 0
                return player.getStatus() != PlayerStatus.FOLDED &&
                    playerContribution < hand.getCurrentBet() &&
                    player.getStack() > 0
            }
            Action.BET, Action.RAISE -> {
                return player.getStatus() != PlayerStatus.FOLDED &&
                    amount > 0 &&
                    player.getStack() >= amount
            }
            Action.ALL_IN -> {
                return player.getStatus() != PlayerStatus.FOLDED &&
                    player.getStack() > 0
            }
        }
    }

    fun execute(hand: Hand, player: Player, action: Action, amount: Int): Boolean {
        if (!validate(player, action, amount, hand)) {
            logger.error("Invalid action: $action for player ${player.name}")
            return false
        }

        when (action) {
            Action.FOLD -> {
                player.setStatus(PlayerStatus.FOLDED)
                logger.info("${player.name} folds")
            }
            Action.CHECK -> {
                logger.info("${player.name} checks")
            }
            Action.CALL -> {
                val playerContribution = hand.getPot().getContributions()[player] ?: 0
                val callAmount = minOf(hand.getCurrentBet() - playerContribution, player.getStack())
                val actualBet = player.bet(callAmount)
                hand.getPot().add(player, actualBet)
                logger.info("${player.name} calls $callAmount")
            }
            Action.BET, Action.RAISE -> {
                val actualBet = player.bet(amount)
                hand.getPot().add(player, actualBet)
                val newContribution = hand.getPot().getContributions()[player] ?: 0
                if (newContribution > hand.getCurrentBet()) {
                    hand.setCurrentBet(newContribution)
                }
                logger.info("${player.name} ${if (action == Action.BET) "bets" else "raises"} $actualBet")
            }
            Action.ALL_IN -> {
                val allInAmount = player.getStack()
                val actualBet = player.bet(allInAmount)
                hand.getPot().add(player, actualBet)
                val newContribution = hand.getPot().getContributions()[player] ?: 0
                if (newContribution > hand.getCurrentBet()) {
                    hand.setCurrentBet(newContribution)
                }
                player.setStatus(PlayerStatus.ALL_IN)
                logger.info("${player.name} goes all-in with $actualBet")
            }
        }

        return true
    }
}
