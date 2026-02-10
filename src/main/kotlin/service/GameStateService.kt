package service

/**
 * Implements the logic responsible for managing the game states.
 *
 * @param rootService The relation to the rootService
 */
class GameStateService(private val rootService: RootService) : AbstractRefreshingService() {
    /**
     * Restores a previous game state from the history based on player name .
     *
     * Moves the history index backwards (same player if possible, otherwise index 0),
     * resets the phase state, and refreshes the UI.
     *
     * Only allowed in offline games and if a previous state exists.
     */
    fun undo() {
        val game = checkNotNull(rootService.currentGame) { "There is no active game." }
        check(!game.isOnline) { "Undo is not allowed in online games." }

        check(game.gameHistoryIndex > 0) {
            "Undo is not possible because the history is already at its beginning."
        }

        val playerName = game.currentGameState.players[game.currentGameState.currentPlayer].name

        var index = game.gameHistoryIndex - 1

        while (index > 0 && game.gameHistory[index] .players[game.gameHistory[index].currentPlayer].name != playerName)
        {
            index--
        }

        game.gameHistoryIndex = index
        game.currentGameState = game.gameHistory[index].copy()
        game.currentGameState.isPhaseTwo = false

        onAllRefreshables { refreshAfterChange() }
    }


    /**
     * Restores the next game state from the history.
     *
     * Moves the history index forward by one, restores the state,
     * and refreshes the UI.
     *
     * Only allowed in offline games and if a future state exists.
     */
    fun redo() {

        val game = checkNotNull(rootService.currentGame) { "There is no active game." }
        check(!game.isOnline) { "Redo is not allowed in online games." }

        // Redo is only possible if we are not already at the newest state
        check(game.gameHistoryIndex < game.gameHistory.lastIndex) {
            "Redo is not possible because the history is already at its end."
        }

        // Move forward one snapshot in the history
        val newIndex = game.gameHistoryIndex + 1
        game.gameHistoryIndex = newIndex
        game.currentGameState = game.gameHistory[newIndex].copy()

        // Update the GUI
        onAllRefreshables { refreshAfterChange() }
    }
}
