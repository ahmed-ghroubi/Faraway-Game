package service.gameStateService

import entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import service.GameStateService
import service.Refreshable
import service.RootService
import org.junit.jupiter.api.assertThrows

/**
 * tests for [GameStateService.undo] and [GameStateService.redo].
 */
class UndoRedoTest {

    /**
     * Refreshable class to verify that undo/redo refresh callbacks are triggered.
     */
    private class TestRefreshable : Refreshable {
        var changeCalled = false
        override fun  refreshAfterChange() { changeCalled = true }

    }

    /**
     * Tests undo() normal behavior (jump back to previous state for same player) and no-game exception.
     */
    @Test
    fun undo() {
        val rootService = RootService()
        val service = GameStateService(rootService)

        val refreshable = TestRefreshable()
        service.addRefreshable(refreshable)
        val players = mutableListOf(Player("Ahmed", PlayerType.LOCAL), Player("meriam", PlayerType.LOCAL))

        val s0 = GameState(players = players.map { it.copy() }.toMutableList()).apply { currentPlayer = 0
            currentRound = 1 }
        val s1 = s0.copy().apply { currentPlayer = 1 }
        val s2 = s0.copy().apply { currentPlayer = 0 }
        val s3 = s0.copy().apply { currentPlayer = 1 } // current state

        val game = FarawayGame(isOnline = false, isSimpleVariant = false)


        val firstmess = assertThrows<IllegalStateException> { service.undo() }
        assertEquals("There is no active game.", firstmess.message)

        game.gameHistory.addAll(mutableListOf(s0, s1, s2, s3))
        game.gameHistoryIndex = 3
        game.currentGameState = s3.copy()

        rootService.currentGame = game

        service.undo()

        // undo from index 3 should jump to index 1 (same currentPlayer=1)
        assertEquals(1, game.gameHistoryIndex)
        assertEquals(1, game.currentGameState.currentPlayer)
        assertTrue(refreshable.changeCalled)

    }

    /**
     * Tests that undo() is rejected for online games.
     */
    @Test
    fun undo_not_online() {
        val root = RootService()
        val service = GameStateService(root)

        val game = FarawayGame(isOnline = true, isSimpleVariant = false)
        game.gameHistory.addAll(
            mutableListOf(
                GameState(players = mutableListOf(Player("Ahmed", PlayerType.LOCAL))),
                GameState(players = mutableListOf(Player("meriam", PlayerType.LOCAL)))
            )
        )
        game.gameHistoryIndex = 1
        game.currentGameState = game.gameHistory[1].copy()
        root.currentGame = game

        val thirdmess = assertThrows<IllegalStateException> { service.undo() }
        assertEquals("Undo is not allowed in online games.", thirdmess.message)
    }

    /**
     * Tests that undo() throws if already at the start of history.
     */
    @Test
    fun undo_not_possible_start_of_history() {

        val root = RootService()
        val service = GameStateService(root)

        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        game.gameHistory.add(GameState(players = mutableListOf(Player("Ahmed", PlayerType.LOCAL))))
        game.gameHistoryIndex = 0
        game.currentGameState = game.gameHistory[0].copy()
        root.currentGame = game

        val forthmess = assertThrows<IllegalStateException> { service.undo() }
        assertEquals(
            "Undo is not possible because the history is already at its beginning.",
            forthmess.message
        )
    }

    /**
     * Tests that undo() falls back to index 0 if no matching previous state exists.
     */

    @Test
    fun undo_to_start() {
        val rootService = RootService()
        val service = GameStateService(rootService)

        val players = mutableListOf(
            Player("Ahmed", PlayerType.LOCAL),
            Player("meriam", PlayerType.LOCAL)
        )

        // history contains ONLY player 0 states before the current one
        val s0 = GameState(players = players.map { it.copy() }.toMutableList()).apply { currentPlayer = 0 }
        val s1 = s0.copy().apply { currentPlayer = 0 }
        val s2 = s0.copy().apply { currentPlayer = 0 }

        // current state is player 1 means there is NO previous player 1 state in history
        val s3 = s0.copy().apply { currentPlayer = 1 }

        val game = FarawayGame(isOnline = false, isSimpleVariant = false).apply {
            gameHistory.addAll(mutableListOf(s0, s1, s2, s3))
            gameHistoryIndex = 3
            currentGameState = s3.copy()
        }

        rootService.currentGame = game

        service.undo()

        //  because no previous state for player 1 exists, undo must jump to index 0
        assertEquals(0, game.gameHistoryIndex)
        assertEquals(0, game.currentGameState.currentPlayer)
    }



    /**
     * Tests redo() normal behavior and no-game exception.
     */
    @Test
    fun redo() {
        val rootService = RootService()
        val service = GameStateService(rootService)

        val refreshable = TestRefreshable()
        service.addRefreshable(refreshable)

        val players = mutableListOf(Player("Ahmed", PlayerType.LOCAL), Player("wiem", PlayerType.LOCAL))

        val s0 = GameState(players = players.map { it.copy() }.toMutableList()).apply { currentPlayer = 0
            currentRound = 1 }
        val s1 = s0.copy().apply { currentPlayer = 1 }
        val s2 = s0.copy().apply { currentPlayer = 0 }

        val game = FarawayGame(isOnline = false, isSimpleVariant = false)

        val warning = assertThrows<IllegalStateException> { service.redo() }
        assertEquals("There is no active game.", warning.message)

        game.gameHistory.addAll(mutableListOf(s0, s1, s2))
        game.gameHistoryIndex = 1
        game.currentGameState = s1.copy()

        rootService.currentGame = game

        service.redo()

        assertEquals(2, game.gameHistoryIndex)
        assertEquals(0, game.currentGameState.currentPlayer)
        assertTrue(refreshable.changeCalled)
    }


    /**
     * Tests that redo() is rejected for online games.
     */
    @Test
    fun redo_not_online() {
        val root = RootService()
        val service = GameStateService(root)

        val game = FarawayGame(isOnline = true, isSimpleVariant = false)
        game.gameHistory.addAll(
            mutableListOf(
                GameState(players = mutableListOf(Player("Ahmed", PlayerType.LOCAL))),
                GameState(players = mutableListOf(Player("meriam", PlayerType.LOCAL)))
            )
        )
        game.gameHistoryIndex = 1
        game.currentGameState = game.gameHistory[1].copy()
        root.currentGame = game

        val fifthmess = assertThrows<IllegalStateException> { service.redo() }
        assertEquals("Redo is not allowed in online games.", fifthmess.message)

    }

    /**
     * Tests that redo() throws when redo is not possible at the end.
     */
    @Test
    fun redo_not_possible_start_of_history() {
        val root = RootService()
        val service = GameStateService(root)

        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        game.gameHistory.add(GameState(players = mutableListOf(Player("Ahmed", PlayerType.LOCAL))))
        game.gameHistoryIndex = 0
        game.currentGameState = game.gameHistory[0].copy()
        root.currentGame = game

        val sixthmess = assertThrows<IllegalStateException> { service.redo() }
        assertEquals(
            "Redo is not possible because the history is already at its end.",
            sixthmess.message
        )
    }
}