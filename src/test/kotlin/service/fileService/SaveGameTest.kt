package service.fileService

import entity.*
import org.junit.jupiter.api.assertThrows
import service.RootService
import service.TestRefreshable
import java.io.File
import java.nio.file.Files
import kotlin.test.*

/**
 * Test class for the `saveGame()` function.
 */
class SaveGameTest {

    private lateinit var rootService: RootService
    private lateinit var testGame: FarawayGame
    private lateinit var testRefreshable: TestRefreshable

    // Test environment variables
    private lateinit var tempUserHome: File
    private lateinit var downloadDir: File
    private val originalUserHome: String? = System.getProperty("user.home")

    /**
     * Sets up a temporary user environment and a valid game state before each test.
     */
    @BeforeTest
    fun setUp() {
        // 1. Set up temporary file system to mimic User Home -> Downloads
        tempUserHome = Files.createTempDirectory("testUserHome").toFile()
        downloadDir = File(tempUserHome, "Downloads")
        downloadDir.mkdir()

        // Redirect system property so the service writes to our temp folder
        System.setProperty("user.home", tempUserHome.absolutePath)

        // Set up Service and Game
        rootService = RootService()
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        testGame = FarawayGame(isOnline = false, isSimpleVariant = true)
        testGame.currentGameState = GameState(
            players = mutableListOf(Player("Player1", PlayerType.LOCAL), Player("Player2", PlayerType.LOCAL))
        )
        rootService.currentGame = testGame
    }

    /**
     * Restores system properties and cleans up temporary files.
     */
    @AfterTest
    fun tearDown() {
        // Restore original user.home
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome)
        } else {
            System.clearProperty("user.home")
        }

        // Recursively delete the temp directory
        tempUserHome.deleteRecursively()
    }

    /**
     * Tests the successful saving of a local game.
     */
    @Test
    fun testSaveGameSuccess() {
        // Act
        rootService.fileService.saveGame()

        // Assert
        val savedFile = downloadDir.listFiles()
            ?.maxByOrNull { it.lastModified() }

        assertNotNull(savedFile, "A save file should have been created.")
        assertTrue(savedFile.name.startsWith("saveGame_faraway_"), "Filename should start with correct prefix.")
        assertTrue(savedFile.length() > 0, "File should contain data.")
        assertTrue(testRefreshable.refreshAfterSaveCalled, "The refreshable should have been called.")
    }

    /**
     * Tests exception handling when the file system prevents writing.
     * - Scenario: 'Downloads' exists but is a file (not a directory),
     * preventing the creation of the save file inside it.
     */
    @Test
    fun testSaveGameExceptionHandling() {
        // Delete the directory and replace it with a file
        downloadDir.delete()
        downloadDir.createNewFile()

        // Should catch the IOException internally and print stacktrace, but NOT crash
        rootService.fileService.saveGame()

        // Confirm that the 'Downloads' file is still a file (no directory created)
        assertTrue(downloadDir.isFile)

        // Refresh should always be called
        assertTrue(testRefreshable.refreshAfterSaveCalled)
    }

    /**
     * Tests that an [IllegalStateException] is thrown if no game is running.
     */
    @Test
    fun testSaveGameNoActiveGame() {
        rootService.currentGame = null

        assertThrows<IllegalStateException> {
            rootService.fileService.saveGame()
        }
        assertFalse(testRefreshable.refreshAfterSaveCalled)
    }

    /**
     * Tests that an [IllegalStateException] is thrown if the game is Online.
     */
    @Test
    fun testSaveGameOnlineGame() {
        val onlineGame = FarawayGame(isOnline = true, isSimpleVariant = true)
        rootService.currentGame = onlineGame

        assertThrows<IllegalStateException> {
            rootService.fileService.saveGame()
        }
        assertFalse(testRefreshable.refreshAfterSaveCalled)
    }
}