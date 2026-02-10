package gui

import tools.aqua.bgw.components.container.CardStack
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.CompoundVisual
import tools.aqua.bgw.visual.TextVisual
import tools.aqua.bgw.visual.Visual

/**
 * This class supports creating objects for the specific scenes in [gui].
 */
class Objecting {
    private val imageLoader = ImageLoader()

    /**
     * This method creates a normed [TextField].
     * @param text Text of the TextField.
     * @param type The Norm.
     * @param posX X-Position of the [TextField].
     * @param posY Y-Position of the [TextField].
     *
     * @return [TextField] A normed TextField.
     */
    fun text(text: String, type: Int = 0, posX: Int = 810, posY: Int = 0): TextField{
        var width = 0
        var height = 0
        var size = 22
        var color: Color = Color.BLUE
        val family = "Arial"
        val fontWeight : Font.FontWeight = Font.FontWeight.BOLD
        var fontStyle: Font.FontStyle = Font.FontStyle.NORMAL
        val visual: Visual = Visual.EMPTY
        when(type) {
            0 -> {
                width = 250
                height = 50
                size = 30
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
            }
        }
        return TextField(
            posX = posX,
            posY = posY,
            width = width,
            height = height,
            text = text,
            font = Font(size = size, color = color,family = family,fontWeight = fontWeight, fontStyle = fontStyle),
            visual = visual
        )
    }

    /**
     * This method [label] creates normed labels. Normed by type.
     * @param text Text of the Label.
     * @param type explained in the following:
     * If the [type] is 0 -> Normal text Label.
     * If the [type] is 3 -> [MainScene]-Sign.
     * @param posX X-Position of the Label.
     * @param posY Y-Position of the Label.
     *
     * @return [Label] A normed Label.
     */
    fun label(text: String, type: Int = 0, posX: Int = 810, posY: Int = 0): Label{
        var width = 0
        var height = 0
        var size = 22
        var color: Color = Color.BLUE
        val family = "Arial"
        val fontWeight : Font.FontWeight = Font.FontWeight.BOLD
        var fontStyle: Font.FontStyle = Font.FontStyle.NORMAL
        var visual: Visual = Visual.EMPTY
        when(type){
            0 -> {
                width = 300
                height = 50
                size = 30
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
            }
            1 -> {
                width = 400
                height = 50
                size = 25
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
            }
            3 -> {
                width = 700
                height = 700
                visual = imageLoader.loadBackground(type)
            }
            4 -> {
                width = 300
                height = 50
                size = 30
                color = Color(245, 245, 235)
                fontStyle = Font.FontStyle.ITALIC
            }
        }

        return Label(
            posX = posX,
            posY = posY,
            width = width,
            height = height,
            text = text,
            font = Font(size = size, color = color,family = family,fontWeight = fontWeight, fontStyle = fontStyle),
            visual = visual
        )
    }

    /**
     * This method [row] creates a row of columns of [CardStack]s.
     * For the design in [GameTableauScene].
     *
     * @param columns Amount of columns.
     * @param width The width of each column.
     * @param height The height of each row.
     * @param posX X-Position of the [GridPane] (row).
     * @param posY Y-Position of the [GridPane] (row).
     *
     * @return [GridPane] of [CardStack] of [CardView] (cards in a specific Layout).
     */
    fun row(columns: Int = 0, width: Double = 300.0, height: Double = 450.0, posX: Int = 960, posY: Int = 350):
            GridPane<CardStack<CardView>>{
        val middleContainer = GridPane<CardStack<CardView>>(
            posX = posX,
            posY = posY,
            rows = 1, // Zwei Reihen für die nebeneinander liegenden Stacks
            columns = columns,
            visual = Visual.EMPTY // Transparenter Hintergrund
        ).apply {
            // Optional: Breite/Höhe des GridPane anpassen, falls nötig
            this.height = height // Die Höhe einer Karte
            this.width = 7 * width // ca. 3 * Kartenbreite
        }

        for (index in 0 until columns) {
            // Die Position des CardStacks ist hier irrelevant, da GridPane sie platziert.
            val childCardStack = CardStack<CardView>(
                height = height,
                width = width,
                posX = 0,
                posY = 0
            ).apply {
                visual = CompoundVisual(
                    ColorVisual(Color(255, 255, 255, 50)),
                    TextVisual(text = "TableauCard ${index + 1}")
                )
            }
            middleContainer.set(columnIndex = index, rowIndex = 0, component = childCardStack)
        }
        return middleContainer
    }

