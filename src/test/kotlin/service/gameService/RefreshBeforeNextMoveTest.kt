package service.gameService

import entity.*
import org.junit.jupiter.api.assertThrows
import service.RootService
import service.TestRefreshable
import kotlin.test.*

/**
 * Test class for the `refreshBeforeNextMove()` function.
 */
class RefreshBeforeNextMoveTest {

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
     * Tests that an [IllegalArgumentException] is thrown if no game has been loaded.
     */
    @Test
    fun testRefreshBeforeNextMoveNoGame() {
        rootService.currentGame = null

        assertThrows<IllegalArgumentException> {
            rootService.gameService.refreshBeforeNextMove()
        }
    }

    /**
     * Tests that `refreshBeforePlayRegionCardCalled` is called when the game history is empty.
     * This simulates the very start of the game.
     */
    @Test
    fun testRefreshBeforeNextMoveGameStart() {
        rootService.gameService.refreshBeforeNextMove()

        assertTrue(testRefreshable.refreshBeforePlayRegionCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseSanctuaryCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseRegionCardCalled)
    }

    /**
     * Tests that `refreshBeforeChooseRegionCard` is called when the game is in Phase 1.
     */
    @Test
    fun testRefreshBeforeNextMovePhaseOne() {
        // Arrange
        val player = Player("Alice", PlayerType.LOCAL)
        val gameState = GameState(
            players = mutableListOf(player),
            currentPlayer = 0,
            isPhaseTwo = false // Phase 1
        )

        testGame.gameHistory.add(gameState)
        testGame.gameHistoryIndex = 0
        testGame.currentGameState = gameState

        // Act
        rootService.gameService.refreshBeforeNextMove()

        // Assert
        assertTrue(testRefreshable.refreshBeforePlayRegionCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseSanctuaryCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseRegionCardCalled)
    }

    /**
     * Tests that `refreshBeforeChooseSanctuaryCard` is called in Phase 2
     * when the player has already played the required number of region cards (equal to round number).
     */
    @Test
    fun testRefreshBeforeNextMoveChooseSanctuary() {
        // Arrange
        val player = Player("Alice", PlayerType.LOCAL)

        // Simulating choosing a starting hand
        repeat(5){
            player.hand.add(createDummyRegionCard())
        }

        val gameState = GameState(
            players = mutableListOf(player),
            currentPlayer = 0,
            isPhaseTwo = true,
            currentRound = 2
        )

        testGame.gameHistory.add(gameState)
        testGame.gameHistoryIndex = 0
        testGame.currentGameState = gameState

        rootService.gameService.refreshBeforeNextMove()

        assertTrue(testRefreshable.refreshBeforeChooseStartingHandCalled)
        assertFalse(testRefreshable.refreshBeforeChooseSanctuaryCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseRegionCardCalled)
        assertFalse(testRefreshable.refreshBeforePlayRegionCardCalled)
    }

    /**
     * Tests that `refreshBeforeChooseSanctuaryCard` is called in Phase 2
     * when the player has already played the required number of region cards (equal to round number).
     */
    @Test
    fun testRefreshBeforeNextMoveChooseStartingHand() {
        // Arrange
        val player = Player("Alice", PlayerType.LOCAL)

        // Simulating Round 2: Player needs 2 cards played.
        // We add 2 cards to simulate they have already played them.
        player.hand.add(createDummyRegionCard())
        player.regionCards.add(createDummyRegionCard())
        player.temporarySanctuaries.add(createDummySanctuaryCard())

        val gameState = GameState(
            players = mutableListOf(player),
            currentPlayer = 0,
            isPhaseTwo = true,
            currentRound = 2
        )

        testGame.gameHistory.add(gameState)
        testGame.gameHistoryIndex = 0
        testGame.currentGameState = gameState

        rootService.gameService.refreshBeforeNextMove()

        assertTrue(testRefreshable.refreshBeforeChooseSanctuaryCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseRegionCardCalled)
        assertFalse(testRefreshable.refreshBeforePlayRegionCardCalled)
    }

    // --- Helper for creating cards ---

    private fun createDummyRegionCard(): RegionCard {
        return RegionCard(
            explorationTime = 1,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = emptyList(),
            quest = Quest(
                night = true,
                clue = true,
                fame = 42,
                wonders = emptyList(),
                biome = listOf(Biome.BLUE)
            )
        )
    }

    private fun createDummySanctuaryCard(): SanctuaryCard {
        return SanctuaryCard(
            cardId = 1,
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = emptyList(),
            quest = Quest(
                night = true,
                clue = true,
                fame = 42,
                wonders = emptyList(),
                biome = listOf(Biome.BLUE)
            )
        )
    }
}