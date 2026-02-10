package service.bot

import entity.GameState
import kotlin.random.Random

private const val WAITTIME = 3_000L  // milliseconds

/**
 * Bot that returns a random card each time
 */
class RandomBot(name: String, gameState: GameState) : Bot(name, gameState) {
    override fun chooseInitialCardsInternal(cards: List<Int>): List<Int> {
        Thread.sleep(WAITTIME)
        return (0 until cards.size).shuffled().take(3)
    }

    override fun exploreRegionInternal(hand: List<Int>): Int {
        Thread.sleep(WAITTIME)
        return Random.nextInt(hand.size)
    }

    override fun chooseSanctuaryInternal(sanctuaries: List<Int>): Int {
        Thread.sleep(WAITTIME)
        return Random.nextInt(sanctuaries.size)
    }

    override fun chooseRegionInternal(revealed: List<Int>): Int {
        Thread.sleep(WAITTIME)
        return Random.nextInt(revealed.size)
    }
}