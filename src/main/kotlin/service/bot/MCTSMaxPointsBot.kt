package service.bot

import entity.GameState
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource


private const val C = 1.4142
private val timeSource = TimeSource.Monotonic

private fun <T> Iterable<T>.combinations(length: Int): Sequence<List<T>> =
    sequence {
        val pool = this@combinations as? List<T> ?: toList()
        val n = pool.size
        if (length > n) return@sequence
        val indices = IntArray(length) { it }
        while (true) {
            yield(indices.map { pool[it] })
            var i = length
            do {
                i--
                if (i == -1) return@sequence
            } while (indices[i] == i + n - length)
            indices[i]++
            for (j in i + 1 until length) indices[j] = indices[j - 1] + 1
        }
    }


private data class MCTSNode(
    var parent: MCTSNode?
) {
    var visits: Int = 0
    var totalRewards: Double = 0.0
    val children = mutableMapOf<Int, MCTSNode>()
    val unvisited: MutableList<Int> = mutableListOf()
    var visited = false

    val uct: Double
        get() {
            if (visits == 0) return Double.POSITIVE_INFINITY

            val exploit = totalRewards / visits
            val explore = C * sqrt(
                ln(
                    1.0 * checkNotNull(parent).visits
                ) / visits
            )

            return exploit + explore
        }
}

/**
 * Monte Carlo Tree Search Bot that maximizes the points.
 */
class MCTSMaxPointsBot(
    name: String,
    gameState: GameState
) : Bot(name, gameState) {
    private val allRevealed: MutableList<List<Int>> = mutableListOf()

    @Transient
    private var currentNode: MCTSNode? = MCTSNode(null)


    private fun evaluateJourney(journey: List<Int>): Double {
        val k = journey.windowed(2).count { (prev, curr) ->
            curr > prev
        } - player.sanctuaries.size

        val sanctuaries = gameState.sanctuaries.shuffled().take(k)

        val points = evaluatePlayer(
            CustomPlayer(
                listOf(),
                journey,
                sanctuaries
            )
        )

        val reward = points.coerceAtMost(100) / 100.0

        return reward
    }

    private fun simulate(
        node: MCTSNode,
        journey: MutableList<Int>,
        hand: MutableList<Int>,
        phase: Phase,
        revealed: List<Int>?
    ): Double {
        var cards: List<Int>
        val roundId = journey.size
        val pickedCard: Int
        val reward: Double

        node.visits += 1

        if (roundId == 8 && phase == Phase.CHOOSE) {
            reward = evaluateJourney(journey)
            node.totalRewards += reward
            return reward
        }

        cards = if (phase == Phase.EXPLORE) hand
        else revealed ?: allRevealed[roundId - 1]

        if (!node.visited) {
            node.unvisited.addAll(cards.shuffled())
            node.visited = true
        }

        if (node.unvisited.isNotEmpty()) {
            pickedCard = node.unvisited.removeLast()
            node.children[pickedCard] = MCTSNode(node)
        } else {
            val uctValues = cards.map {
                checkNotNull(node.children[it]).uct
            }
            pickedCard = cards[uctValues.indexOf(uctValues.max())]
        }

        if (phase == Phase.EXPLORE) {
            journey.add(pickedCard)
            hand.remove(pickedCard)

            reward = simulate(
                checkNotNull(node.children[pickedCard]),
                journey,
                hand,
                Phase.CHOOSE,
                null
            )
        } else {
            hand.add(pickedCard)

            reward = simulate(
                checkNotNull(node.children[pickedCard]),
                journey,
                hand,
                Phase.EXPLORE,
                null
            )
        }

        node.totalRewards += reward

        return reward
    }

    private fun runSimulations(
        hand: List<Int>,
        revealed: List<Int>?,
        duration: Duration
    ) {
        val phase = if (revealed == null) Phase.EXPLORE else Phase.CHOOSE
        val journey = player.journey

        if (currentNode == null) {
            currentNode = MCTSNode(null)
        }

        if (allRevealed.isEmpty()) {
            val revSize = gameState.players.size + 1

            allRevealed += gameState.revealed
            allRevealed += gameState.regions.takeLast(revSize)

            for (i in 1..5) {
                val from = gameState.regions.size - revSize * (i + 1)
                val to = gameState.regions.size - revSize * i
                allRevealed += gameState.regions.subList(from, to).toList()
            }
        }

        val start = timeSource.markNow()

        if (currentNode == null) {
            currentNode = MCTSNode(null)
        }

        while (start.elapsedNow() < duration) {
            simulate(
                checkNotNull(currentNode),
                journey.toMutableList(),
                hand.toMutableList(),
                phase,
                revealed
            )
        }
    }

    override fun exploreRegionInternal(hand: List<Int>): Int {
        runSimulations(
            player.hand,
            null,
            9.0.seconds
        )

        val node = checkNotNull(currentNode)

        val visits = hand.map {
            checkNotNull(node.children[it]).visits
        }

        println("### MCTSMaxPointsBot.exploreRegion $visits")
        val bestCardIdx = visits.indexOf(visits.max())

        currentNode = checkNotNull(node.children[hand[bestCardIdx]])

        return bestCardIdx
    }

    override fun chooseSanctuaryInternal(sanctuaries: List<Int>): Int {
        if (sanctuaries.size == 1)
            return 0

        val scores = sanctuaries.map {
            evaluatePlayer(
                CustomPlayer(
                    hand = emptyList(),
                    journey = player.journey,
                    sanctuaries = player.sanctuaries + it
                )
            )
        }

        return scores.indexOf(scores.max())
    }

    override fun chooseRegionInternal(revealed: List<Int>): Int {
        runSimulations(
            player.hand,
            revealed,
            9.0.seconds
        )

        val node = checkNotNull(currentNode)

        val visits = revealed.map {
            checkNotNull(node.children[it]).visits
        }

        println("### MCTSMaxPointsBot.chooseRegion $visits")
        val bestCardIdx = visits.indexOf(visits.max())

        currentNode = checkNotNull(node.children[revealed[bestCardIdx]])

        return bestCardIdx
    }

    override fun chooseInitialCardsInternal(cards: List<Int>): List<Int> {
        val combs = mutableListOf<List<Int>>()
        val avgPoints = mutableListOf<Double>()
        val sanctuaries = (N_REG until N_CARDS).toList()

        val n = 1000

        for (comb in cards.combinations(3)) {
            combs.add(comb)

            val remainingCards = (0 until N_REG)
                .toList()
                .filter { !(comb.contains(it)) }

            var totalPoints = 0.0

            repeat(n) {
                val journey = (
                        comb + remainingCards.shuffled().take(5)
                        ).shuffled()

                val sanctCount = journey
                    .windowed(2)
                    .count { (prev, curr) -> curr > prev }

                val randomSanctuaries = sanctuaries
                    .shuffled().take(sanctCount)

                val points = evaluatePlayer(
                    CustomPlayer(
                        listOf(),
                        journey,
                        randomSanctuaries
                    )
                )

                totalPoints += points
            }

            avgPoints.add(totalPoints / n)
        }

        println("### MCTSMaxPointsBot.chooseRegion $avgPoints")

        val maxPointsIdx = avgPoints.indexOf(avgPoints.max())
        val bestComb = combs[maxPointsIdx]

        return bestComb.map { cards.indexOf(it) }
    }


    private enum class Phase { EXPLORE, CHOOSE }
}