package app.twicast.tv.data.repository

import android.content.Context
import android.content.res.Resources
import androidx.core.content.edit
import app.twicast.tv.data.model.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface LanguageSettingsRepository {
    /** The chosen language, or the TV's one until the viewer picks in Settings. */
    fun language(): AppLanguage

    fun setLanguage(language: AppLanguage)
}

private const val PreferencesName = "language_settings"
private const val LanguageKey = "language"

/**
 * SharedPreferences, not DataStore like the other settings: the language is needed synchronously in
 * attachBaseContext, before the first string is resolved.
 */
@Singleton
class SharedPreferencesLanguageSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : LanguageSettingsRepository {

    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override fun language(): AppLanguage {
        val stored = preferences.getString(LanguageKey, null)
        return AppLanguage.entries.firstOrNull { it.name == stored }
            ?: AppLanguage.forSystemLanguage(Resources.getSystem().configuration.locales[0].language)
    }

    override fun setLanguage(language: AppLanguage) {
        // commit, not apply: the activity is recreated right after and reads it back.
        preferences.edit(commit = true) { putString(LanguageKey, language.name) }
    }
}
