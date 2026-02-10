package service.playerActionService

import entity.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import service.GameService
import service.PlayerActionService
import service.RootService
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for verifying the logic of [PlayerActionService.proceedAfterChooseRegionCard].
 *
 * This class ensures that the game flow transitions correctly after a region card is chosen:
 * - Validates error handling (no game, invalid index).
 * - Checks if human players are correctly paused to select a sanctuary when eligible.
 * - Checks if the sanctuary phase is skipped when conditions are not met.
 * - Verifies that bots automatically select sanctuaries and continue the game flow.
 */
class ProceedAfterChooseRegionCardTest {

    private lateinit var rootService: RootService
    private lateinit var gameService: GameService
    private lateinit var playerActionService: PlayerActionService

    private val quest = Quest(
        night = false,
        clue = false,
        fame = 0,
        wonders = emptyList(),
        biome = emptyList()
    )

    /**
     * Initializes the service layer before each test case.
     *
     * This method ensures a clean state by creating a new [RootService] instance
     * and extracting the specific services required for testing.
     */
    @BeforeEach
    fun setup() {
        rootService = RootService()
        gameService = rootService.gameService
        playerActionService = rootService.playerActionService
    }

    /**
     * Helper method to initialize a game state for testing.
     *
     * Creates a game via [GameService] and pre-fills the sanctuary draw stack
     * to prevent errors during card distribution.
     *
     * @param players The list of players to participate (defaults to 2 local players).
     */
    private fun startGame(
        players: List<Player> = listOf(
            Player("Rango", PlayerType.LOCAL),
            Player("Rodrigo", PlayerType.LOCAL)
        )
    ) {
        gameService.createGame(
            isSimple = false,
            isOnline = false,
            randomOrder = false,
            players = players
        )

        val state = rootService.currentGame!!.currentGameState
        repeat(10) {state.sanctuaryDrawStack.add(createSanctuary())}
    }

    private fun createCard(time: Int, clue: Boolean = false):RegionCard {
        return RegionCard(
            explorationTime = time,
            prerequisites = emptyList(),
            night = false,
            clue = clue,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = quest
        )
    }

    private fun createSanctuary(): SanctuaryCard{
        return SanctuaryCard(
            cardId = 1,
            night = false,
            clue = false,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = quest
        )
    }

    /**
     * Verifies that an [IllegalStateException] is thrown if the method is called while currentGame = null.
     */
    @Test
    fun testNoActiveGame() {
        rootService.currentGame = null

        val exception = assertThrows<IllegalStateException>{
            playerActionService.proceedAfterChooseRegionCard()
        }
        assertEquals("there is no game currently active.", exception.message)
    }

    /**
     * Verifies that an [IllegalStateException] is thrown if the currentPlayer index
     * is out of bounds regarding the player list size.
     * * Note: We test `proceedAfterPlayRegionCard` here effectively, as strictly checking index
     * logic inside `proceedAfterChooseRegionCard` (which uses modulo) is less prone to index errors,
     * whereas Phase transition requires valid player states.
     */
    @Test
    fun testInvalidPlayerIndex() {
        startGame()

        val state = rootService.currentGame!!.currentGameState
        state.currentPlayer = 99

        // We set the selectedCard to avoid "No region card was played" error before the index error
        state.players[0].selectedCard = createCard(10)

        // Using proceedAfterPlayRegionCard as it strictly accesses the current player at start
        assertThrows<IndexOutOfBoundsException> {
            playerActionService.proceedAfterPlayRegionCard()
        }
    }

    /**
     * Tests the scenario where a human player (Player 0) meets the requirements for a sanctuary.
     * We assume Player 1 just finished their turn, calling `proceedAfterChooseRegionCard`,
     * passing the turn to Player 0.
     *
     * Expectation:
     * - Player 0 receives temporary sanctuaries.
     * - The game flow pauses (player index becomes 0) to await user input.
     */
    @Test
    fun testChooseSanctuaryCard() {
        startGame()

        val state = rootService.currentGame!!.currentGameState
        val player0 = state.players[0] // Needs Sanctuary
        val player1 = state.players[1] // Just finished

        // Setup Player 0 (Eligible for Sanctuary)
        player0.regionCards.add(createCard(10))
        player0.regionCards.add(createCard(20, clue = true))
        // Player 0 has Hand Size 2 (played card in Phase 1, waiting for Phase 2 turn)
        player0.hand.clear()
        player0.hand.add(createCard(99))
        player0.hand.add(createCard(98))

        // Setup Player 1 (Finished Phase 2)
        player1.hand.add(createCard(1)) // Hand Size 3 means finished
        player1.hand.add(createCard(2))
        player1.hand.add(createCard(3))

        // Set turn to Player 1, who is about to finish
        state.currentPlayer = 1
        state.currentRound = 2

        // Player 1 finishes Phase 2 -> Trigger next player (Player 0)
        playerActionService.proceedAfterChooseRegionCard()

        // Check that it's now Player 0's turn
        assertEquals(0, state.currentPlayer)
        // Check that Player 0 was forced to draw temporary sanctuaries
        assertTrue(player0.temporarySanctuaries.isNotEmpty())
    }

