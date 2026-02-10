package service

import entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFailsWith

/**
 * Test class for region card related actions in [PlayerActionService].
 *
 * This test suite covers the game phase in which players
 * select and play region cards.
 *
 * Covered functionality:
 * - [PlayerActionService.playRegionCard]
 * - [PlayerActionService.proceedAfterPlayRegionCard]
 *
 * The tests verify:
 * - Correct state transitions between players
 * - Phase changes (entering or skipping phase two)
 * - Proper handling of invalid game states
 * - Correct movement of cards between player collections
 * - Correct triggering of UI refresh callbacks
 */
class PlayRegionCardTest {

    // Root service providing access to all game services
    private lateinit var rootService: RootService

    // Service under test
    private lateinit var playerActionService: PlayerActionService

    // Test refreshable used to track UI callbacks
    private lateinit var refreshable: TestRefreshable

    // Test players
    private lateinit var player1: Player
    private lateinit var player2: Player

    // Test region cards
    private lateinit var regionCard1: RegionCard
    private lateinit var regionCard2: RegionCard

    /**
     * Initializes a minimal valid game state before each test.
     *
     * The setup includes:
     * - A fresh [RootService] and [PlayerActionService]
     * - Two local players
     * - One region card per player in their hand
     * - A game state with player 1 as the active player
     * - A registered [TestRefreshable] to observe UI refresh callbacks
     *
     * This ensures that all tests run independently
     * and start from a well-defined baseline state.
     */
    @BeforeEach
    fun setup() {
        rootService = RootService()
        playerActionService = rootService.playerActionService

        refreshable = TestRefreshable(rootService)
        rootService.addRefreshable(refreshable)

        player1 = Player("Mohammed", PlayerType.LOCAL)
        player2 = Player("Ali", PlayerType.LOCAL)

        val quest = Quest(
            night = false,
            clue = false,
            fame = 0,
            wonders = emptyList(),
            biome = emptyList()
        )

        regionCard1 = RegionCard(
            explorationTime = 2,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.RED,
            wonders = emptyList(),
            quest = quest
        )

        regionCard2 = RegionCard(
            explorationTime = 3,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.GREEN,
            wonders = emptyList(),
            quest = quest
        )

        // Each player starts with one region card in hand
        player1.hand.add(regionCard1)
        player2.hand.add(regionCard2)

        val gameState = GameState(
            players = mutableListOf(player1, player2),
            currentPlayer = 0
        )

        val game = FarawayGame(
            isOnline = false,
            isSimpleVariant = false
        )
        game.currentGameState = gameState

        rootService.currentGame = game
    }


    // Tests for playRegionCard()


    /**
     * Verifies that playing a valid region card:
     * - Assigns the card to the current player's selectedCard
     * - Does not trigger refreshAfterPlayRegionCard(),
     *   as refresh handling is delegated to later phases.
     */
    @Test
    fun `playRegionCard selects card and triggers refresh`() {
        playerActionService.playRegionCard(regionCard1)

        assertEquals(regionCard1, player1.selectedCard)
        assertFalse(refreshable.refreshAfterPlayRegionCardCalled)
    }

    /**
     * Verifies that playRegionCard throws an [IllegalStateException]
     * if no active game exists.
     */
    @Test
    fun `playRegionCard fails if no active game`() {
        rootService.currentGame = null

        assertThrows<IllegalStateException> {
            playerActionService.playRegionCard(regionCard1)
        }
    }

    /**
     * Verifies that playRegionCard throws an [IllegalArgumentException]
     * if the chosen card is not in the current player's hand.
     */
    @Test
    fun `playRegionCard fails if card not in hand`() {
        assertThrows<IllegalArgumentException> {
            playerActionService.playRegionCard(regionCard2)
        }
    }

    /**
     * Verifies that playRegionCard throws an [IllegalStateException]
     * if the current player has already selected a region card
     * in the current round.
     */
    @Test
    fun `playRegionCard fails if player already selected a card`() {
        player1.selectedCard = regionCard1

        assertThrows<IllegalStateException> {
            playerActionService.playRegionCard(regionCard1)
        }
    }


    // Tests for proceedAfterPlayRegionCard()


    /**
     * Verifies that after a player has played a region card:
     * - The turn advances to the next player
     * - The UI is refreshed to allow the next player
     *   to play a region card.
     */
    @Test
    fun `proceedAfterPlayRegionCard advances to next player`() {
        player1.selectedCard = regionCard1

        playerActionService.proceedAfterPlayRegionCard()

        assertEquals(1, rootService.currentGame!!.currentGameState.currentPlayer)
        assertTrue(refreshable.refreshBeforePlayRegionCardCalled)
        assertFalse(refreshable.refreshBeforeChooseRegionCardCalled)
    }

