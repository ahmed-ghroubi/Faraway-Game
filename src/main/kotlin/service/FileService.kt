package service

import entity.FarawayGame
import java.io.*

/**
 * Implements the logic responsible for saving and loading game states.
 *
 * @param rootService The relation to the rootService
 */
class FileService(private val rootService: RootService) : AbstractRefreshingService() {

    /**
     * Saves the current faraway game state to the Downloads folder.
     *
     * File path: `User/Downloads/saveGame_faraway_<timestamp>.bin`
     *
     * Calls `refreshAfterSave()` after saving.
     *
     * @throws IllegalStateException If no game has been started or an online game is in progress.
     */
    fun saveGame() {
        // Check preconditions
        val game = checkNotNull(rootService.currentGame) { "Game has not yet started." }
        check(!game.isOnline) { "Online games cannot be saved locally." }

        // Get path to Downloads directory
        val userHome = System.getProperty("user.home")
        val downloadFolder = File(userHome, "Downloads")

        // Generate filename with timestamp
        val timestamp = System.currentTimeMillis()
        val file = File(downloadFolder, "saveGame_faraway_$timestamp.bin") // Endung .bin statt .json

        try {
            FileOutputStream(file).use { fileOut ->
                ObjectOutputStream(fileOut).use { objectOut ->
                    // Writes entire object incl. sub-objects
                    objectOut.writeObject(game)
                }
            }
            println("Saved: ${file.absolutePath}")
        } catch (e: IOException) {
            println("An error occurred while saving the game: " + e.message)
        }

        // Update UI
        onAllRefreshables { refreshAfterSave() }
    }

    /**
     * Enables the player to load a previously saved GameState from the menu.
     *
     * Loads the most recent file matching the pattern `saveGame_faraway_*.bin`
     * from the user's Downloads directory.
     *
     * Calls `refreshAfterLoad()` after loading the game.
     *
     * @throws IllegalStateException Thrown if there is already an active game.
     * @throws FileNotFoundException Thrown if no save file is found.
     */
    fun loadGame() {
        // Check Precondition
        check(rootService.currentGame == null) { "Cannot load a game while another game is active." }

        // Find the latest save file
        val saveFile = findLatestSaveGame() ?: throw FileNotFoundException("No save game found in Downloads folder.")

        println("Loading save file: ${saveFile.absolutePath}")

        try {
            FileInputStream(saveFile).use { fileIn ->
                ObjectInputStream(fileIn).use { objectIn ->
                    val loadedGame = objectIn.readObject() as FarawayGame

                    rootService.currentGame = loadedGame

                    onAllRefreshables { refreshAfterLoad() }

                    rootService.gameService.refreshBeforeNextMove()
                }
            }

        } catch (e: IOException) {
            println("An error occurred while loading the game: " + e.message)
        }
    }

    // --- Helper Functions ---

    /**
     * Scans the User's Downloads folder for files matching 'saveGame_faraway_*.bin'
     * and returns the one with the largest timestamp.
     */
    private fun findLatestSaveGame(): File? {
        val userHome = System.getProperty("user.home")
        val downloadFolder = File(userHome, "Downloads")

        if (!downloadFolder.exists() || !downloadFolder.isDirectory) {
            return null
        }

        // Filter files: must start with prefix, end with .bin
        return downloadFolder.listFiles()
            ?.filter { it.name.startsWith("saveGame_faraway_") && it.name.endsWith(".bin") }
            ?.maxByOrNull { file ->
                // Extract timestamp from filename for sorting (safer than lastModified)
                val timestampPart = file.name.substringAfter("saveGame_faraway_").substringBefore(".bin")
                timestampPart.toLongOrNull() ?: 0L
            }
    }
}