package service

import entity.Player
import entity.RegionCard
import entity.SanctuaryCard
import service.network.ConnectionState

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the GUI classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * GUI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 */
interface Refreshable {

    /**
     * Performs refreshes that are necessary after the game has started.
     */
    fun refreshAfterGameStart() {}

    /**
     * Performs refreshes that are necessary after the game has ended.
     */
    fun refreshAfterGameEnd(winner: Player) {}

    /**
     * Performs refreshes that are necessary after a turn has started.
     */
    fun refreshAfterTurnStart() {}

    /**
     * Performs refreshes that are necessary after a turn has ended.
     */
    fun refreshAfterTurnEnd() {}

    /**
     * Performs refreshes that are necessary after a round has started.
     */
    fun refreshAfterRoundStart() {}

    /**
     * Performs refreshes that are necessary after a round has ended.
     */
    fun refreshAfterRoundEnd() {}

    /**
     * Performs refreshes that are necessary before a region card is played.
     */
    fun refreshBeforePlayRegionCard() {}

    /**
     * Performs refreshes that are necessary after the specified [RegionCard] has been played.
     */
    fun refreshAfterPlayRegionCard(card: RegionCard) {}

    /**
     * Performs refreshes that are necessary before a sanctuary card is chosen.
     */
    fun refreshBeforeChooseSanctuaryCard() {}

    /**
     * Performs refreshes that are necessary after the specified sanctuary [SanctuaryCard] has been chosen.
     */
    fun refreshAfterChooseSanctuaryCard(card: SanctuaryCard) {}

    /**
     * Performs refreshes that are necessary before the starting hand is chosen.
     */
    fun refreshBeforeChooseStartingHand() {}

    /**
     * Performs refreshes that are necessary after the starting hand has been chosen.
     */
    fun refreshAfterChooseStartingHand() {}

    /**
     * Performs refreshes that are necessary before a region card is chosen.
     */
    fun refreshBeforeChooseRegionCard() {}

    /**
     * Performs refreshes that are necessary after the specified [RegionCard] has been chosen.
     */
    fun refreshAfterChooseRegionCard(card: RegionCard) {}

    /**
     * Performs refreshes that are necessary after an action has been redone / undone.
     */
    fun refreshAfterChange() {}

    /**
     * Performs refreshes that are necessary after the game state has been saved.
     */
    fun refreshAfterSave() {}

    /**
     * Performs refreshes that are necessary after a game state has been loaded.
     */
    fun refreshAfterLoad() {}



    // --- Network Refreshables ---

    /**
     * Performs refreshes that are necessary after the [ConnectionState] has changed.
     */
    fun refreshAfterConnectionStateChange(newState: ConnectionState) {}

    /**
     * Performs refreshes that are necessary after the lobby has been updated.
     */
    fun refreshAfterLobbyUpdated() {}
}