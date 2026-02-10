package service.playerActionService

import org.junit.jupiter.api.assertThrows
import entity.*
import service.RootService
import service.TestRefreshable
import kotlin.test.*


/**
 * Test class for the `endTurn()` function.
 */
class EndTurnTest {

    private lateinit var rootService: RootService
    private lateinit var gameState: GameState
    private lateinit var player1: Player
    private lateinit var player2: Player
    private lateinit var testRefreshable : TestRefreshable

    /**
     * Sets up a game with 2 players in Round 1.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()

        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        player1 = Player("Player1", PlayerType.LOCAL)
        player2 = Player("Player2", PlayerType.LOCAL)

        // Fill hands initially
        repeat(3) {
            player1.hand.add(createDummyRegionCard(explorationTime = 10 + it))
            player2.hand.add(createDummyRegionCard(explorationTime = 20 + it))
        }

        gameState = GameState(mutableListOf(player1, player2))
        gameState.currentRound = 1
        gameState.currentPlayer = 0 // P1 starts

        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        game.currentGameState = gameState
        rootService.currentGame = game
    }

    /**
     * Tests successful turn completion for the first player (not end of round).
     *
     * Preconditions Simulated:
     * - Player has played 1 card (matching Round 1).
     * - Player has drawn back to 3 cards.
     * - No temporary sanctuaries.
     *
     * Expected: Current player index advances from 0 to 1.
     */
    @Test
    fun testEndTurnNextPlayer() {
        val card = player1.hand.removeAt(0)

        // Play card: Move from hand to regionCards
        player1.regionCards.add(card)
        player1.selectedCard = card

        // Draw card: Add back to hand (Hand size returns to 3)
        player1.hand.add(createDummyRegionCard(explorationTime = 5))

        player1.temporarySanctuaries.clear()

        rootService.playerActionService.endTurn()

        assertEquals(1, gameState.currentPlayer, "Control should pass to Player 2")
        assertTrue(testRefreshable.refreshAfterTurnEndCalled, "The refreshable should have been called.")
    }

    /**
     * Tests successful turn completion that triggers the end of the round.
     *
     * Scenario: Player 1 has already finished. Player 2 finishes now.
     * Expected: Player index resets to 0 (or next start player), Round logic triggers.
     */
    @Test
    fun testEndTurnEndOfRound() {
        val card = createDummyRegionCard(explorationTime = 1)
        val game = rootService.currentGame

        repeat(5) {
            game?.currentGameState?.regionDrawStack?.add(card)
        }

        // P1 finished
        player1.regionCards.add(card) // P1 played
        player1.selectedCard = card

        // P2 finishes now
        game?.currentGameState?.currentPlayer = 1
        player2.regionCards.add(player2.hand.removeAt(0)) // P2 plays
        player2.hand.add(card) // P2 draws
        player2.selectedCard = card

        rootService.playerActionService.endTurn()

        // Test if endRound() was called by checking whether the turnEnd refreshable was called
        assertFalse(testRefreshable.refreshAfterTurnEndCalled, "The refreshable shouldn't have been called.")
    }

    /**
     * Tests that [IllegalStateException] is thrown if the player hasn't played a card.
     */
    @Test
    fun testEndTurnCardNotPlayed() {
        assertThrows<IllegalStateException> {
            rootService.playerActionService.endTurn()
        }
        assertFalse(testRefreshable.refreshAfterTurnEndCalled, "The refreshable shouldn't have been called.")
    }

    /**
     * Tests that [IllegalStateException] is thrown if the player hasn't refilled their hand.
     */
    @Test
    fun testEndTurnCardNotDrawn() {
        // Player played, but forgot to draw
        player1.regionCards.add(player1.hand.removeAt(0))
        // Hand size is now 2. hasDrawnCard() requires 3.

        assertThrows<IllegalStateException> {
            rootService.playerActionService.endTurn()
        }
        assertFalse(testRefreshable.refreshAfterTurnEndCalled, "The refreshable shouldn't have been called.")
    }

    /**
     * Tests that [IllegalStateException] is thrown if a sanctuary selection is pending.
     */
    @Test
    fun testEndTurnWithPendingSanctuary() {
        // Valid play and draw
        player1.regionCards.add(player1.hand.removeAt(0))
        player1.hand.add(createDummyRegionCard(explorationTime = 10))

        // Player has a pending sanctuary
        player1.temporarySanctuaries.add(
            SanctuaryCard(
                1,
                night = false,
                clue = false,
                biome = Biome.RED,
                wonders = listOf(),
                quest = Quest(
                    night = true,
                    clue = true,
                    fame = 42,
                    wonders = emptyList(),
                    biome = listOf(Biome.BLUE)
                )
            )
        )

        assertThrows<IllegalStateException> {
            rootService.playerActionService.endTurn()
        }
        assertFalse(testRefreshable.refreshAfterTurnEndCalled, "The refreshable shouldn't have been called.")
    }

    // --- Helper for creating cards ---

    private fun createDummyRegionCard(explorationTime: Int): RegionCard {
        return RegionCard(
            explorationTime = explorationTime,
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
    /**
     * Checks that endTurn() triggers endGame() in round 8 when the round is complete.
     */
    @Test
    fun testEndTurnCallsEndGameInRound8() {
        // Arrange
        gameState.currentRound = 8
        gameState.currentPlayer = 0

        // Both players must already have 8 region cards to make roundEnded == true
        player1.regionCards.clear()
        player2.regionCards.clear()
        repeat(8) { player1.regionCards.add(createDummyRegionCard(10 + it)) }
        repeat(8) { player2.regionCards.add(createDummyRegionCard(20 + it)) }

        // No pending sanctuary selection
        player1.temporarySanctuaries.clear()
        player2.temporarySanctuaries.clear()

        // Act
        rootService.playerActionService.endTurn()

        // Assert
        assertTrue(testRefreshable.refreshAfterGameEndCalled, "endGame() should be triggered in round 8.")
    }

}