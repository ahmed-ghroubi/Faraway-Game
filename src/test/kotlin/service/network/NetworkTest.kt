package service.network

import entity.Player
import entity.PlayerType
import service.RootService
import kotlin.test.*

/**
 * Test class for hosting and joining a game using [NetworkService].
 */
class NetworkTest {

    private val rootServices: MutableList<RootService> = mutableListOf()

    /**
     * Sets up multiple [RootService] instances for testing.
     */
    @BeforeTest
    fun setup() {
        repeat(5) {
            rootServices.add(RootService())
        }
    }

    /**
     * Tests hosting and joining a game, then disconnecting both host and guest.
     */
    @Test
    fun testConnections() {

        val rootServiceHost = rootServices[0]
        val rootServiceGuest = rootServices[1]

        rootServiceHost.networkService.hostGame("TestHost")
        rootServiceHost.waitForStateChange(ConnectionState.WAITING_FOR_GUESTS)
        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.WAITING_FOR_GUESTS)

        val client = checkNotNull(rootServiceHost.networkService.currentClient)
        val sessionID = checkNotNull(client.sessionId)

        rootServiceGuest.networkService.joinGame(sessionID, "TestGuest", PlayerType.LOCAL)
        rootServiceGuest.waitForStateChange(ConnectionState.WAITING_FOR_INIT)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_INIT)

        rootServiceGuest.networkService.disconnect()
        rootServiceHost.networkService.disconnect()

        assertNull(rootServiceHost.networkService.currentClient)
        assertNull(rootServiceGuest.networkService.currentClient)
    }

    /**
     * Tests the gameplay flow between a host and a guest.
     */
    @Test
    fun testGameplay() {
        val rootServiceHost = rootServices[0]
        val rootServiceGuest = rootServices[1]

        rootServiceHost.networkService.hostGame("TestHost")
        rootServiceHost.waitForStateChange(ConnectionState.WAITING_FOR_GUESTS)
        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.WAITING_FOR_GUESTS)

        val client = checkNotNull(rootServiceHost.networkService.currentClient)
        val sessionID = checkNotNull(client.sessionId)

        rootServiceGuest.networkService.joinGame(sessionID, "TestGuest", PlayerType.LOCAL)
        rootServiceGuest.waitForStateChange(ConnectionState.WAITING_FOR_INIT)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_INIT)

        val playerInLobby = client.players.map { name ->
            val type = if (name == client.playerName) PlayerType.LOCAL else PlayerType.REMOTE
            Player(name, type)
        }

        rootServiceHost.networkService.startNewHostedGame(
            isSimple = true,
            randomOrder = false,
            players = playerInLobby
        )

        rootServiceHost.waitForStateChange(ConnectionState.PLAYING_SELECT_REGION_CARD)
        assertEquals(ConnectionState.PLAYING_SELECT_REGION_CARD, rootServiceHost.networkService.connectionState)
        rootServiceGuest.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        assertEquals(ConnectionState.PLAYING_WAITING_FOR_OPPONENT, rootServiceGuest.networkService.connectionState)

        val hostGame = checkNotNull(rootServiceHost.currentGame)
        val currentHostPlayer = hostGame.currentGameState.players[0]
        val minExplorationTimeHost = currentHostPlayer.hand.minOf { it.explorationTime }
        val firstHand = currentHostPlayer.hand.find { it.explorationTime == minExplorationTimeHost }
        checkNotNull(firstHand)
        rootServiceHost.playerActionService.playRegionCard(firstHand)
        rootServiceHost.playerActionService.proceedAfterPlayRegionCard()

        rootServiceHost.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        assertEquals(ConnectionState.PLAYING_WAITING_FOR_OPPONENT, rootServiceHost.networkService.connectionState)

        Thread.sleep(3000)

        rootServiceGuest.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
        rootServiceGuest.playerActionService.proceedAfterPlayRegionCard()

        val guestGame = checkNotNull(rootServiceGuest.currentGame)
        val currentGuestPlayer = guestGame.currentGameState.players[guestGame.currentGameState.currentPlayer]
        val currentHostPlayerAfterSend = hostGame.currentGameState.players[hostGame.currentGameState.currentPlayer]

        assertEquals(currentHostPlayerAfterSend.name, currentGuestPlayer.name)

        val minExplorationTimeGust = currentGuestPlayer.hand.minOf { it.explorationTime }
        val secondHand = currentGuestPlayer.hand.find { it.explorationTime == minExplorationTimeGust }
        checkNotNull(secondHand)
        rootServiceGuest.playerActionService.playRegionCard(secondHand)
        rootServiceGuest.playerActionService.proceedAfterPlayRegionCard()

        Thread.sleep(3000)

        rootServiceHost.playerActionService.proceedAfterPlayRegionCard()

        Thread.sleep(3000)

        assertEquals(true, hostGame.currentGameState.isPhaseTwo)
        assertEquals(true, guestGame.currentGameState.isPhaseTwo)

        if (hostGame.currentGameState.players[0].name == "TestHost") {
            val middleCard = hostGame.currentGameState.centerCards[0]
            rootServiceHost.playerActionService.chooseRegionCard(middleCard)
            rootServiceHost.playerActionService.proceedAfterChooseRegionCard()

            rootServiceHost.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(2000)
            rootServiceGuest.playerActionService.proceedAfterChooseRegionCard()
        } else {
            val middleCard = guestGame.currentGameState.centerCards[0]
            rootServiceGuest.playerActionService.chooseRegionCard(middleCard)
            rootServiceGuest.playerActionService.proceedAfterChooseRegionCard()

            rootServiceGuest.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(2000)
            rootServiceHost.playerActionService.proceedAfterChooseRegionCard()
        }

        Thread.sleep(3000)

        if (hostGame.currentGameState.players[hostGame.currentGameState.currentPlayer].name == "TestHost") {
            rootServiceHost.networkService.setConnectionState(ConnectionState.PLAYING_PHASE_TWO)
            val middleCard = hostGame.currentGameState.centerCards[1]
            rootServiceHost.playerActionService.chooseRegionCard(middleCard)
            rootServiceHost.playerActionService.proceedAfterChooseRegionCard()

            rootServiceHost.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(2000)
            rootServiceGuest.playerActionService.proceedAfterChooseRegionCard()
        } else {
            rootServiceGuest.networkService.setConnectionState(ConnectionState.PLAYING_PHASE_TWO)
            val middleCard = guestGame.currentGameState.centerCards[1]
            rootServiceGuest.playerActionService.chooseRegionCard(middleCard)
            rootServiceGuest.playerActionService.proceedAfterChooseRegionCard()

            rootServiceGuest.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(2000)
            rootServiceHost.playerActionService.proceedAfterChooseRegionCard()
        }

        assertEquals(false, hostGame.currentGameState.isPhaseTwo)
        assertEquals(false, guestGame.currentGameState.isPhaseTwo)

        Thread.sleep(5000)

         if (hostGame.currentGameState.players[hostGame.currentGameState.currentPlayer].name == "TestHost") {
             rootServiceHost.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
             val maxExplorationTimeHost = currentHostPlayer.hand.maxOf { it.explorationTime }
             val roundTwoHand = currentHostPlayer.hand.find { it.explorationTime == maxExplorationTimeHost }
             checkNotNull(roundTwoHand)
             rootServiceHost.playerActionService.playRegionCard(roundTwoHand)
             rootServiceHost.playerActionService.proceedAfterPlayRegionCard()

             rootServiceHost.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
             assertEquals(ConnectionState.PLAYING_WAITING_FOR_OPPONENT, rootServiceHost.networkService.connectionState)

             Thread.sleep(3000)

             rootServiceGuest.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
             rootServiceGuest.playerActionService.proceedAfterPlayRegionCard()
        } else {
            rootServiceGuest.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
            val maxExplorationTimeGust = currentGuestPlayer.hand.maxOf { it.explorationTime }
            val roundTwoHand = currentGuestPlayer.hand.find { it.explorationTime == maxExplorationTimeGust }
            checkNotNull(roundTwoHand)
            rootServiceGuest.playerActionService.playRegionCard(roundTwoHand)
            rootServiceGuest.playerActionService.proceedAfterPlayRegionCard()

            rootServiceGuest.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
            assertEquals(ConnectionState.PLAYING_WAITING_FOR_OPPONENT, rootServiceGuest.networkService.connectionState)

             Thread.sleep(3000)

             rootServiceHost.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
             rootServiceHost.playerActionService.proceedAfterPlayRegionCard()
        }

        if (hostGame.currentGameState.players[hostGame.currentGameState.currentPlayer].name == "TestHost") {
            rootServiceHost.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
            val maxExplorationTimeHost = currentHostPlayer.hand.maxOf { it.explorationTime }
            val roundTwoHand = currentHostPlayer.hand.find { it.explorationTime == maxExplorationTimeHost }
            checkNotNull(roundTwoHand)
            rootServiceHost.playerActionService.playRegionCard(roundTwoHand)
            rootServiceHost.playerActionService.proceedAfterPlayRegionCard()

            rootServiceHost.networkService.setConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(3000)

            rootServiceGuest.playerActionService.proceedAfterPlayRegionCard()
        } else {
            rootServiceGuest.networkService.setConnectionState(ConnectionState.PLAYING_SELECT_REGION_CARD)
            val maxExplorationTimeGust = currentGuestPlayer.hand.maxOf { it.explorationTime }
            val roundTwoHand = currentGuestPlayer.hand.find { it.explorationTime == maxExplorationTimeGust }
            checkNotNull(roundTwoHand)
            rootServiceGuest.playerActionService.playRegionCard(roundTwoHand)
            rootServiceGuest.playerActionService.proceedAfterPlayRegionCard()

            rootServiceGuest.networkService.setConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(3000)

            rootServiceHost.playerActionService.proceedAfterPlayRegionCard()
        }

        Thread.sleep(5000)

        if (hostGame.currentGameState.players[0].name == "TestHost") {
            val sanctuary = hostGame.currentGameState.players[hostGame.currentGameState.currentPlayer].temporarySanctuaries[0]
            rootServiceHost.playerActionService.chooseSanctuaryCard(sanctuary)
            rootServiceHost.playerActionService.proceedAfterChooseSanctuaryCard()

            val middleCard = hostGame.currentGameState.centerCards[0]
            rootServiceHost.playerActionService.chooseRegionCard(middleCard)
            rootServiceHost.playerActionService.proceedAfterChooseRegionCard()

            rootServiceHost.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(2000)
            rootServiceGuest.playerActionService.proceedAfterChooseRegionCard()
        } else {
            val sanctuary = guestGame.currentGameState.players[guestGame.currentGameState.currentPlayer].temporarySanctuaries[0]
            rootServiceGuest.playerActionService.chooseSanctuaryCard(sanctuary)
            rootServiceGuest.playerActionService.proceedAfterChooseSanctuaryCard()

            val middleCard = guestGame.currentGameState.centerCards[0]
            rootServiceGuest.playerActionService.chooseRegionCard(middleCard)
            rootServiceGuest.playerActionService.proceedAfterChooseRegionCard()

            rootServiceGuest.waitForStateChange(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)

            Thread.sleep(2000)
            rootServiceHost.playerActionService.proceedAfterChooseRegionCard()
        }
    }

    /**
     * Tests the game in advanced mode
     */
    @Test
    fun testAdvancedMode() {
        val rootServiceHost = rootServices[0]
        val rootServiceGuest = rootServices[1]

        rootServiceHost.networkService.hostGame("TestHost")
        rootServiceHost.waitForStateChange(ConnectionState.WAITING_FOR_GUESTS)
        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.WAITING_FOR_GUESTS)

        val client = checkNotNull(rootServiceHost.networkService.currentClient)
        val sessionID = checkNotNull(client.sessionId)

        rootServiceGuest.networkService.joinGame(sessionID, "TestGuest", PlayerType.LOCAL)
        rootServiceGuest.waitForStateChange(ConnectionState.WAITING_FOR_INIT)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_INIT)

        val playerInLobby = client.players.map { name ->
            val type = if (name == client.playerName) PlayerType.LOCAL else PlayerType.REMOTE
            Player(name, type)
        }

        rootServiceHost.networkService.startNewHostedGame(
            isSimple = false,
            randomOrder = false,
            players = playerInLobby
        )

        rootServiceHost.waitForStateChange(ConnectionState.SELECT_ADVANCED_MODE_CARDS)
        assertEquals(ConnectionState.SELECT_ADVANCED_MODE_CARDS, rootServiceHost.networkService.connectionState)
        rootServiceGuest.waitForStateChange(ConnectionState.SELECT_ADVANCED_MODE_CARDS)
        assertEquals(ConnectionState.SELECT_ADVANCED_MODE_CARDS, rootServiceGuest.networkService.connectionState)

        val hostGame = checkNotNull(rootServiceHost.currentGame)
        val guestGame = checkNotNull(rootServiceGuest.currentGame)
        val hostPlayer = hostGame.currentGameState.players[0]
        val guestPlayer = guestGame.currentGameState.players[1]
        rootServiceHost.networkService.sendSelectedStartingHandMessage(hostPlayer.hand)
        rootServiceGuest.networkService.sendSelectedStartingHandMessage(guestPlayer.hand)

        repeat(2) {
            hostGame.currentGameState.regionDrawStack.add(hostPlayer.hand.removeLast())
        }

        repeat(2) {
            guestGame.currentGameState.regionDrawStack.add(guestPlayer.hand.removeLast())
        }

        Thread.sleep(5000)

        rootServiceHost.networkService.waitForAllPlayersToSelectStartingHand(hostGame)
        rootServiceGuest.networkService.waitForAllPlayersToSelectStartingHand(guestGame)

    }

    /**
     * Waits for the [RootService]'s network service to change to the specified [state] within the given [timeout].
     *
     * @param state the desired connection state to wait for.
     * @param timeout the maximum time to wait in milliseconds (default is 5000ms).
     *
     * @throws IllegalStateException if the state does not change within the timeout.
     */
    private fun RootService.waitForStateChange(state: ConnectionState, timeout: Int = 15000) {
        var timePassed = 0
        while (timePassed < timeout) {
            if (networkService.connectionState == state) return
            else {
                Thread.sleep(100)
                timePassed += 100
            }
        }
        error("Did not change to state $state in time. Current state: ${networkService.connectionState}")
    }
}
