package service

import entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test class for the score calculation logic in [GameService].
 *
 * This class verifies the correct behavior of:
 * - calculateAllScores() (via reflection)
 *
 * The tests cover:
 * - Fame scoring from region cards and sanctuary cards
 * - Biome set scoring
 * - Night and clue scoring
 * - Quest priority and evaluation order
 * - Prerequisite handling and visibility order
 * - Error handling for invalid game states
 */
class CalculateAllScoreTest {

    /** Root service providing access to the game state */
    private lateinit var rootService: RootService

    /** Service under test */
    private lateinit var gameService: GameService

    /**
     * Initializes a fresh [RootService] and [GameService] before each test.
     */
    @BeforeEach
    fun setup() {
        rootService = RootService()
        gameService = GameService(rootService)
    }


    // Helper methods


    /**
     * Creates a simple [Quest] with a fixed fame value.
     *
     * @param fame Fame points awarded by the quest
     * @return a minimal quest instance for testing
     */
    private fun fixedQuest(fame: Int): Quest =
        Quest(
            fame = fame,
            night = false,
            clue = false,
            wonders = emptyList(),
            biome = emptyList()
        )

    /**
     * Creates a basic [RegionCard] with a quest that awards the given fame.
     *
     * @param fame Fame points of the quest attached to the region card
     * @return a region card suitable for score calculation tests
     */
    private fun regionCard(fame: Int): RegionCard =
        RegionCard(
            explorationTime = 1,
            prerequisites = emptyList(),
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = emptyList(),
            quest = fixedQuest(fame)
        )

    /**
     * Prepares a game state that matches the expectations of [GameService.endGame].
     *
     * Important:
     * Due to operator precedence in the production code
     * (e.g. `8 - 1 % 1`), `currentPlayer` must be set to 8,
     * even though this value would not make sense in a real game
     * with only one player.
     *
     * @param player the single player participating in the game
     */
    private fun setupGameWithPlayer(player: Player) {
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        val state = GameState(players = mutableListOf(player))

        // Final round
        state.currentRound = 8

        // Required by current production logic
        state.currentPlayer = 0

        game.currentGameState = state
        rootService.currentGame = game
    }


    // Tests


    /**
     * Verifies that endGame correctly fills the score array
     * according to the defined scoring rules.
     */
    @Test
    fun `endGame fills score array correctly according to scoring rules`() {
        val player = Player(name = "Mohammed", playerType = PlayerType.LOCAL)

        // Eight region cards with increasing fame values
        player.regionCards.addAll(listOf(
            regionCard(1), regionCard(2), regionCard(3), regionCard(4),
            regionCard(5), regionCard(6), regionCard(7), regionCard(8)
        ))

        // Two sanctuary cards contributing additional fame
        player.sanctuaries.addAll(listOf(
            SanctuaryCard(1, night = false, false, Biome.GREEN, emptyList(), fixedQuest(10)),
            SanctuaryCard(2, night = false, false, Biome.GREEN, emptyList(), fixedQuest(20))
        ))

        setupGameWithPlayer(player)
        gameService.endGame()

        // Fame from region cards
        assertEquals(8, player.score[0])

        // Fame from sanctuary cards
        assertEquals(30, player.score[8])

        // Total score
        assertEquals(66, player.score[9])
    }

    /**
     * Verifies biome set scoring when a complete biome set
     * is provided by sanctuary quests.
     */
    @Test
    fun `test biome set scoring`() {
        val player = Player("Hamoud", playerType = PlayerType.LOCAL)

        val setQuest = Quest(
            night = false,
            clue = false,
            fame = 10,
            wonders = emptyList(),
            biome = listOf(
                Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.RED
            )
        )

        val biomes = listOf(
            Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.RED,
            Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.RED
        )

        val cards = biomes.map {
            RegionCard(
                explorationTime = 1,
                prerequisites = emptyList(),
                night = false,
                clue = false,
                biome = it,
                wonders = emptyList(),
                quest = fixedQuest(0)
            )
        }

        player.regionCards.addAll(cards)
        player.sanctuaries.add(
            SanctuaryCard(99, night = false, false, Biome.NONE, emptyList(), setQuest)
        )

        setupGameWithPlayer(player)
        gameService.endGame()

        // One complete biome set yields 20 points
        assertEquals(20, player.score[8])
    }

