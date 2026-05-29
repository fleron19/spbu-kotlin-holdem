import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.system.exitProcess

private val suitSymbol: (Suit) -> String = {
    when (it) {
        Suit.SPADES -> "\u2660"
        Suit.HEARTS -> "\u2665"
        Suit.DIAMONDS -> "\u2666"
        Suit.CLUBS -> "\u2663"
    }
}
private val suitColor: (Suit) -> Color = {
    if (it == Suit.HEARTS || it == Suit.DIAMONDS) Color(0xFFE53935) else Color.Black
}

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

    var setupDealerIndex by mutableStateOf(-1)

    var setupGameNameInput by mutableStateOf("")
    var setupSBText by mutableStateOf("50")
    var setupBBText by mutableStateOf("100")
    var setupPlayersList by mutableStateOf<List<SetupPlayer>>(emptyList())
    var setupFormInited by mutableStateOf(false)

    var lastGameName by mutableStateOf("")
    var lastSB by mutableStateOf(50)
    var lastBB by mutableStateOf(100)
    var lastPlayers by mutableStateOf<List<PersistedPlayer>>(emptyList())
    var showExitDialog by mutableStateOf(false)
    var showSaveOverwriteDialog by mutableStateOf(false)
    var pendingSaveGameName by mutableStateOf("")
    var showLoadErrorDialog by mutableStateOf(false)
    var pendingLoadErrorName by mutableStateOf("")
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

    override fun printOnlyOnePlayerLeft() {
        message = "Only one player left!"
    }
    override fun printBettingRoundStart(startingPlayer: Player) {
        message = "Betting round: ${startingPlayer.name}"
    }
    override fun printBettingRoundComplete() {
        message = "Betting round complete"
    }
    override fun printToCall(amount: Int) {
        toCall = amount
    }
    override fun printInvalidAction() {
        message = "Invalid action!"
    }
    override fun printAllPlayersFolded() {
        message = "All players folded!"
        gameState = GameState.SHOWDOWN
        showdownRevealed = true
        _handRanks.clear()
        handRanks = emptyMap()
    }
    override fun printShowdownStart() {
        gameState = GameState.SHOWDOWN
        message = "SHOWDOWN!"
        showdownRevealed = true
        _handRanks.clear()
        handRanks = emptyMap()
    }
    override fun printCommunityCards(cards: List<Card>) {
        communityCards = cards
    }
    override fun printHandRank(player: Player, rank: HandRank) {
        _handRanks[player.getId()] = rank
        handRanks = _handRanks.toMap()
    }
    override fun printMessage(msg: String) {
        this.message = msg
    }
    override fun printError(msg: String) {
        message = "ERROR: $msg"
    }
    override fun printPlayerStatus(player: Player) {}
    override fun printAllPlayersStatus(players: List<Player>) {}
    override fun printGameSaved() {
        message = "Game saved!"
    }
    override fun printGameLoaded() {
        message = "Game loaded!"
    }
    override fun printGameEnded() {
        gameState = GameState.GAME_OVER
        message = "GAME OVER!"
    }
    override fun printGameAlreadyEnded() {
        message = "Game already over!"
    }
    override fun dealFlop() {
        message = "Dealing Flop..."
    }
    override fun dealTurn() {
        message = "Dealing Turn..."
    }
    override fun dealRiver() {
        message = "Dealing River..."
    }

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
    WAITING,
    PLAYER_TURN,
    SHOWDOWN,
    GAME_OVER,
}

