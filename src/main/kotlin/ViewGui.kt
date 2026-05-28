import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
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
    var gameName by mutableStateOf("")
    var gameStarted by mutableStateOf(false)

    var showHoleCards by mutableStateOf(false)
    var showdownRevealed by mutableStateOf(false)
    private val _handRanks = mutableMapOf<UUID, HandRank>()
    var handRanks by mutableStateOf<Map<UUID, HandRank>>(emptyMap())

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
        _handRanks.clear()
        showdownRevealed = false
        handRanks = emptyMap()
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
    override fun printAllPlayersFolded() { message = "All players folded!"; gameState = GameState.SHOWDOWN; showdownRevealed = true; _handRanks.clear(); handRanks = emptyMap() }
    override fun printShowdownStart() { gameState = GameState.SHOWDOWN; message = "SHOWDOWN!"; showdownRevealed = true; _handRanks.clear(); handRanks = emptyMap() }
    override fun printCommunityCards(cards: List<Card>) { communityCards = cards }
    override fun printHandRank(player: Player, rank: HandRank) {
        _handRanks[player.getId()] = rank
        handRanks = _handRanks.toMap()
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
        val result = runBlocking { intChannel.receive() }
        showDialog = false
        return result
    }

    override fun readPlayerName(): String {
        dialogTitle = "Enter player name"
        dialogInput = ""
        showDialog = true
        val result = runBlocking { stringChannel.receive() }
        showDialog = false
        return result
    }

    override fun readPositiveInt(prompt: String, default: Int): Int {
        dialogTitle = prompt
        dialogDefault = default
        dialogInput = default.toString()
        showDialog = true
        val result = runBlocking { intChannel.receive() }
        showDialog = false
        return result
    }

    override fun askAddPlayer(): Boolean {
        dialogTitle = "Add another player?"
        showDialog = true
        val result = runBlocking { booleanChannel.receive() }
        showDialog = false
        return result
    }

    override fun askNewHand(): Boolean {
        dialogTitle = "Start new hand?"
        showDialog = true
        val result = runBlocking { booleanChannel.receive() }
        showDialog = false
        return result
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
        val result = runBlocking { intChannel.receive() }
        showDialog = false
        return result
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

    Window(
        onCloseRequest = { exitApplication() },
        title = "Texas Hold'em Poker",
        state = rememberWindowState(width = 1920.dp, height = 1200.dp)
    ) {
        PokerApp(view, controller)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PokerApp(view: ViewGui, controller: Controller) {
    if (view.gameStarted) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                controller.startGame()
                while (true) {
                    controller.startHand()
                    if (view.gameState == GameState.GAME_OVER) break
                    if (!view.askNewHand()) break
                }
                view.printMessage("Thanks for playing!")
            }
        }
    }

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
                    GameSetupScreen(view)
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

    if (view.showDialog && view.setupPlayers.isNotEmpty()) {
        InputDialog(view)
    }
}

data class SetupPlayer(val id: Int, val name: String, val stack: Int)

