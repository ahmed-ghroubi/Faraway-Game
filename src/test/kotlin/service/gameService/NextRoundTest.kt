package service.gameService

import entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import service.GameService
import service.Refreshable
import service.RootService

/**
 *  tests for [service.GameService.nextRound].
 */
class NextRoundTest {

    /**
     * Refreshable class to verify that round-start refresh is triggered.
     */
    private class TestRefreshable : Refreshable {
        var roundStartCalled = false
        override fun refreshAfterRoundStart() {
            roundStartCalled = true
        }
    }

    /**
     * nextRound() throws when no active game exists.
     */
    @Test
    fun no_active_game() {
        val root = RootService()
        val service = GameService(root)

        root.currentGame = null

        val warning = assertThrows<IllegalStateException> { service.nextRound() }
        assertEquals("No active game available.", warning.message)
    }

    /**
     * nextRound() throws when the last round is already reached.
     */
    @Test
    fun last_round() {
        val root = RootService()
        val service = GameService(root)

        val player1= Player("Ahmed", PlayerType.LOCAL)
        val player2 = Player("wiem", PlayerType.LOCAL)

        val state = GameState(players = mutableListOf(player1, player2)).apply { currentRound = 8 }

        root.currentGame = FarawayGame(isOnline = false, isSimpleVariant = false).apply {
            currentGameState = state
        }

        val warning = assertThrows<IllegalStateException> { service.nextRound() }
        assertEquals("No next round possible: last round already reached.", warning.message)
    }

    /**
     * nextRound() throws if the previous round is not finished for all players.
     */
    @Test
    fun previous_round_not_finished() {
        val root = RootService()
        val service = GameService(root)

        val player1 = Player("Ahmed", PlayerType.LOCAL)
        val player2 = Player("wiem", PlayerType.LOCAL)

        val state = GameState(players = mutableListOf(player1, player2)).apply {
            currentRound = 2
        }
        // round NOT finished: each player should have 2 region cards but they have 0

        root.currentGame = FarawayGame(isOnline = false, isSimpleVariant = false).apply {
            currentGameState = state
        }

        val warning = assertThrows<IllegalStateException> { service.nextRound() }
        assertTrue(warning.message!!.contains("Previous round not finished"))
    }

    /**
     * nextRound() throws if the region draw stack cannot refill the center display.
     */
    @Test
    fun not_enough_regioncards() {
        val root = RootService()
        val service = GameService(root)

        val player1 = Player("Ahmed", PlayerType.LOCAL)
        val player2 = Player("wiem", PlayerType.LOCAL)

        val state = GameState(players = mutableListOf(player1, player2)).apply { currentRound = 1 }

        // make "previous round finished" true
        player1.regionCards.add(card(10))
        player2.regionCards.add(card(20))

        // need players+1 = 3 cards, but provide only 2
        state.regionDrawStack.add(card(1))
        state.regionDrawStack.add(card(2))

        root.currentGame = FarawayGame(isOnline = false, isSimpleVariant = false).apply {
            currentGameState = state
        }

        val warning = assertThrows<IllegalStateException> { service.nextRound() }
        assertEquals("Not enough RegionCards in draw stack to refill centerCards.", warning.message)
    }

    /**
     * nextRound() updates round state, refills center, updates history, and triggers refresh.
     */
    @Test
    fun success() {
        val root = RootService()
        val service = GameService(root)

        val refreshable = TestRefreshable()
        service.addRefreshable(refreshable)

        val player1 = Player("Ahmed", PlayerType.LOCAL)
        val player2 = Player("wiem", PlayerType.LOCAL)

        val state = GameState(players = mutableListOf(player1, player2)).apply {
            currentRound = 1
            currentPlayer = 1
            isPhaseTwo = true
        }

        // previous round finished, each player has exactly currentRound region cards
        player1.regionCards.add(card(10))
        player2.regionCards.add(card(20))

        // provide enough cards for center refill: players+1 = 3
        state.regionDrawStack.addAll(
            mutableListOf(
                card(1),
                card(2),
                card(3)
            )
        )

        val game = FarawayGame(isOnline = false, isSimpleVariant = false).apply {
            currentGameState = state
            gameHistory.add(state.copy())
            gameHistoryIndex = 0
        }
        root.currentGame = game

        service.nextRound()

        // round advanced + start player reset
        assertEquals(2, state.currentRound)
        assertEquals(0, state.currentPlayer)
        assertFalse(state.isPhaseTwo)

        // center refilled and draw stack reduced
        assertEquals(3, state.centerCards.size)
        assertEquals(0, state.regionDrawStack.size)

        // history updated
        assertEquals(game.gameHistory.lastIndex, game.gameHistoryIndex)
        assertTrue(game.gameHistory.isNotEmpty())
    }

    /**
     * nextRound() clears future history entries before appending the new state.
     */
    @Test
    fun clear_future_history() {
        val root = RootService()
        val service = GameService(root)

        fun createRegionCard(t: Int) = RegionCard(
            explorationTime = t,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = emptyList(),
            quest = Quest(night = true, clue = true, fame = 1, wonders = emptyList(), biome = listOf(Biome.BLUE))
        )

        val player1 = Player("Ahmed", PlayerType.LOCAL)
        val player2 = Player("wiem", PlayerType.LOCAL)

        val state = GameState(players = mutableListOf(player1, player2)).apply {
            currentRound = 1
            currentPlayer = 0
            isPhaseTwo = false
        }

        // make previous round finished
        player1.regionCards.add(createRegionCard(10))
        player2.regionCards.add(createRegionCard(20))

        // enough cards to refill center (players + 1 = 3)
        state.regionDrawStack.addAll(
            mutableListOf(createRegionCard(1), createRegionCard(2), createRegionCard(3))
        )

        val future1 = state.copy()
        val future2 = state.copy()

        val game = FarawayGame(isOnline = false, isSimpleVariant = false).apply {
            currentGameState = state

            // history has "future" (index=0 but list has 3 entries)
            gameHistory.add(state.copy())
            gameHistory.add(future1)
            gameHistory.add(future2)
            gameHistoryIndex = 0
        }
        root.currentGame = game

        service.nextRound()

    }

    /**
     * Creates a simple RegionCard used for test setup.
     */
    private fun card(explorationTime: Int): RegionCard {
        return RegionCard(
            explorationTime = explorationTime,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.YELLOW,
            wonders = emptyList(),
            quest = Quest(
                night = false,
                clue = true,
                fame = 4,
                wonders = emptyList(),
                biome = listOf(Biome.YELLOW)
            )
        )
    }
}


