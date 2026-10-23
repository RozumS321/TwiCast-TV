package app.twicast.tv.feature.player.components

/** Commands the player screen sends to whichever player is on screen (TwiCast or the Twitch embed). */
interface PlaybackController {
    fun togglePause()

    fun setQuality(id: String)

    /** Recordings: plays from [positionMs]. */
    fun seekTo(positionMs: Long)

    /** Only the embed draws the chat itself; the TwiCast player shows it as a separate panel. */
    fun setChatVisible(visible: Boolean, title: String, hint: String)
}
