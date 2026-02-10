package service

import entity.*
import kotlin.test.*
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * [ProceedAfterGameStartTest] checks the logic of [service.PlayerActionService.proceedAfterGameStart].
 */
class ProceedAfterGameStartTest {

    /**
     * [advancedLocalGameTest]Tests behavior for a LOCAL player in the Advanced Variant (Hand.Size > 3).
     *
     */
    @Test
    fun advancedLocalGameTest() {
        val rootService = RootService()
        val service = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game

        // Setup player with 5 cards (Advanced Variant)
        val player = Player("Player", PlayerType.LOCAL)
        repeat(5) {
            player.hand.add(
                RegionCard(
                    it, emptyList(), false, false,
                    Biome.GREEN, emptyList(), Quest(false, false,
                        0, emptyList(), emptyList())
                )
            )
        }

        game.currentGameState = GameState(mutableListOf(player))

        service.proceedAfterGameStart()

        // Verify that Hand size is still 5
        assertEquals(5, player.hand.size)
        assertTrue(game.currentGameState.regionDrawStack.isEmpty())
    }

    /**
     * [simpleLocalGameTest]Tests behavior for a LOCAL player in the Simple Variant (Hand.Size = 3).
     */
    @Test
    fun simpleLocalGameTest() {
        val rootService = RootService()
        val service = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game

        // Setup player with 3 cards

        val player = Player("Player", PlayerType.LOCAL)
        repeat(3) {
            player.hand.add(
                RegionCard(
                    it, emptyList(), false, false,
                    Biome.GREEN, emptyList(), Quest(false, false,
                        0, emptyList(), emptyList())
                )
            )
        }

        game.currentGameState = GameState(mutableListOf(player))

        service.proceedAfterGameStart()

        // Verify: Hand size 3, no selected card
        assertEquals(3, player.hand.size)
        assertNull(player.selectedCard, "Local player should not have a selected card yet.")
    }
    /**
     *
     * This test ensures proceedAfterGameStart applies exactly those 3 cards into the hand.
     */
    @Test
    fun advancedBotStartingHandIsAlwaysFirstThreeOfferedCards() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game

        val player = Player("BotPlayer", PlayerType.BOT_EASY)

        // Put the 5 cards into player's hand BEFORE calling proceedAfterGameStart
        val cards = (0 until 5).map {
            RegionCard(
                it + 1,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        }
        player.hand.addAll(cards)

        val state = GameState(mutableListOf(player))
        game.currentGameState = state

        game.bots[player.name] = service.bot.GreedyBot("BotPlayer", game.currentGameState)

        actionService.proceedAfterGameStart()

        assertEquals(3, player.hand.size,
            "After choosing starting hand, player must have exactly 3 cards")

        assertEquals(2, state.regionDrawStack.size,
            "2 cards must be returned to regionDrawStack")

        // The union of player's hand + draw stack should equal original 5 cards
        val combined = (player.hand + state.regionDrawStack)
        assertEquals(5, combined.distinct().size)
        assertTrue(combined.containsAll(cards))
    }


    /**
     * Verifies BOT flow in advanced variant (hand size > 3).
     *
     * Expected behaviour:
     * - PlayerActionService looks up a bot in game.bots[player.name].
     * - It calls bot.chooseInitialCards(player.hand) and then chooseStartingHand().
     * - After execution the player's hand has exactly 3 cards.
     * - The 2 discarded cards are returned to regionDrawStack.
     */

    @Test
    fun advancedBotGameCallsChooseInitialCardsAndAppliesStartingHand() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game

        val player = Player("BotPlayer", PlayerType.BOT_EASY)

        val cards = (0 until 5).map {
            RegionCard(
                it + 1,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        }
        player.hand.addAll(cards)

        val state = GameState(mutableListOf(player))
        game.currentGameState = state

        game.bots[player.name] = service.bot.GreedyBot("BotPlayer", game.currentGameState)

        actionService.proceedAfterGameStart()

        assertEquals(3, player.hand.size,
            "After choosing starting hand, player must have exactly 3 cards")
        assertEquals(2, state.regionDrawStack.size,
            "2 cards must be returned to regionDrawStack")
        val combined = (player.hand + state.regionDrawStack)
        assertEquals(5, combined.distinct().size)
        assertTrue(combined.containsAll(cards))
    }

