import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.util.UUID

class ViewGui(private val logger: Logger) : View {
    var message by mutableStateOf("Welcome to Texas Hold'em!")
    var currentHand: Hand? by mutableStateOf(null)
    var currentPlayer: Player? by mutableStateOf(null)
    var players by mutableStateOf<List<Player>>(emptyList())
    var communityCards by mutableStateOf<List<Card>>(emptyList())
    var pot by mutableStateOf(0)
    var phase by mutableStateOf(GamePhase.WAITING)
    var dealerIndex by mutableStateOf(-1)
    var smallBlind by mutableStateOf(50)
    var bigBlind by mutableStateOf(100)
    var toCall by mutableStateOf(0)
    var gameState by mutableStateOf(GameState.WAITING)
    var setupPlayers by mutableStateOf<List<Player>>(emptyList())

    var showHoleCards by mutableStateOf(false)
    val handRanks = mutableStateMapOf<UUID, HandRank>()

    var showDialog by mutableStateOf(false)
    var dialogTitle by mutableStateOf("")
    var dialogInput by mutableStateOf("")
    var dialogDefault by mutableStateOf(0)

    private val actionChannel = Channel<Action>(Channel.UNLIMITED)
    private val intChannel = Channel<Int>(Channel.UNLIMITED)
    private val stringChannel = Channel<String>(Channel.UNLIMITED)
    private val booleanChannel = Channel<Boolean>(Channel.UNLIMITED)

    // --- View interface implementation ---

    override fun printWelcome() {
        message = "Welcome to Texas Hold'em!"
        gameState = GameState.WAITING
    }

    override fun printGameInfo(game: Game) {
        players = game.players
        setupPlayers = game.players
        message = "Game started with ${players.size} players"
    }

    override fun printBlindsConfigured(smallBlind: Int, bigBlind: Int) {
        this.smallBlind = smallBlind
        this.bigBlind = bigBlind
        message = "Blinds: $smallBlind/$bigBlind"
    }

    override fun printPlayerSetupHeader(playerNumber: Int) {}

    override fun printHandInfo(hand: Hand) {
        currentHand = hand
        phase = hand.phase
        dealerIndex = hand.getDealerIndex()
        communityCards = hand.getCommunity()
        pot = hand.getPot().getTotal()
        handRanks.clear()
        message = "Phase: ${hand.phase}, Pot: $pot"
    }

    override fun printPlayerOrder(players: List<Player>, dealerIndex: Int, sbIndex: Int, bbIndex: Int) {
        this.dealerIndex = dealerIndex
        this.players = players
    }

    override fun printPlayerTurn(player: Player, isDealer: Boolean) {
        currentPlayer = player
        showHoleCards = false
        message = "${player.name}'s turn${if (isDealer) " (DEALER)" else ""}"
        val contrib = currentHand?.getPot()?.getContributions()?.get(player) ?: 0
        val curBet = currentHand?.getCurrentBet() ?: 0
        toCall = curBet - contrib
    }

    override fun printAvailableActions(hand: Hand, player: Player) {
        val contrib = hand.getPot().getContributions()[player] ?: 0
        val curBet = hand.getCurrentBet()
        toCall = curBet - contrib
    }

    override fun printOnlyOnePlayerLeft() { message = "Only one player left!" }
    override fun printBettingRoundStart(startingPlayer: Player) { message = "Betting round: ${startingPlayer.name}" }
    override fun printBettingRoundComplete() { message = "Betting round complete" }
    override fun printToCall(amount: Int) { toCall = amount }
    override fun printInvalidAction() { message = "Invalid action!" }
    override fun printAllPlayersFolded() { message = "All players folded!"; gameState = GameState.SHOWDOWN }
    override fun printShowdownStart() { gameState = GameState.SHOWDOWN; message = "SHOWDOWN!" }
    override fun printCommunityCards(cards: List<Card>) { communityCards = cards }
    override fun printHandRank(player: Player, rank: HandRank) {
        handRanks[player.getId()] = rank
    }
    override fun printMessage(msg: String) { this.message = msg }
    override fun printError(msg: String) { message = "ERROR: $msg" }
    override fun printPlayerStatus(player: Player) {}
    override fun printAllPlayersStatus(players: List<Player>) {}
    override fun printGameSaved() { message = "Game saved!" }
    override fun printGameLoaded() { message = "Game loaded!" }
    override fun printGameEnded() { gameState = GameState.GAME_OVER; message = "GAME OVER!" }
    override fun printGameAlreadyEnded() { message = "Game already over!" }
    override fun dealFlop() { message = "Dealing Flop..." }
    override fun dealTurn() { message = "Dealing Turn..." }
    override fun dealRiver() { message = "Dealing River..." }

    override fun printWinners(winners: List<Player>) {
        message = "Winners: ${winners.joinToString(", ") { it.name }}"
        gameState = GameState.WAITING
    }

