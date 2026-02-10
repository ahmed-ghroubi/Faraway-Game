package entity

import java.io.Serializable

/**
 * Represents a region card in the game.
 *
 * @param explorationTime The time required to explore the region.
 * @param prerequisites The list of wonders to unlock the region's quest.
 * @param night Indicates if the card has a night symbol.
 * @param clue Indicates if the card provides a clue.
 * @param biome The biome type of the card.
 * @param wonders The list of wonders associated with the card.
 * @param quest The quest for gaining fame from the card.
 */
class RegionCard(
    val explorationTime: Int,
    val prerequisites: List<Wonder>,
    night: Boolean,
    clue: Boolean,
    biome: Biome,
    wonders: List<Wonder>,
    quest: Quest
) : Card(night, clue, biome, wonders, quest), Comparable<RegionCard>, Serializable {

    /**
     * Compares this RegionCard with another based on exploration time.
     */
    override fun compareTo(other: RegionCard): Int {
        return this.explorationTime.compareTo(other.explorationTime)
    }

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = 7667106286702200633L
    }
}