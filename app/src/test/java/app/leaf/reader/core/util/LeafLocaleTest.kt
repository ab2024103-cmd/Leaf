package app.leaf.reader.core.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** The nine launch languages, unknown-tag fallback, and the synchronous startup mirror (§10). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class LeafLocaleTest {

    @Test
    fun supports_the_nine_launch_languages_in_picker_order() {
        assertEquals(listOf("en", "es", "fr", "de", "pt", "ar", "yo", "hi", "ja"), LeafLocale.supported)
    }

    @Test
    fun unknown_or_missing_tags_fall_back_to_english() {
        assertEquals("en", LeafLocale.normalize(null))
        assertEquals("en", LeafLocale.normalize("xx"))
        assertEquals("ar", LeafLocale.normalize("ar"))
    }

    @Test
    fun each_language_is_named_in_its_own_script() {
        assertEquals("العربية", LeafLocale.displayName("ar"))
        assertEquals("日本語", LeafLocale.displayName("ja"))
        assertEquals("English", LeafLocale.displayName("zz"))
    }

    @Test
    fun saved_tag_is_read_back_synchronously_for_startup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        LeafLocale.save(context, "yo")
        assertEquals("yo", LeafLocale.savedTag(context))
        LeafLocale.save(context, "klingon")
        assertEquals("en", LeafLocale.savedTag(context))
    }

    @Test
    fun arabic_is_laid_out_right_to_left() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val localized = LeafLocale.localized(context, "ar")
        assertEquals("ar", localized.resources.configuration.locales[0].language)
        assertEquals(Configuration.SCREENLAYOUT_LAYOUTDIR_RTL, localized.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
        assertTrue(Locale.getDefault().language == "ar")
        LeafLocale.localized(context, "en")
    }
}
