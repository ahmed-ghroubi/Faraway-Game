package gui

import entity.Player
import entity.PlayerType
import service.Refreshable
import service.RootService
import service.network.ConnectionState
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.visual.ColorVisual

/**
 * [MainMenuScene] serves as the primary entry point for game configuration, including
 * player management, game mode selection (online/offline), and rule variants.
 *
 * @param rootService The [RootService] used to access game and network logic layers.
 *
 * @property imageLoader Helper to load graphical assets.
 * @property o Factory helper for creating UI components.
 * @property selectedVariant Index of the currently selected game variant ("Basic" or "Professional").
 * @property selectedSimulation Index of the currently selected simulation speed.
 * @property selectedOrder Index of the currently selected player order mode ("Random" or "Manual").
 * @property online Boolean flag indicating if the session is an online game.
 * @property join Boolean flag indicating if the player is currently joining an existing game.
 * @property variant List of available game variants.
 * @property simulation List of available simulation speeds.
 * @property order List of available player order modes.
 * @property onlineModeButton Button to initiate the online game setup.
 * @property offlineModeButton Button to initiate the offline (hotseat) game setup.
 * @property selectedVariantLabel Label displaying the current game variant.
 * @property previousVariantLabel Button to cycle to the previous game variant.
 * @property nextVariantLabel Button to cycle to the next game variant.
 * @property selectedSimulationLabel Label displaying the current simulation speed.
 * @property previousSimulationLabel Button to cycle to the previous simulation speed.
 * @property nextSimulationLabel Button to cycle to the next simulation speed.
 * @property selectedOrderLabel Label displaying the current player order mode.
 * @property previousOrderLabel Button to cycle to the previous player order mode.
 * @property nextOrderLabel Button to cycle to the next player order mode.
 * @property redArrow The primary action button used to start the game or join a session.
 * @property addressLabel Text field used for displaying or entering the Session ID (IP/Join Code).
 * @property playerLabels UI labels identifying player slots (e.g., "Player 0").
 * @property playerTextField UI text fields displaying the names of added players.
 * @property playerConfigButtons Buttons to edit an existing player's configuration.
 * @property playerAddButtons Buttons to add a new player to an empty slot.
 * @property playerRemoveButtons Buttons to remove a player from a slot.
 * @property playerList The actual list of [Player] entities currently configured for the game.
 */
