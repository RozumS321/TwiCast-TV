package app.twicast.tv.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReleasesCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val release = AppRelease(
        versionCode = 31,
        versionName = "0.1.31",
        notes = ReleaseNotes(added = listOf("Новый логотип"), fixed = listOf("Фокус в настройках"), improved = emptyList()),
        apkUrl = "https://example.test/twicast-tv-0.1.31.apk",
        apkSizeBytes = 3_752_128,
        publishedAtMillis = 1_795_280_000_000,
    )

    @Test
    fun `the last loaded list comes back when GitHub refuses`() {
        val cache = ReleasesCache(File(folder.root, "releases.json"))

        cache.save(listOf(release))

        assertEquals(listOf(release), cache.load())
    }

    @Test
    fun `nothing saved yet, nothing to show`() {
        assertNull(ReleasesCache(File(folder.root, "releases.json")).load())
    }
}
