package service

import entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Test class for the sanctuary selection logic in [PlayerActionService].
 *
 * This class verifies the behavior of the method:
 * - chooseSanctuaryCard()
 *
 * The tests ensure that:
 * - A chosen sanctuary card is moved from temporary to permanent sanctuaries
 * - Unchosen sanctuary cards are returned to the draw stack
 * - Temporary sanctuaries are cleared after the choice
 * - Invalid calls result in appropriate exceptions
 */
class ChooseSanctuaryTest {

    /** Root service providing access to all game services */
    private lateinit var rootService: RootService

    /** Service under test */
    private lateinit var playerActionService: PlayerActionService

    /** Test refreshable used to track UI callbacks (if needed) */
    private lateinit var refreshable: TestRefreshable

    /** Single test player */
    private lateinit var player: Player

    /** Temporary sanctuary cards used in tests */
    private lateinit var sanctuaryCard1: SanctuaryCard
    private lateinit var sanctuaryCard2: SanctuaryCard

    /**
     * Sets up a minimal valid game state before each test.
     *
     * The setup includes:
     * - One local player
     * - Two temporary sanctuary cards
     * - A sanctuary draw stack with one card
     * - A registered [TestRefreshable]
     */
    @BeforeEach
    fun setup() {
        rootService = RootService()
        playerActionService = rootService.playerActionService

        refreshable = TestRefreshable(rootService)
        rootService.addRefreshable(refreshable)

        player = Player("Player 1", PlayerType.LOCAL)

        val quest = Quest(
            night = false,
            clue = false,
            fame = 1,
            wonders = emptyList(),
            biome = emptyList()
        )

        sanctuaryCard1 = SanctuaryCard(
            cardId = 1,
            night = false,
            clue = false,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = quest
        )

        sanctuaryCard2 = SanctuaryCard(
            cardId = 2,
            night = false,
            clue = false,
            biome = Biome.GREEN,
            wonders = emptyList(),
            quest = quest
        )

        // Prepare temporary sanctuary cards for the player
        player.temporarySanctuaries.add(sanctuaryCard1)
        player.temporarySanctuaries.add(sanctuaryCard2)

        val gameState = GameState(
            players = mutableListOf(player),
            currentPlayer = 0
        )

        // Prepare the sanctuary draw stack
        gameState.sanctuaryDrawStack.add(
            SanctuaryCard(
                cardId = 99,
                night = false,
                clue = false,
                biome = Biome.RED,
                wonders = emptyList(),
                quest = quest
            )
        )

        val game = FarawayGame(
            isOnline = false,
            isSimpleVariant = false
        )
        game.currentGameState = gameState

        rootService.currentGame = game
    }




    /**
     * Verifies that choosing a sanctuary card moves it
     * from the temporary sanctuaries to the permanent sanctuaries.
     */
    @Test
    fun `chooseSanctuaryCard moves card to permanent sanctuaries`() {
        playerActionService.chooseSanctuaryCard(sanctuaryCard1)

        assertTrue(player.sanctuaries.contains(sanctuaryCard1))
        assertFalse(player.temporarySanctuaries.contains(sanctuaryCard1))
    }

    /**
     * Verifies that all unchosen temporary sanctuary cards
     * are returned to the sanctuary draw stack.
     */
    @Test
    fun `chooseSanctuaryCard returns unchosen cards to draw stack`() {
        val drawStack =
            rootService.currentGame!!.currentGameState.sanctuaryDrawStack
        val sizeBefore = drawStack.size

        playerActionService.chooseSanctuaryCard(sanctuaryCard1)

        assertEquals(sizeBefore + 1, drawStack.size)
        assertTrue(drawStack.contains(sanctuaryCard2))
    }

    /**
     * Verifies that the list of temporary sanctuaries
     * is cleared after a sanctuary card has been chosen.
     */
    @Test
    fun `chooseSanctuaryCard clears temporary sanctuaries`() {
        playerActionService.chooseSanctuaryCard(sanctuaryCard1)

        assertTrue(player.temporarySanctuaries.isEmpty())
    }

    /**
     * Verifies that choosing a sanctuary card fails
     * if the card is not part of the player's temporary sanctuaries.
     */
    @Test
    fun `chooseSanctuaryCard fails if card not in temporary sanctuaries`() {
        val foreignCard = SanctuaryCard(
            cardId = 42,
            night = false,
            clue = false,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = Quest(night = false,  clue = false, 1, emptyList(), emptyList())
        )

        assertThrows<IllegalArgumentException> {
            playerActionService.chooseSanctuaryCard(foreignCard)
        }
    }

    /**
     * Verifies that chooseSanctuaryCard fails
     * if no active game exists.
     */
    @Test
    fun `chooseSanctuaryCard fails if no active game`() {
        rootService.currentGame = null

        assertThrows<IllegalStateException> {
            playerActionService.chooseSanctuaryCard(sanctuaryCard1)
        }
    }

    /**
     * When the current player is a BOT and has finished the Sanctuary selection (or skipped it),
     * the service should call the bot to choose a region card from centerCards immediately.
     *
     * Updated Logic: proceedAfterChooseSanctuaryCard does NOT increment the player index anymore.
     * It transitions the CURRENT player from Sanctuary Phase to Region Phase.
     */
    @Test
    fun proceedAfterChooseSanctuaryCardBotPicksFromCenter() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game

        val human = Player("Human", PlayerType.LOCAL)
        val botPlayer = Player("Bot", PlayerType.BOT_EASY)

        /*
         * Setup:
         * The Bot is the current player.
         * The Bot has just finished/skipped the Sanctuary selection.
         * We expect the Bot to immediately pick a region card.
         */
        val state = GameState(mutableListOf(human, botPlayer))
        state.currentPlayer = 1 // Bot is at index 1
        game.currentGameState = state

        botPlayer.hand.clear()

        // Provide deterministic center cards
        val centerCards = listOf(
            RegionCard(
                1,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            ),

            RegionCard(
                2,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList())
            )
        )
        state.centerCards.addAll(centerCards)

        // ensure no blocking temporary sanctuaries
        human.temporarySanctuaries.clear()
        botPlayer.temporarySanctuaries.clear()

        // Instantiate a Bot
        game.bots[botPlayer.name] = service.bot.GreedyBot(botPlayer.name, game.currentGameState)

        val beforeCenter = state.centerCards.size
        val beforeHand = botPlayer.hand.size

        assertDoesNotThrow {
            actionService.proceedAfterChooseSanctuaryCard()
        }

        // Either the bot's hand grew or the center shrank -> chooseRegionCard executed successfully
        val afterCenter = state.centerCards.size
        val afterHand = botPlayer.hand.size

        assertTrue(
            (afterHand > beforeHand) && (afterCenter < beforeCenter),
            "chooseRegionCard should have been executed by the bot: hand increase and center decrease expected"
        )
    }
}
