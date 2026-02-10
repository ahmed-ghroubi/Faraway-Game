package service

import entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test class for the private counter and helper functions in [GameService].
 *
 * This class verifies the correctness of internal helper methods such as:
 * - countWonder()
 * - countBiome()
 * - countClue()
 * - countNight()
 * - checkPrerequisites()
 * - isBiomeAllSet()
 *
 * Since these methods are private, reflection is used to invoke them.
 * The tests ensure correct counting logic and proper handling of edge cases.
 */
class CounterFunctionsTest {

    /** GameService instance under test */
    private lateinit var gameService: GameService

    /**
     * Initializes a fresh [GameService] before each test.
     */
    @BeforeEach
    fun setup() {
        gameService = GameService(RootService())
    }

    // ---------------------------------------------------------
    // Reflection helper for private counter methods
    // ---------------------------------------------------------

    /**
     * Invokes a private counter method of [GameService] via reflection.
     *
     * The parameter types are explicitly defined to avoid ambiguity
     * caused by method overloading and generic types.
     *
     * @param name Name of the private method to invoke
     * @param args Arguments passed to the method
     * @return The integer result returned by the counter method
     */
    private fun invokePrivateCounter(name: String, vararg args: Any?): Int {

        // Explicit parameter definitions to avoid reflection ambiguity
        val parameterTypes = when (name) {
            "countWonder" -> arrayOf(Wonder::class.java, List::class.java)
            "countBiome" -> arrayOf(Biome::class.java, List::class.java)
            "countClue", "countNight" -> arrayOf(List::class.java)
            else -> throw IllegalArgumentException("Unknown method: $name")
        }

        val reflectiveMethod =
            GameService::class.java.getDeclaredMethod(name, *parameterTypes)
        reflectiveMethod.isAccessible = true

        return reflectiveMethod.invoke(gameService, *args) as Int
    }


    // Test data helpers


    /**
     * Creates a concrete [Card] instance for testing purposes.
     *
     * Since [Card] is abstract, a [SanctuaryCard] is used
     * as a concrete implementation.
     *
     * @param biome Biome of the card
     * @param wonders List of wonder symbols on the card
     * @param clue Whether the card contains a clue symbol
     * @param night Whether the card is a night card
     * @return A card instance suitable for counter tests
     */
    private fun createCard(
        biome: Biome = Biome.NONE,
        wonders: List<Wonder> = emptyList(),
        clue: Boolean = false,
        night: Boolean = false
    ): Card {
        return SanctuaryCard(
            cardId = 0,
            night = night,
            clue = clue,
            biome = biome,
            wonders = wonders,
            quest = Quest(night = false, clue = false, fame = 0, wonders = emptyList(), biome = emptyList())
        )
    }


    // Counter function tests


    /**
     * Verifies correct counting of wonder symbols.
     */
    @Test
    fun `test countWonder`() {
        val visible = listOf(
            createCard(wonders = listOf(Wonder.MINERAL, Wonder.PLANT)),
            createCard(wonders = listOf(Wonder.MINERAL)),
            createCard(wonders = listOf(Wonder.ANIMAL))
        )

        assertEquals(
            2,
            invokePrivateCounter("countWonder", Wonder.MINERAL, visible),
            "Should count two MINERAL wonders."
        )
        assertEquals(
            1,
            invokePrivateCounter("countWonder", Wonder.PLANT, visible),
            "Should count one PLANT wonder."
        )
        assertEquals(
            0,
            invokePrivateCounter("countWonder", Wonder.MINERAL, emptyList<Card>()),
            "Empty list should result in zero."
        )
    }

    /**
     * Verifies correct counting of biome occurrences.
     */
    @Test
    fun `test countBiome`() {
        val visible = listOf(
            createCard(biome = Biome.BLUE),
            createCard(biome = Biome.BLUE),
            createCard(biome = Biome.RED),
            createCard(biome = Biome.GREEN)
        )

        assertEquals(
            2,
            invokePrivateCounter("countBiome", Biome.BLUE, visible),
            "Should find two BLUE biomes."
        )
        assertEquals(
            1,
            invokePrivateCounter("countBiome", Biome.RED, visible),
            "Should find one RED biome."
        )
        assertEquals(
            0,
            invokePrivateCounter("countBiome", Biome.YELLOW, visible),
            "Should find no YELLOW biomes."
        )
    }

    /**
     * Verifies correct counting of clue symbols.
     */
    @Test
    fun `test countClue`() {
        val visible = listOf(
            createCard(clue = true),
            createCard(clue = false),
            createCard(clue = true)
        )

        assertEquals(
            2,
            invokePrivateCounter("countClue", visible),
            "Should count two clue symbols."
        )
        assertEquals(
            0,
            invokePrivateCounter("countClue", listOf(createCard(clue = false))),
            "Should return zero when no clue symbols are present."
        )
    }