    /**
     * This method [button] creates normed buttons.
     *
     * @param text Text of the Button
     * @param type One of the following types of Button:
     * If type is:
     * 0 -> [MainScene] Join-, Load-Game Button.
     * 1 -> [MainMenuScene] and [GameTableauScene] Arrow-Select Button.
     * 2 -> [MainMenuScene]  Online-, Offline-Button.
     * 3 -> [MainScene] Start new Game Button.
     * 4 -> [MainMenuScene] Close Book Button.
     * 5 -> [MainMenuScene] Red Arrow Button.
     * 6 -> ...
     * @param posX X-Position of the Button.
     * @param posY Y-Position of the Button.
     *
     * @return [Button] normed.
     */
    fun button(text: String, type: Int = 0, posX: Int = 810, posY: Int = 0): Button{
        if(type <= 4){
            return buttonInfoU4(text, type, posX, posY)
        }
        return buttonInfoO4(text, type, posX, posY)
    }

    /**
     * This method [buttonInfoO4] creates normed buttons.
     *
     * @param text Text of the Button
     * @param type One of the following types of Button:
     * If type is:
     * 5 -> [MainMenuScene] Red Arrow Button.
     * 6 -> ...
     * @param posX X-Position of the Button.
     * @param posY Y-Position of the Button.
     *
     * @return [Button] normed.
     */
    private fun buttonInfoO4(text: String, type: Int, posX: Int, posY: Int): Button {
        var width = 0
        var height = 0
        var size = 22
        var color: Color = Color.BLUE
        val family = "Arial"
        val fontWeight: Font.FontWeight = Font.FontWeight.BOLD
        var fontStyle: Font.FontStyle = Font.FontStyle.NORMAL
        var visual: Visual = ColorVisual.GRAY
        when (type) {
            // Red Arrow Button
            5 -> {
                width = 100
                height = 100
                size = 30
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
            }

            6 -> {
                width = 143
                height = 145
                size = 10
                color = Color.ORANGE
                visual = imageLoader.loadBackground(type)
            }

            7 -> {
                width = 534
                height = 110
                size = 60
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
                visual = imageLoader.loadBackground(4)
            }

            8 -> {
                width = 534
                height = 110
                size = 60
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
                visual = imageLoader.loadBackground(0)
            }
        }
        return Button(
            posX = posX,
            posY = posY,
            width = width,
            height = height,
            text = text,
            font = Font(size = size, color = color,family = family,fontWeight = fontWeight, fontStyle = fontStyle),
            visual = visual
        )
    }

    /**
     * This method [button] creates normed buttons.
     *
     * @param text Text of the Button
     * @param type One of the following types of Button:
     * If type is:
     * 0 -> [MainScene] Join-, Load-Game Button.
     * 1 -> [MainMenuScene] and [GameTableauScene] Arrow-Select Button.
     * 2 -> [MainMenuScene]  Online-, Offline-Button.
     * 3 -> [MainScene] Start new Game Button.
     * 4 -> [MainMenuScene] Close Book Button.
     * 5 -> [MainMenuScene] Red Arrow Button.
     * @param posX X-Position of the Button.
     * @param posY Y-Position of the Button.
     *
     * @return [Button] normed.
     */
    private fun buttonInfoU4(text: String, type: Int, posX: Int, posY: Int): Button {
        var width = 0
        var height = 0
        var size = 22
        var color: Color = Color.BLUE
        val family = "Arial"
        val fontWeight: Font.FontWeight = Font.FontWeight.BOLD
        var fontStyle: Font.FontStyle = Font.FontStyle.NORMAL
        var visual: Visual = ColorVisual.GRAY
        when (type) {
            // Join-, Load-Game Button
            0 -> {
                width = 300
                height = 75
                size = 30
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
                visual = imageLoader.loadBackground(type)
            }
            // Arrow-Select Button
            1 -> {
                width = 50
                height = 50
                size = 30
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
            }
            // Online-, Offline-Button
            2 -> {
                width = 410
                height = 125
                size = 60
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
                visual = Visual.EMPTY
            }
            // Start new Game Button
            3 -> {
                width = 300
                height = 75
                size = 30
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
                visual = imageLoader.loadBackground(4)
            }
            // Close Book Button
            4 -> {
                width = 200
                height = 50
                size = 40
                color = Color.ORANGE
                fontStyle = Font.FontStyle.ITALIC
            }
        }
        return Button(
            posX = posX,
            posY = posY,
            width = width,
            height = height,
            text = text,
            font = Font(size = size,color = color,family = family,fontWeight = fontWeight,fontStyle = fontStyle),
            visual = visual
        )
    }

