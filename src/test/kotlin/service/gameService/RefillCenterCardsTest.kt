package service.gameService

import entity.*
import service.RootService
import kotlin.test.*

/**
 * [RefillCenterCardsTest]Testklasse für die Methode [service.GameService.refillCenterCards].
 *
 * Diese Klasse stellt sicher, dass die Karten in der Mitte korrekt vom Nachziehstapel aufgefüllt werden,
 * dass die verbleibende Karte in der Mitte unter den Nachziehstapel gelegt wird und dass
 * Exceptions geworfen werden, wenn die Vorbedingungen nicht erfüllt sind.
 */
class RefillCenterCardsTest {
    /**
     * [succeedsRefillTest]test that,
     * [service.GameService.refillCenterCards] succeeds in the normal Case.
     *
     *
     */

    @Test
    fun succeedsRefillTest() {
        val root = RootService()
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        root.currentGame = game
        fun c(id: Int) = RegionCard(id, emptyList(), night = false, clue = false,
            biome = Biome.GREEN, wonders = emptyList(), quest = Quest(
                night = false, clue = false, fame = 0, wonders = emptyList(),
                biome = emptyList()
            )
        )

        val p1 = Player("A", PlayerType.LOCAL).apply { regionCards.add(c(10)) }
        val p2 = Player("B", PlayerType.LOCAL).apply { regionCards.add(c(20)) }
        game.currentGameState = GameState(mutableListOf(p1, p2))

        with(game.currentGameState) {
            centerCards.add(c(100))
            regionDrawStack.addAll(listOf(c(1), c(2), c(3)))
        }

        root.gameService.refillCenterCards()

        assertEquals(3, game.currentGameState.centerCards.size)
        assertEquals(0, game.currentGameState.regionDrawStack.size)
        assertFalse((game.currentGameState.centerCards + game.currentGameState.regionDrawStack).any
        { it.explorationTime == 100 })
    }

    /**
     * [centerExtraCardsTest] throws an Exception,
     * when the Center contains an invalid number of cards
     *
     *
     */

    @Test
    fun centerExtraCardsTest () {
        val rootService = RootService()
        val gameService = rootService.gameService



        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game
        val gameState = game.currentGameState
        val quest = Quest(night = false, clue = false, fame = 0, wonders = emptyList(), biome = emptyList())

        // Case A
        gameState.centerCards.clear()
        var exception = assertFailsWith<IllegalStateException> {
            gameService.refillCenterCards()
        }
        assertEquals("Center must contain exactly 1 card to be refilled.", exception.message)

        // Case B
        gameState.centerCards.add(
            RegionCard(1,emptyList(),
                night = false, clue = false, biome = Biome.GREEN, wonders = emptyList(), quest = quest
            )
        )
        gameState.centerCards.add(
            RegionCard(2,emptyList(),
                false, clue = false, biome = Biome.GREEN, wonders = emptyList(), quest = quest
            )
        )
        exception = assertFailsWith<IllegalStateException> {
            gameService.refillCenterCards()
        }
        assertEquals("Center must contain exactly 1 card to be refilled.", exception.message)
    }

    /**
     * [succeedsRefillTest]test that,
     * [service.GameService.refillCenterCards] succeeds in the normal Case.
     */
    @Test
    fun succeedsRefillTest2() {
        val root = RootService()
        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        root.currentGame = game
        fun c(id: Int) = RegionCard(id, emptyList(), night = false, clue = false,
            biome = Biome.GREEN, wonders = emptyList(), quest = Quest(
                night = false, clue = false, fame = 0, wonders = emptyList(),
                biome = emptyList()
            )
        )

        val p1 = Player("A", PlayerType.LOCAL).apply { regionCards.add(c(10)) }
        val p2 = Player("B", PlayerType.LOCAL).apply{ regionCards.add(c(20)) }
        game.currentGameState = GameState(mutableListOf(p1, p2))

        with(game.currentGameState) {
            centerCards.add(c(100))
            regionDrawStack.addAll(listOf(c(1), c(2), c(3)))
        }

        root.gameService.refillCenterCards()

        assertEquals(3, game.currentGameState.centerCards.size)
        assertEquals(0, game.currentGameState.regionDrawStack.size)
        assertFalse((game.currentGameState.centerCards + game.currentGameState.regionDrawStack).any
        { it.explorationTime == 100 })
    }

    /**
     * [centerExtraCardsTest] throws an Exception,
     * when the Center contains an invalid number of cards
     */
    @Test
    fun centerExtraCardsTest2() {
        val rootService = RootService()
        val gameService = rootService.gameService

        val game = FarawayGame(isOnline = false, isSimpleVariant = true)
        rootService.currentGame = game
        val gameState = game.currentGameState
        val quest = Quest(night = false, clue = false, fame = 0, wonders = emptyList(), biome = emptyList())

        // Case A
        gameState.centerCards.clear()
        var exception = assertFailsWith<IllegalStateException> {
            gameService.refillCenterCards()
        }
        assertEquals("Center must contain exactly 1 card to be refilled.", exception.message)

        // Case B
        gameState.centerCards.add(
            RegionCard(1,emptyList(),
                night = false, clue = false, biome = Biome.GREEN, wonders = emptyList(), quest = quest
            )
        )
        gameState.centerCards.add(
            RegionCard(2,emptyList(),
                false, clue = false, biome = Biome.GREEN, wonders = emptyList(), quest = quest
            )
        )
        exception = assertFailsWith<IllegalStateException> {
            gameService.refillCenterCards()
        }
        assertEquals("Center must contain exactly 1 card to be refilled.", exception.message)
    }

}