    /**
     * Verifies correct scoring of night and clue quests.
     */
    @Test
    fun `test clue and night scoring`() {
        val player = Player("Mohammed", playerType = PlayerType.LOCAL)

        val cards = MutableList(8) { regionCard(0) }

        cards[7] = RegionCard(
            explorationTime = 1,
            prerequisites = emptyList(),
            night = false,
            clue = true,
            biome = Biome.NONE,
            wonders = emptyList(),
            quest = Quest(
                night = false,
                clue = true,
                fame = 5,
                wonders = emptyList(),
                biome = emptyList()
            )
        )

        player.regionCards.addAll(cards)

        setupGameWithPlayer(player)
        gameService.endGame()

        assertEquals(5, player.score[0])
    }

    /**
     * Verifies that quest priority is respected when a quest
     * contains multiple conditions (e.g. night and clue).
     */
    @Test
    fun `test quest priority order`() {
        val player = Player("Mohammed", playerType = PlayerType.LOCAL)

        val multiQuest = Quest(
            night = true,
            clue = true,
            fame = 10,
            wonders = emptyList(),
            biome = emptyList()
        )

        val cards = MutableList(8) { regionCard(0) }

        cards[7] = RegionCard(
            explorationTime = 1,
            prerequisites = emptyList(),
            night = true,
            clue = true,
            biome = Biome.NONE,
            wonders = emptyList(),
            quest = multiQuest
        )

        player.regionCards.addAll(cards)

        setupGameWithPlayer(player)
        gameService.endGame()

        assertEquals(10, player.score[0])
    }

    /**
     * Verifies correct handling of prerequisites and
     * the visibility sequence of wonders.
     */
    @Test
    fun `test prerequisites and visibility sequence`() {
        val player = Player("Mohammed", playerType = PlayerType.LOCAL)

        // Sanctuary provides the PLANT wonder
        player.sanctuaries.add(
            SanctuaryCard(
                1,
                night = false,
                false,
                Biome.NONE,
                listOf(Wonder.PLANT),
                fixedQuest(0)
            )
        )

        val cards = MutableList(8) { regionCard(0) }

        // Card that depends on PLANT and reveals MINERAL
        cards[7] = RegionCard(
            explorationTime = 1,
            prerequisites = listOf(Wonder.PLANT),
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = listOf(Wonder.MINERAL),
            quest = fixedQuest(10)
        )

        // Card that depends on MINERAL
        cards[6] = RegionCard(
            explorationTime = 1,
            prerequisites = listOf(Wonder.MINERAL),
            night = false,
            clue = false,
            biome = Biome.BLUE,
            wonders = emptyList(),
            quest = fixedQuest(5)
        )

        player.regionCards.addAll(cards)

        setupGameWithPlayer(player)
        gameService.endGame()

        assertEquals(10, player.score[0])
        assertEquals(5, player.score[1])
    }

    /**
     * Verifies that endGame fails if a player has not collected
     * exactly eight region cards.
     */
    @Test
    fun `test endGame outer card check`() {
        val player = Player("Bob", playerType = PlayerType.LOCAL)

        // Only seven region cards instead of eight
        player.regionCards.addAll(MutableList(7) { regionCard(0) })

        setupGameWithPlayer(player)

        val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            gameService.endGame()
        }

        assertEquals(
            "Not all players have collected 8 region cards.",
            exception.message
        )
    }

    /**
     * Verifies that calculateAllScores fails if no active game is available.
     *
     * The private method is invoked via reflection and is therefore
     * expected to throw an InvocationTargetException whose cause
     * is an IllegalStateException.
     */
    @Test
    fun `test calculateAllScores fails when no active game available`() {
        // GIVEN: no active game
        rootService.currentGame = null

        // Access private method via reflection
        val method =
            gameService.javaClass.getDeclaredMethod("calculateAllScores")
        method.isAccessible = true

        // WHEN
        val exception =
            org.junit.jupiter.api.assertThrows<java.lang.reflect.InvocationTargetException> {
                method.invoke(gameService)
            }

        // THEN
        val cause = exception.cause as IllegalStateException
        assertEquals("No active game available.", cause.message)
    }
}