// GUI Entry point
fun main() = application {
    val view = ViewGui(ConsoleLogger())
    val logger = ConsoleLogger()
    val storage = FileStorage("./saves", logger)
    val controller = Controller(storage, logger, view)

    Window(
        onCloseRequest = { view.showExitDialog = true },
        title = "Texas Hold'em Poker",
        state = rememberWindowState(width = 1920.dp, height = 1200.dp),
    ) {
        pokerApp(view, controller)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun pokerApp(view: ViewGui, controller: Controller) {
    val sqliteStorage = remember { SqliteStorage() }

    if (view.gameStarted) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                controller.startGame()
                if (view.setupDealerIndex >= 0) {
                    controller.getGame()?.setDealer(view.setupDealerIndex)
                }
                while (true) {
                    controller.startHand()
                    if (view.gameState == GameState.GAME_OVER) break
                    if (!view.askNewHand()) break
                }
                view.lastGameName = view.gameName
                view.lastSB = view.smallBlind
                view.lastBB = view.bigBlind
                view.lastPlayers = view.players.mapIndexed { i, p ->
                    PersistedPlayer(p.name, p.getStack(), i == view.dealerIndex)
                }
                view.setupPlayers = emptyList()
                view.setupFormInited = false
                view.gameStarted = false
                view.printMessage("Thanks for playing!")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A))) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { view.showExitDialog = true }) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            },
            title = {
                val titleText = if (view.gameName.isNotBlank()) view.gameName else "Texas Hold'em Poker"
                Text("\u2660  $titleText  \u2663", color = Color.White)
            },
            actions = {
                TextButton(onClick = {
                    val name = view.setupGameNameInput
                    if (name.isBlank()) {
                        view.message = "Enter a game name first"
                        return@TextButton
                    }
                    val existing = sqliteStorage.loadGameSetup(name)
                    if (existing != null) {
                        view.pendingSaveGameName = name
                        view.showSaveOverwriteDialog = true
                    } else {
                        sqliteStorage.saveGameSetup(
                            name,
                            view.setupPlayersList.map { it.name },
                            view.setupPlayersList.map { it.stack },
                            view.setupPlayersList.indexOfFirst { it.isDealer }.coerceAtLeast(0),
                            view.setupSBText.toIntOrNull() ?: 0,
                            view.setupBBText.toIntOrNull() ?: 0,
                        )
                        view.message = "Game saved!"
                    }
                }) {
                    Text("Save", color = Color.White, fontSize = 18.sp)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    val name = view.setupGameNameInput
                    if (name.isBlank()) {
                        view.message = "Enter a game name to load"
                        return@TextButton
                    }
                    val data = sqliteStorage.loadGameSetup(name)
                    if (data != null) {
                        view.setupGameNameInput = data.gameName
                        view.setupSBText = data.sb.toString()
                        view.setupBBText = data.bb.toString()
                        view.setupPlayersList = data.playerNames.mapIndexed { i, pn ->
                            SetupPlayer(i + 1, pn, data.playerStacks[i], isDealer = i == data.dealerIndex)
                        }
                        view.message = "Game loaded!"
                    } else {
                        view.pendingLoadErrorName = name
                        view.showLoadErrorDialog = true
                    }
                }) {
                    Text("Load", color = Color.White, fontSize = 18.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1a1a1a),
                titleContentColor = Color.White,
            ),
        )

        when (view.gameState) {
            GameState.WAITING -> {
                if (view.setupPlayers.isEmpty()) {
                    if (!view.setupFormInited) {
                        view.setupGameNameInput = view.lastGameName
                        view.setupSBText = view.lastSB.toString()
                        view.setupBBText = view.lastBB.toString()
                        view.setupPlayersList = if (view.lastPlayers.isNotEmpty()) {
                            view.lastPlayers.mapIndexed { i, p -> SetupPlayer(i + 1, p.name, p.stack, p.isDealer) }
                        } else {
                            emptyList()
                        }
                        view.setupFormInited = true
                    }
                    gameSetupScreen(view)
                } else {
                    pokerTable(view)
                }
            }
            GameState.PLAYER_TURN, GameState.SHOWDOWN -> {
                pokerTable(view)
            }
            GameState.GAME_OVER -> {
                gameOverScreen(view)
            }
        }
    }

    if (view.showDialog && view.setupPlayers.isNotEmpty()) {
        inputDialog(view)
    }

    if (view.showExitDialog) {
        AlertDialog(
            onDismissRequest = { view.showExitDialog = false },
            containerColor = Color(0xFF1B2838),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFBDBDBD),
            iconContentColor = Color.White,
            title = { Text("Exit", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to exit? Unsaved data will not be saved.",
                    color = Color(0xFFBDBDBD),
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { exitProcess(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    ) { Text("Yes") }
                    Button(
                        onClick = { view.showExitDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161)),
                    ) { Text("No") }
                }
            },
        )
    }

    if (view.showSaveOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { view.showSaveOverwriteDialog = false },
            containerColor = Color(0xFF1B2838),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFBDBDBD),
            title = { Text("Overwrite?") },
            text = {
                Text("Game '${view.pendingSaveGameName}' already exists. Overwrite?", color = Color(0xFFBDBDBD))
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val name = view.pendingSaveGameName
                            sqliteStorage.saveGameSetup(
                                name,
                                view.setupPlayersList.map { it.name },
                                view.setupPlayersList.map { it.stack },
                                view.setupPlayersList.indexOfFirst { it.isDealer }.coerceAtLeast(0),
                                view.setupSBText.toIntOrNull() ?: 0,
                                view.setupBBText.toIntOrNull() ?: 0,
                            )
                            view.showSaveOverwriteDialog = false
                            view.message = "Game saved!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    ) { Text("Yes") }
                    Button(
                        onClick = { view.showSaveOverwriteDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161)),
                    ) { Text("No") }
                }
            },
        )
    }

    if (view.showLoadErrorDialog) {
        AlertDialog(
            onDismissRequest = { view.showLoadErrorDialog = false },
            containerColor = Color(0xFF1B2838),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFBDBDBD),
            title = { Text("Game not found") },
            text = {
                Text("Game '${view.pendingLoadErrorName}' not found!", color = Color(0xFFBDBDBD))
            },
            confirmButton = {
                Button(
                    onClick = { view.showLoadErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                ) { Text("OK") }
            },
        )
    }
}

