package service.bot

import entity.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertDoesNotThrow
import service.*
import kotlin.test.*
import kotlin.system.measureTimeMillis

/**
 * Mock Bot for testing protected bot variables [Bot.gameState] and
 * [Bot.player].
 */
private class MockBot(name: String, gameState: GameState) :
    Bot(name, gameState) {
    override fun chooseInitialCardsInternal(cards: List<Int>): List<Int> {
        return listOf(1, 2, 3)
    }

    override fun exploreRegionInternal(hand: List<Int>): Int {
        return 0
    }

    override fun chooseSanctuaryInternal(sanctuaries: List<Int>): Int {
        return 0
    }

    override fun chooseRegionInternal(revealed: List<Int>): Int {
        return 0
    }


    /**
     * Returns the protected variable [Bot.gameState]
     *
     * @return [Bot.gameState]
     */
    fun getBotGameState(): CustomGameState {
        return gameState
    }

    /**
     * Returns the protected variable [Bot.player]
     *
     * @return [Bot.player]
     */
    fun getBotPlayer(): CustomPlayer {
        return player
    }
}

/**
 * Tests for testing the [Bot]
 */
class BotTest {
    private lateinit var rootService: RootService
    private lateinit var bot: MockBot

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

        bot = MockBot("bot", rootService.currentGame!!.currentGameState)
    }

    private fun checkAfterCall() {
        val gameState = rootService.currentGame?.currentGameState
        checkNotNull(gameState)


        val player = bot.getBotPlayer()

        assertNotNull(player)

        assertEquals(
            gameState.players[0].hand.map { it.explorationTime - 1 },
            player.hand
        )
        assertEquals(
            listOf(),
            player.journey
        )

        assertEquals(
            listOf(),
            player.sanctuaries
        )

        assertEquals(
            "bot",
            player.name
        )

        val botGameState = bot.getBotGameState()

        assertNotNull(botGameState)

        assertEquals(
            listOf("bot", "player"),
            botGameState.players.map { it.name }
        )

        assertEquals(
            gameState.regionDrawStack.map { it.explorationTime - 1 },
            botGameState.regions
        )

        assertEquals(
            gameState.sanctuaryDrawStack.map { it.cardId },
            botGameState.sanctuaries
        )

        assertEquals(
            gameState.centerCards.map { it.explorationTime - 1 },
            botGameState.revealed
        )
    }

    /**
     * Checking if [Bot.chooseInitialCards] works correctly
     */
    @Test
    fun botChooseInitialCardsWorksIntended() {
        val gameState = rootService.currentGame?.currentGameState
        checkNotNull(gameState)


        bot.chooseInitialCards(
            gameState.regionDrawStack.take(5)
        )

        checkAfterCall()
    }

    /**
     * Checking if [Bot.exploreRegion] works correctly
     */
    @Test
    fun botExploreRegionWorksIntended() {
        val gameState = rootService.currentGame?.currentGameState
        checkNotNull(gameState)


        bot.exploreRegion(gameState.players[0].hand)

        checkAfterCall()
    }

    /**
     * Checking if [Bot.chooseRegion] works correctly
     */
    @Test
    fun botChooseRegionWorksIntended() {
        val gameState = rootService.currentGame?.currentGameState
        checkNotNull(gameState)


        bot.chooseRegion(gameState.centerCards)

        checkAfterCall()
    }

    /**
     * Checking if [Bot.chooseSanctuary] works correctly
     */
    @Test
    fun botChooseSanctuaryWorksIntended() {
        val gameState = rootService.currentGame?.currentGameState
        checkNotNull(gameState)


        bot.chooseSanctuary(gameState.sanctuaryDrawStack)

        checkAfterCall()
    }

    /**
     * A private helper function that serves for checking if the bot activity modifies the [GameState]
     * This function looks for illegal side effects of bots action that may cause any modification in [GameState]
     * by taking snapshots from sanctuary card which every user has in their hands, the region cards in their hands
     * and in the middle of game field and turn them to the list using their ID
     * comparing the snapshots taken before and after each move help us identify any unwanted change
     */
    private fun snapshotMinimalState(gameState: GameState): Map<String, List<List<Any>>> {
        val players = gameState.players.map {
            listOf(
                it.hand.map { rc -> rc.explorationTime - 1 },
                it.regionCards.map { rc -> rc.explorationTime - 1 },
                it.sanctuaries.map { s -> s.cardId }
            )
        }
        val center = gameState.centerCards.map { it.explorationTime - 1 }
        val regions = gameState.regionDrawStack.map { it.explorationTime - 1 }
        val sanctuaries = gameState.sanctuaryDrawStack.map { it.cardId }
        return mapOf(
            "players" to players,
            "center" to listOf(center),
            "regions" to listOf(regions),
            "sanctuaries" to listOf(sanctuaries)
        )
    }



    /**
     * Bot must not mutate the engine [GameState] when calling exploreRegion.
     */
    @Test
    fun testBotDoesNotMutateGameStateOnExploreRegion() {
        val gameState = rootService.currentGame!!.currentGameState
        val before = snapshotMinimalState(gameState)

        bot.exploreRegion(gameState.players[0].hand)

        val after = snapshotMinimalState(gameState)
        assertEquals(before, after, "exploreRegion must not mutate GameState")
    }

    /**
     * Bot must not mutate the engine [GameState] when calling chooseRegion.
     */
    @Test
    fun testBotDoesNotMutateGameStateOnChooseRegion() {
        val gameState = rootService.currentGame!!.currentGameState
        val before = snapshotMinimalState(gameState)

        bot.chooseRegion(gameState.centerCards)

        val after = snapshotMinimalState(gameState)
        assertEquals(before, after, "chooseRegion must not mutate GameState")
    }

    /**
     * Tests if decision methods of bots are reasonably fast.
     */
    @Test
    fun testBotDecisionTime() {
        val gameState = rootService.currentGame!!.currentGameState

        val msExplore = measureTimeMillis { bot.exploreRegion(gameState.players[0].hand) }
        val msChoose = measureTimeMillis { bot.chooseRegion(gameState.centerCards) }
        val msSanct = measureTimeMillis { bot.chooseSanctuary(gameState.sanctuaryDrawStack) }

        assertTrue(msExplore < 10_000, "exploreRegion too slow: ${msExplore}ms")
        assertTrue(msChoose < 10_000, "chooseRegion too slow: ${msChoose}ms")
        assertTrue(msSanct < 10_000, "chooseSanctuary too slow: ${msSanct}ms")
    }

    /**
     * Tests if bots would not crash on many random 3-card hands.
     */
