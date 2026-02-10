package service.playerActionService

import entity.*
import service.RootService
import kotlin.test.*

/**
 * [ChooseStartingHandTest]Test class for the [service.PlayerActionService.chooseStartingHand] method.
 *
 */
class ChooseStartingHandTest {
    /**
     * Testet den erfolgreichen Ablauf von [service.PlayerActionService.chooseStartingHand].
     *
     * Überprüft, ob:
     * - Die Handgröße nach der Auswahl korrekt ist (3 Karten).
     * - Die ausgewählten Karten in der Hand verbleiben.
     * - Die nicht ausgewählten Karten auf den Nachziehstape lzurückgelegt werden.
     */
    @Test
    fun chooseStartingHandValid() {
        val rootService = RootService()
        val service = rootService.playerActionService

        fun card(id: Int) = RegionCard(id, emptyList(), false, false,
            Biome.GREEN, emptyList(), Quest(false, false, 0, emptyList(),
                emptyList()))

        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        rootService.currentGame = game
        val player = Player("Player1", PlayerType.LOCAL)

        val card1 = card(1)
        val card2 = card(2)
        val card3 = card(3)
        val card4 = card(4)
        val card5 = card(5)

        val hand = mutableListOf(card1, card2, card3, card4, card5)
        player.hand.addAll(hand)

        game.currentGameState = GameState(mutableListOf(player))

        val selectedCards = mutableListOf(card1, card2, card3)

        service.chooseStartingHand(selectedCards)

        assertEquals(3, player.hand.size)
        assertTrue(player.hand.contains(card1))
        assertTrue(player.hand.contains(card2))
        assertTrue(player.hand.contains(card3))

        assertEquals(2, game.currentGameState.regionDrawStack.size)
    }

    /**
     *[chooseStartingHandFailures] Testet die Fehlerfälle von [service.PlayerActionService.chooseStartingHand].
     *
     * Überprüft verschiedene Fehlerszenarien in der Reihenfolge ihrer Implementierung im Service:
     * 1. Kein aktives Spiel ([IllegalStateException]).
     * 2. Spiel ist in der einfachen Variante ([IllegalStateException]).
     * 3. Handgröße ist nicht 5 ([IllegalStateException]).
     * 4. Anzahl der ausgewählten Karten ist nicht 3 ([IllegalArgumentException]).
     */
    @Test
    fun chooseStartingHandFailures() {
        val rootService = RootService()
        val service = rootService.playerActionService

        val player = Player("Player1", PlayerType.LOCAL)
        val hand = mutableListOf(
            RegionCard(1, emptyList(), false, false, Biome.GREEN,
                emptyList(), Quest(false, false, 0, emptyList(),
                    emptyList())),
            RegionCard(2, emptyList(), false, false, Biome.GREEN,
                emptyList(), Quest(false, false, 0, emptyList(),
                    emptyList())),
            RegionCard(3, emptyList(), false, false, Biome.GREEN,
                emptyList(), Quest(false, false, 0, emptyList(),
                    emptyList())),
            RegionCard(4, emptyList(), false, false, Biome.GREEN,
                emptyList(), Quest(false, false, 0, emptyList(),
                    emptyList())),
            RegionCard(5, emptyList(), false, false, Biome.GREEN,
                emptyList(), Quest(false, false, 0, emptyList(),
                    emptyList()))
        )
        player.hand.addAll(hand)
        val selectedCards = mutableListOf(hand[0], hand[1], hand[2])

        // 1. No Game
        // The service checks `checkNotNull(rootService.currentGame)` first.
        rootService.currentGame = null
        val ex1 = assertFailsWith<IllegalStateException> { service.chooseStartingHand(selectedCards) }
        assertEquals("No active game found.", ex1.message)


        // Setup valid game for subsequent tests
        val game = FarawayGame(isOnline = false, isSimpleVariant = false)
        game.currentGameState = GameState(mutableListOf(player))
        rootService.currentGame = game

        // 2. Simple Variant
        // `if (game.isSimpleVariant)`
        val simpleGame = FarawayGame(isOnline = false, isSimpleVariant = true)
        simpleGame.currentGameState = GameState(mutableListOf(player))
        rootService.currentGame = simpleGame
        val ex2 = assertFailsWith<IllegalStateException> { service.chooseStartingHand(selectedCards) }
        assertEquals("chooseStartingHand is only allowed in advanced variant.", ex2.message)

        // Reset to advanced game
        rootService.currentGame = game


        // 3. Hand size != 5
        // `if (player.hand.size != 5)`
        player.hand.removeAt(0) // Now size is 4
        val ex3 = assertFailsWith<IllegalStateException> { service.chooseStartingHand(selectedCards) }
        assertEquals("Player must have 5 cards to choose from.", ex3.message)
        player.hand.add(0, hand[0]) // Restore to 5


        // 4. Selection size != 3
        // `if (cards.size != 3)`
        val invalidSelSize = mutableListOf(hand[0], hand[1])
        val ex4 = assertFailsWith<IllegalArgumentException> { service.chooseStartingHand(invalidSelSize) }
        assertEquals("Player must choose exactly 3 cards.", ex4.message)
    }
}