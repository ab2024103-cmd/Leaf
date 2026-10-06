package app.leaf.reader.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leaf.reader.R
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.theme.LeafShape
import app.leaf.reader.core.ui.theme.FontWeight800
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.isScrolled

/**
 * Settings (§6.5). M1 ships the About group — the one block whose content is static
 * and therefore complete. The Appearance, Reading, Device and Data groups land in M7
 * with the controls they contain.
 */
@Composable
fun SettingsScreen() {
    val scrollState = rememberScrollState()
    LeafScreen(
        title = stringResource(R.string.nav_settings),
        subtitle = stringResource(R.string.settings_subtitle),
        scrollState = scrollState,
        scrolled = isScrolled(scrollState)
    ) {
        SettingsGroup(title = stringResource(R.string.settings_group_about)) {
            SettingsRow(
                icon = R.drawable.ic_settings_about,
                label = stringResource(R.string.settings_version),
                value = stringResource(R.string.settings_version_value)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_leaf_mark),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = stringResource(R.string.settings_footer),
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.settings_about_seed),
                style = LeafType.supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/**
 * A grouped card (§6.5): `surfaceContainerLow`, a 20 dp radius, an uppercase
 * `primary` group title and hairline dividers between rows.
 */
@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 14.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp)
            )
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            style = LeafType.supporting.copy(fontWeight = FontWeight800),
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
        )
        content()
    }
}

/** One settings row: 36 dp tonal chip, 14 sp label, 12.5 sp value (§6.5, mockup .srow). */
@Composable
fun SettingsRow(
    icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    /** The mockup draws the hairline between rows, never above the first one. */
    divider: Boolean = false
) {
    if (divider) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(LeafShape.m)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = LeafType.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        Text(
            text = value,
            style = LeafType.body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
