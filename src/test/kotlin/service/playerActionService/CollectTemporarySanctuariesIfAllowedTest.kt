package service

import entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test class for the private method
 * `collectTemporarySanctuariesIfAllowed()` in [PlayerActionService].
 *
 * The tests verify:
 * - Preconditions (active game required)
 * - Rule checks (minimum regions, increasing exploration time)
 * - Correct handling of side effects (draw stack and temporary sanctuaries)
 *
 * Since the method is private, reflection is used to invoke it.
 */
class CollectTemporarySanctuariesIfAllowedTest {

    /** Root service providing access to all game services */
    private lateinit var rootService: RootService

    /** Service under test */
    private lateinit var playerActionService: PlayerActionService

    /** Test players */
    private lateinit var player1: Player
    private lateinit var player2: Player

    /**
     * Sets up a minimal valid game state before each test.
     *
     * A game with two local players is created and assigned
     * as the current game in the [RootService].
     */
    @BeforeEach
    fun setup() {
        rootService = RootService()
        playerActionService = rootService.playerActionService

        player1 = Player("Leon", PlayerType.LOCAL)
        player2 = Player("Luca", PlayerType.LOCAL)

        val state = GameState(
            players = mutableListOf(player1, player2),
            currentPlayer = 0
        )

        val game = FarawayGame(
            isOnline = false,
            isSimpleVariant = false
        )
        game.currentGameState = state
        rootService.currentGame = game
    }


    /**
     * Invokes the private method `collectTemporarySanctuariesIfAllowed()`
     * via reflection.
     *
     * @return true if at least one temporary sanctuary was created,
     *         false otherwise
     *
     * @throws java.lang.reflect.InvocationTargetException
     *         if the invoked method throws an exception internally
     */
    private fun invokeCollectTemporarySanctuaries(): Boolean {
        val method = PlayerActionService::class.java
            .getDeclaredMethod("collectTemporarySanctuariesIfAllowed")
        method.isAccessible = true
        return method.invoke(playerActionService) as Boolean
    }


    /**
     * Creates a minimal [Quest] object for test purposes.
     */
    private fun mkQuest(): Quest =
        Quest(
            night = false,
            clue = false,
            fame = 0,
            wonders = emptyList(),
            biome = emptyList()
        )

    /**
     * Creates a [RegionCard] with the given exploration time and clue flag.
     *
     * @param explorationTime Exploration time of the region
     * @param clue Whether the region provides a clue symbol
     */
    private fun mkRegion(explorationTime: Int, clue: Boolean): RegionCard =
        RegionCard(
            explorationTime = explorationTime,
            prerequisites = emptyList(),
            night = false,
            clue = clue,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = mkQuest()
        )

    /**
     * Creates a [SanctuaryCard] for use in the sanctuary draw stack.
     *
     * @param cardId Unique identifier of the sanctuary card
     * @param clue Whether the sanctuary provides a clue symbol
     */
    private fun mkSanctuary(
        cardId: Int = 1,
        clue: Boolean
    ): SanctuaryCard =
        SanctuaryCard(
            cardId = cardId,
            night = false,
            clue = clue,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = mkQuest()
        )


    /**
     * Ensures that the method fails if no active game exists.
     *
     * Because the method is invoked via reflection, the expected
     * [IllegalStateException] is wrapped inside an
     * [java.lang.reflect.InvocationTargetException].
     */
    @Test
    fun `throws if no active game`() {
        rootService.currentGame = null

        val ex = assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
            invokeCollectTemporarySanctuaries()
        }

