package service.gameService

import entity.*
import org.junit.jupiter.api.assertThrows
import service.RootService
import service.TestRefreshable
import kotlin.test.*

/**
 * Test class for the `updatePlayerOrder()` function.
 */
class UpdatePlayerOrderTest {

    private lateinit var rootService: RootService
    private lateinit var testRefreshable: TestRefreshable
    private lateinit var testGame: FarawayGame

    /**
     * Sets up the RootService, TestRefreshable, and a basic Game instance before each test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        // Initialize a standard game
        testGame = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = testGame
    }

    /**
     * Tests that an [IllegalStateException] is thrown if no game has been loaded.
     */
    @Test
    fun testUpdatePlayerOrderNoGame() {
        rootService.currentGame = null

        assertThrows<IllegalStateException> {
            rootService.gameService.updatePlayerOrder()
        }
    }

    /**
     * Tests that players are correctly sorted by the exploration time of their last played RegionCard (ascending).
     * Also verifies that currentPlayer is reset to 0.
     */
    @Test
    fun testUpdatePlayerOrderSortsCorrectly() {
        val player1 = Player("SlowPlayer", PlayerType.LOCAL)
        val player2 = Player("FastPlayer", PlayerType.LOCAL)
        val player3 = Player("MediumPlayer", PlayerType.LOCAL)

        // Player 1 played a card with time 50
        player1.regionCards.add(createDummyRegionCard(50))
        // Player 2 played a card with time 10
        player2.regionCards.add(createDummyRegionCard(10))
        // Player 3 played a card with time 30
        player3.regionCards.add(createDummyRegionCard(30))

        // Current order is random/insertion order: P1, P2, P3
        val gameState = GameState(
            players = mutableListOf(player1, player2, player3),
            currentPlayer = 2 // Pointing to the last player
        )
        testGame.currentGameState = gameState
        testGame.gameHistory.add(gameState)
        testGame.gameHistoryIndex = 0

        rootService.gameService.updatePlayerOrder()

        val sortedPlayers = testGame.currentGameState.players

        // Expected Order: Fast (10) -> Medium (30) -> Slow (50)
        assertEquals(player2, sortedPlayers[0], "First player should be FastPlayer (Time 10)")
        assertEquals(player3, sortedPlayers[1], "Second player should be MediumPlayer (Time 30)")
        assertEquals(player1, sortedPlayers[2], "Third player should be SlowPlayer (Time 50)")

        // Verify currentPlayer index reset
        assertEquals(0, testGame.currentGameState.currentPlayer, "Current player index must be reset to 0")

        // Verify Refreshable call
        assertTrue(testRefreshable.refreshAfterRoundEndCalled, "refreshAfterRoundEnd should be called")
    }



    /**
     * Tests behavior when a player has no region cards.
     */
    @Test
    fun testUpdatePlayerOrderFailsWithNoCards() {
        val player = Player("Alice", PlayerType.LOCAL)

        // No cards added to player.regionCards

        val gameState = GameState(players = mutableListOf(player))
        testGame.currentGameState = gameState

        assertThrows<IllegalStateException> {
            rootService.gameService.updatePlayerOrder()
        }
    }

    // --- Helper for creating cards ---

    private fun createDummyRegionCard(time: Int): RegionCard {
        return RegionCard(
            explorationTime = time,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = emptyList(),
            quest = Quest(
                night = false,
                clue = false,
                fame = 0,
                wonders = emptyList(),
                biome = emptyList()
            )
        )
    }
}
