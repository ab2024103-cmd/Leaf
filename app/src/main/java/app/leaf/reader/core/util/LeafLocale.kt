package app.leaf.reader.core.util

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import java.util.Locale

/**
 * The nine launch languages (LEAF-SPEC §3) and the in-app locale override (§10).
 *
 * The chosen tag is mirrored into a plain SharedPreferences file so that
 * `attachBaseContext` can apply it synchronously, before any string is resolved;
 * DataStore is asynchronous and would otherwise flash English on every cold start.
 * Arabic is right-to-left: `Configuration` carries the layout direction with the locale,
 * and the manifest declares `supportsRtl` so Compose mirrors the chrome.
 */
object LeafLocale {
    const val DEFAULT = "en"

    private const val PREFS = "leaf_locale"
    private const val KEY_LANGUAGE = "language"

    /** Language tags in the order the picker shows them. */
    val supported: List<String> = listOf("en", "es", "fr", "de", "pt", "ar", "yo", "hi", "ja")

    fun normalize(tag: String?): String = if (tag != null && tag in supported) tag else DEFAULT

    /**
     * Each language named in its own script, as the mockup's Language sheet shows them.
     * These are endonyms (names), not translated copy, so they are not in strings.xml.
     */
    fun displayName(tag: String): String = when (normalize(tag)) {
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "pt" -> "Português"
        "ar" -> "العربية"
        "yo" -> "Yoruba"
        "hi" -> "हिन्दी"
        "ja" -> "日本語"
        else -> "English"
    }

    fun savedTag(context: Context): String =
        normalize(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, DEFAULT))

    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalize(tag))
            .apply()
    }

    /** Context for a base context whose configuration carries the saved language. */
    fun wrap(base: Context): Context = localized(base, savedTag(base))

    fun localized(base: Context, tag: String): Context {
        val locale = Locale.forLanguageTag(normalize(tag))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        applyLocale(config, locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Updates the application's own resources in place, so `getString` calls made from
     * view models pick up a language change without a process restart. Used only after the
     * user changes the language; startup goes through [wrap].
     */
    @Suppress("DEPRECATION")
    fun applyToApplication(application: Application, tag: String) {
        val locale = Locale.forLanguageTag(normalize(tag))
        Locale.setDefault(locale)
        val resources = application.resources
        val config = Configuration(resources.configuration)
        applyLocale(config, locale)
        val metrics: DisplayMetrics = resources.displayMetrics
        resources.updateConfiguration(config, metrics)
    }

    private fun applyLocale(config: Configuration, locale: Locale) {
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)
    }
}
