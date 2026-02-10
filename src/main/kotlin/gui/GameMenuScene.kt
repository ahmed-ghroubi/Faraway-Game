package gui

import service.Refreshable
import tools.aqua.bgw.core.MenuScene

/**
 * Represents the in-game overlay menu (pause menu).
 *
 * This scene provides the user interface for interrupting the current gameplay session.
 * It facilitates returning to the active match, persisting the game state to local storage,
 * or terminating the current session to return to the main menu.
 *
 * @property imageLoader Utility used to retrieve and render the visual assets for the menu background.
 * @property o UI factory object used to maintain a consistent visual style across menu components.
 * @property resumeGameButton Triggers the transition back to the active game scene.
 * @property saveGameButton Invokes the [service.FileService.saveGame] logic to store the current progress.
 * @property exitGameButton Handles the logic for exiting the current game session.
 */
class GameMenuScene : MenuScene(1920, 1080), Refreshable{
    private val imageLoader = ImageLoader()
    private val o = Objecting()

    val resumeGameButton = o.button(text = "Resume Game", type = 7, posX = 646, posY = 362)
    val saveGameButton = o.button(text = "Save Game", type = 8, posX = 646, posY = 477)
    val exitGameButton = o.button(text = "Exit Game", type = 8, posX = 646, posY = 592)

    init{
        background = imageLoader.loadBackground(5)
        addComponents(resumeGameButton,saveGameButton,exitGameButton)
    }
}