    /**
     * Tests invalid starting-hand handling by directly invoking chooseStartingHand with wrong size.
     *
     * Expected behaviour:
     * - An IllegalArgumentException is thrown when chooseStartingHand is called
     *   with a list of cards that does not have exactly 3 cards.
     */
    @Test
    fun advancedBotGameInvalidStartingHandThrows() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game

        val player = Player("BotPlayer", PlayerType.BOT_EASY)

        val cards = (0 until 5).map {
            RegionCard(
                it + 1,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        }
        player.hand.addAll(cards)

        val state = GameState(mutableListOf(player))
        game.currentGameState = state

        game.bots[player.name] = service.bot.GreedyBot("BotPlayer", game.currentGameState)

        // normal flow should not throw
        assertDoesNotThrow {
            actionService.proceedAfterGameStart()
        }

        // simulate invalid input to chooseStartingHand (2 cards instead of 3)
        val player2 = Player("BotPlayer2", PlayerType.BOT_EASY)
        player2.hand.addAll(cards)
        game.currentGameState = GameState(mutableListOf(player2))

        val invalidSelected = listOf(cards[0], cards[1]).map {
            RegionCard(
                it.explorationTime,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        }

        assertFailsWith<IllegalArgumentException> {
            actionService.chooseStartingHand(invalidSelected)
        }
    }

    /**
     * Verifies BOT flow in simple variant (hand size == 3).
     *
     * Expected behaviour:
     * - PlayerActionService looks up the bot and calls bot.exploreRegion(player.hand).
     * - The returned RegionCard is set as player.selectedCard.
     */
    @Test
    fun simpleBotGameCallsExploreRegionAndPlaysCard() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game

        val player = Player("BotPlayer", PlayerType.BOT_EASY)

        val cards = (0 until 3).map {
            RegionCard(
                it + 1,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        }
        player.hand.addAll(cards)

        val state = GameState(mutableListOf(player))
        game.currentGameState = state

        game.bots[player.name] = service.bot.GreedyBot("BotPlayer", game.currentGameState)

        actionService.proceedAfterGameStart()

        assertNotNull(player.selectedCard,
            "Bot should have played a region card (selectedCard must be set)")
        assertTrue(player.hand.contains(player.selectedCard), "Selected card must come from hand")
    }

    /**
     * Verifies that proceedAfterGameStart throws when a BOT player exists but no bot
     * implementation is registered under game.bots[player.name].
     *
     * Expected behaviour:
     * - checkNotNull(game.bots[player.name]) should fail and throw an IllegalStateException.
     */
    @Test
    fun botMissingFromBotMapThrows() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game

        val player = Player("BotPlayer", PlayerType.BOT_EASY)
        val cards = (0 until 5).map {
            RegionCard(
                it + 1,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        }
        player.hand.addAll(cards)

        val state = GameState(mutableListOf(player))
        game.currentGameState = state


        assertFailsWith<IllegalStateException> {
            actionService.proceedAfterGameStart()
        }
    }

    /**
     * Helper method to create a dummy region card.
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
     * Ensures all [Refreshable] components are notified in both advanced and simple game variants.
     */
    @Test
    fun testRefreshableCalled() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game

        var refreshCalled = false
        val mockRefreshable = object : Refreshable {
            override fun refreshBeforeChooseStartingHand() {
                refreshCalled = true
            }
            override fun refreshBeforePlayRegionCard() {
                refreshCalled = true
            }
        }
        rootService.addRefreshables(mockRefreshable)

        val player = Player("TestPlayer", PlayerType.LOCAL)
        repeat(5) {player.hand.add(createDummyCard())}
        game.currentGameState = GameState(mutableListOf(player))

        actionService.proceedAfterGameStart()
        assertTrue(refreshCalled)

        refreshCalled = false
        game.isSimpleVariant
        player.hand.clear()
        repeat(3) {player.hand.add(createDummyCard())}

        actionService.proceedAfterGameStart()
        assertTrue(refreshCalled)
    }

}