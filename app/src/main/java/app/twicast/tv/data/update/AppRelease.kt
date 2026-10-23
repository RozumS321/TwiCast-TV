package app.twicast.tv.data.update

/** "What's new" of a release, from the notes the release workflow writes (sections "## Новое" etc.). */
data class ReleaseNotes(
    val added: List<String>,
    val fixed: List<String>,
    val improved: List<String>,
) {
    val isEmpty: Boolean get() = added.isEmpty() && fixed.isEmpty() && improved.isEmpty()

    companion object {
        private const val SectionPrefix = "## "
        private const val ItemPrefix = "- "
        private const val AddedTitle = "Новое"
        private const val FixedTitle = "Исправлено"
        private const val ImprovedTitle = "Улучшено"

        /** One list for several releases (pass them newest first); a line repeated across releases is kept once. */
        fun merge(notes: List<ReleaseNotes>) = ReleaseNotes(
            added = notes.flatMap { it.added }.distinct(),
            fixed = notes.flatMap { it.fixed }.distinct(),
            improved = notes.flatMap { it.improved }.distinct(),
        )

        /** Parses the release body; unknown sections and free text are ignored. */
        fun parse(markdown: String): ReleaseNotes {
            val sections = mutableMapOf<String, MutableList<String>>()
            var current: MutableList<String>? = null
            markdown.lineSequence().map { it.trim() }.forEach { line ->
                when {
                    line.startsWith(SectionPrefix) -> current = sections.getOrPut(line.removePrefix(SectionPrefix).trim()) { mutableListOf() }
                    line.startsWith(ItemPrefix) -> current?.add(line.removePrefix(ItemPrefix).trim())
                }
            }
            return ReleaseNotes(
                added = sections[AddedTitle].orEmpty(),
                fixed = sections[FixedTitle].orEmpty(),
                improved = sections[ImprovedTitle].orEmpty(),
            )
        }
    }
}

/** A published release with its APK. Tags are "v0.1.N" where N is both the last version part and versionCode. */
data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val notes: ReleaseNotes,
    val apkUrl: String,
    val apkSizeBytes: Long,
) {
    companion object {
        private const val TagPrefix = "v"

        /** "v0.1.42" → (42, "0.1.42"); null for tags this app did not publish. */
        fun parseTag(tag: String): Pair<Int, String>? {
            val versionName = tag.removePrefix(TagPrefix).takeIf { tag.startsWith(TagPrefix) } ?: return null
            val versionCode = versionName.substringAfterLast('.').toIntOrNull() ?: return null
            return versionCode to versionName
        }
    }
}
