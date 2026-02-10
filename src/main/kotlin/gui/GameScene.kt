package gui

import entity.GameState
import entity.PlayerType
import entity.RegionCard
import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.components.container.CardStack
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.BidirectionalMap
import tools.aqua.bgw.visual.*

/**
 * [GameScene] is the primary gameplay scene responsible for rendering the game board,
 * player hands, and the central card market.
 *
 * It manages the visual representation of the [GameState] and provides interactive
 * elements for player actions such as playing cards, choosing starting hands,
 * and performing undo/redo operations. It implements [Refreshable] to update the UI
 * automatically when the underlying game state changes.
 *
 * @param rootService The central service coordinator used to trigger player actions and state changes.
 *
 * @property imageLoader Utility to load static UI assets and button visuals and card textures.
 * @property o Factory-like helper for creating standardized BGW UI components.
 * @property regionCardMap A [BidirectionalMap] maintaining the link between [RegionCard]
 * entities and their [CardView] visuals.
 * @property playerRegionStacks Grid structures representing the tableau/hand areas for each player.
 * @property middleRegionStack Grid representing the available region cards in the center of the table.
 * @property regionStack Visual representation of the draw pile.
 * @property display Full-screen background overlay used when viewing the expanded draw stack.
 * @property displayRegionDrawStack Grid layout used to display all cards currently in the draw stack.
 * @property "to-do" Label indicating the current required action to the active player.
 * @property undoButton Button to revert the last game action.
 * @property redoButton Button to re-apply a previously undone action.
 * @property handSelector Boolean flag to enable/disable card interaction logic during the "Play Card" phase.
 */