    /**
     * This method [middleGrid] creates the region Stacks in the middle of the Scene [GameScene].
     * ([playerCount] + 1 Region cards).
     *
     * @param playerCount Amount of Player.
     *
     * @return [GridPane] of [CardStack] of [CardView] (The regions in the middle of the table).
     */
    fun middleGrid(playerCount: Int): GridPane<CardStack<CardView>> {
        val middleContainer = GridPane<CardStack<CardView>>(
            posX = 960,
            posY = 540,
            rows = 2, // Zwei Reihen für die nebeneinander liegenden Stacks
            columns = 4,
            visual = Visual.EMPTY // Transparenter Hintergrund
        ).apply {
            // Optional: Breite/Höhe des GridPane anpassen, falls nötig
            this.height = 155.0 // Die Höhe einer Karte
            this.width = 7 * 155.0 // ca. 3 * Kartenbreite
        }

        // 3. hinzufügen der 3 CardStacks zum GridPane
        for (index in 0 until playerCount+1) {
            // Die Position des CardStacks ist hier irrelevant, da GridPane sie platziert.
            val childCardStack = CardStack<CardView>(
                height = 150,
                width = 150,
                posX = 0,
                posY = 0
            ).apply {
                visual = CompoundVisual(
                    ColorVisual(Color(255, 255, 255, 50)),
                    TextVisual(text = "MiddleCard ${index + 1}")
                )
            }

            // 4. Hinzufügen des Kind-Stacks zum GridPane
            if(index < 4){
                middleContainer.set(columnIndex = index, rowIndex = 0, component = childCardStack)
            }else{
                middleContainer.set(columnIndex = index%4, rowIndex = 1, component = childCardStack)
            }
        }
        return middleContainer
    }

    /**
     * This method [handStack] creates and returns a normed [MutableList] of [GridPane] of [CardStack] of [CardView]
     * Object. It creates the handStack (all 3 stacks) with the drag able function for the [GameScene] class.
     *
     * @return [MutableList] of [GridPane] of [CardStack] of [CardView], the handStack with the functionality.
     */
    fun handStack(playerCount: Int, cardCounter: Int): MutableList<GridPane<CardStack<CardView>>> {

        val allPlayerContainers: MutableList<GridPane<CardStack<CardView>>> = mutableListOf()

        for(playerIndex in 0 until playerCount){
            val infos = getInfo(playerIndex)
            val posX = infos.first
            val posY = infos.second
            val rotation = infos.third

            val handContainer = GridPane<CardStack<CardView>>(
                posX = posX,
                posY = posY,
                rows = 2,
                columns = cardCounter,
                visual = Visual.EMPTY
            ).apply {
                this.rotation = rotation

                this.height = 155.0
                this.width = cardCounter * 155.0
            }

            for(index in 0 until cardCounter){
                val childCardStack = CardStack<CardView>(
                    height = 150,
                    width = 150,
                    posX  = 0,
                    posY = 0
                ).apply {
                    visual = CompoundVisual(
                        ColorVisual(Color(255, 255, 255, 50)),
                        TextVisual(text = "HandCard ${index + 1}")
                    )
                }

                handContainer.set(columnIndex = index, rowIndex = 1, component = childCardStack)
            }
            val playedCard = CardStack<CardView>(
                height = 150,
                width = 150,
                posX  = 0,
                posY = 0
            ).apply {
                visual = CompoundVisual(
                    ColorVisual(Color(255, 255, 255, 50)),
                    TextVisual(text = "PlayedCard")
                )
            }
            handContainer.set(columnIndex = 1, rowIndex = 0, component = playedCard)
            allPlayerContainers.add(handContainer)
        }
        return allPlayerContainers
    }

