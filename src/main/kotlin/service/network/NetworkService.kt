package service.network

import edu.udo.cs.sopra.ntf.InitMessage
import edu.udo.cs.sopra.ntf.PhaseTwoMessage
import edu.udo.cs.sopra.ntf.SelectedRegionCardMessage
import edu.udo.cs.sopra.ntf.SelectedStartingHandMessage
import entity.*
import service.AbstractRefreshingService
import service.GameService
import service.RootService
import service.bot.MCTSMaxPointsBot
import service.bot.RandomBot

/**
 * Service responsible for managing network connections and online gameplay.
 *
 * @param root The [RootService] that this service is part of.
 */
@Suppress("TooManyFunctions")
class NetworkService(private val root: RootService) : AbstractRefreshingService() {

    /**
     * Reference to the [NetworkClient] if we are currently connected to a server.
     */
    var currentClient: NetworkClient? = null
        private set

    /**
     * The current state of the connection.
     *
     * @see ConnectionState
     */
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    /**
     * List to track selected cards by each player.
     */
    val selectedCardList: MutableList<Pair<String, MutableList<Int>>> = mutableListOf()

    /**
     * Connects to the server and creates a new game session.
     *
     * @param name The name of the hosting player.
     *
     * @throws IllegalStateException if already connected to a server.
     */
    fun hostGame(name: String) {
        check(connectionState == ConnectionState.DISCONNECTED) { "Cannot host game while not disconnected." }

        val success = connect(name, null)
        check(success) { "Couldn't connect to the server." }

        setConnectionState(ConnectionState.CONNECTED)

        val client = checkNotNull(currentClient)

        client.createGame(GAME_ID, "Hello")

        setConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }

