package gui

import entity.RegionCard
import entity.SanctuaryCard
import tools.aqua.bgw.visual.ImageVisual

private const val REGION_CARD_FILE_PREFIX = "region300/tile0"
private const val REGION_CARD_WIDTH = 300 // Anzeigebreite
private const val REGION_CARD_HEIGHT = 300 // Anzeigehöhe

private const val SANCTUARY_CARD_FILE_PREFIX = "sanctuaries/tile0"
private const val SANCTUARY_CARD_WIDTH = 189 // Anzeigebreite
private const val SANCTUARY_CARD_HEIGHT = 291 // Anzeigehöhe

/**
 * This class provides to load the Images of the Objects.
 */
class ImageLoader {
    /**
     * Provides the back side image of the card deck
     */
    val regionBackImage: ImageVisual get() = loadImage("region300/back.jpg", 765, 765)

    /**
     * Provides the image of the Region cards.
     * @param card The card which has to be displayed.
     *
     * @return [ImageVisual] The Image of the card.
     */
    fun imageFor(card: RegionCard): ImageVisual {
        return loadRegionCard(card.explorationTime)
    }

    /**
     * Provides the image of the Sanctuary cards.
     * @param card The card which has to be displayed.
     *
     * @return [ImageVisual] The Image of the card.
     */
    fun imageFor(card: SanctuaryCard): ImageVisual {
        return loadSanctuaryCard(card.cardId-67)
    }

    /**
     * Provides the image of the Region cards.
     * @param id The ID of the Region card.
     *
     * @return [ImageVisual] The Image of the Region card.
     */
    private fun loadRegionCard(id: Int): ImageVisual {
        val filePath = "${REGION_CARD_FILE_PREFIX}${id}.jpg"

        return loadImage(
            filePath = filePath,
            width = REGION_CARD_WIDTH,
            height = REGION_CARD_HEIGHT
        )
    }

    /**
     * Provides the image of the Sanctuary cards.
     * @param id The ID of the Sanctuary card.
     *
     * @return [ImageVisual] The Image of the Sanctuary card.
     */
    private fun loadSanctuaryCard(id: Int): ImageVisual {
        val filePath = "${SANCTUARY_CARD_FILE_PREFIX}${id}.jpg"

        return loadImage(
            filePath = filePath,
            width = SANCTUARY_CARD_WIDTH,
            height = SANCTUARY_CARD_HEIGHT
        )
    }

    /**
     * This method [loadButton] provides the Image of the Buttons.
     * @param id The id of the Image.
     *
     * @return [ImageVisual] The Image of the Button.
     */
    fun loadButton(id: Int): ImageVisual {
        val filePath = "buttons/${id}.png"
        var width = 800
        var height = 600

        when(id) {
            3, 4 -> {
                width = 600
                height = 800
            }
            5, 6, 7, 8 -> {
                width = 1000
                height = 1000
            }
        }
        return loadImage(
            filePath = filePath,
            width = width,
            height = height
        )
    }

    /**
     * This method [loadBackground] provides the Image of the Background.
     * @param id The id of the Image.
     *
     * @return [ImageVisual] The Image of the Background.
     */
    fun loadBackground(id: Int): ImageVisual {
        val filePath = "background${id}.png"
        var width = 0
        var height = 0

        when(id){
            // Button Background
            0 -> {
                width = 315
                height = 110
            }
            4 -> {
                width = 315
                height = 70
            }
            // Full Scene Background
            112 -> {
                width = 1920
                height = 1080
            }
            1,2,5,14 -> {
                width = 1536
                height = 1024
            }
            3 -> {
                width = 600
                height = 600
            }
            7,8,9,11,12,13 -> {
                width = 1300
                height = 900
            }
            10 -> {
                width = 1400
                height = 1000
            }
            6 -> {
                width = 600
                height = 665
            }
        }

        return loadImage(
            filePath = filePath,
            width = width,
            height = height
        )
    }

    /**
     * This method loads the Image.
     *
     * @param filePath The path of the Image.
     * @param width The width of the Image.
     * @param height The height of the Image.
     *
     * @return [ImageVisual] The Image.
     */
    private fun loadImage(
        filePath: String,
        width: Int,
        height: Int,
    ) = ImageVisual(
        path = filePath,
        width = width,
        height = height,
        offsetX = 0,
        offsetY = 0,
    )
}