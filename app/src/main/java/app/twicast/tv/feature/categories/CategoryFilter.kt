package app.twicast.tv.feature.categories

import androidx.annotation.StringRes
import app.twicast.tv.R

/**
 * Chips of the Categories screen. Helix has no category type, so the non-game groups are a short,
 * hand-kept list of Twitch's own non-game categories (by id); everything else counts as a game.
 */
enum class CategoryFilter(@StringRes val labelRes: Int) {
    All(R.string.categories_filter_all),
    Games(R.string.categories_filter_games),
    Irl(R.string.categories_filter_irl),
    Music(R.string.categories_filter_music),
    Creative(R.string.categories_filter_creative),
    ;

    fun matches(categoryId: String): Boolean = when (this) {
        All -> true
        Games -> categoryId !in IrlIds && categoryId !in MusicIds && categoryId !in CreativeIds
        Irl -> categoryId in IrlIds
        Music -> categoryId in MusicIds
        Creative -> categoryId in CreativeIds
    }

    private companion object {
        /** Just Chatting, Travel & Outdoors, Sports, ASMR, Talk Shows & Podcasts, Food & Drink, Special Events, Pools…, Animals… */
        val IrlIds = setOf("509658", "509672", "518203", "509659", "417752", "509667", "509663", "116747788", "272263131")
        /** Music. */
        val MusicIds = setOf("26936")
        /** Art, Software and Game Development, Makers & Crafting, Science & Technology. */
        val CreativeIds = setOf("509660", "1469308723", "509673", "509670")
    }
}
