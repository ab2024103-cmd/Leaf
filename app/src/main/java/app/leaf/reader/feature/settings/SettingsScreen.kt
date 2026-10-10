package app.leaf.reader.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.leaf.reader.R
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.theme.LeafShape
import app.leaf.reader.core.ui.theme.FontWeight800
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.util.isScrolled
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Settings (§6.5). The maximum open-tabs control is live here because tab-limit
 * enforcement is part of M5; the remaining settings groups stay in their planned milestone.
 */
@Composable
fun SettingsScreen(settingsStore: SettingsStore? = null) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val resolvedStore = remember(settingsStore) {
        settingsStore ?: GlobalContext.getOrNull()?.getOrNull<SettingsStore>()
    }
    val settingsFlow = remember(resolvedStore) { resolvedStore?.settings ?: flowOf(LeafSettings()) }
    val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = LeafSettings())
    var fallbackTabLimit by remember { androidx.compose.runtime.mutableIntStateOf(LeafSettings().tabLimit) }
    val tabLimit = if (resolvedStore == null) fallbackTabLimit else settings.tabLimit
    LeafScreen(
        title = stringResource(R.string.nav_settings),
        subtitle = stringResource(R.string.settings_subtitle),
        scrollState = scrollState,
        scrolled = isScrolled(scrollState)
    ) {
        SettingsGroup(title = stringResource(R.string.settings_group_device)) {
            TabLimitSettingsRow(
                value = tabLimit,
                onChange = { value ->
                    if (resolvedStore == null) fallbackTabLimit = value
                    else scope.launch {
                        resolvedStore.update { current -> current.copy(tabLimit = value) }
                    }
                }
            )
        }
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

@Composable
private fun TabLimitSettingsRow(value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(LeafShape.m)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tab_new),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = stringResource(R.string.settings_maximum_open_tabs),
            style = LeafType.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        IconButton(
            onClick = { onChange((value - 1).coerceAtLeast(LeafSettings.TAB_LIMIT_MIN)) },
            enabled = value > LeafSettings.TAB_LIMIT_MIN,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_minus),
                contentDescription = stringResource(R.string.settings_tab_limit_decrease),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = value.toString(),
            style = LeafType.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(32.dp).wrapContentHeight(Alignment.CenterVertically)
        )
        IconButton(
            onClick = { onChange((value + 1).coerceAtMost(LeafSettings.TAB_LIMIT_MAX)) },
            enabled = value < LeafSettings.TAB_LIMIT_MAX,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = stringResource(R.string.settings_tab_limit_increase),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
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
