package service.bot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 * Tests for [evaluatePlayer]
 */
class BotEvaluatePlayerTest {

    private val playerScores = listOf(
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = emptyList(),
                sanctuaries = emptyList()
            ),
            0
        ),

        // Example from https://youtu.be/J7N3u4x46FI?si=ytzAOM104QayJhn7
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(16, 14, 37, 61, 6, 23, 34, 32),
                sanctuaries = listOf(86, 81, 71, 92, 107)
            ),
            53
        ),

        // Example from https://youtu.be/AScPQ1LOkIw?si=IHDRkLWGfbVgovkl
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(51, 23, 37, 58, 31, 38, 2, 7),
                sanctuaries = listOf(88, 81, 84, 73)
            ),
            69
        ),

        // Example from https://youtu.be/jwYv9MNEhZg?si=87VGGxpKXuwbGCIW
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(21, 61, 54, 13, 15, 38, 3, 19),
                sanctuaries = listOf(89, 103, 70, 110)
            ),
            56
        ),

        // Examples from https://youtu.be/nQluQEAedjU?si=1Zx9CvhL5UA2syCJ
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(20, 0, 44, 56, 13, 48, 28, 1),
                sanctuaries = listOf(85, 79, 77)
            ),
            46
        ),
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(24, 39, 54, 58, 5, 14, 17, 38),
                sanctuaries = listOf(101, 99, 78, 112, 94, 100)
            ),
            64
        ),
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(31, 62, 11, 8, 12, 16, 2, 15),
                sanctuaries = listOf(88, 87, 104, 111)
            ),
            53
        ),
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(45, 65, 55, 42, 52, 27, 37, 57),
                sanctuaries = listOf(75, 86, 81, 90)
            ),
            55
        ),

        // Additional Tests
        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(10, 14, 45, 57, 54, 21, 62, 8),
                sanctuaries = listOf(83, 88, 90, 92, 105)
            ),
            51
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(17, 22, 42, 34, 26, 30, 24, 46),
                sanctuaries = listOf(68, 69, 70, 71, 89)
            ),
            89
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(9, 13, 19, 20, 23, 27, 31, 36),
                sanctuaries = listOf(101, 102, 109, 110)
            ),
            102
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(12, 16, 29, 31, 45, 50, 65, 67),
                sanctuaries = listOf(79, 82, 86, 93, 97)
            ),
            87
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(1, 3, 44, 50, 31, 29, 16, 67),
                sanctuaries = listOf(79)
            ),
            27
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(24, 26, 30, 41, 43, 46, 55, 58),
                sanctuaries = listOf(72, 73, 74, 75, 76, 77)
            ),
            45
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(21),
                sanctuaries = listOf(105)
            ),
            4
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(42),
                sanctuaries = listOf(68, 69, 71)
            ),
            13
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(10, 14),
                sanctuaries = listOf(105)
            ),
            6
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(9, 19),
                sanctuaries = listOf(102, 109)
            ),
            12
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(10, 14, 16, 47, 9),
                sanctuaries = listOf(105, 102, 109)
            ),
            15
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(41, 17, 43, 10, 14),
                sanctuaries = listOf(102, 103)
            ),
            15
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(23, 41, 7, 2),
                sanctuaries = listOf(100, 79)
            ),
            12
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(3, 14, 57),
                sanctuaries = listOf(105, 89)
            ),
            3
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(0, 11, 57, 18, 30),
                sanctuaries = listOf(81, 77, 80)
            ),
            8
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(42, 22, 17, 52, 59),
                sanctuaries = listOf(100)
            ),
            24
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(23, 1, 37, 21),
                sanctuaries = listOf(110, 89, 74)
            ),
            2
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(7, 2),
                sanctuaries = listOf(74)
            ),
            4
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(17, 29, 11),
                sanctuaries = listOf(69)
            ),
            4
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(0),
                sanctuaries = emptyList()
            ),
            0
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(28, 60, 8),
                sanctuaries = listOf(107, 72)
            ),
            12
        ),

        Pair(
            CustomPlayer(
                hand = emptyList(),
                journey = listOf(7, 2, 31, 24, 54, 0, 16),
                sanctuaries = listOf(70, 85, 99, 102, 68)
            ),
            25
        ),

        )

    /**
     * Tests if [evaluatePlayer] returns the correct score for each game
     */
    @Test
    fun testEvaluatePlayerCalculatesExpectedScores() {
        for ((player, expectedScore) in playerScores) {
            assertEquals(expectedScore, evaluatePlayer(player))
        }
    }

    /**
     * A randomized sanity test that makes sure
     * [evaluatePlayer] never return negative values or crash on random inputs.
     */
    @Test
    fun testEvaluatePlayerRandomNonNegative() {
        val regionIds = (0 until 68).toList()
        val sanctuaryIds = (68 until CARDS.size).toList()

        repeat(300) {
            val journeySize = Random.nextInt(0, 8)
            val sanctuarySize = Random.nextInt(0, 6)

            val journey = List(journeySize) { regionIds.random() }
            val sanctuary = List(sanctuarySize) { sanctuaryIds.random() }

            val player = CustomPlayer(hand = emptyList(), journey = journey, sanctuaries = sanctuary)
            val score = evaluatePlayer(player)

            assertTrue(score >= 0, "score must be non-negative (was $score)")
        }
    }
}