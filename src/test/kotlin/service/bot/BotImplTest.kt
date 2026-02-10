package service.bot

import entity.GameState
import entity.Player
import entity.PlayerType
import service.RootService
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

/**
 * Testing the bot implementations (subclasses)
 */
abstract class BotImplTest<T : Bot> {
    private lateinit var rootService: RootService
    private lateinit var bot: T

    protected abstract fun create(name: String, gameState: GameState): T

    /**
     * Setup for the tests
     */
    @BeforeEach
    fun setUp() {
        val players = listOf(
            Player("bot", PlayerType.BOT_EASY),
            Player("player", PlayerType.LOCAL),
        )

        rootService = RootService()
        rootService.gameService.createGame(
            isSimple = true,
            isOnline = false,
            randomOrder = false,
            players = players
        )

        bot = create("bot", rootService.currentGame!!.currentGameState)
    }

    /**
     * Testing [Bot.chooseInitialCards]
     */
    @Test
    fun chooseInitialCardsReturnsCorrectNumberOfCards() {
        val gameState = rootService.currentGame?.currentGameState
        checkNotNull(gameState)

        val regionCards = gameState.regionDrawStack.take(5)
        val regCardIds = regionCards.map { it.explorationTime - 1 }

        val cards = bot.chooseInitialCards(
            regionCards
        )

        assertEquals(
            3,
            cards.map { it.explorationTime - 1 }.toSet().size
        )

        for (card in cards) {
            assert(card.explorationTime - 1 in regCardIds)
        }
    }
}

/**
 * Testing [RandomBot]
 */
class RandomBotTest : BotImplTest<RandomBot>() {
    override fun create(name: String, gameState: GameState): RandomBot =
        RandomBot(name, gameState)
}

/**
 * Testing [GreedyBot]
 */
class GreedyBotTest : BotImplTest<GreedyBot>() {
    override fun create(name: String, gameState: GameState): GreedyBot =
        GreedyBot(name, gameState)
}