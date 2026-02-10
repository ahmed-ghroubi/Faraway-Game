package service.bot

import entity.GameState
import entity.Player
import entity.RegionCard
import entity.SanctuaryCard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Biome of the card
 *
 * @param value name of the biome
 */
@Serializable
enum class Biome(val value: String) : java.io.Serializable {
    @SerialName("none")
    NONE("none"),

    @SerialName("blue")
    BLUE("blue"),

    @SerialName("green")
    GREEN("green"),

    @SerialName("yellow")
    YELLOW("yellow"),

    @SerialName("red")
    RED("red")
}

/**
 * Stores the duration and if the card is at night.
 *
 * @param night if it's night or not
 * @param duration duration of the card
 */
@Serializable
data class Time(
    val night: Boolean,
    val duration: Int
) : java.io.Serializable

/**
 * How many wonders does the card have
 *
 * @param mineral mineral wonder count
 * @param animal animal wonder count
 * @param plant plant wonder count
 */
@Serializable
data class Wonders(
    var mineral: Int,
    var animal: Int,
    var plant: Int
) : java.io.Serializable

/**
 * typealias for [Wonders]
 */
typealias QuestPrerequisites = Wonders

/**
 * Quest of the card
 *
 * @param fame points worth
 * @param prerequisites quest prerequisites
 * @param types types of quests (fame * sum of types completed)
 */
@Serializable
data class Quest(
    val fame: Int,
    val prerequisites: QuestPrerequisites,
    val types: List<String>
) : java.io.Serializable

/**
 * Faraway card
 *
 * @param id id of card
 * @param sanctuary if card is sanctuary
 * @param biome biome of card
 * @param time night and duration
 * @param wonders wonder to count mapping (mineral, animal, plant)
 * @param clue if the card has the clue symbol
 * @param quest quest of card (optional)
 */
@Serializable
data class Card(
    val id: Int,
    val sanctuary: Boolean,
    val biome: Biome,
    val time: Time,
    val wonders: Wonders,
    val clue: Boolean,
    val quest: Quest?
) : java.io.Serializable

/**
 * Custom player class for bots
 *
 * @param hand ids of the cards in the current hand of player
 * @param journey ids of region cards placed down
 * @param sanctuaries ids of the sanctuaries collected by player
 * @param name name of the player
 */
data class CustomPlayer(
    val hand: List<Int>,
    val journey: List<Int>,
    val sanctuaries: List<Int>,
    val name: String? = null
) : java.io.Serializable

/**
 * Custom game state for bots
 *
 * @param players list of players
 * @param revealed ids of the revealed cards
 * @param regions ids of the regions in the regions draw stack
 * @param sanctuaries ids of the sanctuaries in the sanctuaries draw stack
 */
data class CustomGameState(
    val players: List<CustomPlayer>,
    val revealed: List<Int>,
    val regions: List<Int>,
    val sanctuaries: List<Int>
) : java.io.Serializable


private fun loadCardsFromResources(): List<Card> {
    val stream = object {}.javaClass.getResourceAsStream(
        "/faraway_cards.json"
    ) ?: error("faraway_cards.json not found")
    val jsonString = stream.bufferedReader().use { it.readText() }
    return Json.decodeFromString(jsonString)
}

val CARDS = loadCardsFromResources()
const val N_REG = 68
const val N_CARDS = 113

private data class Track(
    var clue: Int,
    var night: Int,
    var wonders: Wonders,
    val biome: MutableMap<Biome, Int>
)

private fun updateTrack(track: Track, card: Card) {
    if (card.clue) {
        track.clue += 1
    }

    if (card.time.night) {
        track.night += 1
    }

    track.wonders.mineral += card.wonders.mineral
    track.wonders.animal += card.wonders.animal
    track.wonders.plant += card.wonders.plant

    if (card.biome != Biome.NONE) {
        track.biome[card.biome] = track.biome.getValue(card.biome) + 1
    }
}

private fun checkPrereqs(track: Track, quest: Quest): Boolean {
    val prereq = quest.prerequisites
    val w = track.wonders
    return w.mineral >= prereq.mineral &&
            w.animal >= prereq.animal &&
            w.plant >= prereq.plant
}

private fun getQuestScore(track: Track, quest: Quest): Int {
    if (!checkPrereqs(track, quest)) return 0

    val types = quest.types
    val nTypes = types.size

    if (nTypes == 1) {
        val type = types[0]
        val name = type.lowercase()
        val parts = name.split("_", limit = 2)
        val prefix = parts[0]
        val suffix = parts.getOrNull(1)

        return when (prefix) {
            "wonders" -> {
                val value = when (suffix) {
                    "mineral" -> track.wonders.mineral
                    "animal" -> track.wonders.animal
                    "plant" -> track.wonders.plant
                    else -> 0
                }
                value * quest.fame
            }

            "biome" -> {
                val multi = if (suffix == "all") {
                    track.biome.values.min()
                } else {
                    val biome = Biome.valueOf(
                        checkNotNull(suffix).uppercase()
                    )
                    track.biome.getValue(biome)
                }
                multi * quest.fame
            }

            "clue" -> track.clue * quest.fame
            "night" -> track.night * quest.fame
            else -> quest.fame
        }
    }

    if (nTypes == 2) {
        val biomes = types.map {
            Biome.valueOf(it.substringAfter("_").uppercase())
        }
        val multi = track.biome.getValue(biomes[0]) +
                track.biome.getValue(biomes[1])
        return multi * quest.fame
    }

    return quest.fame
}