    /**
     * Connects to the server and joins the given lobby.
     *
     * @param sessionId The identifier for the session/lobby to join. A.k.a. the "lobby code".
     * @param name The name of the joining player.
     *
     * @throws IllegalStateException if already connected to a server.
     */
    fun joinGame(sessionId: String, name: String, playerType: PlayerType) {
        check(connectionState == ConnectionState.DISCONNECTED) { "Can't join while already connected to a server." }

        val success = connect(name, playerType)
        check(success) { "Couldn't connect to the server." }

        setConnectionState(ConnectionState.CONNECTED)

        val client = checkNotNull(currentClient)
        client.joinGame(sessionId, "Hello")

        setConnectionState(ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
    }

    /**
     * Connects to the server and creates a [NetworkClient] if successful.
     *
     * @param name The name of the player.
     *
     * @return true if the connection was successfully established.
     *
     * @throws IllegalStateException if already connected to a server.
     */
    private fun connect(name: String, playerType: PlayerType?): Boolean {
        check(connectionState == ConnectionState.DISCONNECTED) { "Can't connect while already connected to a server." }
        check(currentClient == null) { "client must be null." }

        val newClient = NetworkClient(
            clientName = name,
            server = SERVER_ADDRESS,
            secret = SECRET,
            networkService = this
        )

        // the host can choose the player type later
        if (playerType != null) {
            newClient.playerType = playerType
        }

        val success = newClient.connect()

        if (success) {
            currentClient = newClient
        }

        return success
    }

    /**
     * Disconnects the [currentClient] from the server if it exists.
     *
     * Can safely be called even if no connection is currently active.
     */
    fun disconnect() {
        // If no client exists, we exit immediately.
        val client = currentClient ?: return

        client.apply {
            if (sessionId != null) leaveGame("Bye")
            if (isOpen) disconnect()
        }
        currentClient = null

        setConnectionState(ConnectionState.DISCONNECTED)
    }

    /**
     * Sets up a local game using [GameService.createGame] and sends the entire starting game state to the
     * other clients connected to the lobby via the [InitMessage].
     *
     * @param isSimple Whether the simple variant of the game should be played.
     * @param randomOrder Whether the player order should be randomized.
     * @param players The list of players participating in the game.
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_GUESTS]
     */
    fun startNewHostedGame(isSimple: Boolean, randomOrder: Boolean, players: List<Player>) {
        check(connectionState == ConnectionState.WAITING_FOR_GUESTS)
        { "Tried to start a game while not in lobby." }

        val client = checkNotNull(currentClient) { "Client was null." }

        // Start the game locally.
        root.gameService.createGame(isSimple, true, randomOrder, players)
        val game = checkNotNull(root.currentGame)

        validateConnectionStateAfterInit(game, client)

        val message = convertGameToInitMessage(game)
        client.sendGameActionMessage(message)

        onAllRefreshables { refreshAfterGameStart() }
    }

    /**
     * Converts a [FarawayGame] into an [InitMessage] for sending to other clients.
     *
     * @param game The [FarawayGame] to convert.
     *
     * @return The resulting [InitMessage].
     */
    fun convertGameToInitMessage(game: FarawayGame): InitMessage {
        val currentGameState = game.currentGameState

        val players = currentGameState.players.map { player ->
            player.name to player.hand.map { it.explorationTime - 1 }
        }

        val drawStack = (currentGameState.centerCards + currentGameState.regionDrawStack)
            .map { it.explorationTime - 1 }

        val sanctuaryStack = currentGameState.sanctuaryDrawStack.map { it.cardId }

        return InitMessage(players, drawStack, sanctuaryStack, !game.isSimpleVariant)
    }

    /**
     * Sets up a local game using an [InitMessage] that was received from the [NetworkClient].
     *
     * Bypasses the need to call [GameService.createGame].
     *
     * @param message The received [InitMessage].
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_INIT]
     */
    internal fun startNewJoinedGame(message: InitMessage) {
        check(connectionState == ConnectionState.WAITING_FOR_INIT)
        { "Tried to start a game while not in lobby." }

        val client = checkNotNull(currentClient) { "Client was null." }
        val playerType = checkNotNull(client.playerType) { "Client player type was null." }

        // Read the message and transform it into a complete entity layer.
        val game = convertInitMessageToGame(message, client.playerName, playerType)

        game.currentGameState.players.forEach {
            when (it.playerType) {
                PlayerType.BOT_EASY -> game.bots[it.name] = (RandomBot(it.name, game.currentGameState))
                PlayerType.BOT_HARD -> game.bots[it.name] = (MCTSMaxPointsBot(it.name, game.currentGameState))
                else -> {}
            }
        }

        // Set the currently active game here
        root.currentGame = game

        validateConnectionStateAfterInit(game, client)

        onAllRefreshables { refreshAfterGameStart() }
    }

    /**
     * Converts an [InitMessage] into a [FarawayGame] representing the game state described by the message.
     *
     * @param message The [InitMessage] to convert.
     * @param clientName The name of the local client.
     * @param type The type of the local client. 0 = local, 1 = bot easy, 2 = bot hard.
     *
     * @return The resulting [FarawayGame].
     */
    fun convertInitMessageToGame(
        message: InitMessage,
        clientName: String,
        type: PlayerType
    ): FarawayGame {

        val sanctuaryById: MutableMap<Int, SanctuaryCard> =
            root.gameService.loadSanctuaryCards().associateByTo(mutableMapOf()) { it.cardId }

        val sanctuaryDrawStack = mutableListOf<SanctuaryCard>()
        for (id in message.sanctuaryStack) {
            val card = sanctuaryById.remove(id)
                ?: error("Sanctuary card with ID $id not found.")
            sanctuaryDrawStack.add(card)
        }

        val regionByExplorationTime: MutableMap<Int, RegionCard> =
            root.gameService.loadRegionCards().associateByTo(mutableMapOf()) { it.explorationTime }

        // "convert" IDs to exploration times
        val drawStackExplorationTimes = message.drawStack.map { it + 1 }

        val regionDrawStack = mutableListOf<RegionCard>()
        for (explorationTime in drawStackExplorationTimes) {
            val card = regionByExplorationTime.remove(explorationTime)
                ?: error("Region card with ID $explorationTime not found.")
            regionDrawStack.add(card)
        }

        val centerCards = mutableListOf<RegionCard>()
        repeat(message.players.size + 1) {
            centerCards.add(regionDrawStack.removeFirst())
        }

        // Create player list.
        val players = mutableListOf<Player>()
        for ((playerName, handIds) in message.players) {
            val isClient = playerName == clientName

            val playerType = when {
                !isClient -> PlayerType.REMOTE
                else -> type
            }

            val player = Player(playerName, playerType)

            for (cardId in handIds) {
                val card = regionByExplorationTime.remove(cardId + 1)
                    ?: error("Region card with ID $cardId not found (or duplicated in message).")
                player.hand.add(card)
            }

            players.add(player)
        }

        require(regionByExplorationTime.isEmpty()) { "Not all region cards were used." }
        require(sanctuaryById.isEmpty()) { "Not all sanctuary cards were used." }

        val gameState = GameState(
            players = players,
            regionDrawStack = regionDrawStack,
            sanctuaryDrawStack = sanctuaryDrawStack,
            centerCards = centerCards,
        )

        return FarawayGame(isOnline = true, isSimpleVariant = !message.isAdvanced).apply {
            currentGameState = gameState
        }
    }

    /**
     * Validates and sets the appropriate [connectionState] after initializing a game.
     *
     * @param game The initialized [FarawayGame].
     * @param client The [NetworkClient] used for the connection.
     */
    private fun validateConnectionStateAfterInit(game: FarawayGame, client: NetworkClient) {
        if (!game.isSimpleVariant) {
            println("Switching to SELECT_ADVANCED_MODE_CARDS")
            setConnectionState(ConnectionState.SELECT_ADVANCED_MODE_CARDS)
        } else {
            val currentPlayer = game.currentGameState.players[game.currentGameState.currentPlayer]
            if (currentPlayer.name == client.playerName) {
                println("Switching to PLAYING_SELECT_REGION_CARD")
                setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
            } else {
                println("Switching to PLAYING_WAITING_FOR_OPPONENT")
                setConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
            }
        }
    }


    /**
     * Sends a [SelectedStartingHandMessage] to the server containing the selected starting hand.
     *
     * @param regionCard The list of [RegionCard]s selected by the player.
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.SELECT_ADVANCED_MODE_CARDS]
     */
    fun sendSelectedStartingHandMessage(regionCard: List<RegionCard>) {
        require(connectionState == ConnectionState.SELECT_ADVANCED_MODE_CARDS)
        { "Cannot send SelectedStartingHandMessage while not in SELECT_ADVANCED_MODE_CARDS state." }

        val client = checkNotNull(currentClient) { "Client not initialized." }

        val ids = regionCard.map { it.explorationTime - 1 }

        val message = SelectedStartingHandMessage(ids.take(3))

        client.sendGameActionMessage(message)

        selectedCardList.add(client.playerName to ids.toMutableList())
    }

    /**
     * Receives a [SelectedStartingHandMessage] from the server and updates the local game state accordingly.
     *
     * @param message The received [SelectedStartingHandMessage].
     * @param sender The name of the player who sent the message.
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.SELECT_ADVANCED_MODE_CARDS]
     */
    fun receiveSelectedStartingHandMessage(
        message: SelectedStartingHandMessage,
        sender: String,
    ) {
        require(connectionState == ConnectionState.SELECT_ADVANCED_MODE_CARDS)
        { "Unexpected SelectedStartingHandMessage." }

        selectedCardList.add(sender to message.selectedCards.toMutableList())
    }

    /**
     * Validates and sets the appropriate [connectionState] after all players have selected their starting hand.
     *
     * @param game The current [FarawayGame].
     */
    private fun validateConnectionStateAfterStaringHand(game: FarawayGame) {

        val participants = game.currentGameState.players.size

        if (selectedCardList.size == participants) {
            val client = checkNotNull(currentClient) { "Client not initialized." }
            val currentPlayer = game.currentGameState.players[0]

            if (client.playerName == currentPlayer.name) {
                println("Switching to PLAYING_SELECT_REGION_CARD")
                setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
                when (currentPlayer.playerType) {
                    PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                        val bot = checkNotNull(game.bots[currentPlayer.name])
                        val chosenCard = bot.exploreRegion(currentPlayer.hand)
                        root.playerActionService.playRegionCard(chosenCard)
                    }
                    else -> {}
                }
            } else {
                println("Switching to PLAYING_WAITING_FOR_OPPONENT")
                setConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
            }
        } else {
            println("Switching to SELECT_ADVANCED_MODE_CARDS")
            setConnectionState(ConnectionState.SELECT_ADVANCED_MODE_CARDS)
        }
    }