class MainMenuScene(private val rootService: RootService) : MenuScene(1620, 1080),
    Refreshable {
    private val imageLoader = ImageLoader()
    private val o = Objecting()

    // Online-, Offline Game Mode
    var online = false

    private val onlineModeButton =
        o.button(text = "Online", type = 2, posX = 955, posY = 450).apply {
            onMouseClicked = {
                // Hier dies, das, Ananas machen! Server starten und so.
                online = true
                nextPageEvent()
            }
        }
    private val offlineModeButton =
        o.button(text = "Offline", type = 2, posX = 955, posY = 640).apply {
            onMouseClicked = {
                online = false
                nextPageEvent()
            }
        }
    var join = false

    // Variant-Configuration
    private var selectedVariant = 0
    private val variant = listOf("Basic", "Professional")
    private val selectedVariantLabel =
        o.label(variant[selectedVariant], posX = 1200, posY = 250)
    private val previousVariantLabel =
        o.button(text = "", type = 1, posX = 1200, posY = 250).apply {
            visual = imageLoader.loadButton(2)
            onMouseClicked = {
                selectedVariant--
                if (selectedVariant == -1) selectedVariant = 1
                selectedVariantLabel.text = variant[selectedVariant]
            }
        }
    private val nextVariantLabel =
        o.button(text = "", type = 1, posX = 1460, posY = 250).apply {
            visual = imageLoader.loadButton(1)
            onMouseClicked = {
                selectedVariant++
                if (selectedVariant == 2) selectedVariant = 0
                selectedVariantLabel.text = variant[selectedVariant]
            }
        }

    // Simulation-Configuration
    private var selectedSimulation = 2
    private val simulation = listOf("Zero", "Low", "Normal", "Extreme")
    private val selectedSimulationLabel =
        o.label(simulation[selectedSimulation], posX = 1200, posY = 325)
    private val previousSimulationLabel =
        o.button(text = "", type = 1, posX = 1200, posY = 325).apply {
            visual = imageLoader.loadButton(2)
            onMouseClicked = {
                selectedSimulation--
                if (selectedSimulation == -1) selectedSimulation = 3
                selectedSimulationLabel.text = simulation[selectedSimulation]
            }
        }
    private val nextSimulationLabel =
        o.button(text = "", type = 1, posX = 1460, posY = 325).apply {
            visual = imageLoader.loadButton(1)
            onMouseClicked = {
                selectedSimulation++
                if (selectedSimulation == 4) selectedSimulation = 0
                selectedSimulationLabel.text = simulation[selectedSimulation]
            }
        }

    // Player-Order-Configuration
    private var selectedOrder = 0
    private val order = listOf("Random", "Manual")
    private val selectedOrderLabel =
        o.label(order[selectedOrder], posX = 1200, posY = 400)
    private val previousOrderLabel =
        o.button(text = "", type = 1, posX = 1200, posY = 400).apply {
            visual = imageLoader.loadButton(2)
            onMouseClicked = {
                selectedOrder--
                if (selectedOrder == -1) selectedOrder = 1
                selectedOrderLabel.text = order[selectedOrder]
            }
        }
    private val nextOrderLabel =
        o.button(text = "", type = 1, posX = 1460, posY = 400).apply {
            visual = imageLoader.loadButton(1)
            onMouseClicked = {
                selectedOrder++
                if (selectedOrder == 2) selectedOrder = 0
                selectedOrderLabel.text = order[selectedOrder]
            }
        }

    // Player-Order Page or Start
    private val redArrow =
        o.button(text = "", type = 5, posX = 1450, posY = 970).apply {
            visual = imageLoader.loadButton(5)
            onMouseClicked = {
                if(join){
                    rootService.networkService.joinGame(addressLabel.text, playerList[0].name, playerList[0].playerType)
                    rootService.currentGame?.simulationSpeed = when(simulation[selectedSimulation]) {
                        "Zero" -> 0
                        "Low" -> 500
                        "Normal" -> 1000
                        else -> 2000
                    }
                }else if (playerList.size >= 2 && !online) {
                    rootService.gameService.createGame(
                        isSimple = (selectedVariant == 0),
                        isOnline = false,
                        randomOrder = (selectedOrder == 0),
                        playerList.toList()
                    )
                    rootService.currentGame?.simulationSpeed = when(simulation[selectedSimulation]) {
                        "Zero" -> 0
                        "Low" -> 500
                        "Normal" -> 1000
                        else -> 2000
                    }
                }else if(playerList.size >= 2){
                    rootService.networkService.startNewHostedGame(
                        isSimple = (selectedVariant == 0),
                        randomOrder = (selectedOrder == 0),
                        playerList.toList()
                    )
                    rootService.currentGame?.simulationSpeed = when(simulation[selectedSimulation]) {
                        "Zero" -> 0
                        "Low" -> 500
                        "Normal" -> 1000
                        else -> 2000
                    }
                }
            }
        }

    // Session-ID
    private val addressLabel = o.text(text = "", posX = 1200, posY = 475).apply {
        prompt = "SessionID"
    }

    // Player List
    private val playerLabels: MutableList<Label> = mutableListOf()
    private val playerTextField: MutableList<TextField> = mutableListOf()
    private val playerAddButtons: MutableList<Button> = mutableListOf()
    private val playerConfigButtons: MutableList<Button> = mutableListOf()
    private val playerRemoveButtons: MutableList<Button> = mutableListOf()
    val playerList: MutableList<Player> = mutableListOf()

    // Player Config...
    private val playerTypeList: MutableList<String> =
        mutableListOf("Local", "easy Bot", "hard Bot")
    private val playerOrderList: MutableList<String> =
        mutableListOf("1", "2", "3", "4", "5", "6")
    private val configPlayer =
        Label(posX = 1000, posY = 500, width = 400, height = 300).apply {
            visual = ColorVisual(95, 95, 95)
        }
    private val playerName = o.label("Name:", posX = 940, posY = 525)
    private val playerNameInput = o.text("", posX = 1140, posY = 525)
    private val playerType = o.label("Type:", posX = 940, posY = 575)
    private val playerTypeInput = o.text("", posX = 1140, posY = 575).apply {
        isDisabled = true
    }
    private val playerTypeInputUp = Button(
        posX = 1340,
        posY = 575,
        width = 20,
        height = 20
    ).apply {
        visual = imageLoader.loadButton(3)
        onMouseClicked = {
            playerTypeInput.text = when (playerTypeInput.text) {
                "Local" -> playerTypeList[1]
                "easy Bot" -> playerTypeList[2]
                "hard Bot" -> playerTypeList[0]
                else -> "Remote"
            }
        }
    }
    private val playerTypeInputDown = Button(
        posX = 1340,
        posY = 600,
        width = 20,
        height = 20
    ).apply {
        visual = imageLoader.loadButton(4)
        onMouseClicked = {
            playerTypeInput.text = when (playerTypeInput.text) {
                "Local" -> playerTypeList[2]
                "easy Bot" -> playerTypeList[0]
                "hard Bot" -> playerTypeList[1]
                else -> "Remote"
            }
        }
    }
    private val playerOrder = o.label("Order:", posX = 940, posY = 625)
    private val playerOrderInput =
        o.text("", posX = 1140, posY = 625).apply { isDisabled = true }
    private val playerOrderInputUp = Button(
        posX = 1340,
        posY = 625,
        width = 20,
        height = 20
    ).apply {
        visual = imageLoader.loadButton(3)
        onMouseClicked = {
            var order = playerOrderInput.text.toInt()
            order = if (order > playerList.size) {
                1
            } else if (order == playerList.size) {
                if (addPlayer == true) {
                    playerList.size + 1
                } else {
                    1
                }
            } else {
                order + 1
            }
            playerOrderInput.text = order.toString()
        }
    }
    private val playerOrderInputDown = Button(
        posX = 1340,
        posY = 650,
        width = 20,
        height = 20
    ).apply {
        visual = imageLoader.loadButton(4)
        onMouseClicked = {
            var order = playerOrderInput.text.toInt()
            order = if (order <= 1) {
                playerList.size + if (addPlayer == true) {
                    1
                } else {
                    0
                }
            } else {
                order - 1
            }
            playerOrderInput.text = order.toString()
        }
    }
    private val playerButtonConfirm =
        Button(
            posX = 1025,
            posY = 700,
            width = 150,
            height = 50,
            text = "Confirm"
        ).apply {
            onMouseClicked = {
                if (addPlayer == true) {
                    if (createPlayer(
                            playerNameInput.text,
                            playerTypeInput.text,
                            playerOrderInput.text.toInt()
                        )
                    ) {
                        showPlayerConfig(false)
                        addPlayer = null
                    }
                }
                if (addPlayer == false) {
                    if (changePlayer(
                            playerNameInput.text,
                            playerTypeInput.text,
                            playerOrderInput.text.toInt()
                        )
                    ) {
                        showPlayerConfig(false)
                        addPlayer = null
                    }
                }
            }
        }
    private var addPlayer: Boolean? = null
    private val playerButtonCancel = Button(
        posX = 1225,
        posY = 700,
        width = 150,
        height = 50,
        text = "Cancel"
    ).apply {
        onMouseClicked = {
            addPlayer = null
            showPlayerConfig(false)
        }
    }

    // Load Game
    private val loadGame = o.label("Game1", posX = 1000, posY = 475)

    /**
     * It initializes with an opacity of 0.5 and the headLineLabel, logPanel and backButton.
     */
    init {
        background = imageLoader.loadBackground(7)
        opacity = .5

        initPlayerList()

        playerTextField.forEach { addComponents(it) }
        playerLabels.forEach { addComponents(it) }
        playerConfigButtons.forEach { addComponents(it) }
        playerAddButtons.forEach { addComponents(it) }
        playerRemoveButtons.forEach { addComponents(it) }

        addComponents(
            onlineModeButton,
            offlineModeButton,
            addressLabel,
            redArrow,
            loadGame,
            selectedVariantLabel,
            previousVariantLabel,
            nextVariantLabel,
            selectedSimulationLabel,
            previousSimulationLabel,
            nextSimulationLabel,
            selectedOrderLabel,
            previousOrderLabel,
            nextOrderLabel
        )

        // Player Config.
        addComponents(
            configPlayer,
            playerName,
            playerNameInput,
            playerType,
            playerTypeInput,
            playerTypeInputUp,
            playerTypeInputDown,
            playerOrder,
            playerOrderInput,
            playerOrderInputUp,
            playerOrderInputDown,
            playerButtonConfirm,
            playerButtonCancel
        )
        notVisible()
    }

    /**
     * Sets the visibility of all primary configuration UI components to false.
     * Used to clear the screen before switching between menu pages.
     */
    private fun notVisible() {
        // Page 1
        onlineModeButton.isVisible = false
        offlineModeButton.isVisible = false

        // Page 2
        selectedVariantLabel.isVisible = false
        previousVariantLabel.isVisible = false
        nextVariantLabel.isVisible = false
        selectedSimulationLabel.isVisible = false
        previousSimulationLabel.isVisible = false
        nextSimulationLabel.isVisible = false
        selectedOrderLabel.isVisible = false
        previousOrderLabel.isVisible = false
        nextOrderLabel.isVisible = false
        addressLabel.isVisible = false
        playerTextField.forEach { it.isVisible = false }
        playerLabels.forEach { it.isVisible = false }
        playerConfigButtons.forEach { it.isVisible = false }
        playerAddButtons.forEach { it.isVisible = false }
        playerRemoveButtons.forEach { it.isVisible = false }
        redArrow.isVisible = false
        showPlayerConfig(false)

        // Load Game
        loadGame.isVisible = false
    }

    /**
     * Validates if a player name is not blank, alphanumeric, and not already taken.
     * @param name The name to validate.
     * @return True if the name is valid, false otherwise.
     */
    private fun checkName(name: String): Boolean {
        return name.isNotBlank() &&
                name.all { it.isLetterOrDigit() } &&
                !playerList.any { it.name == name }
    }

    /**
     * Synchronizes the UI player slots with the [playerList]. Updates visibility of
     * add, remove, and config buttons based on the current number of players.
     */
    private fun refreshPlayerList() {
        playerList.forEachIndexed { index, player ->
            playerTextField[index].text = player.name
            playerAddButtons[index].isVisible = false
            playerLabels[index].isVisible = true
            playerTextField[index].isVisible = true
            playerConfigButtons[index].isVisible = true
            playerRemoveButtons[index].isVisible = true && !online
        }
        if (playerList.size != 6) {
            if (!join && !online) {
                for (index in (6 - (6 - playerList.size)) until 6) {
                    playerTextField[index].text = "NaN"
                    playerConfigButtons[index].isVisible = false
                    playerRemoveButtons[index].isVisible = false
                    playerAddButtons[index].isVisible = true
                }
            } else if (playerList.isEmpty()) {
                playerTextField[0].text = "NaN"
                playerConfigButtons[0].isVisible = false
                playerRemoveButtons[0].isVisible = false
                playerAddButtons[0].isVisible = true
            }
        }
    }

    /**
     * Creates a new player and adds them to the [playerList].
     * * @param name The name of the player.
     * @param type The string representation of the [PlayerType].
     * @param order The desired position in the player list (1-indexed).
     * @return True if the player was successfully created, false if validation failed.
     */
    private fun createPlayer(name: String, type: String, order: Int): Boolean {
        if (playerList.size == 6) return false
        if (!playerOrderList.contains(order.toString())) return false
        val playerType = when (type) {
            "Local" -> PlayerType.LOCAL
            "easy Bot" -> PlayerType.BOT_EASY
            "hard Bot" -> PlayerType.BOT_HARD
            else -> PlayerType.REMOTE
        }
        if (!checkName(name)) {
            println("Name Check nicht bestanden!")
            return false
        }
        playerList.add(order - 1, Player(name = name, playerType = playerType))
        refreshPlayerList()

        if(online && playerList.size == 1 && rootService.networkService.currentClient == null){
            rootService.networkService.hostGame(name = name)
        }

        return true
    }

    /**
     * Removes a player from the list based on their order.
     * @param order The position of the player to remove (1-indexed).
     */
    private fun deletePlayer(order: Int) {
        playerList.removeAt(order - 1)
        refreshPlayerList()
    }

    /**
     * Updates an existing player's information.
     * * @param name The name of the player.
     * @param type The string representation of the [PlayerType].
     * @param order The new desired position.
     * @return True if the change was successful.
     */
    private fun changePlayer(name: String, type: String, order: Int): Boolean {
        if (!playerOrderList.contains(order.toString())) return false
        val tmpPlayer = playerList.first {
            it.name == name
        }
        playerList.remove(tmpPlayer)
        refreshPlayerList()
        if (createPlayer(name, type, order)) {
            refreshPlayerList()
            return true
        } else {
            playerList.add(order - 1, tmpPlayer)
            refreshPlayerList()
            return false
        }
    }

    /**
     * Toggles the visibility of the player configuration sub-menu.
     * @param show If true, the sub-menu is shown; otherwise, it is hidden.
     */
    private fun showPlayerConfig(show: Boolean = true) {
        configPlayer.isVisible = show
        playerName.isVisible = show
        playerNameInput.isVisible = show
        playerType.isVisible = show
        playerTypeInput.isVisible = show
        playerTypeInputUp.isVisible = show
        playerTypeInputDown.isVisible = show
        playerOrder.isVisible = show
        playerOrderInput.isVisible = show
        playerOrderInputUp.isVisible = show
        playerOrderInputDown.isVisible = show
        playerButtonConfirm.isVisible = show
        playerButtonCancel.isVisible = show
    }

    /**
     * Navigates to the first page of the menu (Mode Selection).
     */
    fun firstPageEvent() {
        notVisible()
        background = imageLoader.loadBackground(7)
        join = false

        onlineModeButton.isVisible = true
        offlineModeButton.isVisible = true
    }

    /**
     * Navigates to the second page of the menu (Game and Player Setup).
     */
    private fun nextPageEvent() {
        notVisible()
        if (online) {
            background = imageLoader.loadBackground(13)
            addressLabel.isVisible = true
            addressLabel.text = "create Host!"
            addressLabel.isDisabled = true
            playerLabels[0].isVisible = true
            playerTextField[0].isVisible = true
            playerConfigButtons[0].isVisible = false
            playerAddButtons[0].isVisible = true
            playerRemoveButtons[0].isVisible = false
        } else {
            background = imageLoader.loadBackground(8)
            playerLabels.forEach { it.isVisible = true }
            playerTextField.forEach { it.isVisible = true }
        }

        selectedVariantLabel.isVisible = true
        previousVariantLabel.isVisible = true
        nextVariantLabel.isVisible = true
        selectedSimulationLabel.isVisible = true
        previousSimulationLabel.isVisible = true
        nextSimulationLabel.isVisible = true
        selectedOrderLabel.isVisible = true
        previousOrderLabel.isVisible = true
        nextOrderLabel.isVisible = true
        refreshPlayerList()

        redArrow.isVisible = true
    }

    /**
     * Configures the UI for joining an existing online game session.
     */
    fun joinPageEvent() {
        notVisible()
        background = imageLoader.loadBackground(11)
        previousSimulationLabel.isVisible = true
        nextSimulationLabel.isVisible = true
        selectedSimulationLabel.isVisible = true
        addressLabel.isVisible = true
        addressLabel.isDisabled = false
        playerLabels[0].isVisible = true
        playerTextField[0].isVisible = true
        playerTextField[0].text = "NaN"
        playerConfigButtons[0].isVisible = false
        playerAddButtons[0].isVisible = true
        playerRemoveButtons[0].isVisible = false
        redArrow.isVisible = true
        join = true
    }

    /**
     * Handles the logic when a player configuration button is clicked.
     * @param index The index of the player slot clicked.
     */
    private fun addAction(index: Int){
        playerNameInput.text = playerList[index].name
        playerNameInput.isDisabled = true
        playerOrderInput.text = playerOrderList[index]
        val type = when (playerList[index].playerType) {
            PlayerType.LOCAL -> playerTypeList[0]
            PlayerType.BOT_EASY -> playerTypeList[1]
            PlayerType.BOT_HARD -> playerTypeList[2]
            PlayerType.REMOTE -> "Remote"
        }
        playerTypeInput.text = type
        addPlayer = false
        showPlayerConfig()
    }

    /**
     * Configures the functional logic (onMouseClicked) for all player-related buttons.
     */
    private fun buttonConfig(){
        playerConfigButtons.forEachIndexed { index, button ->
            button.isVisible = false
            button.apply {
                visual = imageLoader.loadButton(6)
                onMouseClicked = {
                    addAction(index)
                }
            }
        }
        playerAddButtons.forEachIndexed { index, button ->
            button.visual = imageLoader.loadButton(7)
            button.onMouseClicked = {
                if (index < 6) {
                    showPlayerConfig()
                    if (index + 1 > playerList.size) {
                        playerNameInput.text = "player$index"
                        playerNameInput.isDisabled = false
                        playerNameInput.prompt = "Enter a Name"
                        playerOrderInput.text =
                            playerOrderList[playerList.size]
                        playerTypeInput.text = playerTypeList[0]
                        addPlayer = true
                    }
                }
            }
        }
        playerRemoveButtons.forEachIndexed { index, button ->
            button.apply {
                visual = imageLoader.loadButton(8)
                isVisible = false
                onMouseClicked = {
                    if(!online)deletePlayer(index + 1)
                }
            }
        }
    }

    /**
     * Initializes the player list UI components (labels, text fields, buttons) for the maximum
     * allowed number of players (6).
     */
    private fun initPlayerList() {
        for (index in 0 until 6) {
            playerLabels += o.label(
                text = "Player $index:",
                posX = 800,
                posY = (550 + (75 * index))
            )
            playerTextField += o.text(
                text = "NaN",
                posX = 1050,
                posY = (550 + (75 * index))
            )
            playerConfigButtons += o.button(
                text = "",
                type = 1,
                posX = 1340,
                posY = (550 + (75 * index))
            )
            playerAddButtons += o.button(
                text = "",
                type = 1,
                posX = 1400,
                posY = (550 + (75 * index))
            )
            playerRemoveButtons += o.button(
                text = "",
                type = 1,
                posX = 1460,
                posY = (550 + (75 * index))
            )
        }
        playerTextField.forEach {
            it.isDisabled = true
        }
        buttonConfig()
    }

    /**
     * Callback for when the network lobby is updated. Syncs the local [playerList]
     * with the remote player names provided by the [rootService].
     */
    override fun refreshAfterLobbyUpdated() {
        val network = rootService.networkService.currentClient
        val onlinePlayerList: MutableList<String> = network?.players ?: mutableListOf()
        val deleteList: MutableList<Int> = mutableListOf()

        // Remove disconnected Player
        val host = rootService.networkService.currentClient?.host

        if(host == null){
            println("Failure! No Host available")
        }else {
            playerList.forEachIndexed { index, player ->
            if(!onlinePlayerList.contains(player.name) && player.name != host){
                    deleteList += index
                }
            }
            deleteList.forEach { order ->
                deletePlayer((order+1))
            }

            playerLabels.forEach { it.isVisible = false }
            playerTextField.forEach { it.isVisible = false }
            playerConfigButtons.forEach { it.isVisible = false }

            playerList.forEachIndexed { index, _ ->
                playerLabels[index].isVisible = true
                playerTextField[index].isVisible = true
                playerConfigButtons[index].isVisible = true
            }

            // Add Joined Player
            val existingNames = playerList.map { it.name }.toSet()
            val notInPlayerList = onlinePlayerList.filterNot { it in existingNames }
            notInPlayerList.forEach {
                createPlayer(name = it, type = "Remote", order = playerList.size+1)
            }
        }
    }

    /**
     * Callback for when the network connection state changes. Updates the [addressLabel]
     * with the Session ID when hosting.
     * @param newState The new [ConnectionState].
     */
    override fun refreshAfterConnectionStateChange(newState: ConnectionState) {
        if(newState == ConnectionState.WAITING_FOR_GUESTS){
            addressLabel.text = rootService.networkService.currentClient?.sessionId ?: "Failure!"
        }
    }
}