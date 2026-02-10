package gui

import service.Refreshable
import tools.aqua.bgw.core.BoardGameScene

/**
 * This class [MainScene] provides the Start of the Application of the Faraway Game.
 *
 * @property o The [Objecting] supports to create Objects.
 * @property sign The sign Background behind the Buttons.
 * @property startNewGameButton The Button to start a new Game (Open Settings).
 * @property joinGameButton The Button to join a Game (Open join Settings).
 * @property loadGameButton The Button to load a Game (Open load Settings).
 */
class MainScene: BoardGameScene(1920, 1080), Refreshable {
    private val o = Objecting()

    private val sign = o.label("", type = 3, posX = 610, posY = 195)
    val startNewGameButton = o.button(text = "Start New Game", type = 3, posY = 445)
    val joinGameButton = o.button(text = "Join Game", posY = 525)
    val loadGameButton = o.button(text = "Load Game", posY = 605)

    init{
        background = ImageLoader().loadBackground(2)
        addComponents(sign,startNewGameButton,joinGameButton,loadGameButton)
    }
}