package service.fileService

import entity.*
import org.junit.jupiter.api.assertThrows
import service.RootService
import service.TestRefreshable
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import kotlin.test.*

/**
 * Test class for the `loadGame()` function.
 */
class LoadGameTest {

    private lateinit var rootService: RootService
    private lateinit var testRefreshable: TestRefreshable

    // Test environment
    private lateinit var tempUserHome: File
    private lateinit var downloadDir: File
    private val originalUserHome: String? = System.getProperty("user.home")

    private lateinit var dummySaveFile: File

    /**
     * Sets up the service, temp environment, and a dummy save file.
     */
    @BeforeTest
    fun setUp() {
        // Set up temporary file system
        tempUserHome = Files.createTempDirectory("testUserHome").toFile()
        downloadDir = File(tempUserHome, "Downloads")
        downloadDir.mkdir()

        System.setProperty("user.home", tempUserHome.absolutePath)

        // Service Setup
        rootService = RootService()
        rootService.currentGame = null
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        // Create a standard dummy save file (Round 5)
        val dummyGame = FarawayGame(isOnline = false, isSimpleVariant = true)
        val dummyGameState = GameState(mutableListOf(Player("SavedPlayer", PlayerType.LOCAL)))
        dummyGameState.currentRound = 5

        dummyGame.gameHistory.add(dummyGameState)
        dummyGame.currentGameState = dummyGameState
        dummyGame.gameHistoryIndex = 0
        dummySaveFile = File(downloadDir, "saveGame_faraway_999999999.bin")
        writeObject(dummySaveFile, dummyGame)
    }

