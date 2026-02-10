package service
import entity.*
import kotlin.test.*

/**
* [IsNextPlayerHandSizeValidTest] Test class for the [service.GameService.isNextPlayerHandSizeValid] method.
* Verifies the correct transition of players and UI updates based on hand size.
*/

class IsNextPlayerHandSizeValidTest {

    /**
     * [testHandSizeThree] Tests correctly handles for hand size of 3.
     *
     */

    @Test
    fun testHandSizeThree() {
        val rootService = RootService()
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        val player1 = Player("Sami", PlayerType.LOCAL)
        val player2 = Player("Alex", PlayerType.LOCAL)

        // Setup game
        rootService.gameService.createGame(true, false, false,
            listOf(player1, player2))
        val game = rootService.currentGame!!
        val gameState = game.currentGameState

        // Ensure player 2 has 3 cards
        assertEquals(3, gameState.players[1].hand.size)

        // Set current player to 0
        gameState.currentPlayer = 0

        rootService.gameService.isNextPlayerHandSizeValid(gameState)

        // Verify
        assertEquals(1, gameState.currentPlayer)
        assertTrue(testRefreshable.refreshBeforePlayRegionCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseStartingHandCalled)
    }

    /**
     * [testHandSizeFive]Tests correctly handles for hand size of 5.
     *
     */

    @Test
    fun testHandSizeFive() {
        val rootService = RootService()
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        val player1 = Player("Aaa", PlayerType.LOCAL)
        val player2 = Player("Bbb", PlayerType.LOCAL)

        // Setup game: isSimpleVariant = false (5 cards)
        rootService.gameService.createGame(false, false, false,
            listOf(player1, player2))
        val game = rootService.currentGame!!
        val gameState = game.currentGameState

        // Ensure player 2 has 5 cards
        assertEquals(5, gameState.players[1].hand.size)

        // Set current player to 0
        gameState.currentPlayer = 0

        rootService.gameService.isNextPlayerHandSizeValid(gameState)

        // Verify
        assertEquals(1, gameState.currentPlayer)
        assertTrue(testRefreshable.refreshBeforeChooseStartingHandCalled
            )
        assertFalse(testRefreshable.refreshBeforePlayRegionCardCalled)
    }

    /**
     * Checks that if the next player is a BOT (hand size 3), the system
     * automatically triggers the bot's logic to pick and play a card.
     *
     * We aren't testing the bot's specific choice here—just making sure
     * the turn advances, the state refreshes, and a card actually gets selected.
     */
    @Test
    fun isNextPlayerHandSizeValidHandSize3_botPlaysRegionCardSetsSelectedCard() {
        val rootService = RootService()
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        val p0 = Player("Human0", PlayerType.LOCAL)
        val p1 = Player("Bot1", PlayerType.BOT_EASY)


        rootService.gameService.createGame(
            isSimple = true,
            isOnline = false,
            randomOrder = false,
            players = listOf(p0, p1)
        )

        val game = rootService.currentGame!!
        val state = game.currentGameState


        state.currentPlayer = 0

        // Precondition sanity
        val botPlayer = state.players[1]
        assertEquals(PlayerType.BOT_EASY, botPlayer.playerType)
        assertEquals(3, botPlayer.hand.size)
        assertNull(botPlayer.selectedCard)


        rootService.gameService.isNextPlayerHandSizeValid(state)


        assertEquals(1, state.currentPlayer)


        assertTrue(testRefreshable.refreshBeforePlayRegionCardCalled)
        assertFalse(testRefreshable.refreshBeforeChooseStartingHandCalled)


        assertNotNull(botPlayer.selectedCard, "Bot must have played a card via playRegionCard(), " +
                "which sets selectedCard.")
        assertTrue(
            botPlayer.selectedCard in botPlayer.hand,
            "Selected card must be a card from the bot's hand."
        )
    }

    /**
     * Ensures the Bot correctly picks its starting hand (dropping from 5 to 3 cards).
     *
     * We’re checking that the turn advances, the UI refreshes, and the 2 discarded
     * cards actually end up back in the draw stack. We don't care which cards the
     * bot keeps, as long as it follows the "keep 3, discard 2" rule.
     */
    @Test
    fun isNextPlayerHandSizeValidHandSize5_botChoosesStartingHandReducesHandAndReturnsTwoCards() {
        val rootService = RootService()
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        val p0 = Player("Human0", PlayerType.LOCAL)
        val p1 = Player("Bot1", PlayerType.BOT_EASY)


        rootService.gameService.createGame(
            isSimple = false,
            isOnline = false,
            randomOrder = false,
            players = listOf(p0, p1)
        )

        val game = rootService.currentGame!!
        val state = game.currentGameState


        state.currentPlayer = 0

        val botPlayer = state.players[1]
        assertEquals(PlayerType.BOT_EASY, botPlayer.playerType)
        assertEquals(5, botPlayer.hand.size)

        val drawStackSizeBefore = state.regionDrawStack.size


        rootService.gameService.isNextPlayerHandSizeValid(state)


        assertEquals(1, state.currentPlayer)


        assertTrue(testRefreshable.refreshBeforeChooseStartingHandCalled)
        assertFalse(testRefreshable.refreshBeforePlayRegionCardCalled)


        assertEquals(
            3,
            botPlayer.hand.size,
            "After chooseStartingHand(), bot must keep exactly 3 cards in hand."
        )


        assertEquals(
            drawStackSizeBefore + 2,
            state.regionDrawStack.size,
            "After chooseStartingHand(), exactly 2 discarded cards must be returned to regionDrawStack."
        )
    }

    /**
     * Ensures the game fails fast if a player is marked as a BOT but isn't
     * registered in the bot map. This guards the strict requirement that
     * every bot player must have a corresponding logic instance to run.
     */
    @Test
    fun isNextPlayerHandSizeValidBotMissingThrowsIllegalStateException() {
        val rootService = RootService()
        val p0 = Player("Human0", PlayerType.LOCAL)
        val p1 = Player("Bot1", PlayerType.BOT_EASY)


        rootService.gameService.createGame(
            isSimple = true,
            isOnline = false,
            randomOrder = false,
            players = listOf(p0, p1)
        )

        val game = rootService.currentGame!!
        val state = game.currentGameState
        state.currentPlayer = 0

        // Deliberately break the invariant to cover checkNotNull(bot)
        game.bots.remove(p1.name)

        val ex = assertFailsWith<IllegalStateException> {
            rootService.gameService.isNextPlayerHandSizeValid(state)
        }


        assertTrue(
            ex.message?.contains("null") == true || ex.message?.contains("Required value")
                    == true || ex.message?.isNotBlank() == true,
            "Must throw due to missing bot instance for BOT player."
        )
    }
}