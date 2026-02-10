package service.bot

import entity.Player
import entity.PlayerType
import service.GameService
import service.RootService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test if [MCTSMaxPointsBot] is java serializable
 */
class MCTSMaxPointsBotSerializationTest {

    private lateinit var hardBot: Bot
    private lateinit var rootService: RootService

    /**
     * test setup
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        val gameService = GameService(rootService)

        gameService.createGame(
            isSimple = true,
            isOnline = false,
            randomOrder = false,
            players = mutableListOf(
                Player("hardbot", PlayerType.BOT_HARD),
                Player("Player1", PlayerType.LOCAL),
            )
        )

        val game = rootService.currentGame!!
        hardBot = game.bots["hardbot"]!!

        // force internal state (tree, currentNode, etc.)
        hardBot.exploreRegion(game.currentGameState.players[0].hand)
    }

    private fun serialize(obj: Any): ByteArray =
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(obj) }
            bos.toByteArray()
        }

    private fun <T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as T
        }

    /**
     * ...
     */
    @Test
    fun `hardBot can be serialized`() {
        val bytes = serialize(hardBot)
        assertTrue(bytes.isNotEmpty())
    }

    /**
     * ...
     */
    @Test
    fun `hardBot can be serialized and deserialized`() {
        val bytes = serialize(hardBot)
        val restored = deserialize<Bot>(bytes)
        assertNotNull(restored)
    }

    /**
     * ...
     */
    @Test
    fun `hardBot works after deserialization`() {
        val bytes = serialize(hardBot)
        val restored = deserialize<Bot>(bytes)

        val game = rootService.currentGame!!

        restored.chooseRegion(game.currentGameState.centerCards)
    }
}