data class SetupPlayer(val id: Int, val name: String, val stack: Int, val isDealer: Boolean = false)
data class PersistedPlayer(val name: String, val stack: Int, val isDealer: Boolean)

@Composable
fun gameSetupScreen(view: ViewGui) {
    val isValid = view.setupGameNameInput.isNotBlank() &&
        view.setupSBText.toIntOrNull() != null && view.setupSBText.toIntOrNull()!! > 0 &&
        view.setupBBText.toIntOrNull() != null && view.setupBBText.toIntOrNull()!! > 0 &&
        view.setupPlayersList.size >= 2 && view.setupPlayersList.all { it.name.isNotBlank() && it.stack > 0 }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A)), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth(0.5f).padding(top = 40.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header with suit decorations
            Text(
                "\u2660 \u2665 \u2663 \u2666",
                fontSize = 28.sp,
                color = Color(0xFF4CAF50),
                letterSpacing = 8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Game Setup",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Configure your poker game",
                fontSize = 18.sp,
                color = Color(0xFF9E9E9E),
            )
            Spacer(Modifier.height(32.dp))

            // Form card
            Card(
                colors = CardDefaults.cardColors(Color(0xFF1B2838)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = view.setupGameNameInput,
                        onValueChange = { view.setupGameNameInput = it },
                        label = { Text("Game Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF555555),
                            cursorColor = Color.White,
                            focusedLabelColor = Color(0xFF4CAF50),
                            unfocusedLabelColor = Color(0xFF9E9E9E),
                        ),
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = view.setupSBText,
                            onValueChange = { view.setupSBText = it.filter { c -> c.isDigit() } },
                            label = { Text("Small Blind") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF555555),
                                cursorColor = Color.White,
                                focusedLabelColor = Color(0xFF4CAF50),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                            ),
                        )
                        OutlinedTextField(
                            value = view.setupBBText,
                            onValueChange = { view.setupBBText = it.filter { c -> c.isDigit() } },
                            label = { Text("Big Blind") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF555555),
                                cursorColor = Color.White,
                                focusedLabelColor = Color(0xFF4CAF50),
                                unfocusedLabelColor = Color(0xFF9E9E9E),
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            // Players section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Players", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("(${view.setupPlayersList.size}/10)", fontSize = 20.sp, color = Color(0xFF9E9E9E))
                Spacer(Modifier.width(16.dp))
                if (view.setupPlayersList.size < 10) {
                    OutlinedButton(
                        onClick = {
                            val newId = view.setupPlayersList.size + 1
                            view.setupPlayersList = view.setupPlayersList + SetupPlayer(newId, "", 1000, isDealer = view.setupPlayersList.isEmpty())
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                    ) { Text("+ Add Player", fontSize = 16.sp) }
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                view.setupPlayersList.forEachIndexed { idx, p ->
                    Card(
                        colors = CardDefaults.cardColors(Color(0xFF1B2838)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Number badge
                            Box(
                                modifier = Modifier.width(36.dp).height(36.dp)
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${p.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            // Dealer toggle
                            Box(
                                modifier = Modifier.width(36.dp).height(36.dp)
                                    .background(
                                        if (p.isDealer) Color(0xFFFFD700) else Color(0xFF555555),
                                        RoundedCornerShape(18.dp),
                                    ).clickable {
                                        view.setupPlayersList = view.setupPlayersList.mapIndexed { i, pl -> pl.copy(isDealer = i == idx) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "D",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (p.isDealer) Color.Black else Color.White,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            OutlinedTextField(
                                value = p.name,
                                onValueChange = { newName ->
                                    view.setupPlayersList = view.setupPlayersList.mapIndexed { i, pl -> if (i == idx) pl.copy(name = newName) else pl }
                                },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color(0xFF555555),
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color(0xFF4CAF50),
                                    unfocusedLabelColor = Color(0xFF9E9E9E),
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = p.stack.toString(),
                                onValueChange = { newStack ->
                                    val v = newStack.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                    view.setupPlayersList = view.setupPlayersList.mapIndexed { i, pl -> if (i == idx) pl.copy(stack = v) else pl }
                                },
                                label = { Text("Stack") },
                                singleLine = true,
                                modifier = Modifier.width(120.dp),
                                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color(0xFF555555),
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color(0xFF4CAF50),
                                    unfocusedLabelColor = Color(0xFF9E9E9E),
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                val wasDealer = view.setupPlayersList[idx].isDealer
                                val filtered = view.setupPlayersList.filterIndexed { i, _ -> i != idx }
                                view.setupPlayersList = filtered.mapIndexed { i, pl ->
                                    SetupPlayer(
                                        i + 1, pl.name, pl.stack,
                                        isDealer = if (wasDealer) i == 0 else pl.isDealer,
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Close, "Delete", tint = Color(0xFFE53935))
                            }
                        }
                    }
                }
            }

            if (view.setupPlayersList.size < 2) {
                Spacer(Modifier.height(12.dp))
                Text("Minimum 2 players required", fontSize = 16.sp, color = Color(0xFF9E9E9E))
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    view.gameName = view.setupGameNameInput
                    view.setupDealerIndex = view.setupPlayersList.indexOfFirst { it.isDealer }
                    view.submitInt(view.setupSBText.toInt())
                    view.submitInt(view.setupBBText.toInt())
                    view.setupPlayersList.forEachIndexed { i, p ->
                        view.submitString(p.name)
                        view.submitInt(p.stack)
                        view.submitBoolean(i < view.setupPlayersList.size - 1)
                    }
                    view.gameStarted = true
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            ) { Text("Start Game", fontSize = 26.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun inputDialog(view: ViewGui) {
    val isBoolean = view.dialogTitle.endsWith("?")
    val isString = !isBoolean && view.dialogTitle.startsWith("Enter player name")
    var text by remember { mutableStateOf(view.dialogInput) }

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFF1B2838),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFBDBDBD),
        iconContentColor = Color.White,
        title = { Text(view.dialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            if (isBoolean) {
                Text("Please choose an option:", color = Color(0xFFBDBDBD))
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isString) "Name" else "Amount") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = Color.White,
                        focusedLabelColor = Color(0xFF4CAF50),
                        unfocusedLabelColor = Color(0xFF9E9E9E),
                    ),
                )
            }
        },
        confirmButton = {
            if (isBoolean) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { view.submitBoolean(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    ) { Text("Yes") }
                    Button(
                        onClick = { view.submitBoolean(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161)),
                    ) { Text("No") }
                }
            } else {
                Button(
                    onClick = {
                        if (isString) {
                            if (text.isNotBlank()) view.submitString(text)
                        } else {
                            val value = text.toIntOrNull()
                            if (value != null && value > 0) view.submitInt(value)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                ) { Text("OK") }
            }
        },
    )
}

@Composable
fun pokerTable(view: ViewGui) {
    Box(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color(0xFF3E2723), RoundedCornerShape(80.dp))
                .padding(24.dp),
        ) {
            // Felt interior
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFF1B5E20), RoundedCornerShape(64.dp))
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Opponent cards row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val showAll = view.gameState == GameState.SHOWDOWN || view.showdownRevealed
                        view.players.filter {
                            it.getStatus() != PlayerStatus.OUT && it.getStatus() != PlayerStatus.FOLDED
                        }.forEach { player ->
                            val isThisPlayer = player == view.currentPlayer
                            val show = showAll || (isThisPlayer && view.showHoleCards)
                            playerCard(
                                player,
                                isDealer = view.dealerIndex == view.players.indexOf(player),
                                showCards = show,
                                onCardClick = if (isThisPlayer && !showAll) {
                                    { view.toggleHoleCards() }
                                } else {
                                    null
                                },
                                isCurrentPlayer = isThisPlayer,
                                handRank = view.handRanks[player.getId()],
                            )
                        }
                    }

                    // Center area
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Pot chip display
                            Box(
                                modifier = Modifier.background(
                                    Color(0x33FFFFFF),
                                    RoundedCornerShape(16.dp),
                                ).padding(horizontal = 32.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    "Pot: ${view.pot}",
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                )
                            }
                            Spacer(Modifier.height(32.dp))

                            // Community cards
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                view.communityCards.forEach { card ->
                                    cardView(card)
                                }
                                repeat(5 - view.communityCards.size) {
                                    cardPlaceholder()
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Message
                            Text(
                                view.message,
                                fontSize = 36.sp,
                                color = Color.White,
                                fontWeight = if (view.gameState == GameState.SHOWDOWN) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }

                    // Bottom area - current player
                    Column(
                        modifier = Modifier.fillMaxWidth().height(480.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        view.currentPlayer?.let { player ->
                            val show = view.gameState == GameState.SHOWDOWN || view.showdownRevealed || view.showHoleCards
                            playerCard(
                                player,
                                isDealer = view.dealerIndex == view.players.indexOf(player),
                                showCards = show,
                                onCardClick = { view.toggleHoleCards() },
                                isCurrentPlayer = true,
                                handRank = view.handRanks[player.getId()],
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        actionButtons(view)
                    }
                }
            }
        }
    }
}

private val actionColors = mapOf(
    Action.FOLD to Color(0xFFD32F2F),
    Action.CALL to Color(0xFF1976D2),
    Action.CHECK to Color(0xFF616161),
    Action.BET to Color(0xFF388E3C),
    Action.ALL_IN to Color(0xFFE64A19),
)

@Composable
fun actionButtons(view: ViewGui) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        pokeractionButton("Fold", Action.FOLD) { view.submitAction(Action.FOLD) }
        if (view.toCall > 0) {
            pokeractionButton("Call ${view.toCall}", Action.CALL) { view.submitAction(Action.CALL) }
        } else {
            pokeractionButton("Check", Action.CHECK) { view.submitAction(Action.CHECK) }
        }
        pokeractionButton("Bet", Action.BET) { view.submitAction(Action.BET) }
        pokeractionButton("All-in", Action.ALL_IN) { view.submitAction(Action.ALL_IN) }
    }
}

@Composable
fun pokeractionButton(text: String, action: Action, onClick: () -> Unit) {
    val color = actionColors[action] ?: Color.Gray
    Button(
        onClick = onClick,
        modifier = Modifier.width(200.dp).height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
    ) {
        Text(text, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun playerCard(player: Player, isDealer: Boolean, showCards: Boolean = false, onCardClick: (() -> Unit)? = null, isCurrentPlayer: Boolean = false, handRank: HandRank? = null) {
    val borderColor = if (isCurrentPlayer) Color(0xFFFFD700) else Color(0xFF555555)
    val borderWidth = if (isCurrentPlayer) 3.dp else 1.dp
    val bg = if (isCurrentPlayer) Color(0xFF2A2A2A) else Color(0xFF1E1E1E)
    Card(
        modifier = Modifier.width(320.dp).height(320.dp).then(
            Modifier.border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
        ).then(
            if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(bg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (isDealer) {
                    Box(
                        modifier = Modifier.background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                    ) {
                        Text("DEALER", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "\uD83D\uDCB0 ${player.getStack()}",
                fontSize = 24.sp,
                color = Color(0xFFFFD700),
            )
            handRank?.let {
                Spacer(Modifier.height(4.dp))
                Text(formatHandRank(it), fontSize = 18.sp, color = Color(0xFF00BCD4), fontWeight = FontWeight.SemiBold)
            }
            if (player.getHole().isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showCards) {
                        player.getHole().forEach { card -> miniCard(card) }
                    } else {
                        repeat(player.getHole().size) { cardBack() }
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
fun cardBack() {
    Box(
        modifier = Modifier.width(100.dp).height(150.dp)
            .background(Color(0xFF1a3a8a), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF0d2860), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("\u2660", fontSize = 48.sp, color = Color(0x44FFFFFF))
    }
}

@Composable
fun cardView(card: Card) {
    val color = suitColor(card.suit)
    val sym = suitSymbol(card.suit)
    Card(
        modifier = Modifier.width(160.dp).height(240.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFFF5F5F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Top-left corner
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.rank.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
                Text(sym, fontSize = 20.sp, color = color)
            }
            // Bottom-right corner (inverted)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).rotate(180f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(card.rank.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
                Text(sym, fontSize = 20.sp, color = color)
            }
            // Center suit
            Text(
                sym,
                fontSize = 80.sp,
                color = color,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
fun miniCard(card: Card) {
    val color = suitColor(card.suit)
    val sym = suitSymbol(card.suit)
    Box(
        modifier = Modifier.width(100.dp).height(150.dp)
            .background(Color(0xFFF5F5F0), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.rank.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
            Text(sym, fontSize = 30.sp, color = color)
        }
    }
}

@Composable
fun cardPlaceholder() {
    Card(
        modifier = Modifier.width(160.dp).height(240.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFF1a1a1a)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2660", fontSize = 48.sp, color = Color(0xFF333333))
        }
    }
}

@Composable
fun actionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(240.dp).height(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun gameOverScreen(view: ViewGui) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u2660 \u2665 \u2663 \u2666", fontSize = 32.sp, color = Color(0xFF4CAF50), letterSpacing = 8.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "GAME OVER",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                view.message,
                fontSize = 28.sp,
                color = Color(0xFFBDBDBD),
            )
        }
    }
}