/**
 * Calculates the total points the player collected
 *
 * @param player [CustomPlayer] object
 *
 * @return total points
 */
fun evaluatePlayer(player: CustomPlayer): Int {
    var score = 0
    val track = Track(
        clue = 0,
        night = 0,
        wonders = Wonders(0, 0, 0),
        biome = mutableMapOf(
            Biome.BLUE to 0,
            Biome.GREEN to 0,
            Biome.YELLOW to 0,
            Biome.RED to 0
        )
    )

    for (cardIdx in player.sanctuaries) {
        updateTrack(track, CARDS[cardIdx])
    }

    for (cardIdx in player.journey.asReversed()) {
        val card = CARDS[cardIdx]
        updateTrack(track, card)
        card.quest?.let {
            score += getQuestScore(track, it)
        }
    }

    for (cardIdx in player.sanctuaries) {
        val card = CARDS[cardIdx]
        card.quest?.let {
            score += getQuestScore(track, it)
        }
    }

    return score
}

/**
 * Abstract bot class.
 * The methods that  have to be implemented:
 *   - [Bot.exploreRegion]: chooses the next region card for
 *                          the journey from hand
 *   - [Bot.chooseSanctuary]: chooses a sanctuary card
 *   - [Bot.chooseRegion]: chooses a region card from the revealed region cards
 */
abstract class Bot(
    protected var name: String, gameState: GameState
): java.io.Serializable {
    protected var actualGameState: GameState = gameState
    protected var gameState: CustomGameState
    protected var player: CustomPlayer

    init {
        this.gameState = toCustomGameState(gameState)
        this.player = this.gameState.players.first { it.name == name }
    }


    protected fun toCustomPlayer(player: Player): CustomPlayer {
        return CustomPlayer(
            hand = player.hand.map { it.explorationTime - 1 },
            journey = player.regionCards.map { it.explorationTime - 1 },
            sanctuaries = player.sanctuaries.map { it.cardId },
            name = player.name
        )
    }

    protected fun toCustomGameState(gameState: GameState): CustomGameState {
        return CustomGameState(
            players = gameState.players.map { toCustomPlayer(it) },
            revealed = gameState.centerCards.map { it.explorationTime - 1 },
            regions = gameState.regionDrawStack.map { it.explorationTime - 1 },
            sanctuaries = gameState.sanctuaryDrawStack.map { it.cardId }
        )
    }

    protected abstract fun exploreRegionInternal(hand: List<Int>): Int
    protected abstract fun chooseSanctuaryInternal(sanctuaries: List<Int>): Int
    protected abstract fun chooseRegionInternal(revealed: List<Int>): Int
    protected abstract fun chooseInitialCardsInternal(
        cards: List<Int>
    ): List<Int>

    private fun regionCardsSetup(cards: List<RegionCard>): List<Int> {
        gameState = toCustomGameState(actualGameState)
        player = gameState.players.first { it.name == name }

        val cardIds = cards.map { it.explorationTime - 1 }
        return cardIds
    }

    /**
     * Picks 3 initial cards, when the game is played in advanced mode.
     *
     * @param cards list of possible region cards
     *
     * @return list of 3 cards
     */
    fun chooseInitialCards(cards: List<RegionCard>): List<RegionCard> {
        val cardIds = regionCardsSetup(cards)
        val chosenCards = chooseInitialCardsInternal(cardIds)
        return chosenCards.mapIndexed { index, _ -> cards[index] }
    }

    /**
     * Chooses the next card for the journey
     *
     * @param hand current hand of bot
     *
     * @return next card for journey
     */
    fun exploreRegion(hand: List<RegionCard>): RegionCard {
        val cardIds = regionCardsSetup(hand)
        val chosenIndex = exploreRegionInternal(cardIds)
        return hand[chosenIndex]
    }

    /**
     * Chooses a sanctuary card
     *
     * @param sanctuaries sanctuary cards
     *
     * @return sanctuary card chosen
     */
    fun chooseSanctuary(sanctuaries: List<SanctuaryCard>): SanctuaryCard {
        gameState = toCustomGameState(actualGameState)
        player = gameState.players.first { it.name == name }

        val cardIds = sanctuaries.map { it.cardId }
        val chosenIndex = chooseSanctuaryInternal(cardIds)
        return sanctuaries[chosenIndex]
    }

    /**
     * Chooses a region card from the revealed cards for the hand
     *
     * @param revealed revealed cards
     *
     * @return region card
     */
    fun chooseRegion(revealed: List<RegionCard>): RegionCard {
        val cardIds = regionCardsSetup(revealed)
        val chosenIndex = chooseRegionInternal(cardIds)
        return revealed[chosenIndex]
    }
}