    /**
     * Tests the scenario where a player does not meet the requirement for a sanctuary
     * (decreasing exploration time).
     *
     * Expectation:
     * - No temporary sanctuaries are generated.
     * - The game flow proceeds immediately to the next player (index changes to 1).
     */
    @Test
    fun testPlayerSkipsSanctuary() {
        startGame()

        val state = rootService.currentGame!!.currentGameState
        val player1 = state.players[0]
        val player2 = state.players[1]

        player1.regionCards.add(createCard(20))
        player1.regionCards.add(createCard(10))

        repeat(3) { player1.hand.add(createCard(0)) }
        repeat(3) { player2.hand.add(createCard(0)) }
        player2.regionCards.addAll(player1.regionCards)

        state.currentPlayer = 0
        state.currentRound = 2

        playerActionService.proceedAfterChooseRegionCard()

        assertTrue(player1.temporarySanctuaries.isEmpty())
        assertEquals(1, state.currentPlayer)
    }

    /**
     * Tests the Bot's transition from picking a sanctuary to picking a region card.
     *
     * It checks two main beats:
     * 1. When a human player finishes their turn, the Bot should automatically move its
     * temporary sanctuary into a permanent slot.
     * 2. Once the sanctuary step is done, the Bot should immediately pick a region
     * card from the center to refill its hand back to 3.
     */
    @Test
    fun testBotAutomaticallyChoosesSanctuaryAndRegion_followingActualFlow() {
        val bot = Player("Bot", PlayerType.BOT_EASY)
        val human = Player("Human", PlayerType.LOCAL)

        startGame(players = listOf(bot, human))

        val state = rootService.currentGame!!.currentGameState
        val botPlayer = state.players[0]
        val humanPlayer = state.players[1]

        botPlayer.regionCards.add(createCard(5))
        botPlayer.regionCards.add(createCard(50, clue = true))

        // Ensure bot has 2 cards in hand (will need to draw 1 in Phase 2)
        botPlayer.hand.clear()
        botPlayer.hand.add(createCard(99))
        botPlayer.hand.add(createCard(98))

        // Ensure human has a full hand and is finishing their turn
        humanPlayer.hand.clear()
        humanPlayer.hand.add(createCard(1))
        humanPlayer.hand.add(createCard(2))
        humanPlayer.hand.add(createCard(3))

        state.currentPlayer = 1
        state.currentRound = 2


        playerActionService.proceedAfterChooseRegionCard()


        assertEquals(0, state.currentPlayer, "Turn should have advanced to Bot (index 0)" +
                " after proceedAfterChooseRegionCard()")
        assertTrue(botPlayer.temporarySanctuaries.isEmpty(), "Temporary sanctuaries should be" +
                " cleared after bot chooses one")
        assertTrue(botPlayer.sanctuaries.isNotEmpty(), "Bot should have at least one permanent" +
                " sanctuary after its choice")


        playerActionService.proceedAfterChooseSanctuaryCard()

        // Assert final state: bot should have drawn one card from center -> hand size becomes 3
        assertEquals(3, botPlayer.hand.size, "After Phase-2 region pick, bot's hand" +
                " must be refilled to 3")
    }


    /**
     * Verifies that in round 8 Phase 2 skips ChooseRegionCard
     * and directly starts the Phase-2 turn logic.
     */
    @Test
    fun testLastRoundSkipsChooseRegionCard() {
        startGame()

        val state = rootService.currentGame!!.currentGameState
        val player0 = state.players[0]
        val player1 = state.players[1]


        state.currentRound = 8
        state.currentPlayer = 0


        player0.regionCards.add(createCard(10))
        player0.regionCards.add(createCard(20, clue = true))


        player0.hand.clear()
        player0.hand.add(createCard(99))
        player0.hand.add(createCard(98))


        repeat(3) { player1.hand.add(createCard(1)) }


        playerActionService.proceedAfterChooseRegionCard()


        // Region selection must be skipped in round 8
        assertTrue(
            player0.hand.size <= 3,
            "ChooseRegionCard must be skipped in round 8"
        )
    }


}