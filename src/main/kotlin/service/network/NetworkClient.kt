package service.network

import edu.udo.cs.sopra.ntf.InitMessage
import edu.udo.cs.sopra.ntf.PhaseTwoMessage
import edu.udo.cs.sopra.ntf.SelectedRegionCardMessage
import edu.udo.cs.sopra.ntf.SelectedStartingHandMessage
import entity.PlayerType
import tools.aqua.bgw.net.client.BoardGameClient
import tools.aqua.bgw.net.client.NetworkLogging
import tools.aqua.bgw.net.common.annotations.GameActionReceiver
import tools.aqua.bgw.net.common.notification.PlayerJoinedNotification
import tools.aqua.bgw.net.common.notification.PlayerLeftNotification
import tools.aqua.bgw.net.common.response.*

/**
 * Client class for connecting to a Faraway game server.
 *
 * Extends [BoardGameClient] to handle Faraway-specific messages.
 *
 * @param clientName The name of the client/player.
 * @param server The address of the server to connect to.
 * @param secret The secret/password for authentication with the server.
 * @param networkService The [NetworkService] managing this client.
 */
class NetworkClient(
    clientName: String,
    server: String,
    secret: String,
    var networkService: NetworkService
) : BoardGameClient(clientName, server, secret, NetworkLogging.VERBOSE) {

    /**
     * The identifier of this game session.
     *
     * Can be null if no session was started yet.
     */
    var sessionId: String? = null
        private set

    /**
     * The [entity.PlayerType] of the player represented by this client.
     * 0 = player, 1 = easy bot, 2 = hard bot
     *
     * Can be null if no session was started yet.
     */
    var playerType: PlayerType? = null
        internal set

    /**
     * List of players in the current game lobby.
     */
    val players: MutableList<String> = mutableListOf(clientName)

    /**
     * Handle a [CreateGameResponse] sent by the server.
     *
     * Will await the guest player when its status is [CreateGameResponseStatus.SUCCESS].
     * Will disconnect and show an error when the status is anything else.
     *
     * @throws IllegalStateException if the client wasn't expecting this response.
     */
    override fun onCreateGameResponse(response: CreateGameResponse) {
        check(networkService.connectionState == ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
        { "Received unexpected CreateGameResponse." }

        when (response.status) {
            CreateGameResponseStatus.SUCCESS -> {
                sessionId = response.sessionID
                networkService.setConnectionState(ConnectionState.WAITING_FOR_GUESTS)
            }

            else -> disconnectAndError(response.status)
        }
    }

    /**
     * Handle a [JoinGameResponse] sent by the server.
     *
     * Will await the init message when its status is [CreateGameResponseStatus.SUCCESS].
     * Will disconnect and show an error when the status is anything else.
     *
     * @throws IllegalStateException if the client wasn't expecting this response.
     */
    override fun onJoinGameResponse(response: JoinGameResponse) {
        check(networkService.connectionState == ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
        { "Received unexpected JoinGameResponse." }

        when (response.status) {
            JoinGameResponseStatus.SUCCESS -> {
                sessionId = response.sessionID
                networkService.setConnectionState(ConnectionState.WAITING_FOR_INIT)
            }

            else -> disconnectAndError(response.status)
        }
    }

    /**
     * Handle a [PlayerJoinedNotification] sent by the server.
     *
     * Will forward this message to the gui to update the lobby scene.
     *
     * @throws IllegalStateException if the client wasn't expecting this notification.
     */
    override fun onPlayerJoined(notification: PlayerJoinedNotification) {
        check(
            networkService.connectionState in setOf(
                ConnectionState.WAITING_FOR_GUESTS,
                ConnectionState.WAITING_FOR_INIT
            )
        ) { "Received unexpected PlayerJoinedNotification." }

        if (networkService.connectionState == ConnectionState.WAITING_FOR_GUESTS) {
            players.add(notification.sender)
            networkService.onLobbyUpdate()
        }
    }

    /**
     * Handle a [PlayerLeftNotification] sent by the server.
     */
    override fun onPlayerLeft(notification: PlayerLeftNotification) {
        check(
            networkService.connectionState == ConnectionState.WAITING_FOR_GUESTS ||
                    networkService.connectionState == ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        { "Received unexpected PlayerLeftNotification." }

        if (networkService.connectionState == ConnectionState.WAITING_FOR_GUESTS) {
            players.remove(notification.sender)
            networkService.onLobbyUpdate()
        }
    }

    /**
     * Handle a [GameActionResponse] sent by the server.
     *
     * Does nothing unless the status is not [GameActionResponseStatus.SUCCESS], in which case the
     * client will disconnect and show an error.
     *
     * @throws IllegalStateException if the client wasn't expecting this notification.
     */
    override fun onGameActionResponse(response: GameActionResponse) {
        check(
            networkService.connectionState in setOf(
                ConnectionState.SELECT_ADVANCED_MODE_CARDS,
                ConnectionState.PLAYING_SELECT_REGION_CARD,
                ConnectionState.PLAYING_WAITING_FOR_OPPONENT,
                ConnectionState.PLAYING_PHASE_TWO
            )
        ) { "Received unexpected GameActionResponse." }

        if (response.status != GameActionResponseStatus.SUCCESS) {
            disconnectAndError(response.status)
        }
    }

    /**
     * Handle an [InitMessage] sent by the server.
     *
     * Forwards this message to the [NetworkService].
     *
     * @see NetworkService.startNewJoinedGame
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onInitReceived(message: InitMessage, sender: String) {
        networkService.startNewJoinedGame(message)
    }

    /**
     * Handle a [SelectedStartingHandMessage] sent by the server.
     *
     * Forwards this message to the [NetworkService].
     *
     * @see NetworkService.receiveSelectedStartingHandMessage
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectedStartingHandReceived(message: SelectedStartingHandMessage, sender: String) {
        networkService.receiveSelectedStartingHandMessage(message, sender)
    }

    /**
     * Handle a [SelectedRegionCardMessage] sent by the server.
     *
     * Forwards this message to the [NetworkService].
     *
     * @see NetworkService.receiveSelectedRegionCardMessage
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectedRegionCardReceived(message: SelectedRegionCardMessage, sender: String) {
        networkService.receiveSelectedRegionCardMessage(message)
    }

    /**
     * Handle a [PhaseTwoMessage] sent by the server.
     *
     * Forwards this message to the [NetworkService].
     *
     * @see NetworkService.receivePhaseTwoMessage
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onPhaseTwoReceived(message: PhaseTwoMessage, sender: String) {
        networkService.receivePhaseTwoMessage(message)
    }

    /**
     * Disconnects from the service with the given error message
     *
     * @param message represents an error message
     */
    private fun disconnectAndError(message: Any) {
        networkService.disconnect()
        error(message)
    }
}
