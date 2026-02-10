package entity

import java.io.Serializable

/**
 * Represents a card in the game with its attributes.
 *
 * @param night Indicates if the card has a night symbol.
 * @param clue Indicates if the card provides a clue.
 * @param biome The biome type of the card.
 * @param wonders The list of wonders associated with the card.
 * @param quest The quest for gaining fame from the card.
 */
abstract class Card(
    val night: Boolean,
    val clue: Boolean,
    val biome: Biome,
    val wonders: List<Wonder>,
    val quest: Quest
): Serializable {

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = 2767031320512066629L
    }
}