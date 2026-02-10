package service.gameService

import entity.*
import service.RootService
import kotlin.test.*

/**
 * [InitializePlayfieldTest] Testklasse für die Initialisierung des Spielfelds.
 * Überprüft, ob Karten korrekt verteilt und Stapel initialisiert werden.
 */
/**
 * [InitializePlayfieldTest] Test class for the initialization of the playfield.
 * Verifies that cards are correctly distributed and stacks are initialized.
 */
class InitializePlayfieldTest {

    /**
     * Tests initialization for the simple variant.
     *
     * Expected behavior:
     * - Each player receives 3 hand cards.
     * - The center contains (number of players + 1) cards.
     * - The region draw stack and sanctuary draw stack are correctly filled.
     */
    @Test
    fun testInitializePlayfieldSimpleVariant() {
        val rootService = RootService()
        val gameService = rootService.gameService

        // Create game (Simple Variant = true)
        gameService.createGame(
            true,
            false,
            false,
            listOf(Player("Ahmed", PlayerType.LOCAL), Player("Sami", PlayerType.LOCAL))
        )

        val game = rootService.currentGame!!
        val gameState = game.currentGameState

        // Check: Each player has 3 hand cards
        assertEquals(3, gameState.players[0].hand.size, "Player 1 should have 3 cards")
        assertEquals(3, gameState.players[1].hand.size, "Player 2 should have 3 cards")

        // Check: Center has player count + 1 cards (2 + 1 = 3)
        assertEquals(3, gameState.centerCards.size, "The center should contain 3 cards")

        // Check: Region draw stack
        // Total 68 - (2 * 3 hand cards) - 3 center = 68 - 6 - 3 = 59
        assertEquals(59, gameState.regionDrawStack.size, "Region stack should contain 59 cards")

        // Check: Sanctuary stack (always 45)
        assertEquals(45, gameState.sanctuaryDrawStack.size, "Sanctuary stack should contain 45 cards")
    }

    /**
     * Tests initialization for the advanced variant.
     *
     * Expected behavior:
     * - Each player receives 5 hand cards.
     * - The center contains (number of players + 1) cards.
     * - The region draw stack and sanctuary draw stack are correctly filled.
     */
    @Test
    fun testInitializePlayfieldAdvancedVariant() {
        val rootService = RootService()
        val gameService = rootService.gameService

        // Create game (Simple Variant = false(Advanced) )
        gameService.createGame(
            false,
            false,
            false,
            listOf(Player("Ali", PlayerType.LOCAL), Player("Alex", PlayerType.LOCAL))
        )

        val game = rootService.currentGame!!
        val gameState = game.currentGameState

        // Check: Each player has 5 hand cards
        assertEquals(5, gameState.players[0].hand.size, "Player 1 should have 5 cards")
        assertEquals(5, gameState.players[1].hand.size, "Player 2 should have 5 cards")

        // Check: Center has player count + 1 cards (2 + 1 = 3)
        assertEquals(3, gameState.centerCards.size, "The center should contain 3 cards")

        // Check: Region draw stack
        // Total 68 - (2 * 5 hand cards) - 3 center = 68 - 10 - 3 = 55
        assertEquals(55, gameState.regionDrawStack.size, "Region stack should contain 55 cards")

        // Check: Sanctuary stack (always 45)
        assertEquals(45, gameState.sanctuaryDrawStack.size, "Sanctuary stack should contain 45 cards")
    }
}