    /**
     * Clean up temp environment.
     */
    @AfterTest
    fun tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome)
        } else {
            System.clearProperty("user.home")
        }
        tempUserHome.deleteRecursively()
    }

    /**
     * Tests successful loading of the standard dummy file.
     */
    @Test
    fun testLoadGameSuccess() {
        rootService.fileService.loadGame()

        val loadedGame = rootService.currentGame
        assertNotNull(loadedGame, "Game should be initialized after loading.")
        assertEquals(5, loadedGame.currentGameState.currentRound)
        assertEquals("SavedPlayer", loadedGame.currentGameState.players[0].name)
        assertTrue(testRefreshable.refreshAfterLoadCalled)
    }

    /**
     * Tests logic for selecting the *correct* file when multiple exist.
     * Should ignore invalid names and pick the highest timestamp.
     */
    @Test
    fun testLoadGameSortingAndFiltering() {
        // Create older file (Round 1) - Timestamp 1000
        val oldFile = File(downloadDir, "saveGame_faraway_1000.bin")
        val oldGame = FarawayGame(isOnline = false, isSimpleVariant = true)
        writeObject(oldFile, oldGame)

        // Create newer file (Round 10) - Timestamp 9000000000000
        val newFile = File(downloadDir, "saveGame_faraway_9000000000000.bin")
        val newGame = FarawayGame(isOnline = false, isSimpleVariant = false)
        writeObject(newFile, newGame)

        // Create file with invalid timestamp (should be ignored or treated as 0)
        val invalidFile = File(downloadDir, "saveGame_faraway_text.bin")
        writeObject(invalidFile, FarawayGame(isOnline = false, isSimpleVariant = true))

        rootService.fileService.loadGame()

        val loadedGame = rootService.currentGame
        assertNotNull(loadedGame)
        assertFalse(loadedGame.isSimpleVariant, "Should load the file with highest timestamp")
    }

    /**
     * Tests behavior when a save file is corrupted.
     * The service should catch the exception and game should remain null.
     */
    @Test
    fun testLoadGameCorruptedFile() {
        // Overwrite dummy file with random text
        dummySaveFile.writeText("Not a valid binary object")

        rootService.fileService.loadGame()

        assertNull(rootService.currentGame, "Game should not load from corrupted file")

        // Refresh should not be called
        assertFalse(testRefreshable.refreshAfterLoadCalled)
    }

    /**
     * Tests that loading fails if a game is already active.
     */
    @Test
    fun testLoadGameWhileActive() {
        rootService.currentGame = FarawayGame(isOnline = false, isSimpleVariant = false)

        assertThrows<IllegalStateException> {
            rootService.fileService.loadGame()
        }
        assertFalse(testRefreshable.refreshAfterLoadCalled)
    }

    /**
     * Tests reaction when no save file exists.
     */
    @Test
    fun testLoadGameFileNotFound() {
        // Ensure no files exist
        dummySaveFile.delete()

        assertThrows<FileNotFoundException> {
            rootService.fileService.loadGame()
        }
        assertFalse(testRefreshable.refreshAfterLoadCalled)
    }

    /**
     * Tests when the Downloads directory itself is missing or invalid.
     */
    @Test
    fun testLoadGameFolderMissing() {
        // Delete the downloads folder (recursively because it's not empty)
        downloadDir.deleteRecursively()

        assertThrows<FileNotFoundException> {
            rootService.fileService.loadGame()
        }
    }

    /**
     * Tests loading a [FarawayGame] where ALL properties (wrapper & inner state)
     * are set to non-default values.
     */
    @Test
    fun testLoadGameComplexProperties() {
        // Create complex game state
        val complexState = createComplexGameState()
        val gameToWrite = FarawayGame(isOnline = false, isSimpleVariant = true).apply {
            currentGameState = complexState
            simulationSpeed = 500
            gameHistoryIndex = 2
        }

        // Save and load
        val complexSaveFile = File(downloadDir, "saveGame_faraway_9999999999999.bin")

        writeObject(complexSaveFile, gameToWrite)

        rootService.fileService.loadGame()

        // Assert
        val loadedGame = rootService.currentGame
        assertNotNull(loadedGame, "FarawayGame must be loaded")

        // Assert game values
        assertEquals(500, loadedGame.simulationSpeed, "Wrapper: simulationSpeed wrong")
        assertEquals(false, loadedGame.isOnline, "Wrapper: isOnline wrong")

        // Assert game state
        assertComplexGameState(loadedGame.currentGameState)
    }

    // --- Helper ---

    private fun writeObject(file: File, obj: Any) {
        FileOutputStream(file).use { fileOut ->
            ObjectOutputStream(fileOut).use { objectOut ->
                objectOut.writeObject(obj)
            }
        }
    }

    private fun createComplexGameState(): GameState {
        val testBiome = Biome.entries.first()
        val testWonder = Wonder.entries.first()

        val testQuest = Quest(night = true, biome = listOf(testBiome), wonders = listOf(testWonder),
            clue = true, fame = 6)

        val complexRegionCard = RegionCard(explorationTime = 42, prerequisites = listOf(testWonder),
            night = true, clue = false, biome = testBiome,
            wonders = listOf(testWonder, testWonder), quest = testQuest)

        val complexSanctuaryCard = SanctuaryCard(cardId = 101, night = false, clue = true,
            biome = testBiome, wonders = listOf(), quest = testQuest)

        return GameState(
            players = mutableListOf(Player("Alice", PlayerType.LOCAL), Player("Bob", PlayerType.BOT_EASY)),
            regionDrawStack = mutableListOf(complexRegionCard, complexRegionCard),
            sanctuaryDrawStack = mutableListOf(complexSanctuaryCard),
            centerCards = mutableListOf(complexRegionCard),
            currentPlayer = 1,
            currentRound = 8,
            isPhaseTwo = true
        )
    }

    private fun assertComplexGameState(state: GameState) {
        val testBiome = Biome.entries.first()

        // Primitive types & Logic
        assertEquals(8, state.currentRound, "currentRound loading error")
        assertEquals(1, state.currentPlayer, "currentPlayer loading error")
        assertTrue(state.isPhaseTwo, "isPhaseTwo should be true")

        // List sizes
        assertEquals(2, state.players.size)
        assertEquals(2, state.regionDrawStack.size)
        assertEquals(1, state.sanctuaryDrawStack.size)
        assertEquals(1, state.centerCards.size)

        // Content Deep Check
        val loadedRegionCard = state.centerCards[0]
        assertEquals(42, loadedRegionCard.explorationTime, "RegionCard: explorationTime wrong")
        assertTrue(loadedRegionCard.night, "RegionCard: night wrong")
        assertEquals(testBiome, loadedRegionCard.biome, "RegionCard: biome wrong")
        assertEquals(2, loadedRegionCard.wonders.size, "RegionCard: wonders list size wrong")

        val loadedSanctuary = state.sanctuaryDrawStack[0]
        assertEquals(101, loadedSanctuary.cardId, "SanctuaryCard: cardId wrong")
        assertTrue(loadedSanctuary.clue, "SanctuaryCard: clue wrong")
    }
}