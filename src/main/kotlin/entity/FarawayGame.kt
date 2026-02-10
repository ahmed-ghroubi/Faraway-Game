package entity

import service.bot.Bot
import java.io.Serializable

/**
 * Represents a Faraway game
 *
 * @param isOnline Indicates if the game is played online.
 * @param isSimpleVariant Indicates if the simple variant of the game is used.
 *
 * @property gameHistory List of GameStates.
 * @property simulationSpeed Speed of the simulation.
 * @property gameHistoryIndex Index to determine the currentGameState in gameHistory.
 * @property currentGameState Represents the currentGameState.
 */
class FarawayGame(val isOnline: Boolean, val isSimpleVariant: Boolean) : Serializable {
    val gameHistory: MutableList<GameState> = mutableListOf()
    var simulationSpeed: Int = 1000
    var gameHistoryIndex: Int = 0
    var currentGameState: GameState = GameState()
    val bots: MutableMap<String, Bot> = mutableMapOf()

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = -1950749518930862581L
    }
}