    // --- Blocking input methods ---

    override fun readBlindAmount(blindType: String, default: Int): Int {
        dialogTitle = "Enter $blindType amount"
        dialogDefault = default
        dialogInput = default.toString()
        showDialog = true
        return runBlocking { intChannel.receive() }
    }

    override fun readPlayerName(): String {
        dialogTitle = "Enter player name"
        dialogInput = ""
        showDialog = true
        return runBlocking { stringChannel.receive() }
    }

    override fun readPositiveInt(prompt: String, default: Int): Int {
        dialogTitle = prompt
        dialogDefault = default
        dialogInput = default.toString()
        showDialog = true
        return runBlocking { intChannel.receive() }
    }

    override fun askAddPlayer(): Boolean {
        dialogTitle = "Add another player?"
        showDialog = true
        return runBlocking { booleanChannel.receive() }
    }

    override fun askNewHand(): Boolean {
        dialogTitle = "Start new hand?"
        showDialog = true
        return runBlocking { booleanChannel.receive() }
    }

    override fun readAction(hand: Hand, player: Player): Action {
        currentHand = hand
        currentPlayer = player
        gameState = GameState.PLAYER_TURN
        return runBlocking { actionChannel.receive() }
    }

    override fun readBetAmount(prompt: String, minRaise: Int): Int {
        dialogTitle = prompt
        dialogDefault = minRaise
        dialogInput = minRaise.toString()
        showDialog = true
        return runBlocking { intChannel.receive() }
    }

    // --- UI callbacks ---

    fun submitString(value: String) {
        showDialog = false
        stringChannel.trySend(value)
    }

    fun submitInt(value: Int) {
        showDialog = false
        intChannel.trySend(value)
    }

    fun submitBoolean(value: Boolean) {
        showDialog = false
        booleanChannel.trySend(value)
    }

    fun toggleHoleCards() {
        showHoleCards = !showHoleCards
    }

    fun submitAction(action: Action) {
        gameState = GameState.WAITING
        actionChannel.trySend(action)
    }
}

enum class GameState {
    WAITING, PLAYER_TURN, SHOWDOWN, GAME_OVER
}

// GUI Entry point
fun main() = application {
    val view = ViewGui(ConsoleLogger())
    val logger = ConsoleLogger()
    val storage = FileStorage("./saves", logger)
    val controller = Controller(storage, logger, view)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            controller.startGame()
            while (true) {
                controller.startHand()
                if (!view.askNewHand()) break
            }
            view.printMessage("Thanks for playing!")
        }
    }

    Window(
        onCloseRequest = { exitApplication() },
        title = "Texas Hold'em Poker",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        PokerApp(view)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PokerApp(view: ViewGui) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF003d00))) {
        TopAppBar(
            title = { Text("Texas Hold'em Poker", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1a1a1a),
                titleContentColor = Color.White
            )
        )

        when (view.gameState) {
            GameState.WAITING -> {
                if (view.setupPlayers.isEmpty()) {
                    SetupWaitingScreen()
                } else {
                    PokerTable(view)
                }
            }
            GameState.PLAYER_TURN, GameState.SHOWDOWN -> {
                PokerTable(view)
            }
            GameState.GAME_OVER -> {
                GameOverScreen(view)
            }
        }
    }

    if (view.showDialog) {
        InputDialog(view)
    }
}

@Composable
fun SetupWaitingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Setting up game...", fontSize = 24.sp, color = Color.White)
    }
}

@Composable
fun InputDialog(view: ViewGui) {
    val isBoolean = view.dialogTitle.endsWith("?")
    val isString = !isBoolean && view.dialogTitle.startsWith("Enter player name")
    var text by remember { mutableStateOf(view.dialogInput) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(view.dialogTitle) },
        text = {
            if (isBoolean) {
                Text("Please choose an option:", color = Color.Gray)
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isString) "Name" else "Amount") }
                )
            }
        },
        confirmButton = {
            if (isBoolean) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { view.submitBoolean(true) }) { Text("Yes") }
                    Button(onClick = { view.submitBoolean(false) }) { Text("No") }
                }
            } else {
                Button(onClick = {
                    if (isString) {
                        if (text.isNotBlank()) view.submitString(text)
                    } else {
                        val value = text.toIntOrNull()
                        if (value != null && value > 0) view.submitInt(value)
                    }
                }) { Text("OK") }
            }
        }
    )
}

