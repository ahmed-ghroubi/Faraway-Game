package service.network

import edu.udo.cs.sopra.ntf.InitMessage
import entity.PlayerType
import entity.FarawayGame
import service.RootService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for the `convertInitMessageToGame()` function in [NetworkService].
 */
class ConvertInitMessageToGameTest {

    lateinit var rootService: RootService

    /**
     * Tests the conversion of an [InitMessage] to a [FarawayGame].
     */
    @Test
    fun testConvertInitMessageToGame() {

        rootService = RootService()

        val gamePlayers = listOf(
            Pair("Alice", listOf(0, 1, 2, 3, 4)),
            Pair("Bob", listOf(5, 6, 7, 8, 9)),
            Pair("Charlie", listOf(10, 11, 12, 13, 14))
        )

        val drawStack = (15..67).toList()

        val sanctuaryStack = (68..112).toList()

        val initMessage = InitMessage(
            players = gamePlayers,
            drawStack = drawStack,
            sanctuaryStack = sanctuaryStack,
            isAdvanced = true
        )

        val game = rootService.networkService.convertInitMessageToGame(
            message = initMessage,
            clientName = "Alice",
            type = PlayerType.LOCAL
        )

        val gameState = game.currentGameState

        assertTrue(game.isOnline)
        assertFalse(game.isSimpleVariant)

        assertEquals(listOf("Alice", "Bob", "Charlie"), gameState.players.map { it.name })
        assertEquals(PlayerType.LOCAL, gameState.players.first { it.name == "Alice" }.playerType)
        assertEquals(PlayerType.REMOTE, gameState.players.first { it.name == "Bob" }.playerType)
        assertEquals(PlayerType.REMOTE, gameState.players.first { it.name == "Charlie" }.playerType)

        fun handTimesOf(name: String) = gameState.players.first { it.name == name }.hand.map { it.explorationTime }
        assertEquals(listOf(1, 2, 3, 4, 5), handTimesOf("Alice"))
        assertEquals(listOf(6, 7, 8, 9, 10), handTimesOf("Bob"))
        assertEquals(listOf(11, 12, 13, 14, 15), handTimesOf("Charlie"))

        assertEquals(sanctuaryStack, gameState.sanctuaryDrawStack.map { it.cardId })

        val expectedCenterTimes = listOf(16, 17, 18, 19)
        assertEquals(expectedCenterTimes, gameState.centerCards.map { it.explorationTime })

        val expectedRegionDrawTimes = (20..68).toList()
        assertEquals(expectedRegionDrawTimes, gameState.regionDrawStack.map { it.explorationTime })
    }

    /**
     * Tests that converting an [InitMessage] to a [FarawayGame] and back to an [InitMessage]
     * yields the original [InitMessage].
     */
    @Test
    fun testInitMessageTwice() {
        rootService = RootService()

        val gamePlayers = listOf(
            Pair("Alice", listOf(0, 2, 1, 14, 9)),
            Pair("Bob", listOf(5, 10, 7, 8, 4)),
            Pair("Charlie", listOf(6, 11, 12, 13, 3))
        )

        val drawStack = (15..67).toList().shuffled()

        val sanctuaryStack = (68..112).toList().shuffled()

        val initMessage = InitMessage(
            players = gamePlayers,
            drawStack = drawStack,
            sanctuaryStack = sanctuaryStack,
            isAdvanced = false
        )

        val game = rootService.networkService.convertInitMessageToGame(
            message = initMessage,
            clientName = "Alice",
            type = PlayerType.LOCAL
        )

        val newInitMessage = rootService.networkService.convertGameToInitMessage(game)

        assertEquals(initMessage, newInitMessage)
    }
}