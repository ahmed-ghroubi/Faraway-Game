package gui

import entity.PlayerType
import entity.RegionCard
import entity.SanctuaryCard
import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.animation.MovementAnimation
import tools.aqua.bgw.components.container.CardStack
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.BidirectionalMap
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.CompoundVisual
import tools.aqua.bgw.visual.TextVisual
import tools.aqua.bgw.visual.Visual

/**
 * This class displays the Tableaus (player boards) of the players.
 * It allows switching between different players' views and handles the display of
 * collected cards and temporary selection areas.
 *
 * @param rootService The [RootService] used to communicate with the logic layer.
 * * @property imageLoader Loads general background and UI images.
 * @property imageLoader Loads specific card front and back images.
 * @property o Factory-like helper to create UI objects and layouts.
 * @property currPlayer The index of the player whose tableau is currently being viewed.
 * @property currentlyPlaying The index of the player whose turn it currently is.
 * @property headLineLabel Label showing the name of the player currently being viewed.
 * @property previousPlayerButton Button to cycle to the previous player's tableau.
 * @property nextPlayerButton Button to cycle to the next player's tableau.
 * @property sanctuaryStack Visual representation of the Sanctuary draw stack.
 * @property temporarySanctuaries Row for displaying Sanctuary cards during a selection process (Tier 1).
 * @property temporarySanctuaries1 Row for displaying Sanctuary cards during a selection process (Tier 2).
 * @property temporarySanctuaries2 Row for displaying Sanctuary cards during a selection process (Tier 3).
 * @property sanctuaryTableau0 The first row of the player's acquired Sanctuary cards.
 * @property sanctuaryTableau1 The second row of the player's acquired Sanctuary cards.
 * @property regionTableau0 The first row of the player's acquired Region cards.
 * @property regionTableau1 The second row of the player's acquired Region cards.
 * @property playerRegionStack The row representing the player's current hand of Region cards.
 */
