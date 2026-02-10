package entity

import java.io.Serializable

/**
 * Represents the Quest of a card.
 *
 * @property night Indicates if a night is needed to get the score.
 * @property clue Indicates if a clue is needed to get the score.
 * @property fame The fame score provided by the card.
 * @property wonders The list of wonders is needed to get the score.
 * @property biome The list of biomes is needed to get the score.
 */
data class Quest(
    val night: Boolean,
    val clue: Boolean,
    val fame: Int,
    val wonders: List<Wonder>,
    val biome: List<Biome>
) : Serializable {

    /** Serial UID for serialization. */
    companion object {
        private const val serialVersionUID: Long = 6737040886404526233L
    }
}