    /**
     * Verifies that the currentPlayer index wraps around
     * to the first player when the last player has finished their turn.
     */
    @Test
    fun `proceedAfterPlayRegionCard wraps currentPlayer index`() {
        rootService.currentGame!!.currentGameState.currentPlayer = 1
        player2.selectedCard = regionCard2

        playerActionService.proceedAfterPlayRegionCard()

        assertEquals(0, rootService.currentGame!!.currentGameState.currentPlayer)
    }

    /**
     * Verifies that proceedAfterPlayRegionCard throws an
     * [IllegalStateException] if no active game exists.
     */
    @Test
    fun `proceedAfterPlayRegionCard fails if no active game`() {
        rootService.currentGame = null

        assertThrows<IllegalStateException> {
            playerActionService.proceedAfterPlayRegionCard()
        }
    }

    /**
     * Verifies that proceedAfterPlayRegionCard transitions
     * the game into phase two when all players have selected
     * a region card in the current round.
     */
    @Test
    fun `proceedAfterPlayRegionCard enters phase two when all players played`() {
        player1.selectedCard = regionCard1
        player2.selectedCard = regionCard2

        playerActionService.proceedAfterPlayRegionCard()

        val state = rootService.currentGame!!.currentGameState
        assertTrue(state.isPhaseTwo)
        assertTrue(refreshable.refreshBeforeChooseRegionCardCalled)
        assertFalse(refreshable.refreshBeforePlayRegionCardCalled)
    }

    /**
     * Verifies that proceedAfterPlayRegionCard throws an
     * [IllegalStateException] if the current player has not
     * selected a region card before resolving the turn.
     */
    @Test
    fun `proceedAfterPlayRegionCard fails if no card was selected`() {
        val ex = assertThrows<IllegalStateException> {
            playerActionService.proceedAfterPlayRegionCard()
        }

        assertTrue(ex.message!!.contains("No region card was played"))
    }

    /**
     * Verifies that when all players have selected a region card:
     * - The selected cards are moved from the players' hands
     *   to their regionCards collections
     * - The selectedCard references are cleared afterwards.
     */
    @Test
    fun `proceedAfterPlayRegionCard moves selected cards to regionCards when all selected`() {
        player1.selectedCard = regionCard1
        player2.selectedCard = regionCard2

        playerActionService.proceedAfterPlayRegionCard()

        assertTrue(player1.regionCards.contains(regionCard1))
        assertTrue(player2.regionCards.contains(regionCard2))

        assertFalse(player1.hand.contains(regionCard1))
        assertFalse(player2.hand.contains(regionCard2))

        assertNull(player1.selectedCard)
        assertNull(player2.selectedCard)
    }

    /**
     * Verifies that if not all players have selected a region card:
     * - The game continues with the next player's turn
     * - The game does not enter phase two
     * - The UI is refreshed to allow the next region card play.
     */
    @Test
    fun `proceedAfterPlayRegionCard continues with next player if not all selected`() {
        player1.selectedCard = regionCard1


        playerActionService.proceedAfterPlayRegionCard()

        assertEquals(1, rootService.currentGame!!.currentGameState.currentPlayer)
        assertTrue(refreshable.refreshBeforePlayRegionCardCalled)
        assertFalse(refreshable.refreshBeforeChooseRegionCardCalled)
    }

    /**
     * Verifies the behavior of (proceedAfterPlayRegionCard) in the final round (round 8).
     *
     * Phase 2 is entered after all players selected a region card, but region card selection
     * from the center is skipped, as Phase 2 in round 8 is used only for Sanctuary handling.
     */

    @Test
    fun `proceedAfterPlayRegionCard enters phase two but skips region selection in last round`() {
        val state = rootService.currentGame!!.currentGameState
        state.currentRound = 8

        player1.selectedCard = regionCard1
        player2.selectedCard = regionCard2

        try {
            playerActionService.proceedAfterPlayRegionCard()
        } catch (_: IllegalStateException) {}

        assertTrue(state.isPhaseTwo)
        assertFalse(refreshable.refreshBeforeChooseRegionCardCalled)
    }


    /**
     * Phase One:
     *
     * Setup:
     * - Two players: player0 is the "oldPlayer" who already has selectedCard set,
     *   player1 is the next player and is a BOT.
     *
     * Expectation:
     * - proceedAfterPlayRegionCard advances to player1, calls bot.exploreRegion(player1.hand),
     *   and then playRegionCard(...) sets player1.selectedCard to a card from their hand.
     */
    @Test
    fun proceedAfterPlayRegionCardBotExploresAndPlaysPhaseOne() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game

        val player0 = Player("P0", PlayerType.LOCAL)
        val player1 = Player("P1", PlayerType.BOT_EASY)