        val cause = ex.cause
        assertNotNull(cause)
        assertTrue(cause is IllegalStateException)
        assertTrue(cause!!.message!!.contains("No active game available"))
    }

    /**
     * Verifies that the method returns false if the player has
     * fewer than two region cards.
     *
     * No sanctuary cards must be drawn in this case.
     */
    @Test
    fun `returns false if player has less than two region cards`() {
        val state = rootService.currentGame!!.currentGameState
        val p = state.players[state.currentPlayer]

        p.regionCards.clear()
        p.regionCards.add(mkRegion(explorationTime = 1, clue = false))

        state.sanctuaryDrawStack.clear()
        state.sanctuaryDrawStack.addAll(
            mutableListOf(
                mkSanctuary(clue = false),
                mkSanctuary(clue = true),
                mkSanctuary(clue = false)
            )
        )

        val beforeStackSize = state.sanctuaryDrawStack.size
        val beforeTempSize = p.temporarySanctuaries.size

        val result = invokeCollectTemporarySanctuaries()

        assertFalse(result)
        assertEquals(beforeStackSize, state.sanctuaryDrawStack.size)
        assertEquals(beforeTempSize, p.temporarySanctuaries.size)
    }

    /**
     * Verifies that the method returns false if the exploration time
     * of the last region is not strictly greater than the previous one.
     */
    @Test
    fun `returns false if exploration time is not strictly increasing`() {
        val state = rootService.currentGame!!.currentGameState
        val p = state.players[state.currentPlayer]

        p.regionCards.clear()
        p.regionCards.add(mkRegion(3, false))
        p.regionCards.add(mkRegion(3, false)) // not strictly increasing

        state.sanctuaryDrawStack.clear()
        state.sanctuaryDrawStack.addAll(
            mutableListOf(
                mkSanctuary(clue = false),
                mkSanctuary(clue = true),
                mkSanctuary(clue = false)
            )
        )

        val beforeStackSize = state.sanctuaryDrawStack.size

        val result = invokeCollectTemporarySanctuaries()

        assertFalse(result)
        assertEquals(beforeStackSize, state.sanctuaryDrawStack.size)
        assertTrue(p.temporarySanctuaries.isEmpty())
    }

    /**
     * Verifies that exactly one sanctuary is drawn when the total
     * clue count is zero (due to the additional unconditional draw).
     */
    @Test
    fun `returns true and draws exactly 1 sanctuary when clueCount is 0`() {
        val state = rootService.currentGame!!.currentGameState
        val p = state.players[state.currentPlayer]

        p.regionCards.clear()
        p.regionCards.add(mkRegion(1, false))
        p.regionCards.add(mkRegion(2, false))

        p.sanctuaries.clear()

        state.sanctuaryDrawStack.clear()
        state.sanctuaryDrawStack.addAll(
            mutableListOf(
                mkSanctuary(clue = false),
                mkSanctuary(clue = true)
            )
        )

        val beforeStackSize = state.sanctuaryDrawStack.size

        val result = invokeCollectTemporarySanctuaries()

        assertTrue(result)
        assertEquals(1, p.temporarySanctuaries.size)
        assertEquals(beforeStackSize - 1, state.sanctuaryDrawStack.size)
    }

    /**
     * Verifies that the method draws (clueCount + 1) sanctuary cards
     * and returns true if all conditions are fulfilled.
     */
    @Test
    fun `draws clueCount plus 1 sanctuaries and returns true`() {
        val state = rootService.currentGame!!.currentGameState
        val p = state.players[state.currentPlayer]

        p.regionCards.clear()
        p.regionCards.add(mkRegion(1, true))   // +1 clue
        p.regionCards.add(mkRegion(2, false))  // +0 clue

        p.sanctuaries.clear()
        p.sanctuaries.add(mkSanctuary(clue = true)) // +1 clue

        val expectedDraws = 3

        state.sanctuaryDrawStack.clear()
        state.sanctuaryDrawStack.addAll(
            mutableListOf(
                mkSanctuary(clue = false),
                mkSanctuary(clue = false),
                mkSanctuary(clue = true),
                mkSanctuary(clue = false)
            )
        )

        val beforeStackSize = state.sanctuaryDrawStack.size

        val result = invokeCollectTemporarySanctuaries()

        assertTrue(result)
        assertEquals(expectedDraws, p.temporarySanctuaries.size)
        assertEquals(beforeStackSize - expectedDraws, state.sanctuaryDrawStack.size)
    }
}
