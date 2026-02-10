package service

import entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test class for the private quest evaluation logic in [GameService].
 *
 * This class verifies the correct behavior of the private method
 * `evaluateQuest(Card, List<Card>)`, which calculates the fame value
 * of a quest based on visible cards.
 *
 * Since the method is private, reflection is used to invoke it.
 *
 * Covered cases include:
 * - Failed prerequisite checks
 * - Night quest evaluation
 * - Wonder-based quest evaluation
 * - Biome-based (partial set) quest evaluation
 */
class EvaluateQuestTest {

    /** Game service under test */
    private lateinit var gameService: GameService

    /**
     * Initializes a fresh [GameService] before each test.
     */
    @BeforeEach
    fun setup() {
        gameService = GameService(RootService())
    }


    // Helper methods


    /**
     * Invokes the private `evaluateQuest` method via reflection.
     *
     * @param card The card whose quest should be evaluated
     * @param visible List of cards that are currently visible
     * @return The calculated fame value for the quest
     */
    private fun invokeEvaluateQuest(card: Card, visible: List<Card>): Int {
        val method = GameService::class.java.getDeclaredMethod(
            "evaluateQuest",
            Card::class.java,
            List::class.java
        )
        method.isAccessible = true
        return method.invoke(gameService, card, visible) as Int
    }

    /**
     * Creates a [Quest] instance with configurable properties
     * for use in test cases.
     *
     * @param fame Fame value of the quest
     * @param night Whether the quest depends on night cards
     * @param clue Whether the quest depends on clue cards
     * @param wonders List of required wonders
     * @param biome List of required biomes
     */
    private fun createQuest(
        fame: Int = 0,
        night: Boolean = false,
        clue: Boolean = false,
        wonders: List<Wonder> = emptyList(),
        biome: List<Biome> = emptyList()
    ): Quest = Quest(night, clue, fame, wonders, biome)


    // Tests


    /**
     * Verifies that the method returns 0 if the prerequisite
     * conditions of a quest are not fulfilled.
     *
     * In this case, the required MINERAL wonder is missing
     * from the visible cards.
     */
    @Test
    fun `test return 0 when prerequisites failed`() {
        val card = RegionCard(
            explorationTime = 10,
            prerequisites = listOf(Wonder.MINERAL),
            night = false,
            clue = false,
            biome = Biome.NONE,
            wonders = emptyList(),
            quest = createQuest(fame = 100)
        )

        // No visible cards providing the required prerequisite
        val visible = emptyList<Card>()

        val result = invokeEvaluateQuest(card, visible)

        assertEquals(
            0,
            result,
            "Should return 0 if quest prerequisites are not fulfilled."
        )
    }

    /**
     * Verifies correct calculation for night-based quests.
     *
     * The fame value is multiplied by the number of visible
     * night cards.
     */
    @Test
    fun `test night quest calculation`() {
        val quest = createQuest(fame = 5, night = true)
        val card = SanctuaryCard(
            1,
            night = false,
            false,
            Biome.NONE,
            emptyList(),
            quest
        )

        val visible = listOf(
            SanctuaryCard(2, night = true, false, Biome.NONE, emptyList(), createQuest()),
            SanctuaryCard(3, night = true, false, Biome.NONE, emptyList(), createQuest()),
            SanctuaryCard(4, night = false, false, Biome.NONE, emptyList(), createQuest())
        )

        val result = invokeEvaluateQuest(card, visible)

        // 2 night cards * 5 fame = 10
        assertEquals(
            10,
            result,
            "Should correctly count night cards and multiply by fame."
        )
    }

    /**
     * Verifies correct calculation for wonder-based quests.
     *
     * The fame value is multiplied by the total number of
     * matching wonder symbols in the visible cards.
     */
    @Test
    fun `test wonders quest calculation`() {
        val quest = createQuest(fame = 10, wonders = listOf(Wonder.ANIMAL))
        val card = SanctuaryCard(
            1,
            night = false,
            false,
            Biome.NONE,
            emptyList(),
            quest
        )

        val visible = listOf(
            SanctuaryCard(
                2,
                night = false,
                false,
                Biome.NONE,
                listOf(Wonder.ANIMAL, Wonder.ANIMAL),
                createQuest()
            ),
            SanctuaryCard(
                3,
                night = false,
                false,
                Biome.NONE,
                listOf(Wonder.ANIMAL),
                createQuest()
            ),
            SanctuaryCard(
                4,
                night = false,
                false,
                Biome.NONE,
                listOf(Wonder.PLANT),
                createQuest()
            )
        )

        val result = invokeEvaluateQuest(card, visible)

        // 3 animal symbols * 10 fame = 30
        assertEquals(
            30,
            result,
            "Should correctly count wonder symbols and multiply by fame."
        )
    }

    /**
     * Verifies correct calculation for biome-based quests
     * with partial biome sets.
     *
     * Each matching biome occurrence contributes to the
     * final score.
     */
    @Test
    fun `test biome partial set calculation`() {
        val quest = createQuest(
            fame = 3,
            biome = listOf(Biome.RED, Biome.BLUE)
        )

        val card = SanctuaryCard(
            1,
            night = false,
            false,
            Biome.NONE,
            emptyList(),
            quest
        )

        val visible = listOf(
            SanctuaryCard(2, night = false, false, Biome.RED, emptyList(), createQuest()),
            SanctuaryCard(3, night = false, false, Biome.RED, emptyList(), createQuest()),
            SanctuaryCard(4, night = false, false, Biome.BLUE, emptyList(), createQuest()),
            SanctuaryCard(5, night = false, false, Biome.GREEN, emptyList(), createQuest())
        )

        val result = invokeEvaluateQuest(card, visible)

        // (2x RED + 1x BLUE) * 3 fame = 9
        assertEquals(
            9,
            result,
            "Should sum matching biomes and multiply by fame."
        )
    }
}
