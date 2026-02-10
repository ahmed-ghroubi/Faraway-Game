package gui

import entity.Player
import entity.PlayerType
import service.Refreshable
import service.RootService
import service.network.ConnectionState
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.event.KeyCode
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Represents the main application for the SoPra board game.
 * The application initializes the [RootService] and displays the scenes.
 *
 * @property rootService The rootService to connect with the other layers of the software.
 * @property gameTableauScene The Scene [GameTableauScene].
 * @property mainMenuScene The Menu Scene [MainMenuScene].
 * @property gameScene The Game Scene [GameScene].
 * @property mainScene The start Main Scene [MainScene].
 */
class SopraApplication : BoardGameApplication("SoPra Game"), Refreshable {
    /**
     * The root service instance. This is used to call service methods and access the entity layer.
     */
    private val rootService: RootService = RootService()

    /**
     * The gameTableauScene, gameScene, mainMenuScene, mainScene displayed in the application.
     */
    private val gameTableauScene: GameTableauScene = GameTableauScene(rootService).apply {
        onKeyPressed = {
            if(it.keyCode == KeyCode.ESCAPE){
                if(display.isVisible){
                    showSanctuaryDrawStack(show = false)
                }else{
                    showGameScene(actualScene)
                }
            }
        }
    }

    private val gameScene: GameScene = GameScene(rootService).apply {
        tableauDiary.onMouseClicked = {
            val gameState = rootService.currentGame?.currentGameState
            if(gameState != null && gameState.players[gameState.currentPlayer].playerType == PlayerType.LOCAL ){
                gameTableauScene.refreshScene(true)
                showGameScene(gameTableauScene)
            }
        }

        onKeyPressed = {
            if(it.keyCode == KeyCode.ESCAPE){
                if(display.isVisible){
                    showRegionDrawStack(show = false)
                }else{
                    showMenuScene(gameMenuScene)
                }
            }
        }
    }

    private val mainMenuScene: MainMenuScene = MainMenuScene(rootService).apply {
        onKeyPressed = {
            if(it.keyCode == KeyCode.ESCAPE){
                reset()
            }
        }
    }

    private val mainScene: MainScene = MainScene().apply {
        startNewGameButton.onMouseClicked = {
            mainMenuScene.firstPageEvent()
            showMenuScene(mainMenuScene)
        }
        loadGameButton.onMouseClicked = {
            rootService.fileService.loadGame()
            showGameScene(gameScene)
        }
        joinGameButton.onMouseClicked = {
            mainMenuScene.joinPageEvent()
            showMenuScene(mainMenuScene)
        }
    }

    private val gameMenuScene: GameMenuScene = GameMenuScene().apply {
        resumeGameButton.onMouseClicked = {
            hideMenuScene()
        }
        saveGameButton.onMouseClicked = {
            if(rootService.currentGame?.isOnline == false){
                rootService.fileService.saveGame()
                reset()
            }
        }
        exitGameButton.onMouseClicked = {
            reset()
        }
    }

    private val resultScene: ResultScene = ResultScene(rootService).apply {
        points.forEachIndexed { _, labels ->
            labels.forEach {
                it.onMouseClicked = {
                    gameTableauScene.todo.isVisible = false
                    gameTableauScene.refreshScene(true)
                    showGameScene(gameTableauScene)
                }
            }
        }
        onKeyPressed = {
            if(it.keyCode == KeyCode.ESCAPE){
                showMenuScene(gameMenuScene)
            }
        }

    }

    private var actualScene: BoardGameScene = gameScene

    private val waitingScene = WaitingScene()

    /**
     * Initializes the application by displaying the GameScene.
     */
    init {
        reset()

        rootService.addRefreshables(
            this,
            mainScene,
            mainMenuScene,
            gameScene,
            gameTableauScene,
            gameMenuScene,
            resultScene
        )

        this.showGameScene(mainScene)
    }

    /**
     * This method resets the whole Game.
     */
    private fun reset() {
        if (mainMenuScene.online || mainMenuScene.join || rootService.currentGame?.isOnline == true){
            rootService.networkService.disconnect()
            mainMenuScene.join = false
            mainMenuScene.online = false
        }
        rootService.currentGame = null
        actualScene = gameScene
        mainMenuScene.playerList.clear()
        hideMenuScene()
        showGameScene(mainScene)
    }

    /**
     * Updates the user interface immediately after the game has started.
     * * This method handles the transition from the main menu to the active gameplay
     * by hiding the [mainMenuScene] and displaying the [gameScene].
     */
    override fun refreshAfterGameStart(){
        this.hideMenuScene()
        this.showGameScene(gameScene)
        this.gameTableauScene.todo.isVisible = true
    }

    /**
     * Updates the user interface when the game session concludes.
     * Switches the [actualScene] to the [resultScene], hides any active menu
     * components, and displays the final result screen to the users.
     * @param winner The [Player] who won the game not used here.
     */
    override fun refreshAfterGameEnd(winner: Player) {
        actualScene = resultScene
        this.hideMenuScene()
        this.showGameScene(resultScene)
    }

    /**
     * Prepares the interface for the Sanctuary card selection phase.
     * This method updates the Label (to-do) to inform the player that they
     * must now choose a Sanctuary card. It acts as a visual prompt to
     * transition the user's focus to the sanctuary selection logic.
     *
     * Shows the [gameTableauScene]
     */
    override fun refreshBeforeChooseSanctuaryCard() {
        gameScene.todo.text = "TODO: Choose SanctuaryCard"
        showGameScene(gameTableauScene)
    }

    /**
     * Updates the UI state before a player is required to choose a region card.
     * Ensures that the [gameScene] is active and visible so the player can
     * interact with the available region card options.
     */
    override fun refreshBeforeChooseRegionCard() {
        showGameScene(gameScene)
    }

    /**
     * Updates the user interface after a player's turn has ended.
     * * This method ensures that the [gameScene] remains or becomes the active scene,
     * resetting or preparing the view for the next player's actions.
     */
    override fun refreshAfterTurnEnd() {
        showGameScene(gameScene)
    }

    override fun refreshAfterConnectionStateChange(newState: ConnectionState) {
        // Waiting for start Game (waitingScene)
        if(newState == ConnectionState.WAITING_FOR_INIT){
            hideMenuScene()
            showGameScene(waitingScene)
        }
    }
}

