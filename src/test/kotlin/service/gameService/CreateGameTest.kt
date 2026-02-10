package service.gameService

import entity.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import service.GameService
import service.RootService
import kotlin.test.*

/**
 * Tests the [service.GameService.createGame] method.
 */
class CreateGameTest {

    private lateinit var rootService: RootService
    private lateinit var gameService: GameService

    /**
     * Initializes the necessary services before each test execution.
     */
    @BeforeEach
    fun setUp() {
        rootService = RootService()
        gameService = GameService(rootService)
    }

    /**
     * Verifies that a game is successfully created with correct initial state and parameters.
     */
    @Test
    fun testCreateGame() {
        val players = listOf(
            Player("Dylan", PlayerType.LOCAL),
            Player("Rango", PlayerType.LOCAL)
        )

        gameService.createGame(
            isSimple = true,
            isOnline = false,
            randomOrder = false,
            players = players
        )

        val game = rootService.currentGame
        assertNotNull(game)

        val state = game.currentGameState
        assertEquals(2, state.players.size)
        assertEquals(0, state.currentPlayer)
        assertEquals(1, state.currentRound)
        assertTrue(game.isSimpleVariant)
        assertFalse(game.isOnline)
        assertTrue(state.centerCards.isNotEmpty(), "Center cards should be initialized")
    }

    /**
     * Ensures that attempting to create a game while one is already active throws an exception.
     */
    @Test
    fun testGameAlreadyRunning() {
        val players = listOf(
            Player("Dylan", PlayerType.LOCAL),
            Player("Rango", PlayerType.LOCAL)
        )

        gameService.createGame(isSimple = true, isOnline = false, randomOrder = false, players = players)

        assertFailsWith<IllegalStateException> {
            gameService.createGame(isSimple = true, isOnline = false, randomOrder = false, players = players)
        }
    }

    /**
     * Verifies that an exception is thrown when trying to start a game with fewer than 2 players.
     */
    @Test
    fun testTooFewPlayers() {
        val players = listOf(Player("Alone", PlayerType.LOCAL))

        assertFailsWith<IllegalArgumentException> {
            gameService.createGame(isSimple = true, isOnline = false, randomOrder = false, players = players)
        }
    }

    /**
     * Verifies that an exception is thrown when trying to start a game with more than 6 players.
     */
    @Test
    fun testTooManyPlayers() {
        val players = MutableList(7) { i -> (Player("Player$i", PlayerType.LOCAL)) }

        assertFailsWith<IllegalArgumentException> {
            gameService.createGame(isSimple = true, isOnline = false, randomOrder = false, players = players)
        }
    }

    /**
     * Checks if the player list is correctly handled and assigned when random order is requested.
     */
    @Test
    fun testRandomOrder() {
        val players = listOf(
            Player("Dylan", PlayerType.LOCAL),
            Player("Rango", PlayerType.LOCAL),
            Player("Alone", PlayerType.LOCAL)
        )

        gameService.createGame(isSimple = true, isOnline = false, randomOrder = true, players = players)

        val statePlayers = rootService.currentGame!!.currentGameState.players
        assertEquals(players.size, statePlayers.size)
        assertTrue(statePlayers.containsAll(players))
    }

    /**
     * Ensures that the player order is preserved exactly as input when random order is disabled.
     */
    @Test
    fun testFixedOrder() {
        val p1 = Player("Dylan", PlayerType.LOCAL)
        val p2 = Player("Rango", PlayerType.LOCAL)
        val players = listOf(p1, p2)

        gameService.createGame(isSimple = true, isOnline = false, randomOrder = false, players = players)

        val statePlayers = rootService.currentGame!!.currentGameState.players

        assertEquals(p1, statePlayers[0])
        assertEquals(p2, statePlayers[1])
    }

    /**
     * Verifies that the game creation fails if player names contain invalid characters.
     */
    @Test
    fun testInvalidName() {
        val nameList = listOf("Dylan.", "Rango!", "Al one")

        for (invalidName in nameList) {
            val players = listOf(
                Player(invalidName, PlayerType.LOCAL),
                Player("ValidName", PlayerType.LOCAL)
            )

            val exception = assertThrows<IllegalArgumentException> {
                gameService.createGame(
                    isSimple = false,
                    isOnline = false,
                    randomOrder = false,
                    players = players
                )
            }

            assertEquals("Player names must only contain letters or digits.", exception.message)
        }
    }

    /**
     *Checks that the game creation is rejected if the list contains duplicate player names.
     */
    @Test
    fun testDuplicateName() {
        val players = listOf(
            Player("Bam", PlayerType.LOCAL),
            Player("Bam", PlayerType.LOCAL)
        )

        val exception = assertThrows<IllegalArgumentException> {
            gameService.createGame(
                isSimple = false,
                isOnline = false,
                randomOrder = false,
                players = players
            )
        }

        assertEquals("Player names must be unique.", exception.message)
    }
}