package gui

import entity.Player
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene

/**
 * The [ResultScene] displays the final screen of the game.
 *
 * This scene highlights the winner and provides a detailed score table for all
 * participating players. It implements the [Refreshable] interface to automatically
 * update its components when the game concludes.
 *
 * @param rootService The [RootService] used to access the game logic and current state.
 *
 * @property o Helper object of type [Objecting] to create and manage UI components.
 * @property winnerName Label displaying the name of the winning player.
 * @property winnerScore Label displaying the total points achieved by the winner.
 * @property points A 2D matrix (array of arrays) of Labels representing names and individual scores in a table format.
 * @property yellowBox A list of visual components (e.g., backgrounds or frames) used to structure the layout.
 */
class ResultScene(private val rootService: RootService): BoardGameScene(1920, 1080), Refreshable {
    private val o = Objecting()
    private val winnerName = o.label("Spielername", posX = 1200, posY = 550)
    private val winnerScore = o.label("Spieler Punkte", posX = 1200, posY = 650)
    val points = pointing()
    private val yellowBox = resultYellowBox()

    init{
        background = ImageLoader().loadBackground(14)

        // Add winner labels
        addComponents(winnerName,winnerScore)

        // Add the scoring matrix components to the scene
        points.forEach {
            it.forEach { component ->
                addComponents(component)
            }
        }
        yellowBox.forEach { addComponents(it) }
    }

    /**
     * This method creates the Labels for the Scoreboard in [ResultScene].
     */
    private fun pointing(): Array<Array<Label>> {
        val rows = 10
        val cols = 6
        val startX = 200
        val startY = 175
        return Array(rows) { rowIndex ->
            Array (cols) { colIndex ->
                val colLine = when(rowIndex){
                    0,1,2,3,4 -> colIndex*15
                    5 -> colIndex*15+10
                    else -> colIndex*15+20
                }
                val rowLine = when(rowIndex){
                    0 -> 0
                    else -> rowIndex*15
                }
                Label(
                    width = 100,
                    height = 50,
                    posX = startX + 100*colIndex+rowLine,
                    posY = startY + 70*rowIndex-colLine,
                    text = ""
                ).apply {
                    rotate(-15)
                }
            }
        }
    }

    /**
     * This method creates the Labels for the Scoreboard (Yellow-Boxes) in [ResultScene].
     */
    private fun resultYellowBox(): Array<Label> {
        val cols = 6

        return Array(cols) { colIndex ->
            Label(
                width = 100,
                height = 50,
                posX =  390+103*colIndex,
                posY = 890-20*colIndex,
                text = ""
            ).apply {
                rotate(-15)
            }
        }
    }

    /**
     * Updates the scene after the game ends with the winner's data and the full score table.
     *
     * This method is triggered by the service layer. It populates the [points] table
     * with player names and their respective score entries from the [Player] entity.
     *
     * @param winner The [Player] who won the game.
     */
    override fun refreshAfterGameEnd(winner: Player) {
        winnerName.text = winner.name
        winnerScore.text = winner.score.last().toString()

        points.forEach { array ->
            array.forEach { label ->
                label.text = ""
            }
        }
        yellowBox.forEach { label ->
            label.text = ""
        }

        val game = rootService.currentGame?.currentGameState ?: return
        game.players.forEachIndexed { index, player ->
            points[0][index].text = player.name
            for(pointIndex in 1 until player.score.size){
                points[pointIndex][index].text = player.score[pointIndex-1].toString()
            }
            yellowBox[index].text = player.score.last().toString()
        }
    }
}