    /**
     * Verifies correct counting of night symbols.
     */
    @Test
    fun `test countNight`() {
        val visible = listOf(
            createCard(night = true),
            createCard(night = true),
            createCard(night = true),
            createCard(night = false)
        )

        assertEquals(
            3,
            invokePrivateCounter("countNight", visible),
            "Should count three night symbols."
        )
    }


    // checkPrerequisites tests


    /**
     * Verifies that prerequisites are fulfilled
     * when all required wonders are available.
     */
    @Test
    fun `test checkPrerequisites - all requirements met`() {
        val needed = listOf(Wonder.MINERAL, Wonder.ANIMAL)

        val visible = listOf(
            createCard(wonders = listOf(Wonder.MINERAL)),
            createCard(wonders = listOf(Wonder.ANIMAL))
        )

        val result = invokePrivateCheckPrerequisites(needed, visible)
        assertEquals(true, result)
    }

    /**
     * Verifies that prerequisite checking fails
     * when one required wonder is missing.
     */
    @Test
    fun `test checkPrerequisites - missing one type`() {
        val needed = listOf(Wonder.ANIMAL, Wonder.PLANT)

        val visible = listOf(
            createCard(wonders = listOf(Wonder.ANIMAL, Wonder.ANIMAL))
        )

        val result = invokePrivateCheckPrerequisites(needed, visible)
        assertEquals(false, result)
    }

    /**
     * Verifies that prerequisites can be satisfied
     * by combining multiple cards.
     */
    @Test
    fun `test checkPrerequisites - multiple cards combined`() {
        val needed = listOf(Wonder.MINERAL, Wonder.MINERAL)

        val visible = listOf(
            createCard(wonders = listOf(Wonder.MINERAL)),
            createCard(wonders = listOf(Wonder.MINERAL)),
            createCard(wonders = listOf(Wonder.PLANT))
        )

        val result = invokePrivateCheckPrerequisites(needed, visible)
        assertEquals(true, result)
    }

    /**
     * Verifies that an empty prerequisite list
     * always evaluates to true.
     */
    @Test
    fun `test checkPrerequisites - empty requirements always true`() {
        val result = invokePrivateCheckPrerequisites(
            emptyList(),
            emptyList()
        )
        assertEquals(true, result)
    }

    /**
     * Invokes the private `checkPrerequisites` method via reflection.
     *
     * @param needed Required wonders
     * @param visible Visible cards
     * @return true if prerequisites are fulfilled, false otherwise
     */
    private fun invokePrivateCheckPrerequisites(
        needed: List<Wonder>,
        visible: List<Card>
    ): Boolean {
        val method = GameService::class.java.getDeclaredMethod(
            "checkPrerequisites",
            List::class.java,
            List::class.java
        )
        method.isAccessible = true
        return method.invoke(gameService, needed, visible) as Boolean
    }


    // isBiomeAllSet tests


    /**
     * Verifies that a perfect biome set
     * containing all four colors is detected correctly.
     */
    @Test
    fun `test isBiomeAllSet - returns true for perfect set`() {
        val perfectSet =
            listOf(Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.RED)

        val result = invokeIsBiomeAllSet(perfectSet)
        assertEquals(true, result)
    }

    /**
     * Verifies that incorrect list sizes
     * result in false.
     */
    @Test
    fun `test isBiomeAllSet - returns false for wrong size`() {
        val tooFew = listOf(Biome.BLUE, Biome.GREEN, Biome.YELLOW)
        assertEquals(false, invokeIsBiomeAllSet(tooFew))

        val tooMany =
            listOf(Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.RED, Biome.BLUE)
        assertEquals(false, invokeIsBiomeAllSet(tooMany))
    }

    /**
     * Verifies that duplicate biomes with a missing color
     * are rejected even if the list size is correct.
     */
    @Test
    fun `test isBiomeAllSet - returns false for duplicates missing a color`() {
        val duplicates =
            listOf(Biome.BLUE, Biome.BLUE, Biome.YELLOW, Biome.RED)

        val result = invokeIsBiomeAllSet(duplicates)
        assertEquals(false, result)
    }

    /**
     * Verifies that invalid biome values
     * cause the check to fail.
     */
    @Test
    fun `test isBiomeAllSet - returns false for invalid biome types`() {
        val invalidSet =
            listOf(Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.NONE)

        val result = invokeIsBiomeAllSet(invalidSet)
        assertEquals(false, result)
    }

    /**
     * Invokes the private `isBiomeAllSet` method via reflection.
     *
     * @param biomes List of biomes to check
     * @return true if the list represents a valid complete biome set
     */
    private fun invokeIsBiomeAllSet(biomes: List<Biome>): Boolean {
        val method =
            GameService::class.java.getDeclaredMethod("isBiomeAllSet", List::class.java)
        method.isAccessible = true
        return method.invoke(gameService, biomes) as Boolean
    }
}
