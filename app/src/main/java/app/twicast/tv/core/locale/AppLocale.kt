package app.twicast.tv.core.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import app.twicast.tv.data.model.AppLanguage
import java.util.Locale

/**
 * A context whose resources speak [language] whatever the TV's language is. The default locale follows too,
 * so numbers ("18.2K" / "18,2 тыс") are formatted for the same language as the text around them.
 */
fun Context.withAppLanguage(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration).apply { setLocales(LocaleList(locale)) }
    return createConfigurationContext(configuration)
}
