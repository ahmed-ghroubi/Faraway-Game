package service

import entity.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import service.bot.MCTSMaxPointsBot
import service.bot.RandomBot
import java.io.InputStream

/**
 * Implements the logic responsible for the game.
 *
 * @param rootService The relation to the rootService
 */
class GameService(private val rootService: RootService) :
    AbstractRefreshingService() {

    /**
     * Starts a new game and initializes the complete game state based on the selected game variant,
     * game mode, player order and the given players.
     *
     * Depending on the parameters, the game is started either in simple or advanced mode, online or offline
     * and with a random or predefined order of players. The initial playfield is set ub by calling
     * [initializePlayfield] and the UI is informed by calling `refreshAfterGameStart`.
     *
     * @param isSimple
     * @param isOnline
     * @param randomOrder
     * @param players
     *
     * @throws IllegalStateException if there is already an active game running.
     * @throws IllegalArgumentException if the number of players is less than 2 or greater than 6.
     */
    fun createGame(
        isSimple: Boolean,
        isOnline: Boolean,
        randomOrder: Boolean,
        players: List<Player>
    ) {

        if (rootService.currentGame != null) {
            throw IllegalStateException("There is already an active game.")
        }

        //ensure the number of players is valid (2-6)
        require(players.size in 2..6) { "A game requires 2 to 6 players." }

        //check for empty names
        require(players.none { it.name.isBlank() }) { "Players must have a name." }

        //check for duplicate names
        val playerNames = players.map { it.name }
        require(playerNames.toSet().size == playerNames.size) {
            "Player names must be unique."
        }

        //check for invalid characters
        require(players.all { player ->
            player.name.all { char -> char.isLetterOrDigit() }
        }) { "Player names must only contain letters or digits." }

        //set player order
        val playerOrder = if (randomOrder) {
            players.shuffled().toMutableList()
        } else {
            players.toMutableList()
        }

        //create game and initialize game state
        val game = FarawayGame(isOnline = isOnline, isSimpleVariant = isSimple)
        val initialGameState = GameState(players = playerOrder)

        initialGameState.currentPlayer = 0
        initialGameState.currentRound = 1

        //set current game
        game.currentGameState = initialGameState


        players.forEach {
            when (it.playerType) {
                PlayerType.BOT_EASY -> game.bots[it.name] = (
                        RandomBot(
                            it.name, initialGameState
                        )
                        )

                PlayerType.BOT_HARD -> game.bots[it.name] = (
                        MCTSMaxPointsBot(
                            it.name, initialGameState
                        ))

                else -> {}
            }
        }

        rootService.currentGame = game

        //initializes playfield
        initializePlayfield()

        game.gameHistory.clear()
        game.gameHistory.add(game.currentGameState.copy())
        game.gameHistoryIndex = 0

        if (!game.isOnline) {
            onAllRefreshables { refreshAfterGameStart() }
        }

    }

    /**
     * Calculates and assigns the final scores for all players in the current game.
     *
     * Scoring follows the official Faraway end-scoring rules:
     * - Each player must have exactly 8 RegionCards.
     * - Cards become visible from right to left during scoring
     *   (the last played RegionCard is revealed first).
     * - Sanctuary cards are considered visible from the beginning.
     * - Sanctuary quests are evaluated only after all RegionCards
     *   have been revealed and evaluated.
     *
     * The score of each player is stored in an IntArray with fixed indices:
     * - index 0..7 : points for the 8 RegionCards
     * - index 8    : total points from all Sanctuary cards
     * - index 9    : total score (sum of indices 0..8)
     *
     * Preconditions:
     * - An active game exists.
     * - Every player owns exactly 8 RegionCards.
     *
     * Post-conditions:
     * - The score array is fully initialized for every player.
     *
     * @throws IllegalStateException
     *         if the game state is invalid (no active game or invalid number of RegionCards).
     */
    private fun calculateAllScores() {
        val game = checkNotNull(rootService.currentGame) {
            "No active game available."
        }

        val players = game.currentGameState.players

        for (player in players) {

            // Each player must have exactly 8 RegionCards at game end
            check(player.regionCards.size == 8) {
                "Player ${player.name} does not have exactly 8 RegionCards."
            }


            // Creates a new array with 10 zeros
            player.score = IntArray(10)

            // Cards that are currently visible during scoring
            val visibleCards = mutableListOf<Card>()

            // Sanctuary cards are visible from the beginning
            for (s in player.sanctuaries) {
                visibleCards.add(s)
            }


            // Evaluate RegionCards from right to left
            var scoreIndex = 0
            val regions = player.regionCards
            var regionScoreIndex = 7
            while (regionScoreIndex >= 0) {
                val region = regions[regionScoreIndex]

                // Reveal the region: it becomes visible for subsequent evaluations.
                visibleCards.add(region)

                // Evaluate quest points for this region
                val points = evaluateQuest(region, visibleCards)

                // Store points in score[0..7]
                player.score[scoreIndex] = points

                scoreIndex++
                regionScoreIndex--
            }

            // Evaluate Sanctuary cards after all regions are visible
            var sanctuarySum = 0
            for (sanctuariesCard in player.sanctuaries) {
                sanctuarySum += evaluateQuest(sanctuariesCard, visibleCards)
            }
            player.score[8] = sanctuarySum


            // Calculate total score (sum of indices 0..8)
            var totalScore = 0

            for (scorePosition in 0..8) {
                totalScore += player.score[scorePosition]
            }

            player.score[9] = totalScore

        }
    }

    /**
     * Evaluates the quest of a single card and returns the points earned from it.
     *
     * The evaluation is driven by the JSON-compatible quest fields:
     * - If the card is a RegionCard, its prerequisites must be satisfied; otherwise it yields 0 points.
     * - If the quest contains no types (no night, no clue, no wonders, no biome),
     *   the card awards a fixed amount of fame.
     * - Otherwise exactly one quest type is evaluated in the following priority order:
     *   1) clue
     *   2) night
     *   3) wonders (exactly one wonder type is expected)
     *   4) biome (either one/two colors, or a complete set of four colors)
     *
     * Points are generally computed as { `matches * quest.fame`}.
     *
     * @param card the card whose quest is being evaluated.
     * @param visible the list of cards currently visible at evaluation time.
     * @return the number of points gained from this card's quest.
     */
    private fun evaluateQuest(card: Card, visible: List<Card>): Int {
        val quest = card.quest

        // Prerequisites apply only to region cards
        if (card is RegionCard && !checkPrerequisites(
                card.prerequisites, visible
            )
        ) {
            return 0
        }


        return when {
            // No quest types set -> fixed points (fame)
            !quest.night && !quest.clue && quest.wonders.isEmpty() && quest.biome.isEmpty() -> {
                quest.fame
            }

            // Clue quest
            quest.clue -> {
                countClue(visible) * quest.fame
            }


            // Night quest
            quest.night -> {
                countNight(visible) * quest.fame
            }

            // Wonders quest (exactly one wonder expected)
            quest.wonders.isNotEmpty() -> {
                val targetWonder = quest.wonders[0]
                countWonder(targetWonder, visible) * quest.fame
            }

            // Biome quest – full set vs partial set
            else -> {
                val matches = if (isBiomeAllSet(quest.biome)) {
                    countAllBiomeSets(visible)
                } else {
                    quest.biome.sumOf { biome ->
                        countBiome(biome, visible)
                    }
                }
                matches * quest.fame
            }
        }

    }

    /**
     * Checks whether the prerequisites for a region card are met based on currently visible cards.
     *
     * This method counts how many of each
     * wonder type (MINERAL/ANIMAL/PLANT) are required and compares them with the amount available
     * in the visible cards.
     *
     * @param needed list of required wonder symbols.
     * @param visible cards currently visible (their wonder symbols count toward satisfying prerequisites).
     * @return true if all required wonder counts are met; false otherwise.
     */
    private fun checkPrerequisites(
        needed: List<Wonder>, visible: List<Card>
    ): Boolean {
        var needMineral = 0
        var needAnimal = 0
        var needPlant = 0

        // Count required wonder symbols.
        for (wonder in needed) {
            when (wonder) {
                Wonder.MINERAL -> needMineral++
                Wonder.ANIMAL -> needAnimal++
                Wonder.PLANT -> needPlant++
            }
        }

        var haveMineral = 0
        var haveAnimal = 0
        var havePlant = 0

        // Count available wonder symbols among visible cards.
        for (card in visible) {
            for (wonder in card.wonders) {
                when (wonder) {
                    Wonder.MINERAL -> haveMineral++
                    Wonder.ANIMAL -> haveAnimal++
                    Wonder.PLANT -> havePlant++
                }
            }
        }

        return haveMineral >= needMineral && haveAnimal >= needAnimal && havePlant >= needPlant
    }

    /**
     * Determines whether a biome quest represents a complete set of all four colors:
     * BLUE + GREEN + YELLOW + RED.
     *
     * If the list does not contain exactly four entries or contains NONE/invalid values,
     * it is not considered a complete set.
     *
     * @param biomes list of biome targets from the quest.
     * @return true if the list represents a complete four-color set; false otherwise.
     */
    private fun isBiomeAllSet(biomes: List<Biome>): Boolean {
        if (biomes.size != 4) return false

        var hasBlue = false
        var hasGreen = false
        var hasYellow = false
        var hasRed = false

        for (biome in biomes) {
            when (biome) {
                Biome.BLUE -> hasBlue = true
                Biome.GREEN -> hasGreen = true
                Biome.YELLOW -> hasYellow = true
                Biome.RED -> hasRed = true
                else -> return false
            }
        }

        return hasBlue && hasGreen && hasYellow && hasRed
    }

    /**
     * Counts how many occurrences of a specific wonder symbol exist among all visible cards.
     *
     * @param target the wonder symbol to count.
     * @param visible the currently visible cards.
     * @return the number of occurrences of { target}.
     */
    private fun countWonder(target: Wonder, visible: List<Card>): Int {
        var count = 0
        for (card in visible) {
            for (wonder in card.wonders) {
                if (wonder == target) count++
            }
        }
        return count
    }

    /**
     * Counts how many cards of a specific biome exist among all visible cards.
     *
     * @param target the biome to count.
     * @param visible the currently visible cards.
     * @return the number of cards whose biome equals {target}.
     */
    private fun countBiome(target: Biome, visible: List<Card>): Int {
        var count = 0
        for (card in visible) {
            if (card.biome == target) count++
        }
        return count
    }

    /**
     * Counts the total number of clue symbols among all visible cards.
     *
     * @param visible the currently visible cards.
     * @return the number of cards that have {clue == true}.
     */
    private fun countClue(visible: List<Card>): Int {
        var count = 0
        for (card in visible) {
            if (card.clue) count++
        }
        return count
    }

    /**
     * Counts the total number of night symbols among all visible cards.
     *
     * @param visible the currently visible cards.
     * @return the number of cards that have {night == true}.
     */
    private fun countNight(visible: List<Card>): Int {
        var count = 0
        for (card in visible) {
            if (card.night) count++
        }
        return count
    }

    /**
     * Counts how many complete biome sets (BLUE + GREEN + YELLOW + RED) exist among visible cards.
     *
     * A "set" is formed by having at least one of each of the four colors. The number of complete sets
     * is therefore the minimum count across the four colors.
     *
     * @param visible the currently visible cards.
     * @return the number of complete four-color biome sets.
     */
    private fun countAllBiomeSets(visible: List<Card>): Int {
        var blue = 0
        var green = 0
        var yellow = 0
        var red = 0

        for (card in visible) {
            when (card.biome) {
                Biome.BLUE -> blue++
                Biome.GREEN -> green++
                Biome.YELLOW -> yellow++
                Biome.RED -> red++
                else -> {}
            }
        }

        var min = blue
        if (green < min) min = green
        if (yellow < min) min = yellow
        if (red < min) min = red

        return min
    }

    /**
     * Starts the next round of the current game.
     *
     * Preconditions (checked here):
     * - An active game exists.
     * - The previous round is fully finished: every player has exactly `currentRound` played RegionCards.
     * - The current round is not already the last one.
     *
     * Post-conditions:
     * - currentRound is incremented, currentPlayer is set to 0, phase is reset.
     * - temporary round information (selectedCard, temporary sanctuaries) is reset.
     * - centerCards is refilled with (players + 1) RegionCards from the regionDrawStack.
     * - refreshAfterRoundStart() is triggered.
     */
    fun nextRound() {
        val game = rootService.currentGame
        checkNotNull(game) {"No active game available."}
        val state = game.currentGameState

        // we have 8 rounds total (end scoring expects exactly 8 RegionCards per player)
        check(state.currentRound < 8){"No next round possible: last round already reached."}

        //to check that previous round was fully ended
        for (p in state.players) {
            if (p.regionCards.size != state.currentRound) {
                throw IllegalStateException(
                    "Previous round not finished: player ${p.name} has ${p.regionCards.size} region cards, " +
                            "expected ${state.currentRound}."
                )
            }
            //  reset round temporary data even it if it s done in endgame() for security
            p.selectedCard = null
            p.temporarySanctuaries.clear()
        }

        // Advance round + reset phase/current player
        state.currentRound += 1
        state.currentPlayer = 0
        state.isPhaseTwo = false

        // Prepare new center display with  (number of players + 1) region cards
        if (state.currentRound < 8) {
            val neededCenterCards = state.players.size + 1
            if (state.regionDrawStack.size < neededCenterCards) {
                throw IllegalStateException("Not enough RegionCards in draw stack to refill centerCards.")
            }

            state.centerCards.clear()

            repeat(neededCenterCards) {
                // draw from top of stack as much as neededCenterCards
                state.centerCards.add(state.regionDrawStack.removeLast())
            }
        }


        onAllRefreshables { refreshBeforePlayRegionCard() }
        val player = state.players[state.currentPlayer]
        when (player.playerType) {
            PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                val bot = checkNotNull(game.bots[player.name])
                val chosenCard = bot.exploreRegion(player.hand)
                rootService.playerActionService.playRegionCard(chosenCard)
            }
            else -> {}
        }

        if (game.isOnline) {
            println("validateConnectionStateAfterGameMessage in nextRound")
            rootService.networkService.validateConnectionStateAfterGameMessage(game)
        }

    }

    /**
     * Updates the player order for the next round.
     *
     * The turn order is determined based on the RegionCards chosen in the current round
     * and their exploration time.
     *
     * Preconditions:
     * - There is an active game.
     * - For every active player, `selectedCard` is set to a valid RegionCard for this round.
     *
     * Post-conditions:
     * - The players list is re-sorted according to exploration time (ascending).
     * - The current player index is set to the first player in the sorted list (`currentPlayer = 0`),
     *   so the new order can be used in the next round.
     *
     * @throws IllegalStateException if no game exists or not all players have an evaluable RegionCard.
     */
    fun updatePlayerOrder() {
        val currentGame = checkNotNull(rootService.currentGame) { "No active game available." }
        val currentGameState = currentGame.currentGameState

        check(currentGameState.players.all { it.regionCards.isNotEmpty() })
        { "Not all players have an evaluable region card" }

        // Sort players by exploration time
        currentGameState.players.sortBy { it.regionCards.last().explorationTime }

        // After sorting, the first player in the list should do next
        currentGameState.currentPlayer = 0


        onAllRefreshables { refreshAfterRoundEnd() }
    }

    /**
     * [refillCenterCards]Refills the center cards for the next round.
     *
     * This method may only be called when exactly one card remains in the center
     * and all players have finished the current round. The remaining center card
     * is moved to the bottom of the draw stack, then the center is refilled with
     * one card per player plus one additional card.
     */
    fun refillCenterCards() {

        val game = rootService.currentGame
        val gameState = game?.currentGameState ?: error("Game must not be null")

        // Check center cards
        check(gameState.centerCards.size == 1) { "Center must contain exactly 1 card to be refilled." }

        // Add remaining card to regionDrawStack
        gameState.centerCards.clear()

        // count cards number
        val cardsNeeded = gameState.players.size + 1

        //refill centerCards
        repeat(cardsNeeded) {
            gameState.centerCards.add(gameState.regionDrawStack.removeLast())
        }
        onAllRefreshables { refreshAfterRoundStart() }

    }

    /**
     * Initializes the playfield. This method creates the RegionDrawStack and
     * distributes the RegionCards held by each player and the n + 1 cards
     * (n is the number of players) that are initially placed in the center of the
     * playfield. Similarly, this method creates the SanctuaryDrawStack.
     *
     * Preconditions:
     * - There should be 68 RegionCards and 45 SanctuaryCards.
     * - Each card must exist at most once.
     *
     * Post-conditions:
     * - There should be n + 1 RegionCards in the center of the playfield.
     * - Each player should have 3 RegionCards in their hand.
     * - The remaining RegionCards should be put into RegionDrawStack.
     * - All SanctuaryCards should be put into SanctuaryDrawStack.
     *
     */

    private fun initializePlayfield() {

        val game =
            checkNotNull(rootService.currentGame) { "Game has not started yet." }
        val gameState = game.currentGameState


        val regionCards = loadRegionCards()
        val sanctuaryCards = loadSanctuaryCards()

        // Check Number of Region cards
        check(regionCards.size == 68) {
            "Invalid Region card count loaded: " + "${regionCards.size} (expected 68)"
        }

        // Check Number of Sanctuary cards
        check(sanctuaryCards.size == 45) {
            "Invalid Sanctuary card count loaded:" + " ${sanctuaryCards.size} (expected 45)"
        }


        // Clear and fill the region draw stack
        gameState.regionDrawStack.clear()
        gameState.regionDrawStack.addAll(regionCards)
        gameState.regionDrawStack.shuffle()

        // Clear and fill the sanctuary draw stack
        gameState.sanctuaryDrawStack.clear()
        gameState.sanctuaryDrawStack.addAll(sanctuaryCards)
        gameState.sanctuaryDrawStack.shuffle()

        // Clear the cards in the center of the playfield
        gameState.centerCards.clear()


        val cardsToDeal = if (game.isSimpleVariant) 3 else 5

        for (player in gameState.players) {
            player.hand.clear()
            repeat(cardsToDeal) {

                player.hand.add(gameState.regionDrawStack.removeFirst())
            }
        }

        // Fill center: number of players + 1
        repeat(gameState.players.size + 1) {
            gameState.centerCards.add(gameState.regionDrawStack.removeLast())
        }

        onAllRefreshables { refreshAfterRoundStart() }

    }


    /**
     * Helper method to read and parse the content of the JSON file into a list of JsonCard objects.
     * This method handles the file loading and basic deserialization using kotlinx.serialization.
     *
     * @return A list of deserialized JsonCard objects.
     */

    private fun getJsonCards(): List<JsonCard> {
        val resourceStream =
            object {}.javaClass.getResourceAsStream("/faraway_cards.json")
        val inputStream: InputStream = checkNotNull(resourceStream) {
            "Could not find faraway_cards.json resource."
        }
        val reader: java.io.InputStreamReader =
            java.io.InputStreamReader(inputStream)
        val jsonString = reader.use { it.readText() }
        return Json.decodeFromString(jsonString)
    }

    /**
     * Helper method that converts a biome string from the JSON to the corresponding Biome Enum.
     *
     * @param biomeStr The string representation of the biome ("red", "blue").
     * @return The corresponding Biome Enum value or `Biome.NONE` if not matched.
     */

    private fun convertBiome(biomeStr: String): Biome {
        val biome: Biome = when (biomeStr) {
            "red" -> Biome.RED
            "blue" -> Biome.BLUE
            "green" -> Biome.GREEN
            "yellow" -> Biome.YELLOW
            else -> Biome.NONE
        }
        return biome
    }

    /**
     * Helper method that converts the count of wonders from the JSON structure into a list of Wonder Enums.
     *
     * @param wonders The JsonWonders object containing counts for each wonder type.
     * @return A MutableList containing the corresponding number of Wonder Enum instances.
     */

    private fun convertWonders(wonders: JsonWonders): MutableList<Wonder> {
        val list = mutableListOf<Wonder>()

        repeat(wonders.mineral) { list.add(Wonder.MINERAL) }
        repeat(wonders.animal) { list.add(Wonder.ANIMAL) }
        repeat(wonders.plant) { list.add(Wonder.PLANT) }
        return list
    }

    /**
     * Helper method that converts a JsonQuest object to a domain Quest object.
     * It processes quest types strings and converts them into lists of Biomes and Wonders.
     *
     * @param jsonQuest The JsonQuest object to convert, can be null.
     * @return A Quest domain object. If jsonQuest is null, returns an empty Quest.
     */

    private fun convertQuest(jsonQuest: JsonQuest?): Quest {

        if (jsonQuest == null) {
            val emptyQuest = Quest(
                night = false,
                clue = false,
                fame = 0,
                wonders = emptyList(),
                biome = emptyList()
            )
            return emptyQuest
        }
        val questWonders: MutableList<Wonder> = mutableListOf()
        val questBiomes: MutableList<Biome> = mutableListOf()

        var questNight: Boolean
        questNight = false

        var questClue: Boolean
        questClue = false

//        if (jsonQuest.prerequisites != null) {
//            questWonders.addAll(convertWonders(jsonQuest.prerequisites))
//        }

        // Iterate over the list of quest types strings if it is not null.
        jsonQuest.types?.forEach { type ->
            when (type) {
                "biome_red" -> questBiomes.add(Biome.RED)
                "biome_blue" -> questBiomes.add(Biome.BLUE)
                "biome_green" -> questBiomes.add(Biome.GREEN)
                "biome_yellow" -> questBiomes.add(Biome.YELLOW)
                "biome_all" -> questBiomes.addAll(
                    listOf(
                        Biome.BLUE, Biome.GREEN, Biome.YELLOW, Biome.RED
                    )
                )

                "wonders_animal" -> questWonders.add(Wonder.ANIMAL)
                "wonders_mineral" -> questWonders.add(Wonder.MINERAL)
                "wonders_plant" -> questWonders.add(Wonder.PLANT)
                "night" -> questNight = true
                "clue" -> questClue = true
            }
        }
        val resultQuest = Quest(
            questNight, questClue, jsonQuest.fame, questWonders, questBiomes
        )
        return resultQuest
    }

    /**
     * Loads only RegionCards from the JSON data.
     * Filters the raw cards for non-sanctuary cards and maps them to RegionCard objects.
     *
     * @return A list of RegionCard objects.
     */
    fun loadRegionCards(): List<RegionCard> {
        val allCards: List<JsonCard> = getJsonCards()
        val regions: MutableList<RegionCard> = mutableListOf()

        for (card in allCards) {
            if (!card.sanctuary) {
                val biome: Biome = convertBiome(card.biome)
                val wondersList: MutableList<Wonder> =
                    convertWonders(card.wonders)
                val quest: Quest = convertQuest(card.quest)
                val prerequisites: MutableList<Wonder> = mutableListOf()
                if (card.quest?.prerequisites != null) {
                    prerequisites.addAll(convertWonders(card.quest.prerequisites))
                }
                val region = RegionCard(
                    card.time.duration,
                    prerequisites,
                    card.time.night,
                    card.clue,
                    biome,
                    wondersList,
                    quest
                )
                regions.add(region)
            }
        }
        return regions
    }

    /**
     * Loads only SanctuaryCards from the JSON data.
     * Filters the raw cards for sanctuary cards and maps them to SanctuaryCard objects.
     *
     * @return A list of SanctuaryCard objects.
     */

    fun loadSanctuaryCards(): List<SanctuaryCard> {
        val allCards: List<JsonCard> = getJsonCards()
        val sanctuaries: MutableList<SanctuaryCard> = mutableListOf()

        for (card in allCards) {
            if (card.sanctuary) {
                val biome: Biome = convertBiome(card.biome)
                val wondersList: MutableList<Wonder> =
                    convertWonders(card.wonders)
                val quest: Quest = convertQuest(card.quest)
                val sanctuary = SanctuaryCard(
                    card.id,
                    card.time.night,
                    card.clue,
                    biome,
                    wondersList,
                    quest
                )
                sanctuaries.add(sanctuary)
            }
        }
        return sanctuaries
    }

    //Data Classes for Serialization

    /**
     * Represents a single card as defined in the JSON file.
     * Contains all raw data of a card before it is converted into a `RegionCard` or `SanctuaryCard`.
     *
     * @property id The unique identification number of the card.
     * @property sanctuary Indicates whether it is a SanctuaryCard (true) or a RegionCard (false).
     * @property biome The name of the biome as a string (e.g., "red", "blue"), which is later converted to an Enum.
     * @property time Information about exploration duration and time of day.
     * @property wonders The wonders contained on the card.
     * @property clue Indicates if the card contains a clue symbol.
     * @property quest Optional quest data, if the card contains a quest.
     */
    @Serializable
    private data class JsonCard(
        val id: Int,
        val sanctuary: Boolean,
        val biome: String,
        val time: JsonTime,
        val wonders: JsonWonders,
        val clue: Boolean,
        val quest: JsonQuest? = null
    )

    /**
     * Represents the time information of a card from the JSON.
     *
     * @property night Indicates if it is a night card.
     * @property duration The duration of exploration (hourglasses).
     */
    @Serializable
    private data class JsonTime(
        val night: Boolean, val duration: Int
    )

    /**
     * Represents the count of different wonders on a card or as a prerequisite.
     *
     * @property mineral Count of mineral wonders .
     * @property animal Count of animal wonders.
     * @property plant Count of plant wonders.
     */
    @Serializable
    private data class JsonWonders(
        val mineral: Int, val animal: Int, val plant: Int
    )

    /**
     * Represents a quest (task) on a card from the JSON.
     *
     * @property fame The fame points this quest provides.
     * @property prerequisites Optional wonder prerequisites to fulfill the quest (for RegionCards).
     * @property types A list of strings defining what gives points ("biome_red", "wonders_animal").
     */
    @Serializable
    private data class JsonQuest(
        val fame: Int,
        val prerequisites: JsonWonders? = null,
        val types: List<String>? = null
    )

    /**
     * Ends the currently active game and initializes the final score evaluation.
     *
     * This method may only be called when all end-of-game conditions are fulfilled (all players own 8 region cards,
     * 8 rounds are played and the last round finishes). It calculates the final scores of all players by
     * calling [calculateAllScores] and informs the UI by calling `refreshAfterGameEnd`.
     */
    fun endGame() {

        val game = rootService.currentGame
        checkNotNull(game) { "there is no game currently active." }

        val state = game.currentGameState

        //all players need to own 8 region cards
        check(state.players.all { it.regionCards.size == 8 }) {
            "Not all players have collected 8 region cards."
        }

        //8 rounds must have been played
        check(state.currentRound == 8) {
            "Only ${state.currentRound} rounds played it needs to be 8."
        }

        //calculate scores
        calculateAllScores()

        val maxScore = state.players.maxOf { it.score.last() }
        val winners = state.players.first { it.score.last() == maxScore }

        //inform UI
        onAllRefreshables { refreshAfterGameEnd(winners) }
    }

    /**
     * Advances to the next player and handles their turn start logic based on their hand size.
     *
     * Checks the new current player's hand size:
     * - If the hand size is 3, prepares for playing a region card.
     * - If the hand size is 5, prepares for choosing the starting hand (setup phase).
     *
     */
    fun isNextPlayerHandSizeValid(gameState: GameState) {
        val game =
            checkNotNull(rootService.currentGame) { "Game must not be null" }
        gameState.currentPlayer =
            ((gameState.currentPlayer + 1) % gameState.players.size)
        val player = gameState.players[gameState.currentPlayer]
        var handSize = player.hand.size

        if (game.isOnline) {
            val localPlayer = gameState.players.first { it.playerType != PlayerType.REMOTE }
            handSize = localPlayer.hand.size
            if (handSize == 3) {
                rootService.networkService.waitForAllPlayersToSelectStartingHand(game)
                return
            }
        }

        if (handSize == 3) {
            onAllRefreshables { refreshBeforePlayRegionCard() }
            when (player.playerType) {
                PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                    val bot = checkNotNull(game.bots[player.name])
                    val chosenCard = bot.exploreRegion(player.hand)
                    rootService.playerActionService.playRegionCard(chosenCard)
                }
                else -> {}
            }
        } else if (handSize == 5) {
            onAllRefreshables { refreshBeforeChooseStartingHand() }
            when (player.playerType) {
                PlayerType.BOT_EASY, PlayerType.BOT_HARD -> {
                    val bot = checkNotNull(game.bots[player.name])
                    val startingHand = bot.chooseInitialCards(player.hand)
                    rootService.playerActionService.chooseStartingHand(
                        startingHand
                    )
                }
                else -> {}
            }
        }

    }

    /**
     * Calls the appropriate refreshable, which must be called after loading a game to match the current game status.
     */
    fun refreshBeforeNextMove() {
        val currentGame =
            requireNotNull(rootService.currentGame) { "Game has not yet been loaded." }

        if(currentGame.currentGameState.players.any { it.hand.size > 3 }) {
            onAllRefreshables { refreshBeforeChooseStartingHand() }
            return
        }

        // Initial game start
        if (currentGame.gameHistory.isEmpty()) {
            onAllRefreshables { refreshBeforePlayRegionCard() }
            return
        }

        val currentState = currentGame.gameHistory[currentGame.gameHistoryIndex]
        val currentPlayer = currentState.players[currentState.currentPlayer]

        // Phase 1: Choose a card (hidden)
        if (!currentState.isPhaseTwo) {
            onAllRefreshables { refreshBeforePlayRegionCard() }
            return
        }

        // --- Phase 2 ---

        // Player has to choose a sanctuary, if he has options to choose from and didn't
        // draw a new card from the center yet
        if (currentPlayer.temporarySanctuaries.isNotEmpty() && currentPlayer.hand.size < 3) {
            onAllRefreshables { refreshBeforeChooseSanctuaryCard() }
            return
        }

        onAllRefreshables { refreshBeforeChooseRegionCard() }
    }

}