package service.bot

import entity.GameState

/**
 * Bot that returns the best card at the current game state.
 */
class GreedyBot(name: String, gameState: GameState) : Bot(name, gameState) {
    override fun chooseInitialCardsInternal(cards: List<Int>): List<Int> {
        return (0 until cards.size).shuffled().take(3)
    }

    override fun exploreRegionInternal(hand: List<Int>): Int {
        val scores = hand.map {
            evaluatePlayer(
                CustomPlayer(
                    hand = emptyList(),
                    journey = player.journey + it,
                    sanctuaries = player.sanctuaries
                )
            )
        }

        return scores.indexOf(scores.max())
    }

    override fun chooseSanctuaryInternal(sanctuaries: List<Int>): Int {
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
        val scores = revealed.map {
            evaluatePlayer(
                CustomPlayer(
                    hand = emptyList(),
                    journey = player.journey + it,
                    sanctuaries = player.sanctuaries
                )
            )
        }

        return scores.indexOf(scores.max())
    }
}