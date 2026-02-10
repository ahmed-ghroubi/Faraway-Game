package entity

import java.io.Serializable

/**
 * Represents the overall state of the game.
 *
 * @property players The list of players in the game.
 * @property regionDrawStack The draw stack for region cards.
 * @property sanctuaryDrawStack The draw stack for sanctuary cards.
 * @property centerCards The list of center region cards.
 * @property currentPlayer The index of the current player.
 * @property currentRound The current round number.
 * @property isPhaseTwo Indicates if the game is in phase two.
 */
class GameState(
    val players: MutableList<Player> = mutableListOf(),
    val regionDrawStack: MutableList<RegionCard> = mutableListOf(),
    val sanctuaryDrawStack: MutableList<SanctuaryCard> = mutableListOf(),
    val centerCards: MutableList<RegionCard> = mutableListOf(),
    var currentPlayer: Int = 0,
    var currentRound: Int = 1,
    var isPhaseTwo: Boolean = false,
    var sendSanctuaryCard: Boolean = false
) : Serializable {

    /**
     * Creates a deep copy of the game state.
     */
    fun copy(): GameState =
        GameState(
            players = players.map { it.copy() }.toMutableList(),
            regionDrawStack = regionDrawStack.toMutableList(),
            sanctuaryDrawStack = sanctuaryDrawStack.toMutableList(),
            centerCards = centerCards.toMutableList(),
            currentPlayer = currentPlayer,
            currentRound = currentRound,
            isPhaseTwo = isPhaseTwo
        )

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = -2520802423922850670L
    }
}