    /**
     * Generates and positions labels for player names based on the total player count.
     *
     * This function retrieves base coordinates for each player and applies specific
     * offsets and rotations to ensure the labels are correctly aligned with the
     * game's UI layout (e.g., rotating text for players seated at the sides or top
     * of the digital table).
     *
     * ### Positioning Logic:
     * * **Index 0:** Adjusted vertically for the bottom position.
     * * **Index 1, 4, 5:** Rotated (180°) for players at the top of the screen.
     * * **Index 2 & 3:** Rotated and offset for side-table positioning (90° / -90°).
     *
     * @param playerCount The number of players currently in the game.
     * @return A [MutableList] of [Label] objects, each configured with its specific
     * coordinates and rotation.
     */
    fun showPlayerNames(playerCount: Int): MutableList<Label>{
        val allPlayerNames: MutableList<Label> = mutableListOf()

        for(playerIndex in 0 until playerCount){
            val infos = getInfo(playerIndex)
            var posX = infos.first
            var posY = infos.second
            var rotation = infos.third
            var align = Alignment.TOP_LEFT

            when(playerIndex){
                0 -> {
                    posX += 100
                    posY -= 50
                }
                1 -> {
                    posX += 100
                    posY += 10
                    rotation += 180.0
                }
                2 -> {
                    posY += 80
                    posX += 20
                    rotation -= 90.0
                }
                3 -> {
                    posX -= 320
                    posY += 80
                    rotation += 90.0
                    align = Alignment.TOP_RIGHT
                }
                4 -> {
                    rotation += 180.0
                    posX -= 250
                    posY -= 170
                    align = Alignment.TOP_CENTER
                }
                5 -> {
                    posX -= 55
                    posY -= 170
                    rotation += 180.0
                    align = Alignment.TOP_CENTER
                }
            }

            val playerLabel = label(text = "Player${playerIndex}", type = 4, posX = posX, posY = posY).apply {
                this.rotation = rotation
                alignment = align
            }
            allPlayerNames += playerLabel
        }
        return allPlayerNames
    }

    /**
     * This method [regionDrawStack] creates the drawStack of Region cards in [GameScene].
     *
     * @return [CardStack] of [CardView] The drawStack of Region cards.
     */
    fun regionDrawStack(): CardStack<CardView> {
        return CardStack<CardView>(
            height = 150,
            width = 150,
            posX = 1300,
            posY = 905
        ).apply {
            visual = CompoundVisual(
                ColorVisual(Color(255, 255, 255, 50)),
                TextVisual(text = "RegionDraw")
            )
        }
    }

    /**
     * This method [sanctuaryDrawStack] creates the drawStack of Sanctuary cards in [GameTableauScene].
     *
     * @return [CardStack] of [CardView] The drawStack for Sanctuary cards.
     */
    fun sanctuaryDrawStack(): CardStack<CardView> {
        return CardStack<CardView>(
            height = 150,
            width = 140,
            posX = 1300,
            posY = 150
        ).apply {
            visual = CompoundVisual(
                ColorVisual(Color(255, 255, 255, 50)),
                TextVisual(text = "SanctuaryDraw")
            )
        }
    }

    /**
     * This method [getInfo] delivers the positions of each player Grids ([handStack]).
     * @param player Index to calculate the Players Position, Rotation.
     *
     * @return [Triple] of X-Position, Y-Position, Rotation.
     */
    private fun getInfo(player: Int): Triple<Int, Int, Double>{
        var posX = 0
        var posY = 0
        var rotation = 0.0

        when(player){
            // Spieler unten
            0 -> {
                posX = 960
                posY = 905
                rotation = 0.0
            }
            // Spieler oben
            1 -> {
                posX = 960
                posY = 175
                rotation = 180.0
            }
            // Spieler links
            2 -> {
                posX = 175
                posY = 700
                rotation = 90.0
            }
            // Spieler rechts
            3 -> {
                posX = 1745
                posY = 700
                rotation = -90.0
            }
            // Spieler oben links
            4 -> {
                posX = 375
                posY = 285
                rotation = 145.0
            }
            // Spieler oben rechts
            5 -> {
                posX = 1545
                posY = 285
                rotation = 215.0
            }
        }

        return Triple(posX, posY, rotation)
    }
}