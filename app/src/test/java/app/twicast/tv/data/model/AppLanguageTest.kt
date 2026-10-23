package app.twicast.tv.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `the TV language is used when the app speaks it, English otherwise`() {
        assertEquals(AppLanguage.Russian, AppLanguage.forSystemLanguage("ru"))
        assertEquals(AppLanguage.Ukrainian, AppLanguage.forSystemLanguage("uk"))
        assertEquals(AppLanguage.English, AppLanguage.forSystemLanguage("en"))
        assertEquals(AppLanguage.English, AppLanguage.forSystemLanguage("de"))
    }
}
