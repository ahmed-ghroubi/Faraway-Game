package service.network

import entity.*
import service.RootService
import kotlin.test.*
import edu.udo.cs.sopra.ntf.InitMessage

/**
 * Test class for the `convertGameToInitMessage()` function in [NetworkService].
 */
class ConvertGameToInitMessageTest {

    lateinit var rootService: RootService

    /**
     * Tests the conversion of a [FarawayGame] to an [InitMessage].
     */
    @Test
    fun testConvertGameToInitMessage() {

        rootService = RootService()

        val players = mutableListOf(
            Player("Alice", PlayerType.LOCAL),
            Player("Bob", PlayerType.REMOTE),
            Player("Charlie", PlayerType.REMOTE)
        )

        val game = FarawayGame(isOnline = true, isSimpleVariant = false)
        val gameState = GameState(players = players)

        game.currentGameState = gameState
        game.gameHistory.add(gameState)

        val regionCards = rootService.gameService.loadRegionCards()
        val sanctuaryCards = rootService.gameService.loadSanctuaryCards()

        gameState.regionDrawStack.addAll(regionCards)
        gameState.sanctuaryDrawStack.addAll(sanctuaryCards)

        repeat(gameState.players.size + 1) {
            gameState.centerCards.add(gameState.regionDrawStack.removeFirst())
        }

        for (player in gameState.players) {
            val times = if (game.isSimpleVariant) 3 else 5
            repeat(times) {
                player.hand.add(gameState.regionDrawStack.removeFirst())
            }
        }

        val initMessage = rootService.networkService.convertGameToInitMessage(game)

        val gamePlayers: List<Pair<String, List<Int>>> = gameState.players.map { player ->
            Pair(player.name, player.hand.map { it.explorationTime - 1 })
        }
        val drawStack = (gameState.centerCards + gameState.regionDrawStack)
            .map { it.explorationTime - 1 }

        assertEquals(initMessage.players, gamePlayers)
        assertEquals(initMessage.drawStack, drawStack)
        assertEquals(initMessage.sanctuaryStack, gameState.sanctuaryDrawStack.map { it.cardId })
        assertEquals(initMessage.isAdvanced, !game.isSimpleVariant)
    }
}