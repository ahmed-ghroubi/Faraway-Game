package entity

import java.io.Serializable

/**
 * Represents a Sanctuary Card in the game.
 *
 * @param cardId Unique identifier for the sanctuary card.
 * @param night Indicates if the card is a night card.
 * @param clue Indicates if the card provides a clue.
 * @param biome The biome type of the card.
 * @param wonders List of wonders associated with the card.
 * @param quest The quest for gaining fame from the card.
 */
class SanctuaryCard(
    val cardId: Int,
    night: Boolean,
    clue: Boolean,
    biome: Biome,
    wonders: List<Wonder>,
    quest: Quest
) : Card(night, clue, biome, wonders, quest), Serializable {

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = 9001344884620510227L
    }
}