@Composable
fun PokerTable(view: ViewGui) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val isShowdown = view.gameState == GameState.SHOWDOWN
            view.players.filter { it.getStatus() != PlayerStatus.OUT }.forEach { player ->
                val isThisPlayer = player == view.currentPlayer
                val show = isShowdown || (isThisPlayer && view.showHoleCards)
                PlayerCard(player, isDealer = view.dealerIndex == view.players.indexOf(player), showCards = show, onCardClick = if (isThisPlayer && !isShowdown) {{ view.toggleHoleCards() }} else null, isCurrentPlayer = isThisPlayer, handRank = view.handRanks[player.getId()])
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pot: ${view.pot}", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                view.communityCards.forEach { card ->
                    CardView(card)
                }
                repeat(5 - view.communityCards.size) {
                    CardPlaceholder()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(view.message, fontSize = 40.sp, color = Color.White)
        }

        Column(
            modifier = Modifier.fillMaxWidth().height(450.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            view.currentPlayer?.let { player ->
                val show = view.gameState == GameState.SHOWDOWN || view.showHoleCards
                PlayerCard(player, isDealer = view.dealerIndex == view.players.indexOf(player), showCards = show, onCardClick = { view.toggleHoleCards() }, isCurrentPlayer = true, handRank = view.handRanks[player.getId()])
            }
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons(view)
        }
    }
}

@Composable
fun ActionButtons(view: ViewGui) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton("Fold", Color.Red) { view.submitAction(Action.FOLD) }
        if (view.toCall > 0) {
            ActionButton("Call ${view.toCall}", Color.Blue) { view.submitAction(Action.CALL) }
        } else {
            ActionButton("Check", Color.Gray) { view.submitAction(Action.CHECK) }
        }
        ActionButton("Bet", Color.Green) { view.submitAction(Action.BET) }
        ActionButton("All-in", Color(0xFFFF6347)) { view.submitAction(Action.ALL_IN) }
    }
}

@Composable
fun PlayerCard(player: Player, isDealer: Boolean, showCards: Boolean = false, onCardClick: (() -> Unit)? = null, isCurrentPlayer: Boolean = false, handRank: HandRank? = null) {
    val borderMod = if (isCurrentPlayer) Modifier.border(3.dp, Color.Yellow, RoundedCornerShape(8.dp)) else Modifier
    Card(
        modifier = Modifier.width(280.dp).height(230.dp).then(borderMod).then(
            if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
        ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color(0xFF2D2D2D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                if (isDealer) Text(" D", color = Color.Yellow, fontSize = 24.sp)
                handRank?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("- ${formatHandRank(it)}", fontSize = 16.sp, color = Color.Cyan)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Chips: ${player.getStack()}", fontSize = 24.sp, color = Color(0xFFFFD700))
            if (player.getHole().isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showCards) {
                        player.getHole().forEach { card ->
                            MiniCard(card)
                        }
                    } else {
                        repeat(player.getHole().size) {
                            CardBack()
                        }
                    }
                }
            }
        }
    }
}

fun formatHandRank(rank: HandRank): String {
    val rankNames = mapOf(
        Combination.HIGH_CARD to "High Card",
        Combination.ONE_PAIR to "Pair",
        Combination.TWO_PAIR to "Two Pair",
        Combination.THREE_OF_A_KIND to "Three of a Kind",
        Combination.STRAIGHT to "Straight",
        Combination.FLUSH to "Flush",
        Combination.FULL_HOUSE to "Full House",
        Combination.FOUR_OF_A_KIND to "Four of a Kind",
        Combination.STRAIGHT_FLUSH to "Straight Flush",
        Combination.ROYAL_FLUSH to "Royal Flush",
    )
    val combo = Combination.values().firstOrNull { it.value == rank.category.value }
    return combo?.let { rankNames[it] } ?: "Unknown"
}

@Composable
fun CardBack() {
    Box(
        modifier = Modifier.width(96.dp).height(136.dp).background(Color(0xFF1a3a8a), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontSize = 40.sp, color = Color.White)
    }
}

@Composable
fun CardView(card: Card) {
    Card(
        modifier = Modifier.width(140.dp).height(200.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val color = when (card.suit) {
                Suit.HEARTS, Suit.DIAMONDS -> Color.Red
                else -> Color.Black
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.rank.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = color)
                Text(card.suit.toString(), fontSize = 48.sp, color = color)
            }
        }
    }
}

@Composable
fun MiniCard(card: Card) {
    Box(
        modifier = Modifier.width(96.dp).height(136.dp).background(Color.White, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        val color = when (card.suit) {
            Suit.HEARTS, Suit.DIAMONDS -> Color.Red
            else -> Color.Black
        }
        Text(card.suit.toString(), fontSize = 40.sp, color = color)
    }
}

@Composable
fun CardPlaceholder() {
    Card(
        modifier = Modifier.width(140.dp).height(200.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color(0xFF3D3D3D))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("?", fontSize = 48.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(180.dp).height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontSize = 24.sp)
    }
}

@Composable
fun GameOverScreen(view: ViewGui) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER!", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
            Spacer(modifier = Modifier.height(16.dp))
            Text(view.message, fontSize = 24.sp, color = Color.White)
        }
    }
}
