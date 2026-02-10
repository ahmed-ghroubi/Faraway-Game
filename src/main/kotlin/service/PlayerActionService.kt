package service

import entity.FarawayGame
import entity.PlayerType
import entity.RegionCard
import entity.SanctuaryCard

/**
 * Implements the logic responsible for the player actions.
 *
 * @param rootService The relation to the rootService
 */
class PlayerActionService(private val rootService: RootService) :
    AbstractRefreshingService() {

    // Tracks which players have already completed Phase 2 in the final round
    private val lastRoundFinishedPlayers = mutableSetOf<Int>()

    /**
     * Continues the game flow after a game started.
     */
    fun proceedAfterGameStart() {
        val game = rootService.currentGame
        val gameState = game?.currentGameState

        checkNotNull(gameState)

        if (game.isOnline && !game.isSimpleVariant) {
            gameState.currentPlayer = gameState.players.indexOfFirst { it.playerType != PlayerType.REMOTE }
            require(gameState.currentPlayer != -1) { "No local player found in online game." }
        }

        val player = gameState.players[gameState.currentPlayer]
        if (player.hand.size > 3) {
            onAllRefreshables { refreshBeforeChooseStartingHand() }
            when (player.playerType) {
                PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                    val bot = checkNotNull(game.bots[player.name])
                    val startingHand = bot.chooseInitialCards(player.hand)
                    chooseStartingHand(startingHand)
                }
                else -> {}
            }


        } else {
            onAllRefreshables { refreshBeforePlayRegionCard() }
            when (player.playerType) {
                PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                    val bot = checkNotNull(game.bots[player.name])
                    val chosenCard = bot.exploreRegion(player.hand)
                    playRegionCard(chosenCard)
                }
                else -> {}
            }


        }
    }

    /**
     * Continues the game flow after a region card has been played.
     *
     * This method advances the current player and checks whether all players
     * have selected a region card. If so, the game transitions to Phase 2 and
     * the player order is updated. Otherwise, the next player continues the
     * current phase.
     *
     * The player who starts the current phase also serves as the starting
     * reference for the next phase.
     *
     * @throws IllegalStateException if no active game is available
     */
    fun proceedAfterPlayRegionCard() {
        val game = checkNotNull(rootService.currentGame)
        val state = game.currentGameState

        // save information about the played card
        val oldPlayer = state.players[state.currentPlayer]
        val playedRegionCard = oldPlayer.selectedCard
        checkNotNull(playedRegionCard) { "No region card was played by the current player." }

        // Always advance to the next player
        state.currentPlayer =
            (state.currentPlayer + 1) % state.players.size

        // Check if all players have played a region card
        val allSelected = state.players.all { it.selectedCard != null }
        state.isPhaseTwo = allSelected

        // Reset tracking when Phase 2 of the final round starts
        if (state.currentRound == 8 && state.isPhaseTwo) {
            lastRoundFinishedPlayers.clear()
        }


        if (allSelected) {
            state.players.forEach {
                it.hand.remove(it.selectedCard)
                it.regionCards += checkNotNull(it.selectedCard)
                it.selectedCard = null
            }
        }

        if (state.isPhaseTwo) {
            // Transition to Phase 2
            rootService.gameService.updatePlayerOrder()

            if (game.isOnline) {
                if (oldPlayer.playerType != PlayerType.REMOTE) {
                    rootService.networkService.sendSelectedRegionCardMessage(playedRegionCard)
                }
                println("validateConnectionStateAfterGameMessage in proceedAfterPlayRegionCard - Phase Two")
                rootService.networkService.validateConnectionStateAfterGameMessage(game)
            }
            // Start the turn for the first player in Phase 2
            startPhaseTwoTurn()

        } else if (!allSelected) {
            if (game.isOnline) {
                if (oldPlayer.playerType != PlayerType.REMOTE) {
                    rootService.networkService.sendSelectedRegionCardMessage(playedRegionCard)
                }
                println("validateConnectionStateAfterGameMessage in proceedAfterPlayRegionCard - Phase One")
                rootService.networkService.validateConnectionStateAfterGameMessage(game)
            }

            // Continue with the next player's turn

            onAllRefreshables { refreshBeforePlayRegionCard() }

            val player = state.players[state.currentPlayer]

            when (state.players[state.currentPlayer].playerType) {
                PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                    val bot = checkNotNull(game.bots[player.name])
                    val chosenCard = bot.exploreRegion(player.hand)
                    playRegionCard(chosenCard)
                }
                else -> {}
            }


        } else {
            onAllRefreshables {
                refreshAfterTurnEnd()
            }
            endTurn()
        }
        recordState(game)
    }

    /**
     * Advances the game flow after the current player has chosen a region card
     *
     * This method checks if the player is eligible to receive Sanctuary Cards based on the
     * played cards (via [collectTemporarySanctuariesIfAllowed]).
     *
     * Sanctuaries available: Triggers the selection process. Bots select immediately,
     * whereas human players require a user interaction.
     * No Sanctuaries: Bypasses the selection phase and automatically proceeds to
     * [proceedAfterChooseSanctuaryCard], sending a network update if required.
     *
     * @throws IllegalStateException If there is no active game, player index is invalid,
     * or a required bot instance is missing.
     */
    /**
     * Advances the game flow after the current player has chosen a region card
     * * This method marks the END of a player's turn in Phase 2
     * * It checks if all players have finished their Phase 2 turn (hand filled back to 3).
     * If not, it initiates the Phase 2 turn for the next player (starting with Sanctuary check).
     *
     * @throws IllegalStateException If there is no active game, player index is invalid or
     * a required bot instance is missing.
     */
    fun proceedAfterChooseRegionCard() {
        val game = rootService.currentGame
        checkNotNull(game) { "there is no game currently active." }

        val state = game.currentGameState

        if (game.isOnline && state.players[state.currentPlayer].playerType != PlayerType.REMOTE) {
            rootService.networkService.sendPhaseTwoMessage()
        }

        // Always advance to the next player (End of Turn for current player)
        state.currentPlayer = (state.currentPlayer + 1) % state.players.size

        if (game.isOnline) {
            println("validateConnectionStateAfterGameMessage in proceedAfterChooseRegionCard")
            rootService.networkService.validateConnectionStateAfterGameMessage(game)
        }
        // In round 8, Phase 2 skips ChooseRegionCard
        if (state.currentRound == 8) {
            startPhaseTwoTurn()
            return
        }

        // Check if all players have finished Phase 2 (hand size back to 3 means they picked a region card)
        val allPlayersChooseRegionCard = state.players.all { it.hand.size == 3 }

        if (!allPlayersChooseRegionCard && state.currentRound < 8) {
            // Continue Phase 2 with the next player
            startPhaseTwoTurn()
        } else {
            // All players finished Phase 2
            onAllRefreshables {
                refreshAfterTurnEnd()
            }
            endTurn()
        }
        recordState(game)
    }

    /**
     * Continues the game flow after a SanctuaryCard was chosen (or skipped).
     * * This method now transitions to the Region Card selection
     */
    fun proceedAfterChooseSanctuaryCard() {
        val game = checkNotNull(rootService.currentGame)
        val state = game.currentGameState

        // Each player resolves Sanctuary selection exactly once.
        // Turn order is controlled manually using lastRoundFinishedPlayers.
        if (state.currentRound == 8) {

            if (game.isOnline && state.players[state.currentPlayer].playerType != PlayerType.REMOTE) {
                rootService.networkService.sendPhaseTwoMessage()
            }

            lastRoundFinishedPlayers.add(state.currentPlayer)

            val nextPlayer = state.players.indices
                .firstOrNull { it !in lastRoundFinishedPlayers }

            if (nextPlayer != null) {
                state.currentPlayer = nextPlayer
                if(game.isOnline)rootService.networkService.validateConnectionStateAfterGameMessage(game)
                startPhaseTwoTurn()
            } else {
                onAllRefreshables { refreshAfterTurnEnd() }
                endTurn()
            }
            return
        }

        // Proceed to Region Card Selection
        onAllRefreshables { refreshBeforeChooseRegionCard() }

        val player = state.players[state.currentPlayer]
        when (player.playerType) {
            PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                val bot = checkNotNull(game.bots[player.name])
                val chosenCard = bot.chooseRegion(state.centerCards)
                chooseRegionCard(chosenCard)
            }
            else -> {}
        }
    }

    /**
     * Executes the logic for playing a RegionCard during a round.
     *
     * The current player selects exactly one RegionCard from their hand.
     * After the action, the UI is updated via { refreshAfterPlayRegionCard()}.
     *
     * Preconditions:
     * - An active game exists.
     * - The current player has exactly three cards in their hand.
     * - The player has not already played a RegionCard in this round.
     *
     * Post-conditions:
     * - The selected RegionCard is stored in the correct order in { regionCards}.
     * - { refreshAfterPlayRegionCard()} is called at the end of the method.
     *
     * @param card the RegionCard selected by the current player.
     * @throws IllegalStateException
     *         if the game is not active or the player has played more than one
     *         RegionCard.
     */
    fun playRegionCard(card: RegionCard) {

        val game = checkNotNull(rootService.currentGame) {
            "No active game available."
        }

        val state = game.currentGameState
        val player =
            state.players[state.currentPlayer] // currentPlayer is an index (Int)

        // Validate that the selected card is actually in the player's hand.
        require(card in player.hand) {
            "The selected region card is not in the player's hand."
        }

        // A player may choose only one region card per round.
        check(player.selectedCard == null) {
            "The player has already selected a region card for this round."
        }

        player.selectedCard = card

        // Notify GUI that a region card was chosen.
        onAllRefreshables { refreshAfterPlayRegionCard(card) }
    }

    /**
     * Allows the current player to take a RegionCard from the center display.
     *
     * The current player selects one RegionCard from the open center area (centerCards).
     * The chosen card is removed from the center and added to the player´s hand.
     * After the action, the UIis updated via `refreshAfterChooseRegionCard`.
     *
     * Preconditions:
     * - An active game exists.
     * - The selected RegionCard is contained in the game´s centerCards.
     *
     * Post-conditions:
     * - The selected RegionCard is removed from centerCards.
     * - The selected RegionCard is added to the hand of the current player.
     * - The UI is notified via `refreshAfterChooseRegionCard`.
     *
     * @param card The RegionCard the current player wants to take from the center.
     *
     * @throws IllegalStateException if no active game exists.
     * @throws IllegalArgumentException if the selected card is not contained in centerCards.
     */
    fun chooseRegionCard(card: RegionCard) {
        val game = rootService.currentGame
        checkNotNull(game) { "there is no game currently active." }

        val state = game.currentGameState
        check(state.currentPlayer < state.players.size) { "Current player index is invalid." }

        val currentPlayer = state.players[state.currentPlayer]

        check(currentPlayer.temporarySanctuaries.isEmpty()) {
            "Player must choose a sanctuary card first before picking a new region card."
        }

        require(card in state.centerCards) {
            "The chosen card is not in the middle."
        }

        //Remove the card from the center
        state.centerCards.remove(card)

        //Add the card to the current player´s hand
        currentPlayer.hand.add(card)


        // Continue with the normal flow after choosing a region card
        onAllRefreshables { refreshAfterChooseRegionCard(card) }
    }

    /**
     * Collects temporary sanctuary cards for the current player if the game rules allow it.
     *
     * A player may collect temporary sanctuaries only if:
     * - There is an active game.
     * - The player has played at least two region cards.
     * - The exploration time of the most recently played region card
     *   is strictly greater than the exploration time of the previously played one.
     *
     * If these conditions are met, the number of temporary sanctuaries drawn equals
     * the total number of clue symbols the player has on their region cards
     * and already collected sanctuaries.
     *
     * @return `true` if at least one temporary sanctuary was collected, `false` otherwise.
     */
    private fun collectTemporarySanctuariesIfAllowed(): Boolean {

        // Ensure there is an active game; otherwise fail fast
        val game = checkNotNull(rootService.currentGame) {
            "No active game available."
        }

        // Access the current game state and the active player
        val state = game.currentGameState
        val player = state.players[state.currentPlayer]

        if (player.playerType == PlayerType.REMOTE) {
            return false
        }

        // The player must have played at least two region cards
        val regions = player.regionCards
        if (regions.size < 2) {
            println("Regions.Size = ${regions.size}")
            return false
        }

        val previous = regions[regions.size - 2]
        val last = regions[regions.size - 1]

        // Rule check: exploration time must be strictly increasing
        if (last.explorationTime <= previous.explorationTime) {
            println("Exploration Time is lower than before!")
            return false
        }

        // Count all clue symbols from region cards and existing sanctuaries
        val clueCount =
            player.regionCards.count { it.clue } +
                    player.sanctuaries.count { it.clue }

        // Draw one temporary sanctuary per clue symbol
        repeat(clueCount) {
            player.temporarySanctuaries.add(
                state.sanctuaryDrawStack.removeFirst()
            )
        }
        // Einfach mal nen Fehler. Es werden nur Sanctuaries für Clues erstellt.
        player.temporarySanctuaries.add(state.sanctuaryDrawStack.removeFirst())

        // Return true if at least one temporary sanctuary was added
        return player.temporarySanctuaries.isNotEmpty()
    }

    /**
     * Allows the current player to choose one SanctuaryCard.
     *
     * The player selects exactly one SanctuaryCard from { temporarySanctuaries}.
     * The chosen card is kept permanently, while all remaining temporary sanctuary
     * cards are returned to the { sanctuaryDrawStack}.
     * After the selection, the UI is updated via {refreshAfterChooseSanctuary()}.
     *
     * Preconditions:
     * - An active game exists.
     * - { temporarySanctuaries} contains at least one SanctuaryCard.
     *
     * Post-conditions:
     * - Exactly one SanctuaryCard is added to the player's permanent sanctuaries.
     * - All remaining cards are returned to { sanctuaryDrawStack}.
     * - In the final round, the game ends after the sanctuary selection.
     * - { refreshAfterChooseSanctuary()} is called at the end of the method.
     *
     * @param card the SanctuaryCard selected by the current player.
     * @throws IllegalStateException
     *  if the game is not active or no sanctuary cards are available to choose.
     */
    fun chooseSanctuaryCard(card: SanctuaryCard) {
        val game = checkNotNull(rootService.currentGame) {
            "No active game available."
        }

        val state = game.currentGameState
        val player =
            state.players[state.currentPlayer] // currentPlayer is an index (Int)

        // The chosen card must be one of the temporarily available sanctuary cards.
        require(card in player.temporarySanctuaries) {
            "The selected sanctuary card is not in temporarySanctuaries."
        }


        // Keep the chosen card permanently.
        player.temporarySanctuaries.remove(card)
        player.sanctuaries.add(card)

        // Return all unchosen temporary sanctuary cards back to the draw stack.
        val sanctuaryStack = state.sanctuaryDrawStack
        sanctuaryStack.addAll(player.temporarySanctuaries)

        // Clear the temporary list after returning the remaining cards.
        player.temporarySanctuaries.clear()

        if (player.playerType != PlayerType.REMOTE) {
            state.sendSanctuaryCard = true
            // Notify GUI that a sanctuary card was chosen but only for local players.
            onAllRefreshables { refreshAfterChooseSanctuaryCard(card) }
        }
    }

    /**
     * Finishes the current player's turn and passes control to the next player.
     *
     * **Preconditions:**
     * - Game is active.
     * - Region card played & refill card drawn.
     * - Pending sanctuary selections resolved.
     *
     * **Post-conditions:**
     * - `currentPlayer` updated.
     * - Calls `nextRound()` if the round is complete.
     * - Calls `refreshAfterTurnEnd()`.
     *
     * @throws IllegalStateException If actions are missing (e.g., no card played/drawn) or game is inactive.
     */
    fun endTurn() {
        val game =
            checkNotNull(rootService.currentGame) { "No active game found." }
        val gameState = game.currentGameState

        // Preconditions
        check(hasPlayedCard()) {
            "Cannot end turn: The current player must play a region card first."
        }
        check(!hasSanctuarySelectionPending()) {
            "Cannot end turn: The current player must select a sanctuary card."
        }
        check(hasDrawnCard()) {
            "Cannot end turn: The current player must draw a region card from the center."
        }

        // Logic

        val playerCount = gameState.players.size
        gameState.currentPlayer = (gameState.currentPlayer + 1) % playerCount

        if (game.isOnline) {
            println("validateConnectionStateAfterGameMessage in endTurn")
            rootService.networkService.validateConnectionStateAfterGameMessage(game)
        }

        // Round ends only if every player has the exact same amount of played region cards.
        val firstPlayerCardCount = gameState.players.first().regionCards.size
        val roundEnded =
            gameState.players.all { it.regionCards.size == firstPlayerCardCount }

        if (!roundEnded) {
            onAllRefreshables { refreshAfterTurnEnd() }
            return
        }


        // Checks whether game ends
        if (gameState.currentRound >= 8) {
            rootService.gameService.endGame()
        } else {
            rootService.gameService.nextRound()
        }
    }


    // --- Helper Functions ---

    /**
     * Checks whether the current player has already placed a region card
     * in their tableau in this round.
     *
     * Logic: At the end of round X, the player must have exactly X cards in their tableau.
     * If `regionCards.size == currentRound`, the card has already been placed for this round.
     */
    private fun hasPlayedCard(): Boolean {
        val game =
            checkNotNull(rootService.currentGame) { "No active game found." }

        val currentState = game.currentGameState
        val player = currentState.players[currentState.currentPlayer]

        return player.regionCards.size == currentState.currentRound
    }

    /**
     * Checks whether the player has drawn Sanctuary cards from which they still have to choose.
     *
     * Logic: The `temporarySanctuaries` list is temporarily filled when a sanctuary is obtained.
     * As long as this list is not empty, the player must make a decision.
     */
    private fun hasSanctuarySelectionPending(): Boolean {
        val game =
            checkNotNull(rootService.currentGame) { "No active game found." }

        val currentState = game.currentGameState
        val player = currentState.players[currentState.currentPlayer]

        return player.temporarySanctuaries.isNotEmpty()
    }

    /**
     * Checks whether the player has already drawn a card (refill).
     *
     * Logic:
     *  1. The player must have played a card first.
     *  2. The hand must be refilled to its full size.
     */
    private fun hasDrawnCard(): Boolean {
        val game =
            checkNotNull(rootService.currentGame) { "No active game found." }

        val currentState = game.currentGameState
        val player = currentState.players[currentState.currentPlayer]

        // Constant for hand size because of the game rules
        val handSize = 3

        // You have only finished drawing when you have already played AND your hand is full again.
        return hasPlayedCard() && (player.hand.size == handSize ||
                // The player doesn't need a new card in the last round
                currentState.currentRound >= 8)
    }

    /**
     * Saves a deep-copy snapshot of the current state for undo/redo.
     * If we are not at the end of history (after an undo), the stored "future" is cleared first.
     */
    private fun recordState(game: FarawayGame) {
        // if we undid before, delete the future
        if (game.gameHistoryIndex < game.gameHistory.lastIndex) {
            game.gameHistory.subList(
                game.gameHistoryIndex + 1,
                game.gameHistory.size
            ).clear()
        }
        // add deep snapshot
        game.gameHistory.add(game.currentGameState.copy())
        game.gameHistoryIndex = game.gameHistory.lastIndex
    }

    /**
     * Helper method to handle the start of a player's turn in Phase 2.
     * 1. Check if Sanctuary is allowed.
     * 2. If yes: Let player/bot choose Sanctuary.
     * 3. If no: Skip directly to Region selection (proceedAfterChooseSanctuaryCard).
     */
    private fun startPhaseTwoTurn() {
        val game = checkNotNull(rootService.currentGame)
        val state = game.currentGameState
        val player = state.players[state.currentPlayer]

        val mustChooseSanctuary = collectTemporarySanctuariesIfAllowed()

        if (mustChooseSanctuary) {
            onAllRefreshables { refreshBeforeChooseSanctuaryCard() }

            when (player.playerType) {
                PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                    val bot = checkNotNull(game.bots[player.name])
                    val chosenCard = bot.chooseSanctuary(
                        player.temporarySanctuaries
                    )
                    chooseSanctuaryCard(chosenCard)
                    // Proceed to next step (Region Choice)
                }
                else -> {}
            }
        } else {
            if (state.currentRound == 8 && player.playerType == PlayerType.REMOTE) {
                return
            }
            proceedAfterChooseSanctuaryCard()
        }
    }

    /**
     * [chooseStartingHand]Lets the current player choose their starting hand.
     *
     * In the advanced game variant, the player selects exactly 3 cards out of their 5-card hand.
     * The remaining 2 cards are returned to the region drawStack.
     */
    fun chooseStartingHand(cards: List<RegionCard>) {
        val game =
            checkNotNull(rootService.currentGame) { "No active game found." }
        val gameState = game.currentGameState
        var player = gameState.players[gameState.currentPlayer]

        if (game.isOnline) {
            player = gameState.players.first { it.playerType != PlayerType.REMOTE }
        }

        // Check Game variant
        check(!game.isSimpleVariant)
        { "chooseStartingHand is only allowed in advanced variant." }

        //Hand size check
        if (player.hand.size != 5) {
            throw IllegalStateException("Player must have 5 cards to choose from.")
        }

        // Chosen Hand check
        require(cards.size == 3)
        { "Player must choose exactly 3 cards." }

        val discardedCards = player.hand.filter { !cards.contains(it) }

        // Add chosen cards in Hand
        player.hand.retainAll(cards)

        // Return discarded to draw stack
        check(gameState.regionDrawStack.addAll(discardedCards))
        { "Draw stack refused the discarded cards." }

        if (game.isOnline && player.playerType != PlayerType.REMOTE) {
            val combined = player.hand + discardedCards
            rootService.networkService.sendSelectedStartingHandMessage(combined)
        }

        onAllRefreshables { refreshAfterChooseStartingHand() }
    }

}