class GameTableauScene(private val rootService: RootService) :
    BoardGameScene(1920, 1080, ColorVisual(95, 95, 95)), Refreshable{
    private val imageLoader = ImageLoader()
    private val o = Objecting()
    private var currPlayer = 0
    private var currentlyPlaying = 0
    private val tempSanctuaryCardMap: BidirectionalMap<SanctuaryCard, CardView> = BidirectionalMap()
    private var showTempSanctuary: Boolean = false

    val display = Label(text= " ",posX = 0, posY = 0, width = 1920, height = 1080).apply {
        visual = ColorVisual(45,50,55)
    }
    private val displaySanctuaryDrawStack = displaySanctuaries()
    private val displayLabel = o.label("Sanctuary DrawStack:", posX = 760, posY = 25).apply {
        width = 400.0
    }

    val todo = o.label("TODO: ", type = 1, posX = 5, posY = 5)

    private val headLineLabel = o.label(text = "", posY = 25)
    private val previousPlayerButton = o.button(text = "", type = 1, posX = 750, posY = 25).apply {
        visual = imageLoader.loadButton(2)
        onMouseClicked = {
            val game = rootService.currentGame?.currentGameState
            if(game != null) {
                val playerList = game.players
                currPlayer--
                if (currPlayer < 0) currPlayer = playerList.size - 1

                headLineLabel.text = playerList[currPlayer].name

                showTemporaryCards()
                refreshScene()
            }
        }
    }
    private val nextPlayerButton = o.button(text = "", type = 1, posX = 1110, posY = 25).apply {
        visual = imageLoader.loadButton(1)
        onMouseClicked = {
            val game = rootService.currentGame?.currentGameState
            if(game != null) {
                val playerList = game.players
                currPlayer++
                if (currPlayer == playerList.size) currPlayer = 0

                headLineLabel.text = playerList[currPlayer].name

                showTemporaryCards()
                refreshScene()
            }
        }
    }

    // Sanctuary drawStack
    private val sanctuaryStack = o.sanctuaryDrawStack().apply {
        onMouseClicked = {
            showSanctuaryDrawStack()
        }
    }

    private val temporarySanctuaries2 = o.row(columns = 6, width = 112.8, height = 145.0, posX = 550, posY = 190)
    private val temporarySanctuaries1 = o.row(columns = 6, width = 112.8, height = 145.0, posX = 550, posY = 340)
    private val temporarySanctuaries = o.row(columns = 3, width = 112.8, height = 145.0, posX = 550, posY = 490)

    private val sanctuaryTableau0 = o.row(columns = 3, width = 155.6, height = 200.0, posX = 550, posY = 690)
    private val sanctuaryTableau1 = o.row(columns = 4, width = 155.6, height = 200.0, posX = 550, posY = 900)
    private val regionTableau0 = o.row(columns = 4, width = 200.0, height = 200.0, posX = 1360, posY = 440)
    private val regionTableau1 = o.row(columns = 4, width = 200.0, height = 200.0, posX = 1360, posY = 650)
    private val playerRegionStack = o.row(columns = 3, width = 200.0, height = 200.0, posX = 1360, posY = 900)

    /**
     * It initializes with an opacity of 0.5 and the headLineLabel, logPanel and backButton.
     */
    init{
        opacity = .5
        background = imageLoader.loadBackground(10)
        addComponents(
            previousPlayerButton,
            nextPlayerButton,
            headLineLabel,
            sanctuaryTableau0,
            sanctuaryTableau1,
            regionTableau0,
            regionTableau1,
            playerRegionStack,
            sanctuaryStack,
            temporarySanctuaries2,
            temporarySanctuaries1,
            temporarySanctuaries,
            todo
        )

        addComponents(display,displayLabel,displaySanctuaryDrawStack)

        display.isVisible = false
        displayLabel.isVisible = false
        displaySanctuaryDrawStack.isVisible = false
        temporarySanctuaries.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }
        temporarySanctuaries1.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }
        temporarySanctuaries2.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }
    }

    /**
     * Creates a [CardView] for a [RegionCard].
     */
    private fun createCardView(regionCard: RegionCard): CardView{
        return CardView(
            height = 200, width = 200,
            front = imageLoader.imageFor(regionCard),
            back = imageLoader.regionBackImage
        ).also{
            it.showFront()
        }
    }

    /**
     * Toggles the visibility of the Sanctuary draw stack overlay and populates it
     * with current cards from the game state.
     * * @param show Whether to show or hide the draw stack.
     */
    fun showSanctuaryDrawStack(show: Boolean = true) {
        val game = rootService.currentGame?.currentGameState

        if (show && game != null) {
            // 1. Erstelle die CardViews aus den Daten des GameStates
            val cardViewList = game.sanctuaryDrawStack.map { sanctuary ->
                CardView(width = 155.6, height = 200, front = imageLoader.imageFor(sanctuary)).apply {
                    showFront()
                }
            }

            // 2. Gehe durch alle Stacks im Grid
            displaySanctuaryDrawStack.forEachIndexed { index, gridIteratorElement ->
                val stack = gridIteratorElement.component
                stack?.clear() // Alten Inhalt entfernen

                // Nur hinzufügen, wenn wir für diesen Slot auch eine Karte haben
                if (index < cardViewList.size) {
                    stack?.add(cardViewList[index])
                }
            }

            display.isVisible = true
            displaySanctuaryDrawStack.isVisible = true
            displayLabel.isVisible = true
        } else {
            display.isVisible = false
            displaySanctuaryDrawStack.isVisible = false
            displayLabel.isVisible = false
        }
    }

    /**
     * Creates a [CardView] for a [SanctuaryCard].
     * * @param small Determines if the card should be rendered in a smaller size (e.g., for selection).
     */
    private fun createCardView(sanctuaryCard: SanctuaryCard, small: Boolean = false): CardView{
        var height = 200.0
        var width = 155.6
        if(small){
            height = 145.0
            width = 112.8
        }

        val cardView = CardView(
            height = height, width = width,
            front = imageLoader.imageFor(sanctuaryCard),
            back = imageLoader.regionBackImage
        ).also{
            it.showFront()
        }

        if(small){
            tempSanctuaryCardMap.add(sanctuaryCard, cardView)
        }

        return cardView
    }

    /**
     * Clears all visual stacks and tableaus in the scene.
     */
    private fun clearer(){
        sanctuaryTableau0.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        sanctuaryTableau1.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        regionTableau0.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        regionTableau1.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        playerRegionStack.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        sanctuaryStack.clear()
    }

    /**
     * Refreshes the visual state of the scene based on the current game data.
     * * @param refresh If true, resets [currPlayer] to the actual current player in the game state.
     */
    fun refreshScene(refresh: Boolean = false){
        val game = rootService.currentGame?.currentGameState
        if(game == null){
            println("Es steht derzeit kein Spiel zur Verfügung. Refresh des GameTableaus nicht möglich!")
            return
        }
        if(refresh)currPlayer = game.currentPlayer
        currentlyPlaying = game.currentPlayer

        val player = game.players[currPlayer]
        headLineLabel.text = player.name

        clearer()

        // Display Player-Hand (Bottom-Right)
        player.hand.forEachIndexed { index, regionCard ->
            playerRegionStack[index,0]?.add(createCardView(regionCard))
        }
        // Display collected RegionCards
        player.regionCards.forEachIndexed { index, regionCard ->
            if(index < 4){
                regionTableau0[index,0]?.add(createCardView(regionCard))
            }else{
                regionTableau1[(index%4),0]?.add(createCardView(regionCard))
            }
        }
        // Display collected SanctuaryCards
        player.sanctuaries.forEachIndexed { index, sanctuaryCard ->
            if(index < 3){
                sanctuaryTableau0[index,0]?.add(createCardView(sanctuaryCard))
            }else{
                sanctuaryTableau1[(index-3),0]?.add(createCardView(sanctuaryCard))
            }
        }
        // Display SanctuaryDrawStack
        game.sanctuaryDrawStack.forEachIndexed { _, sanctuaryCard ->
            sanctuaryStack.add(createCardView(sanctuaryCard))
        }
    }

    /**
     * Logic to execute after a card animation completes.
     */
    private fun actionAfterAnimation() {
        clearTemp()

        rootService.playerActionService.proceedAfterChooseSanctuaryCard()
    }

    /**
     * Orchestrates card movement animations or delays for non-local players.
     */
    private fun animation(playerType: PlayerType, duration: Int, component: CardView, targetX: Double, targetY: Double){
        if(playerType != PlayerType.LOCAL){
            DelayAnimation(duration).apply {
                onFinished = {
                    actionAfterAnimation()
                }
            }.also {
                playAnimation(it)
            }
        }else{
            movingAnimation(component, targetX, targetY, duration)
        }
    }

    /**
     * Performs a smooth movement animation for a card component.
     */
    private fun movingAnimation(
        movedComponent: CardView,
        targetX: Double,
        targetY: Double,
        duration: Int
    ) {
        MovementAnimation(
            componentView = movedComponent,
            fromX = movedComponent.posX,
            toX = targetX,
            fromY = movedComponent.posY,
            toY = targetY,
            duration = duration
        ).apply {
            onFinished = {
                        actionAfterAnimation()
            }
        }.also {
            playAnimation(it)
        }
    }

    /**
     * Updates the visibility of the temporary card selection grids based on
     * the number of cards available to choose from.
     */
    private fun showTemporaryCards(){
        temporarySanctuaries.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }
        temporarySanctuaries1.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }
        temporarySanctuaries2.forEachIndexed { _, gridIteratorElement ->
            gridIteratorElement.component?.isVisible = false
        }

        if(!showTempSanctuary)return

        val game = rootService.currentGame?.currentGameState
        if(game == null){
            println("Failure, no game available")
            return
        }

        val player = game.players[currPlayer]
        val tempSize = player.temporarySanctuaries.size

        // Reset visibility for both grids first to ensure a clean state
        temporarySanctuaries1.forEach { it.component?.isVisible = false }
        temporarySanctuaries2.forEach { it.component?.isVisible = false }

        when {
            // Case 1: All sanctuaries fit into the second grid (up to 6)
            tempSize <= 6 -> {
                temporarySanctuaries2.forEachIndexed { index, slot ->
                    if (index < tempSize) slot.component?.isVisible = true
                }
            }
            // Case 2: Sanctuaries fill the second grid and parts of the first grid (up to 12)
            tempSize <= 12 -> {
                // Grid 2 is full
                temporarySanctuaries2.forEach { it.component?.isVisible = true }
                // Grid 1 shows the remaining cards (7 to tempSize)
                temporarySanctuaries1.forEachIndexed { index, slot ->
                    if (index < (tempSize - 6)) slot.component?.isVisible = true
                }
            }
            // Case 3: More than 12 sanctuaries (both grids are fully visible)
            else -> {
                temporarySanctuaries1.forEach { it.component?.isVisible = true }
                temporarySanctuaries2.forEach { it.component?.isVisible = true }
            }
        }
    }

    /**
     * Creates and initializes a grid layout to display sanctuary card stacks.
     *
     * This function generates a [GridPane] containing 45 [CardStack] objects arranged
     * in a 9x5 grid. Each card stack is initialized with a dark background and
     * a numerical label indicating its position.
     *
     * ### Layout Details:
     * * **Grid Dimensions:** 9 columns by 5 rows (optimized for 45 cards).
     * * **Card Size:** 155.6 x 200.0 units.
     * * **Spacing:** Uses a gap of 10.0 units between cards.
     * * **Positioning:** Fixed coordinates at (950, 580).
     *
     * @return A [GridPane] containing the structured [CardStack] components.
     */
    private fun displaySanctuaries(): GridPane<CardStack<CardView>> {
        val totalCards = 45
        val columns = 9  // Reduziert auf 9 Spalten für ein harmonischeres 9x5 Raster bei 45 Karten
        val rows = 5

        // Originalmaße der Karten
        val cardWidth = 155.6
        val cardHeight = 200.0
        val gap = 10.0

        val container = GridPane<CardStack<CardView>>(
            posX = 950,
            posY = 580,
            rows = rows,
            columns = columns,
            visual = Visual.EMPTY
        ).apply {
            this.width = columns * (cardWidth + gap)
            this.height = rows * (cardHeight + gap)
        }

        for (index in 0 until totalCards) {
            val childCardStack = CardStack<CardView>(
                width = cardWidth,
                height = cardHeight
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
     * Initializes the interactive behavior for selecting sanctuary cards from the temporary
     * sanctuary displays.
     *
     * This function iterates through multiple collections of sanctuary components
     * ([temporarySanctuaries], [temporarySanctuaries1], and [temporarySanctuaries2])
     * and applies logic to each visual component.
     */
    private fun selectSanctuary() {
        tempSanctuaryCardMap.entries.forEach { pair ->
            pair.second.apply {
                onMouseEntered = {
                    posY -= 10
                }
                onMouseExited = {
                    posY += 10
                }
                onMouseClicked = {
                    posY = 0.0
                    rootService.playerActionService.chooseSanctuaryCard(pair.first)
                }
            }
        }
    }

    /**
     * This method clears the temporary Sanctuary stack.
     */
    private fun clearTemp(){
        temporarySanctuaries2.forEachIndexed { _ , gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        temporarySanctuaries1.forEachIndexed { _ , gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }
        temporarySanctuaries.forEachIndexed { _ , gridIteratorElement ->
            gridIteratorElement.component?.clear()
        }

        refreshScene(true)
        tempSanctuaryCardMap.clear()
    }

    /**************************************************************************
     *              --- Refreshable Interface Implementation ---
     **************************************************************************/

    /**
     * Prepares the scene for the player to select a Sanctuary card.
     */
    override fun refreshBeforeChooseSanctuaryCard() {
        println("refreshBeforeChooseSanctuaryCard()")

        val game = rootService.currentGame?.currentGameState
        if(game == null){
            println("Es steht derzeit kein Spiel zur Verfügung. RefreshBeforeChooseSanctuaryCard des GameTableaus " +
                    "nicht möglich!")
            return
        }

        todo.text = "TODO: Choose SanctuaryCard"
        val player = game.players[game.currentPlayer]
        refreshScene(true)

        // Display temporarySanctuaries
        player.temporarySanctuaries.forEachIndexed { index, sanctuaryCard ->
            if(index < 6){
                temporarySanctuaries2[index,0]?.add(createCardView(sanctuaryCard, small = true))
            }else if(index < 12){
                temporarySanctuaries1[(index-6),0]?.add(createCardView(sanctuaryCard, small = true))
            }else{
                temporarySanctuaries[(index-12),0]?.add(createCardView(sanctuaryCard, small = true))
            }
        }

        showTempSanctuary = true
        showTemporaryCards()

        if(player.playerType == PlayerType.LOCAL)selectSanctuary()
    }

    /**
     * Triggers the animation and logic update after a Sanctuary card has been chosen.
     * * @param card The sanctuary card that was selected.
     */
    override fun refreshAfterChooseSanctuaryCard(card: SanctuaryCard) {
        println("refreshAfterChooseSanctuaryCard()")

        val game = rootService.currentGame
        if(game == null){
            println("Kein Game gefunden, Fehler!")
            return
        }

        showTempSanctuary = false
        val duration = game.simulationSpeed
        val player = game.currentGameState.players[game.currentGameState.currentPlayer]

        var targetX = 0.0
        var targetY = 0.0

        val firstEmptyElement = sanctuaryTableau0.firstOrNull {
            it.component?.components?.isEmpty() == true
        }
        val firstEmptyElement1 = sanctuaryTableau1.firstOrNull {
            it.component?.components?.isEmpty() == true
        }

        if (firstEmptyElement != null) {
            val comp = firstEmptyElement.component
            println("ActualTargetPosX: ${comp?.actualPosX}")
            targetX = comp?.actualPosX?: 0.0
            println("ActualTargetPosY: ${comp?.actualPosY}")
            targetY = comp?.actualPosY?: 0.0
        }
        else if(firstEmptyElement1 != null){
            val comp = firstEmptyElement1.component
            println("ActualTargetPosX: ${comp?.actualPosX}")
            targetX = comp?.actualPosX?: 0.0
            println("ActualTargetPosY: ${comp?.actualPosY}")
            targetY = comp?.actualPosY?: 0.0
        }

        val component = tempSanctuaryCardMap.forward(card)

        animation(player.playerType, duration, component, targetX, targetY)
    }

    /**
     * Updates the UI to prompt the current player to choose a [RegionCard] from
     * the available selection (e.g., from the display or a draft).
     */
    override fun refreshBeforeChooseRegionCard() {
        clearTemp()
        todo.text = "TODO: Choose RegionCard"
    }

    /**
     * Prepares the interface for the player to play a [RegionCard] from their hand.
     * This usually enables interaction with the player's hand cards and
     * highlights valid placement areas on the tableau.
     */
    override fun refreshBeforePlayRegionCard() {
        clearTemp()
        todo.text = "TODO: Play RegionCard"
    }

    /**
     * Updates the scene to allow players to select their starting hand
     * at the beginning of the game. This typically involves choosing
     * a subset of cards from an initial deal.
     */
    override fun refreshBeforeChooseStartingHand() {
        todo.text = "TODO: Choose starting Hand"
    }
}