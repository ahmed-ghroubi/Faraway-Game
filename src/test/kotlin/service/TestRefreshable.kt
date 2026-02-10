package service

import entity.*

/**
 * [Refreshable] implementation that refreshes nothing, but remembers
 * if a refresh method has been called (since last [reset])
 *
 * @constructor Creates a new [TestRefreshable] with the given [rootService]
 *
 * @param rootService The root service to which this service belongs
 */
class TestRefreshable(val rootService: RootService) : Refreshable {

    var refreshAfterGameStartCalled: Boolean = false
        private set

    var refreshAfterGameEndCalled: Boolean = false
        private set

    var refreshAfterTurnStartCalled: Boolean = false
        private set

    var refreshAfterTurnEndCalled: Boolean = false
        private set

    var refreshAfterRoundStartCalled: Boolean = false
        private set

    var refreshAfterRoundEndCalled: Boolean = false
        private set

    var refreshBeforePlayRegionCardCalled: Boolean = false
        private set

    var refreshAfterPlayRegionCardCalled: Boolean = false
        private set

    var refreshBeforeChooseRegionCardCalled: Boolean = false
        private set

    var refreshAfterChooseRegionCardCalled: Boolean = false
        private set

    var refreshBeforeChooseSanctuaryCardCalled: Boolean = false
        private set

    var refreshAfterChooseSanctuaryCardCalled: Boolean = false
        private set

    var refreshBeforeChooseStartingHandCalled: Boolean = false
        private set

    var refreshAfterChooseStartingHandCalled: Boolean = false
        private set

    var refreshAfterChangeCalled: Boolean = false
        private set

    var refreshAfterSaveCalled: Boolean = false
        private set

    var refreshAfterLoadCalled: Boolean = false
        private set

    var receivedTemporarySanctuaries: List<SanctuaryCard>? = null
        private set



    /**
     * Resets all called properties to false.
     */
    fun reset() {
        refreshAfterGameStartCalled = false
        refreshAfterGameEndCalled = false
        refreshAfterTurnStartCalled = false
        refreshAfterTurnEndCalled = false
        refreshAfterRoundStartCalled = false
        refreshAfterRoundEndCalled = false

        refreshBeforePlayRegionCardCalled = false
        refreshAfterPlayRegionCardCalled = false

        refreshBeforeChooseRegionCardCalled = false
        refreshAfterChooseRegionCardCalled = false

        refreshBeforeChooseSanctuaryCardCalled = false
        refreshAfterChooseSanctuaryCardCalled = false

        refreshBeforeChooseStartingHandCalled = false
        refreshAfterChooseStartingHandCalled = false

        receivedTemporarySanctuaries = null

        refreshAfterChangeCalled = false
        refreshAfterSaveCalled = false
        refreshAfterLoadCalled = false
    }

    override fun refreshAfterGameStart() {
        refreshAfterGameStartCalled = true
    }

    override fun refreshAfterGameEnd(winner: Player) {
        refreshAfterGameEndCalled = true
    }

    override fun refreshAfterTurnStart() {
        refreshAfterTurnStartCalled = true
    }

    override fun refreshAfterTurnEnd() {
        refreshAfterTurnEndCalled = true
    }

    override fun refreshAfterRoundStart() {
        refreshAfterRoundStartCalled = true
    }

    override fun refreshAfterRoundEnd() {
        refreshAfterRoundEndCalled = true
    }

    override fun refreshBeforePlayRegionCard() {
        refreshBeforePlayRegionCardCalled = true
    }

    override fun refreshBeforeChooseStartingHand() {
        refreshBeforeChooseStartingHandCalled = true
    }

    override fun refreshAfterChooseStartingHand() {
        refreshAfterChooseStartingHandCalled = true
    }

    override fun refreshBeforeChooseSanctuaryCard() {
        refreshBeforeChooseSanctuaryCardCalled = true
    }

    override fun refreshBeforeChooseRegionCard() {
        refreshBeforeChooseRegionCardCalled = true
    }

    override fun refreshAfterChooseRegionCard(card: RegionCard) {
        refreshAfterChooseRegionCardCalled = true
    }

    override fun refreshAfterChange() {
        refreshAfterChangeCalled = true
    }

    override fun refreshAfterSave() {
        refreshAfterSaveCalled = true
    }

    override fun refreshAfterLoad() {
        refreshAfterLoadCalled = true
    }
}