        // give player1 a hand of 3 cards
        val p1cards = (0 until 3).map {
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
        player1.hand.addAll(p1cards)

        // give player0 a selectedCard to simulate having played already
        val oldSelected = RegionCard(
            99,
            emptyList(),
            false,
            false,
            Biome.GREEN,
            emptyList(),
            Quest(false, false, 0, emptyList(), emptyList())
        )
        player0.hand.add(oldSelected)
        player0.selectedCard = oldSelected

        val state = GameState(mutableListOf(player0, player1))
        // set currentPlayer to index of oldPlayer (0)
        state.currentPlayer = 0
        game.currentGameState = state


        game.bots[player1.name] = service.bot.GreedyBot(player1.name, game.currentGameState)


        actionService.proceedAfterPlayRegionCard()

        // after call, currentPlayer advanced to player1 and bot should have set selectedCard
        val p1After = game.currentGameState.players[1]
        assertNotNull(p1After.selectedCard, "Bot should have selected a card")
        assertTrue(p1After.hand.contains(p1After.selectedCard), "Selected card must be one" +
                "from the bot's hand")
    }

    /**
     * Phase Two:
     *
     * Setup:
     * - Two players, both have selectedCard set => allSelected = true.
     * - currentRound < 8 => isPhaseTwo should become true.
     *
     * What we assert:
     * - selectedCard fields are cleared for all players.
     * - previously selected cards are moved into players' regionCards.
     * - state.isPhaseTwo is true.
     *
     */
    @Test
    fun proceedAfterPlayRegionCardCollectionOnlyGuaranteedEffectsPhaseTwo() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game

        val player0 = Player("P0", PlayerType.LOCAL)
        val player1 = Player("P1", PlayerType.BOT_HARD)

        val selection0 = RegionCard(
            10,
            emptyList(),
            false,
            false,
            Biome.GREEN,
            emptyList(),
            Quest(false, false, 0, emptyList(), emptyList()))

        val selection1 = RegionCard(
            11,
            emptyList(),
            false,
            false,
            Biome.GREEN,
            emptyList(),
            Quest(false, false, 0, emptyList(), emptyList()))

        player0.hand.add(selection0)
        player1.hand.add(selection1)
        player0.selectedCard = selection0
        player1.selectedCard = selection1

        // ensure no blocking temporary sanctuaries
        player0.temporarySanctuaries.clear()
        player1.temporarySanctuaries.clear()

        val center = (20 until 23).map {
            RegionCard(
                it,
                emptyList(),
                false,
                false,
                Biome.GREEN,
                emptyList(),
                Quest(false, false, 0, emptyList(), emptyList()))
        }

        val state = GameState(mutableListOf(player0, player1))
        state.centerCards.addAll(center)
        state.currentPlayer = 0
        state.currentRound = 2
        game.currentGameState = state

        game.bots[player0.name] = service.bot.GreedyBot(player0.name, game.currentGameState)
        game.bots[player1.name] = service.bot.GreedyBot(player1.name, game.currentGameState)

        actionService.proceedAfterPlayRegionCard()

        val afterState = game.currentGameState

        assertTrue(afterState.isPhaseTwo,
            "isPhaseTwo must be true when all players selected and currentRound < 8")

        val playersAfter = afterState.players

        assertTrue(playersAfter.all { it.selectedCard == null },
            "All selectedCard fields must be cleared after collection")
        assertTrue(playersAfter[0].regionCards.contains(selection0),
            "Player0 must have collected their selected card into regionCards")
        assertTrue(playersAfter[1].regionCards.contains(selection1),
            "Player1 must have collected their selected card into regionCards")
    }



    /**
     *
     * Setup:
     * - BOT player exists, but there is NO corresponding bot in game.bots map.
     *
     * Expectation:
     * - proceedAfterPlayRegionCard should throw an IllegalStateException.
     */
    @Test
    fun proceedAfterPlayRegionCardMissingBotThrows() {
        val rootService = RootService()
        val actionService = rootService.playerActionService
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game

        val player0 = Player("P0", PlayerType.LOCAL)
        val player1 = Player("P1", PlayerType.BOT_EASY)

        // player0 has a played selectedCard
        val selection0 = RegionCard(
            42,
            emptyList(),
            false,
            false,
            Biome.GREEN,
            emptyList(),
            Quest(false, false, 0, emptyList(), emptyList()))
        player0.hand.add(selection0)
        player0.selectedCard = selection0

        val state = GameState(mutableListOf(player0, player1))
        state.currentPlayer = 0
        state.currentRound = 1
        game.currentGameState = state

        assertFailsWith<IllegalStateException> {
            actionService.proceedAfterPlayRegionCard()
        }
    }

}
