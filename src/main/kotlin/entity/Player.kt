package entity

import java.io.Serializable

/**
 * Represents a player in the game.
 *
 * @param name The name of the player.
 * @param playerType The type of the player (e.g., local, remote, bot).
 *
 * @property hand The list of region cards in the player's hand.
 * @property regionCards The list of region cards owned by the player.
 * @property sanctuaries The list of sanctuary cards owned by the player.
 * @property temporarySanctuaries The list of temporary sanctuary cards owned by the player.
 * @property selectedCard The currently selected region card by the player.
 * @property score The player's score at the end of the game.
 */
class Player(val name: String, val playerType: PlayerType) : Serializable {
    val hand: MutableList<RegionCard> = mutableListOf()
    val regionCards: MutableList<RegionCard> = mutableListOf()
    val sanctuaries: MutableList<SanctuaryCard> = mutableListOf()
    val temporarySanctuaries: MutableList<SanctuaryCard> = mutableListOf()
    var selectedCard: RegionCard? = null
    var score: IntArray = IntArray(10)

    /**
     * Creates a deep copy of this player.
     */
    fun copy(): Player {
        val p = Player(name, playerType)
        p.hand.addAll(hand)
        p.regionCards.addAll(regionCards)
        p.sanctuaries.addAll(sanctuaries)
        p.temporarySanctuaries.addAll(temporarySanctuaries)
        p.selectedCard = selectedCard
        p.score = score
        return p
    }

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = 1322494284615547068L
    }
}