    /**
     * Waits for all players to select their starting hand within the specified timeout.
     *
     * @param game The current [FarawayGame].
     * @param timeout The maximum time to wait in milliseconds. Default is 10000 ms (10 seconds).
     *
     * @throws IllegalStateException if not all players have selected their starting hand within the timeout.
     */
    fun waitForAllPlayersToSelectStartingHand(game: FarawayGame, timeout: Int = 10000) {
        println("Waiting for 10 seconds")
        var timePassed = 0
        while (timePassed < timeout) {
            if (selectedCardList.size == game.currentGameState.players.size) {
                game.currentGameState.currentPlayer = 0
                recalculatedStartingHands()
                validateConnectionStateAfterStaringHand(game)
                onAllRefreshables { refreshBeforePlayRegionCard() }
                return
            } else {
                Thread.sleep(100)
                timePassed += 100
            }
        }
        error( "Timeout while waiting for all players to select starting hand." )
    }

    /**
     * Recalculates the starting hands of all players based on the selections stored in [selectedCardList].
     *
     * @throws IllegalStateException if the local player's starting hand is not found in [selectedCardList].
     * @throws IllegalStateException if not all players have 5 cards in hand after recalculation.
     */
    fun recalculatedStartingHands() {
        val game = checkNotNull(root.currentGame)
        val client = checkNotNull(currentClient)

        val localIndex = selectedCardList.indexOfFirst { it.first == client.playerName }
        require(localIndex != -1) { "Local player's starting hand not found in list." }

        val discardedCards = mutableListOf<Int>()
        repeat(2) {
            discardedCards.add(selectedCardList[localIndex].second.removeLast())
        }
        discardedCards.reverse()

        val cardsToAddToLocalPlayer = discardedCards.map { cardId ->
            game.currentGameState.regionDrawStack.find { it.explorationTime == cardId + 1 }
                ?: error("Region card with explorationTime ${cardId + 1} not found in region draw stack.")
        }

        game.currentGameState.regionDrawStack.removeAll(cardsToAddToLocalPlayer)
        val localPlayer = game.currentGameState.players.first { it.playerType != PlayerType.REMOTE }
        localPlayer.hand.addAll(cardsToAddToLocalPlayer)
        check(game.currentGameState.players.all { it.hand.size == 5 })
        { "Not all players have 5 cards in hand after starting hand selection." }


        for (infos in selectedCardList) {
            val player = game.currentGameState.players.find { it.name == infos.first }
            checkNotNull(player) { "No player with name ${infos.first} found." }

            val cards = infos.second.map { cardId ->
                player.hand.find { it.explorationTime == cardId + 1 }
                    ?: error("Region card with explorationTime ${cardId + 1} not found in player hand.")
            }

            val discardedCards = player.hand.filter { !cards.contains(it) }
            player.hand.retainAll(cards)
            game.currentGameState.regionDrawStack.addAll(discardedCards)
        }
    }