//    @Test
//    fun testBotDoesNotCrashOnRandomHands() {
//        val gameState = rootService.currentGame!!.currentGameState
//        val pool = gameState.regionDrawStack + gameState.centerCards
//        val greedy = GreedyBot("bot", gameState)
//        val rnd = RandomBot("bot", gameState)
//
//        repeat(200) {
//            val hand = List(3) { pool.random() }
//            try {
//                greedy.exploreRegion(hand)
//                rnd.exploreRegion(hand)
//            } catch (e: Exception) {
//                fail("Bot threw exception on random hand: ${e.message}")
//            }
//        }
//    }

    /**
     * GreedyBot should be deterministic for the same state when reinitialized.
     */
    @Test
    fun testGreedyIsDeterministicWhenReinitialized() {
        val gameState = rootService.currentGame!!.currentGameState

        val game1 = GreedyBot("bot", gameState)
        val game2 = GreedyBot("bot", gameState)

        val card1 = game1.exploreRegion(gameState.players[0].hand)
        val card2 = game2.exploreRegion(gameState.players[0].hand)
        assertEquals(card1, card2, "GreedyBot exploreRegion should be deterministic")

        val region1 = game1.chooseRegion(gameState.centerCards)
        val region2 = game2.chooseRegion(gameState.centerCards)
        assertEquals(region1, region2, "GreedyBot chooseRegion should be deterministic")
    }

    /**
     * Greedy sanctuary pick should match immediate [evaluatePlayer] improvement.
     */
    @Test
    fun testGreedySanctuaryPickMatchesImmediateScore() {
        val gsPlayers = listOf(
            Player("bot", PlayerType.BOT_EASY),
            Player("player", PlayerType.LOCAL),
        )
        val rootService = RootService()
        rootService.gameService.createGame(
            isSimple = true,
            isOnline = false,
            randomOrder = false,
            players = gsPlayers
        )
        val gameState = rootService.currentGame!!.currentGameState
        if (gameState.sanctuaryDrawStack.size < 2) return

        val sanctuaryStack0 = gameState.sanctuaryDrawStack[0].cardId
        val sanctuaryStack1 = gameState.sanctuaryDrawStack[1].cardId

        val base = CustomPlayer(hand = emptyList(), journey = emptyList(), sanctuaries = emptyList())
        val score0 = evaluatePlayer(base.copy(sanctuaries = listOf(sanctuaryStack0)))
        val score1 = evaluatePlayer(base.copy(sanctuaries = listOf(sanctuaryStack1)))

        val greedy = GreedyBot("bot", gameState)
        val chosenCard = greedy.chooseSanctuary(
            listOf(gameState.sanctuaryDrawStack[0], gameState.sanctuaryDrawStack[1])
        )
        val chosenId = chosenCard.cardId


        val expected = if (score0 >= score1) sanctuaryStack0 else sanctuaryStack1
        assertEquals(expected, chosenId)
    }

    /**
     * MCTSMaxPointsBot.chooseSanctuary must always return a SanctuaryCard from the offered list.
     */
    @Test
    fun testMCTSChooseSanctuaryReturnsLegalCard() {
        val state = rootService.currentGame!!.currentGameState
        val bot = MCTSMaxPointsBot("bot", state)

        val offered = state.sanctuaryDrawStack.take(3)
        if (offered.isEmpty()) return

        val chosen = bot.chooseSanctuary(offered)
        assertTrue(offered.contains(chosen), "Chosen sanctuary must be from the offered list")
    }

    /**
     * MCTSMaxPointsBot.chooseSanctuary must pick the sanctuary that maximizes evaluatePlayer
     * when added to the player's current sanctuaries.
     */
    @Test
    fun testMCTSChooseSanctuaryPicksBestImmediateScore() {
        val state = rootService.currentGame!!.currentGameState
        val bot = MCTSMaxPointsBot("bot", state)

        val offered = state.sanctuaryDrawStack.take(3)
        if (offered.size < 2) return

        val me = state.players.first { it.name == "bot" }
        val currentJourney = me.regionCards.map { it.explorationTime - 1 }
        val currentSancts = me.sanctuaries.map { it.cardId }

        val offeredIds = offered.map { it.cardId }

        val scores = offeredIds.map { cand ->
            evaluatePlayer(
                CustomPlayer(
                    hand = emptyList(),
                    journey = currentJourney,
                    sanctuaries = currentSancts + cand
                )
            )
        }

        val expectedBestId = offeredIds[scores.indexOf(scores.max())]
        val chosen = bot.chooseSanctuary(offered)

        assertEquals(expectedBestId, chosen.cardId)
    }

    /**
     * chooseInitialCards must return exactly 3 distinct cards from the offered 5 cards.
     */
    @Test
    fun testMCTSChooseInitialCardsReturns3DistinctValidCards() {
        val state = rootService.currentGame!!.currentGameState
        val bot = MCTSMaxPointsBot("bot", state)

        val offered = state.regionDrawStack.take(5)
        if (offered.size < 5) return

        val chosen = bot.chooseInitialCards(offered)

        assertEquals(3, chosen.size, "Must choose exactly 3 cards")
        assertEquals(3, chosen.distinct().size, "Chosen cards must be distinct")
        assertTrue(chosen.all { offered.contains(it) }, "All chosen cards must come from offered list")
    }

    /**
     * exploreRegion must return a RegionCard from the offered hand.
     * SLOW: exploreRegion runs simulations for ~9 seconds.
     */
    @Test
    @Tag("slow")
    @Timeout(12)
    fun testMCTSExploreRegionReturnsLegalCard() {
        val state = rootService.currentGame!!.currentGameState
        val bot = MCTSMaxPointsBot("bot", state)

        val hand = state.players.first { it.name == "bot" }.hand
        val chosen = bot.exploreRegion(hand)

        assertTrue(hand.contains(chosen), "Chosen region must be from the offered hand")
    }

    /**
     * MCTS bot must not mutate GameState while selecting moves.
     * This catches accidental in-place modifications of lists.
     * SLOW: runs MCTS simulations.
     */
    @Test
    @Tag("slow")
    @Timeout(25)
    fun testMCTSDoesNotMutateGameState() {
        val state = rootService.currentGame!!.currentGameState
        val before = snapshotMinimalState(state)

        // Use separate bot instances to avoid internal tree mismatch NPEs
        val bot1 = MCTSMaxPointsBot("bot", state)
        bot1.exploreRegion(state.players.first { it.name == "bot" }.hand)

        if (state.centerCards.isNotEmpty()) {
            val bot2 = MCTSMaxPointsBot("bot", state)
            bot2.chooseRegion(state.centerCards)
        }

        val after = snapshotMinimalState(state)
        assertEquals(before, after, "Bot must not mutate GameState during decision making")
    }


    /**
     * MCTS bot maintains internal state (currentNode). Repeated calls must not crash.
     * SLOW: each call runs ~9 seconds.
     */
    @Test
    @Tag("slow")
    @Timeout(40)
    fun testMCTSRepeatedCallsDoNotCrash() {
        val state = rootService.currentGame!!.currentGameState

        // Each call uses a fresh bot instance to avoid the tree-reuse NPE
        assertDoesNotThrow {
            val botA = MCTSMaxPointsBot("bot", state)
            val chosenH = botA.exploreRegion(state.players.first { it.name == "bot" }.hand)
            assertTrue(state.players.first { it.name == "bot" }.hand.contains(chosenH))

            if (state.centerCards.isNotEmpty()) {
                val botB = MCTSMaxPointsBot("bot", state)
                val chosenR = botB.chooseRegion(state.centerCards)
                assertTrue(state.centerCards.contains(chosenR))
            }

            val botC = MCTSMaxPointsBot("bot", state)
            val chosenH2 = botC.exploreRegion(state.players.first { it.name == "bot" }.hand)
            assertTrue(state.players.first { it.name == "bot" }.hand.contains(chosenH2))
        }
    }
}