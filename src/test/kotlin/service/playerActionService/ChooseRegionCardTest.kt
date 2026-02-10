package service.playerActionService

import entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import service.*
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests the [service.PlayerActionService.chooseRegionCard] method.
 */
class ChooseRegionCardTest {

    private lateinit var rootService: RootService
    private lateinit var gameService: GameService
    private lateinit var playerActionService: PlayerActionService

    /**
     * Initializes the necessary services before each test execution.
     */
    @BeforeEach
    fun setUp() {
        rootService = RootService()
        gameService = GameService(rootService)
        playerActionService = PlayerActionService(rootService)
    }

    /**
     * Helper function to create a dummy region card with a valid Quest object.
     */
    private fun createDummyCard() = RegionCard (
        explorationTime = 1,
        prerequisites = emptyList(),
        night = false,
        clue = false,
        biome = Biome.RED,
        wonders = emptyList(),
        quest = Quest(
            night = false,
            clue = false,
            fame = 0,
            wonders = emptyList(),
            biome = emptyList()
        )
    )

    /**
     * Verifies that calling the method without an active game throws an exception.
     */
    @Test
    fun testNoActiveGame() {
        rootService.currentGame = null
        val card = createDummyCard()

        assertFailsWith<IllegalStateException> {
            playerActionService.chooseRegionCard(card)
        }
    }

    /**
     * Ensures that the method fails if the current player index is invalid.
     */
    @Test
    fun testInvalidPlayerIndex() {
        val players = listOf(Player("Rango", PlayerType.LOCAL), Player("Dylan", PlayerType.LOCAL))
        gameService.createGame(isSimple = true, isOnline = false, randomOrder = false, players = players)

        val game = rootService.currentGame!!
        game.currentGameState.currentPlayer = 99

        val card = createDummyCard()
        game.currentGameState.centerCards.add(card)

        assertFailsWith<IllegalStateException> {
            playerActionService.chooseRegionCard(card)
        }
    }

    /**
     * Verifies that an exception is thrown if the selected card is not in the center.
     */
    @Test
    fun testCardNotInCenter() {
        val players = listOf(Player("Rango", PlayerType.LOCAL), Player("Dylan", PlayerType.LOCAL))
        gameService.createGame(isSimple = false, isOnline = false, randomOrder = false, players = players)

        val card = createDummyCard()
        assertFailsWith<IllegalArgumentException> {
            playerActionService.chooseRegionCard(card)
        }
    }

    /**
     * Verifies the successful execution: the card moves from the center to the players hand.
     */
    @Test
    fun testChooseRegionCardSuccess() {
        val players = listOf(Player("Rango", PlayerType.LOCAL), Player("Dylan", PlayerType.LOCAL))
        gameService.createGame(isSimple = false, isOnline = false, randomOrder = false, players = players)

        val state = rootService.currentGame!!.currentGameState
        val currentPlayer = state.players[0]

        val targetCard = createDummyCard()
        state.centerCards.add(targetCard)

        val initialCenterSize = state.centerCards.size
        val initialHandSize = currentPlayer.hand.size

        assertDoesNotThrow {
            playerActionService.chooseRegionCard(targetCard)
        }

        assertFalse(state.centerCards.contains(targetCard))
        assertTrue(currentPlayer.hand.contains(targetCard))
        assertEquals(initialCenterSize - 1, state.centerCards.size)
        assertEquals(initialHandSize + 1, currentPlayer.hand.size)
    }

}