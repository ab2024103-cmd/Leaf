package app.leaf.reader.feature.settings

import android.app.Application
import androidx.compose.ui.semantics.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertCountEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.ui.theme.LeafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Accessibility pass (§10): every Settings group is a heading, every switch row exposes a
 * switch role, and a destructive row opens its confirmation dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class SettingsSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render() {
        composeRule.setContent {
            LeafTheme(appTheme = AppTheme.LIGHT) {
                SettingsScreen()
            }
        }
    }

    @Test
    fun each_settings_group_is_a_heading() {
        render()
        // Five group headings at minimum; the app bar title may also be a heading.
        val headings = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNodes()
        assertTrue(headings.size >= 5)
    }

    @Test
    fun every_switch_row_exposes_the_switch_role() {
        render()
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertCountEquals(7)
    }

    @Test
    fun destructive_rows_confirm_before_they_run() {
        render()
        composeRule.onNode(hasText("Clear recents", substring = true) and hasClickAction()).performClick()
        composeRule.onNodeWithText("Clear all recents?").assertIsDisplayed()
    }
}
