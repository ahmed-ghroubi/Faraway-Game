package service.network

/**
 * Enum to distinguish the different states that a client can be in, in particular
 * during connection and game setup. Used in [NetworkService] and [NetworkClient].
 */
enum class ConnectionState {
    /**
     * Initial state. No connection is active and no [NetworkClient] exists.
     */
    DISCONNECTED,

    /**
     * The current [NetworkClient] is connected to a server, but has not started, hosted, or joined any game yet.
     */
    CONNECTED,

    /**
     * The current [NetworkClient] is connected and is trying to create a lobby.
     * Currently waiting for a response from the server.
     */
    WAITING_FOR_HOST_CONFIRMATION,

    /**
     * The current [NetworkClient] is connected and is trying to join a lobby.
     * Currently waiting for a response from the server.
     */
    WAITING_FOR_JOIN_CONFIRMATION,

    /**
     * The current [NetworkClient] is connected and has created a lobby, but has not started a game yet.
     */
    WAITING_FOR_GUESTS,

    /**
     * The current [NetworkClient] is connected and has joined a lobby, but the game hasn't started yet.
     */
    WAITING_FOR_INIT,

    /**
     * The current [NetworkClient] is connected and the game has started.
     * The client is in the pre-game phase of selecting starting hands.
     */
    SELECT_ADVANCED_MODE_CARDS,

    /**
     * The current [NetworkClient] is connected and the game has started.
     * The client is in the pre-game phase of selecting starting hands.
     */
    PLAYING_SELECT_REGION_CARD,

    /**
     * The current [NetworkClient] is connected and the game has started.
     * The client is in the main playing phase, waiting for their turn.
     */
    PLAYING_WAITING_FOR_OPPONENT,

    /**
     * The current [NetworkClient] is connected and the game has started.
     * The client is in the main playing phase, taking their turn.
     */
    PLAYING_PHASE_TWO
}