@Composable
fun GameSetupScreen(view: ViewGui) {
    var gameNameInput by remember { mutableStateOf("") }
    var sbText by remember { mutableStateOf("50") }
    var bbText by remember { mutableStateOf("100") }
    var players by remember { mutableStateOf(listOf<SetupPlayer>()) }

    val isValid = gameNameInput.isNotBlank() &&
            sbText.toIntOrNull() != null && sbText.toIntOrNull()!! > 0 &&
            bbText.toIntOrNull() != null && bbText.toIntOrNull()!! > 0 &&
            players.size >= 2 && players.all { it.name.isNotBlank() && it.stack > 0 }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0d1b2a)), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth(0.5f).padding(top = 40.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Game Setup", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = gameNameInput,
                onValueChange = { gameNameInput = it },
                label = { Text("Game Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White, focusedLabelColor = Color(0xFF4CAF50), unfocusedLabelColor = Color.Gray
                )
            )
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = sbText, onValueChange = { sbText = it.filter { c -> c.isDigit() } },
                    label = { Text("Small Blind") }, singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White, focusedLabelColor = Color(0xFF4CAF50), unfocusedLabelColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = bbText, onValueChange = { bbText = it.filter { c -> c.isDigit() } },
                    label = { Text("Big Blind") }, singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White, focusedLabelColor = Color(0xFF4CAF50), unfocusedLabelColor = Color.Gray
                    )
                )
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Players (${players.size}/10)", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(16.dp))
                if (players.size < 10) {
                    OutlinedButton(
                        onClick = { players = players + SetupPlayer(players.size + 1, "", 1000) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) { Text("+ Add", fontSize = 18.sp) }
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                players.forEachIndexed { idx, p ->
                    Card(
                        colors = CardDefaults.cardColors(Color(0xFF1b2838)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${p.id}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50),
                                modifier = Modifier.width(40.dp))
                            OutlinedTextField(
                                value = p.name,
                                onValueChange = { newName ->
                                    players = players.mapIndexed { i, pl -> if (i == idx) pl.copy(name = newName) else pl }
                                },
                                label = { Text("Name") }, singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color.White, focusedLabelColor = Color(0xFF4CAF50), unfocusedLabelColor = Color.Gray
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = p.stack.toString(),
                                onValueChange = { newStack ->
                                    val v = newStack.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                    players = players.mapIndexed { i, pl -> if (i == idx) pl.copy(stack = v) else pl }
                                },
                                label = { Text("Stack") }, singleLine = true,
                                modifier = Modifier.width(120.dp),
                                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color.White, focusedLabelColor = Color(0xFF4CAF50), unfocusedLabelColor = Color.Gray
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                players = players.filterIndexed { i, _ -> i != idx }
                                    .mapIndexed { i, pl -> SetupPlayer(i + 1, pl.name, pl.stack) }
                            }) {
                                Icon(Icons.Default.Close, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            if (players.size < 2) {
                Spacer(Modifier.height(8.dp))
                Text("Minimum 2 players required", fontSize = 16.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    view.gameName = gameNameInput
                    view.submitInt(sbText.toInt())
                    view.submitInt(bbText.toInt())
                    players.forEachIndexed { i, p ->
                        view.submitString(p.name)
                        view.submitInt(p.stack)
                        view.submitBoolean(i < players.size - 1)
                    }
                    view.gameStarted = true
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("Start Game", fontSize = 26.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(40.dp))
        }
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
            val showAll = view.gameState == GameState.SHOWDOWN || view.showdownRevealed
            view.players.filter { it.getStatus() != PlayerStatus.OUT && it.getStatus() != PlayerStatus.FOLDED }.forEach { player ->
                val isThisPlayer = player == view.currentPlayer
                val show = showAll || (isThisPlayer && view.showHoleCards)
                PlayerCard(player, isDealer = view.dealerIndex == view.players.indexOf(player), showCards = show, onCardClick = if (isThisPlayer && !showAll) {{ view.toggleHoleCards() }} else null, isCurrentPlayer = isThisPlayer, handRank = view.handRanks[player.getId()])
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pot: ${view.pot}", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
            Spacer(modifier = Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                view.communityCards.forEach { card ->
                    CardView(card)
                }
                repeat(5 - view.communityCards.size) {
                    CardPlaceholder()
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(view.message, fontSize = 52.sp, color = Color.White)
        }

        Column(
            modifier = Modifier.fillMaxWidth().height(600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            view.currentPlayer?.let { player ->
                val show = view.gameState == GameState.SHOWDOWN || view.showdownRevealed || view.showHoleCards
                PlayerCard(player, isDealer = view.dealerIndex == view.players.indexOf(player), showCards = show, onCardClick = { view.toggleHoleCards() }, isCurrentPlayer = true, handRank = view.handRanks[player.getId()])
            }
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons(view)
        }
    }
}

@Composable
fun ActionButtons(view: ViewGui) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    val borderMod = if (isCurrentPlayer) Modifier.border(4.dp, Color.Yellow, RoundedCornerShape(12.dp)) else Modifier
    Card(
        modifier = Modifier.width(350.dp).height(290.dp).then(borderMod).then(
            if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFF2D2D2D))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Color.White)
                if (isDealer) {
                    Box(
                        modifier = Modifier.padding(start = 12.dp).background(Color.Yellow, RoundedCornerShape(6.dp)).padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("D", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.Black)
                    }
                }
                handRank?.let {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("- ${formatHandRank(it)}", fontSize = 22.sp, color = Color.Cyan)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Chips: ${player.getStack()}", fontSize = 30.sp, color = Color(0xFFFFD700))
            if (player.getHole().isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        modifier = Modifier.width(120.dp).height(170.dp).background(Color(0xFF1a3a8a), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontSize = 50.sp, color = Color.White)
    }
}

@Composable
fun CardView(card: Card) {
    Card(
        modifier = Modifier.width(180.dp).height(260.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val color = when (card.suit) {
                Suit.HEARTS, Suit.DIAMONDS -> Color.Red
                else -> Color.Black
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.rank.toString(), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = color)
                Text(card.suit.toString(), fontSize = 60.sp, color = color)
            }
        }
    }
}

@Composable
fun MiniCard(card: Card) {
    Box(
        modifier = Modifier.width(120.dp).height(170.dp).background(Color.White, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        val color = when (card.suit) {
            Suit.HEARTS, Suit.DIAMONDS -> Color.Red
            else -> Color.Black
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.rank.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = color)
            Text(card.suit.toString(), fontSize = 36.sp, color = color)
        }
    }
}

@Composable
fun CardPlaceholder() {
    Card(
        modifier = Modifier.width(180.dp).height(260.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFF3D3D3D))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("?", fontSize = 60.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(240.dp).height(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
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