class GameScene(private val rootService: RootService):
    BoardGameScene(1920, 1080, background = ImageVisual("fa2.jpg")), Refreshable {
    private var handSelector: Boolean = false
    private var tmpDragged: RegionCard? = null
    private val startHandList: MutableList<RegionCard> = mutableListOf()
    private val regionCardMap: BidirectionalMap<RegionCard, CardView> = BidirectionalMap()

    private val imageLoader = ImageLoader()
    private val o = Objecting()

    // RegionCard DrawStack Display
    val display = Label(text= " ",posX = 0, posY = 0, width = 1920, height = 1080).apply {
        visual = ColorVisual(45,50,55)
    }
    private val displayRegionDrawStack = displayRegionDrawStackCards()
    private val displayLabel = o.label("RegionCard DrawStack:", posX = 760, posY = 50).apply {
        width = 400.0
    }

    // Label, player has to do.
    val todo = o.label("TODO: ", type = 1, posX = 5, posY = 5)

    // Players[] regionStack
    private val playerRegionStacks = o.handStack(6, 3)

    // Players.size + 1 Cards (+ Animation beim Hover + Anklicken führt zu get, nur wenn am Zug.)
    private val middleRegionStack = o.middleGrid(6)

    // Region drawStack
    private val regionStack = o.regionDrawStack().apply {
        onMouseClicked = {
            showRegionDrawStack()
        }
    }

    // Player names
    private val playerNames = o.showPlayerNames(6)

    // UI elements for the starting hand selection phase
    private val contentPane = Pane<Label>(posX = 560, posY = 375, width = 800, height = 325.0, ColorVisual.GRAY)
    private val startingHandLabel = o.label("Choose your Hand", posY = 390)
    private val startingHand = o.row(columns = 5, width = 150.0, height = 148.0, posY = 540)

    /**
     * Submit button for the starting hand selection.
     * Validates that exactly 3 cards are selected before calling the service.
     */
    private val submitStartingHand = o.button("Submit", posX = 800, posY = 625).apply {
        onMouseClicked = {
            val game = rootService.currentGame?.currentGameState
            if (game != null && game.players[game.currentPlayer].hand.size > 3) {
                for (index in 0 until 5) startingHand[index, 0]?.components?.forEach {
                    it.posY += 20
                    it.onMouseClicked = null
                }
                rootService.playerActionService.chooseStartingHand(startHandList)
            }
        }
    }

    // Undo & Redo - Buttons
    private val undoButton = o.button(text = "", type = 1, posX = 25, posY = 1005).apply {
        visual = imageLoader.loadButton(2)
        onMouseClicked = {
            rootService.gameStateService.undo()
        }
    }
    private val redoButton = o.button(text = "", type = 1, posX = 85, posY = 1005).apply {
        visual = imageLoader.loadButton(1)
        onMouseClicked = {
            rootService.gameStateService.redo()
        }
    }

    val tableauDiary = o.button("", type = 6, posX = 550, posY = 905)

    init {
        // Initialize layout and layer components
        playerRegionStacks.forEach { addComponents(it) }
        playerNames.forEach { addComponents(it) }

        addComponents(
            middleRegionStack, regionStack, undoButton, redoButton, tableauDiary,
            contentPane, startingHand, startingHandLabel, submitStartingHand, todo
        )

        // Set initial visibility for dynamic UI elements (Choose Starting-Hand)
        submitStartingHand.isVisible = false
        contentPane.isVisible = false
        startingHand.isVisible = false
        startingHandLabel.isVisible = false

        display.isVisible = false
        displayRegionDrawStack.isVisible = false
        displayLabel.isVisible = false

        addComponents(display,displayRegionDrawStack, displayLabel)
    }

    /**
     * Clears all card grids and resets the bidirectional map to prepare for a clean UI rebuild.
     * This is called before [initAll] to ensure no ghost components remain in the view.
     */
    private fun clearAllViews() {
        regionStack.clear()
        middleRegionStack.forEach { it.component?.clear() }
        playerRegionStacks.forEach { grid ->
            grid.forEach { it.component?.clear() }
        }
        startingHand.forEach { it.component?.clear() }
        regionCardMap.clear()
    }

    /**
     * Creates or retrieves a [CardView] for a given [RegionCard].
     *
     * It checks the [regionCardMap] first to see if a view already exists for the card.
     * If not, it initializes a new [CardView] with the appropriate front and back textures.
     * * @param regionCard The data entity for which a visual representation is needed.
     * @return A [CardView] linked to the provided [RegionCard].
     */
    private fun createRegionView(regionCard: RegionCard): CardView {
        return regionCardMap.forwardOrNull(regionCard) ?: CardView(
            height = 150, width = 148,
            front = imageLoader.imageFor(regionCard),
            back = imageLoader.regionBackImage
        ).also {
            it.showFront()
            regionCardMap.add(regionCard, it)
        }
    }

    /**
     * Initializes and displays the grid for the region draw stack cards.
     *
     * This function generates a [GridPane] containing 68 [CardStack] objects arranged
     * in a 12-column layout. The cards are styled with a square aspect ratio and
     * indexed numerically for identification.
     *
     * ### Layout Details:
     * * **Grid Dimensions:** 12 columns by 6 rows (accommodates 68 cards).
     * * **Card Size:** 150.0 x 150.0 units (square format).
     * * **Spacing:** A gap of 10.0 units is applied between components.
     * * **Positioning:** Centered at coordinates (950, 600).
     *
     * @return A [GridPane] containing the organized region card stacks.
     */
    private fun displayRegionDrawStackCards(): GridPane<CardStack<CardView>> {
        val totalCards = 68
        val columns = 12
        val rows = 6
        val cardSize = 150.0 // Quadratisch und lässt Platz für Abstände
        val gap = 10.0       // Abstand zwischen den Karten

        val container = GridPane<CardStack<CardView>>(
            posX = 950,
            posY = 600,    // Perfekt zentriert
            rows = rows,
            columns = columns,
            visual = Visual.EMPTY
        ).apply {
            this.width = columns * (cardSize + gap)
            this.height = rows * (cardSize + gap)
        }

        for (index in 0 until totalCards) {
            val childCardStack = CardStack<CardView>(
                width = cardSize,
                height = cardSize
            ).apply {
                visual = CompoundVisual(
                    ColorVisual(Color(40, 45, 50)), // Dunkles Design
                    TextVisual(
                        text = "${index + 1}",
                    )
                )
            }

            // Mathematische Verteilung auf das Raster
            val col = index % columns
            val row = index / columns

            container.set(columnIndex = col, rowIndex = row, component = childCardStack)
        }

        return container
    }

    /**
     * Toggles the visibility of the detailed draw stack overlay.
     *
     * When [show] is true, it populates the [displayRegionDrawStack] with all cards
     * remaining in the draw pile, allowing players to inspect them.
     *
     * @param show Whether to show (true) or hide (false) the draw stack overlay.
     */
    fun showRegionDrawStack(show: Boolean = true) {
        val game = rootService.currentGame?.currentGameState

        if (show && game != null) {
            // 1. Create CardViews from GameState
            val cardViewList = game.regionDrawStack.asReversed().map { regionCard ->
                CardView(width = 150, height = 150, front = imageLoader.imageFor(regionCard)).apply {
                    showFront()
                }
            }

            // 2. Iterate through all Stacks in Grid
            displayRegionDrawStack.forEachIndexed { index, gridIteratorElement ->
                val stack = gridIteratorElement.component
                stack?.clear() // remove old components

                // add if card is available
                if (index < cardViewList.size) {
                    stack?.add(cardViewList[index])
                }
            }

            display.isVisible = true
            displayRegionDrawStack.isVisible = true
            displayLabel.isVisible = true
        } else {
            display.isVisible = false
            displayRegionDrawStack.isVisible = false
            displayLabel.isVisible = false
        }
    }

    /**
     * Reconstructs the entire scene based on the current [GameState].
     * * This method:
     * 1. Refreshes the draw stack.
     * 2. Updates the central card market (middle stack).
     * 3. Renders player tableaus and hands relative to the current player's perspective.
     * 4. Updates the enabled/disabled state and opacity of Undo/Redo buttons.
     */
    private fun initAll() {
        val game = rootService.currentGame ?: return
        val gameState = game.currentGameState
        clearAllViews()

        // 1. DrawStack
        gameState.regionDrawStack.forEach {
            regionStack.add(createRegionView(it).apply { showFront() })
        }

        // 2. CenterCards
        gameState.centerCards.forEachIndexed { index, card ->
            val col = index % 4
            val row = index / 4
            middleRegionStack[col, row]?.add(createRegionView(card))
        }

        // 3. Player-Tableaus
        val numPlayers = gameState.players.size
        val currentPlayerIdx = gameState.currentPlayer

        gameState.players.forEachIndexed { pIdx, player ->
            // Index 0 is the current player
            if (player.hand.size <= 3) {
                val relativeIdx = (pIdx - currentPlayerIdx + numPlayers) % numPlayers

                // Player names
                playerNames[relativeIdx].text = player.name

                // HandCards
                player.hand.forEachIndexed { cIdx, card ->
                    playerRegionStacks[relativeIdx][cIdx, 1]?.add(createRegionView(card))
                }

                // played Card
                player.selectedCard?.let { card ->
                    val view = CardView(
                        height = 150,
                        width = 148,
                        front = imageLoader.imageFor(card),
                        back = imageLoader.regionBackImage
                    )
                    if (player.hand.size == 3) {
                        view.showBack()
                    } else {
                        view.showFront()
                    }
                    playerRegionStacks[relativeIdx][1, 0]?.add(view)
                }
            }
        }

        // Logic for Undo/Redo Buttons
        // Are those actions available?
        undoButton.isDisabled = game.gameHistoryIndex == 0
        if (gameState.players[gameState.currentPlayer].playerType != PlayerType.LOCAL) undoButton.isDisabled = true
        redoButton.isDisabled = game.gameHistory.size - 1 == game.gameHistoryIndex

        // visual feedback if the button is clickable
        undoButton.opacity = if (undoButton.isDisabled) 0.3 else 1.0
        redoButton.opacity = if (redoButton.isDisabled) 0.3 else 1.0
    }

    /**
     * Adjusts the visibility of UI components based on the number of players in the current game.
     *
     * This method ensures that only the necessary UI elements are rendered, preventing
     * visual clutter from unused player slots. Specifically, it:
     * 1. Hides all player name labels ([playerNames]), tableau stacks ([playerRegionStacks]),
     * and central market slots ([middleRegionStack]).
     * 2. Re-enables visibility for the labels and stacks belonging to the actual
     * number of players.
     * 3. Scales the central market (middle region) by showing only a number of
     * card slots proportional to the player count (playerSize + 1).
     *
     * This is typically called during initialization or when restoring a game state
     * to align the board layout with the session configuration.
     */
    private fun showPlayerAmount(){
        playerNames.forEach { playerName ->
            playerName.isVisible = false
        }
        playerRegionStacks.forEachIndexed { _, cardStacks ->
            cardStacks.isVisible = false
        }
        middleRegionStack.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }
        val playerSize = rootService.currentGame?.currentGameState?.players?.size ?: 6
        for(playerIndex in 0 until playerSize){
            playerNames[playerIndex].isVisible = true
            playerRegionStacks[playerIndex].isVisible = true
        }

        middleRegionStack.forEachIndexed { index, gridIteratorElement ->
            if(index <= playerSize){
                gridIteratorElement.component?.isVisible = true
            }
        }
    }

    /**************************************************************************
     *              --- Refreshable Interface Implementation ---
     **************************************************************************/

    /**
     * Handles the UI transitions and service calls immediately after the game session begins.
     *
     * This method:
     * 1. Resets the visibility of all player name labels.
     * 2. Dynamically shows only the labels for the actual number of players in the current session.
     * 3. Invokes [initAll] to perform a complete visual reconstruction of the game board.
     * 4. Signals the [service.PlayerActionService] to proceed with the initial game logic.
     */
    override fun refreshAfterGameStart() {
        showPlayerAmount()

        if (rootService.currentGame?.isOnline == true) {
            undoButton.isVisible = false
            redoButton.isVisible = false
        } else {
            undoButton.isVisible = true
            redoButton.isVisible = true
        }

        initAll()
        rootService.playerActionService.proceedAfterGameStart()
    }

    /**
     * Prepares the UI for the starting hand selection phase.
     * Shows the selection pane and attaches click listeners to cards for selection/deselection.
     */
    override fun refreshBeforeChooseStartingHand() {
        todo.text = "TODO: Choose starting Hand"
        println("refreshBeforeChooseStartingHand()")
        contentPane.isVisible = true
        startingHandLabel.isVisible = true
        startingHand.isVisible = true
        submitStartingHand.isVisible = true

        initAll()

        val game = rootService.currentGame ?: return
        val players = game.currentGameState.players

        val currentPlayer = game.currentGameState.currentPlayer
        val handChoice = players[currentPlayer].hand

        handChoice.forEachIndexed { index, regionCard ->
            val cardView = createRegionView(regionCard)
            cardView.onMouseClicked = {
                if(players[currentPlayer].playerType == PlayerType.LOCAL){
                    if (cardView.posY == -20.0) {
                        cardView.posY += 20
                        startHandList.remove(regionCard)
                    } else {
                        cardView.posY -= 20
                        startHandList.add(regionCard)
                    }
                }
            }

            cardView.showFront()
            startingHand[index, 0]?.add(cardView)
        }
    }

    /**
     * Hides the selection UI and triggers the next game phase after the starting hand is confirmed.
     * Includes a delay based on the game's simulation speed for visual smoothness.
     */
    override fun refreshAfterChooseStartingHand() {
        println("refreshAfterChooseStartingHand()")
        contentPane.isVisible = false
        startingHandLabel.isVisible = false
        startingHand.isVisible = false
        submitStartingHand.isVisible = false
        startHandList.clear()

        val game = rootService.currentGame
        if (game != null) {
            val duration = game.simulationSpeed

            DelayAnimation(duration).apply {
                onFinished = {
                    initAll()
                    rootService.gameService.isNextPlayerHandSizeValid(game.currentGameState)
                }
            }.also {
                playAnimation(it)
            }
        } else {
            println("Failure, no game available!")
        }
    }

    /**
     * Enables drag-and-drop functionality for the current player's hand cards.
     * Sets up the target drop zone in the player's tableau area.
     */
    override fun refreshBeforePlayRegionCard() {
        contentPane.isVisible = false
        startingHandLabel.isVisible = false
        startingHand.isVisible = false
        submitStartingHand.isVisible = false
        startHandList.clear()

        todo.text = "TODO: Play RegionCard"
        println("refreshBeforePlayRegionCard()")
        initAll()

        val game = rootService.currentGame?.currentGameState ?: return
        val player = game.players[game.currentPlayer]
        if (player.playerType != PlayerType.LOCAL) return

        handSelector = true

        val hand = game.players[game.currentPlayer].hand
        hand.forEachIndexed { _, regionCard ->
            regionCardMap.forward(regionCard).apply {
                val actualPositionY = posY
                isDraggable = true
                onMouseEntered = {
                    if (handSelector) posY = actualPositionY - 10
                }
                onMouseExited = {
                    if (handSelector) posY = actualPositionY
                }

                onDragGestureStarted = {
                    posY = actualPositionY
                    tmpDragged = regionCard
                }
            }
        }

        playerRegionStacks[0][1, 0]?.apply {
            dropAcceptor = { true }
            onDragDropped = {
                tmpDragged?.let {
                    val tempDragged = tmpDragged as RegionCard
                    regionCardMap.forward(tempDragged).posY += 10
                    hand.forEach { regionCard ->
                        regionCardMap.forward(regionCard).isDraggable = false
                    }
                    rootService.playerActionService.playRegionCard(tempDragged)
                    tmpDragged = null
                    handSelector = false
                }
            }
        }
    }

    /**
     * Updates the UI after a card has been played and proceeds to the next game step.
     */
    override fun refreshAfterPlayRegionCard(card: RegionCard) {
        println("refreshAfterPlayRegionCard()")

        val game = rootService.currentGame

        if (game != null) {
            val duration = game.simulationSpeed

            DelayAnimation(duration).apply {
                onFinished = {
                    initAll()
                    rootService.playerActionService.proceedAfterPlayRegionCard()
                }
            }.also {
                playAnimation(it)
            }
        } else {
            println("Failure, no game available!")
        }
    }

    /**
     * Prepares the central card market for selection.
     * Attaches hover effects and click listeners to the cards available in the [middleRegionStack].
     */
    override fun refreshBeforeChooseRegionCard() {
        initAll()
        todo.text = "TODO: Choose RegionCard "
        println("refreshBeforeChooseRegionCard()")
        val game = rootService.currentGame?.currentGameState ?: return
        val player = game.players[game.currentPlayer]
        if(player.playerType != PlayerType.LOCAL)return

        middleRegionStack.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.components?.forEach { cardView ->
                cardView.apply {
                    val actualPos = posY
                    onMouseEntered = {
                        posY = actualPos-25.0
                    }
                    onMouseExited = {
                        posY = actualPos
                    }
                    onMouseClicked = {
                        posY = actualPos
                        rootService.playerActionService.chooseRegionCard(regionCardMap.backward(cardView))
                    }
                }
            }
        }
    }

    /**
     * Updates the UI after a region card is chosen from the market.
     */
    override fun refreshAfterChooseRegionCard(card: RegionCard) {
        println("refreshAfterChooseRegionCard()")

        val game = rootService.currentGame

        if (game != null) {
            val duration = game.simulationSpeed

            DelayAnimation(duration).apply {
                onFinished = {
                    initAll()
                    rootService.playerActionService.proceedAfterChooseRegionCard()
                }
            }.also {
                playAnimation(it)
            }
        } else {
            println("Failure, no game available!")
        }
    }

    /**
     * Re-initializes the UI to reflect the state after an Undo/Redo action.
     */
    override fun refreshAfterChange() {
        initAll()
        rootService.gameService.refreshBeforeNextMove()
    }

    /**
     * Synchronizes the user interface with the loaded game state.
     * * This method is called after a game has been successfully restored from a
     * save file. It performs the following synchronization steps:
     * 1. Resets the visibility of all player name labels to avoid artifacts from
     * previous sessions.
     * 2. Iterates through the loaded [GameState] to show only the labels
     * corresponding to the players present in the saved game.
     * 3. Calls [initAll] to trigger a comprehensive redraw of the game board,
     * hand cards, and central market based on the restored data.
     */
    override fun refreshAfterLoad() {
        showPlayerAmount()

        if (rootService.currentGame?.isOnline == true) {
            undoButton.isVisible = false
            redoButton.isVisible = false
        } else {
            undoButton.isVisible = true
            redoButton.isVisible = true
        }
        
        initAll()
    }
}