    /**
     * Sends a [SelectedRegionCardMessage] to the server containing the selected region card.
     *
     * @param selectedRegionCard The [RegionCard] selected by the player.
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.PLAYING_SELECT_REGION_CARD]
     */
    fun sendSelectedRegionCardMessage(selectedRegionCard: RegionCard) {
        require(connectionState == ConnectionState.PLAYING_SELECT_REGION_CARD)
        { "Cannot send SelectedRegionCardMessage while not in PLAYING_SELECT_REGION_CARD state." }

        val client = checkNotNull(currentClient) { "Client not initialized." }

        val message = SelectedRegionCardMessage(selectedRegionCard.explorationTime - 1)

        client.sendGameActionMessage(message)
    }

    /**
     * Receives a [SelectedRegionCardMessage] from the server and updates the local game state accordingly.
     *
     * @param message The received [SelectedRegionCardMessage].
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.PLAYING_WAITING_FOR_OPPONENT]
     */
    fun receiveSelectedRegionCardMessage(message: SelectedRegionCardMessage) {
        require(connectionState == ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        { "Unexpected SelectedRegionCardMessage." }

        val game = checkNotNull(root.currentGame) { "No game is currently running" }
        val currentPlayer = game.currentGameState.players[game.currentGameState.currentPlayer]

        val card = currentPlayer.hand.find { it.explorationTime == message.selectedCard + 1 }
        checkNotNull(card) { "Selected card ID does not match player's hand." }

        root.playerActionService.playRegionCard(card)
    }


    /**
     * Sends a [PhaseTwoMessage] to the server containing the selected sanctuary and region cards.
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.PLAYING_PHASE_TWO]
     */
    fun sendPhaseTwoMessage() {
        require(connectionState == ConnectionState.PLAYING_PHASE_TWO)
        { "Cannot send PhaseTwoMessage while not in PLAYING_PHASE_TWO state." }

        val game = checkNotNull(root.currentGame) { "No game is currently running" }

        val client = checkNotNull(currentClient) { "Client not initialized." }

        val currentPlayer = game.currentGameState.players[game.currentGameState.currentPlayer]

        var selectedSanctuaryCard: Int? = null
        if (game.currentGameState.sendSanctuaryCard) {
            selectedSanctuaryCard = currentPlayer.sanctuaries.last().cardId
            game.currentGameState.sendSanctuaryCard = false
        }

        var selectedRegionCard: Int? = null
        if (game.currentGameState.currentRound != 8) {
            // the last card in hand is always the selected region card in phase two
            selectedRegionCard = currentPlayer.hand.last().explorationTime - 1
        }

        val message = PhaseTwoMessage(
            selectedSanctuaryCard = selectedSanctuaryCard,
            selectedRegionCard = selectedRegionCard
        )

        client.sendGameActionMessage(message)
    }

    /**
     * Receives a [PhaseTwoMessage] from the server and updates the local game state accordingly.
     *
     * @param message The received [PhaseTwoMessage].
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.PLAYING_WAITING_FOR_OPPONENT]
     */
    fun receivePhaseTwoMessage(message: PhaseTwoMessage) {
        require(connectionState == ConnectionState.PLAYING_WAITING_FOR_OPPONENT) { "Unexpected PhaseTwoMessage." }

        val game = checkNotNull(root.currentGame) { "No game is currently running" }

        message.selectedSanctuaryCard?.let { cardId ->
            for (player in game.currentGameState.players) {
                game.currentGameState.sanctuaryDrawStack.addAll(player.temporarySanctuaries)
                player.temporarySanctuaries.clear()
            }
            val card = game.currentGameState.sanctuaryDrawStack.find { it.cardId == cardId }
            checkNotNull(card) { "Selected sanctuary card ID does not match sanctuary draw stack." }

            game.currentGameState.sanctuaryDrawStack.remove(card)
            game.currentGameState.players[game.currentGameState.currentPlayer].temporarySanctuaries.add(card)

            root.playerActionService.chooseSanctuaryCard(card)
        }

        if (game.currentGameState.currentRound != 8) {
            val cardId = checkNotNull(message.selectedRegionCard) { "Region card can only be null in the last round" }
            val regionCard = game.currentGameState.centerCards.find { it.explorationTime == cardId + 1 }
            checkNotNull(regionCard) { "Selected region card ID does not match player's region cards." }

            root.playerActionService.chooseRegionCard(regionCard)
        } else {
            root.playerActionService.proceedAfterChooseSanctuaryCard()
        }
    }

    /**
     * Validates and sets the appropriate [connectionState] after a game action message has been processed.
     *
     * @param game The current [FarawayGame].
     */
    fun validateConnectionStateAfterGameMessage(game: FarawayGame) {

        val newCurrentPlayer = game.currentGameState.players[game.currentGameState.currentPlayer]

        val client = checkNotNull(currentClient) { "Client not initialized." }

        if (newCurrentPlayer.name == client.playerName) {
            if (game.currentGameState.isPhaseTwo) {
                println("Switching to PLAYING_PHASE_TWO")
                setConnectionState(ConnectionState.PLAYING_PHASE_TWO)
            } else {
                println("Switching to PLAYING_SELECT_REGION_CARD")
                setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
            }
        }
        else {
            println("Switching to PLAYING_WAITING_FOR_OPPONENT")
            setConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        }
    }

    /**
     * Sets the [connectionState] to the given value and notifies refreshables.
     *
     * @param newState The new [ConnectionState] for [connectionState].
     */
    fun setConnectionState(newState: ConnectionState) {
        connectionState = newState
        onAllRefreshables { refreshAfterConnectionStateChange(newState) }
    }

    /**
     * Notifies all refreshables that the lobby has been updated.
     */
    internal fun onLobbyUpdate() {
        onAllRefreshables { refreshAfterLobbyUpdated() }
    }

    /**
     * Constants for server connection.
     */
    companion object {

        /** Address of the server. */
        const val SERVER_ADDRESS = "sopra.cs.tu-dortmund.de:80/bgw-net/connect"

        /** Name of the game as registered on the server. */
        const val GAME_ID = "FarAway"

        /** Secret to connect to the server. */
        const val SECRET = "SoFarAway"
    }
}