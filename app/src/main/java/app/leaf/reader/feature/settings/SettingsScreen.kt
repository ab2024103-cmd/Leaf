package app.leaf.reader.feature.settings

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.leaf.reader.R
import app.leaf.reader.core.data.prefs.SettingsStore
import app.leaf.reader.core.model.AppTheme
import app.leaf.reader.core.model.LeafSettings
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.Typeface
import app.leaf.reader.core.ui.components.LeafConfirmDialogOverlay
import app.leaf.reader.core.ui.components.LeafScreen
import app.leaf.reader.core.ui.components.LeafCheck
import app.leaf.reader.core.ui.components.LeafSheetItem
import app.leaf.reader.core.ui.components.LeafSheetOverlay
import app.leaf.reader.core.ui.components.LeafSheetSurface
import app.leaf.reader.core.ui.components.LeafSnackbar
import app.leaf.reader.core.ui.theme.LeafShape
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.theme.FontWeight800
import app.leaf.reader.core.ui.theme.resolve
import app.leaf.reader.core.ui.util.isScrolled
import app.leaf.reader.core.util.LeafLocale
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.flow.flowOf

/** The sheets the Settings screen can open (§6.5). */
private enum class SettingsSheet { NONE, APP_THEME, LANGUAGE, ZOOM, SPEC_COVERAGE }

/**
 * Settings (§6.5), in the mockup's five groups: Appearance, Reading, Device, Data & storage, About.
 * Every toggle persists immediately through [SettingsStore] (§8.12). Destructive rows confirm,
 * then post a snackbar with the §11 copy.
 *
 * Dependencies are resolved from Koin when they are not passed in, so the screenshot tests can
 * render the screen without starting the app graph.
 */
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore? = null,
    dataController: SettingsDataController? = null
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val store = remember(settingsStore) {
        settingsStore ?: GlobalContext.getOrNull()?.getOrNull<SettingsStore>()
    }
    val data = remember(dataController) {
        dataController ?: GlobalContext.getOrNull()?.getOrNull<SettingsDataController>()
    }
    val vm = remember(store, data, scope) { SettingsViewModel(store, data, scope) }
    val settings by vm.settings.collectAsStateWithLifecycle(initialValue = LeafSettings())
    val documentCount by vm.documentCount.collectAsStateWithLifecycle(initialValue = 0)
    val recentCount by vm.recentCount.collectAsStateWithLifecycle(initialValue = 0)
    val highlightTotal by vm.highlightTotal.collectAsStateWithLifecycle(initialValue = 0)
    val snack by vm.snack.collectAsStateWithLifecycle()

    var sheet by remember { mutableStateOf(SettingsSheet.NONE) }
    var pendingClear by remember { mutableStateOf<SettingsClear?>(null) }

    fun persist(transform: (LeafSettings) -> LeafSettings) = vm.update(transform)

    fun chooseLanguage(tag: String) {
        val normalized = vm.setLanguage(tag)
        LeafLocale.save(context, normalized)
        (context.applicationContext as? Application)?.let { LeafLocale.applyToApplication(it, normalized) }
        sheet = SettingsSheet.NONE
        (context as? Activity)?.recreate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LeafScreen(
            title = stringResource(R.string.nav_settings),
            subtitle = stringResource(R.string.settings_subtitle),
            scrollState = scrollState,
            scrolled = isScrolled(scrollState)
        ) {
            SettingsGroup(title = stringResource(R.string.settings_group_appearance)) {
                SettingsNavRow(
                    icon = R.drawable.ic_palette,
                    label = stringResource(R.string.settings_app_theme),
                    description = stringResource(R.string.settings_app_theme_desc),
                    value = stringResource(appThemeLabel(settings.appTheme)),
                    onClick = { sheet = SettingsSheet.APP_THEME },
                    divider = false
                )
                ReadingThemeRow(
                    selected = settings.readingTheme,
                    onSelect = { theme -> persist { it.copy(readingTheme = theme) } }
                )
                SettingsSwitchRow(
                    icon = R.drawable.ic_view_layout,
                    label = stringResource(R.string.settings_animations),
                    description = stringResource(R.string.settings_animations_desc),
                    checked = settings.animations,
                    onCheckedChange = { value -> persist { it.copy(animations = value) } }
                )
                SettingsSwitchRow(
                    icon = R.drawable.ic_highlight,
                    label = stringResource(R.string.settings_haptics),
                    description = stringResource(R.string.settings_haptics_desc),
                    checked = settings.haptics,
                    onCheckedChange = { value -> persist { it.copy(haptics = value) } }
                )
                SettingsNavRow(
                    icon = R.drawable.ic_document,
                    label = stringResource(R.string.settings_language),
                    description = stringResource(R.string.settings_language_desc),
                    value = LeafLocale.displayName(settings.language),
                    onClick = { sheet = SettingsSheet.LANGUAGE }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_reading)) {
                SettingsNavRow(
                    icon = R.drawable.ic_progress,
                    label = stringResource(R.string.settings_scroll_direction),
                    description = stringResource(
                        if (settings.scrollDir == ScrollDir.VERTICAL) {
                            R.string.settings_scroll_vertical_desc
                        } else {
                            R.string.settings_scroll_horizontal_desc
                        }
                    ),
                    value = stringResource(
                        if (settings.scrollDir == ScrollDir.VERTICAL) R.string.reader_vertical else R.string.reader_horizontal
                    ),
                    onClick = {
                        persist {
                            it.copy(
                                scrollDir = if (it.scrollDir == ScrollDir.VERTICAL) ScrollDir.HORIZONTAL else ScrollDir.VERTICAL
                            )
                        }
                    },
                    divider = false
                )
                SettingsSwitchRow(
                    icon = R.drawable.ic_document,
                    label = stringResource(R.string.settings_reflow),
                    description = stringResource(R.string.settings_reflow_desc),
                    checked = settings.reflow,
                    onCheckedChange = { value -> persist { it.copy(reflow = value) } }
                )
                SettingsNavRow(
                    icon = R.drawable.ic_zoom_in,
                    label = stringResource(R.string.settings_zoom),
                    description = stringResource(R.string.settings_zoom_desc),
                    value = "${(settings.zoom * 100).toInt()}%",
                    onClick = { sheet = SettingsSheet.ZOOM }
                )
                SettingsNavRow(
                    icon = R.drawable.ic_rename,
                    label = stringResource(R.string.settings_typeface),
                    description = stringResource(R.string.settings_typeface_desc),
                    value = stringResource(
                        if (settings.typeface == Typeface.SERIF) R.string.reader_typeface_serif else R.string.reader_typeface_sans
                    ),
                    onClick = {
                        persist {
                            it.copy(typeface = if (it.typeface == Typeface.SERIF) Typeface.SANS else Typeface.SERIF)
                        }
                    }
                )
                SettingsSwitchRow(
                    icon = R.drawable.ic_clock,
                    label = stringResource(R.string.settings_remember_page),
                    description = stringResource(R.string.settings_remember_page_desc),
                    checked = settings.rememberPage,
                    onCheckedChange = { value -> persist { it.copy(rememberPage = value) } }
                )
                SettingsSwitchRow(
                    icon = R.drawable.ic_document,
                    label = stringResource(R.string.settings_page_numbers),
                    description = stringResource(R.string.settings_page_numbers_desc),
                    checked = settings.showPageNumbers,
                    onCheckedChange = { value -> persist { it.copy(showPageNumbers = value) } }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_device)) {
                SettingsSwitchRow(
                    icon = R.drawable.ic_view_layout,
                    label = stringResource(R.string.settings_auto_rotate),
                    description = stringResource(R.string.settings_auto_rotate_desc),
                    checked = settings.autoRotate,
                    onCheckedChange = { value -> persist { it.copy(autoRotate = value) } },
                    divider = false
                )
                SettingsSwitchRow(
                    icon = R.drawable.ic_fullscreen,
                    label = stringResource(R.string.settings_lock_orientation),
                    description = stringResource(R.string.settings_lock_orientation_desc),
                    checked = settings.orientationLock,
                    onCheckedChange = { value -> persist { it.copy(orientationLock = value) } }
                )
                TabLimitSettingsRow(
                    value = settings.tabLimit,
                    onChange = { value -> persist { it.copy(tabLimit = value) } }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_data)) {
                SettingsActionRow(
                    icon = R.drawable.ic_clock,
                    label = stringResource(R.string.settings_clear_recents),
                    description = pluralStringResource(R.plurals.settings_recents_count, recentCount, recentCount),
                    actionLabel = stringResource(R.string.settings_action_clear),
                    onClick = { pendingClear = SettingsClear.RECENTS },
                    divider = false
                )
                SettingsActionRow(
                    icon = R.drawable.ic_filter,
                    label = stringResource(R.string.settings_clear_search),
                    description = pluralStringResource(
                        R.plurals.settings_search_count,
                        settings.searchHistory.size,
                        settings.searchHistory.size
                    ),
                    actionLabel = stringResource(R.string.settings_action_clear),
                    onClick = { pendingClear = SettingsClear.SEARCH }
                )
                SettingsActionRow(
                    icon = R.drawable.ic_progress,
                    label = stringResource(R.string.settings_clear_positions),
                    description = stringResource(R.string.settings_clear_positions_desc),
                    actionLabel = stringResource(R.string.settings_action_reset),
                    onClick = { pendingClear = SettingsClear.POSITIONS }
                )
                SettingsActionRow(
                    icon = R.drawable.ic_highlight,
                    label = stringResource(R.string.settings_clear_highlights),
                    description = pluralStringResource(R.plurals.settings_highlights_count, highlightTotal, highlightTotal),
                    actionLabel = stringResource(R.string.settings_action_clear),
                    onClick = { pendingClear = SettingsClear.HIGHLIGHTS }
                )
                SettingsActionRow(
                    icon = R.drawable.ic_reader_error,
                    label = stringResource(R.string.settings_reset_library),
                    description = stringResource(R.string.settings_reset_library_desc),
                    actionLabel = stringResource(R.string.settings_action_reset),
                    onClick = { pendingClear = SettingsClear.RESET }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_about)) {
                SettingsNavRow(
                    icon = R.drawable.ic_settings_about,
                    label = stringResource(R.string.settings_spec_coverage),
                    description = stringResource(R.string.settings_spec_coverage_desc),
                    value = "",
                    onClick = { sheet = SettingsSheet.SPEC_COVERAGE },
                    divider = false
                )
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
                    text = stringResource(
                        R.string.settings_library_summary,
                        pluralStringResource(R.plurals.settings_documents_count, documentCount, documentCount),
                        pluralStringResource(R.plurals.settings_highlights_count, highlightTotal, highlightTotal)
                    ),
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    text = stringResource(R.string.settings_about_seed),
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        when (sheet) {
            SettingsSheet.NONE -> Unit
            SettingsSheet.APP_THEME -> LeafSheetOverlay(onDismiss = { sheet = SettingsSheet.NONE }) {
                LeafSheetSurface(
                    title = stringResource(R.string.settings_app_theme),
                    subtitle = stringResource(R.string.settings_app_theme_sheet_sub)
                ) {
                    AppTheme.entries.forEach { theme ->
                        val selected = settings.appTheme == theme
                        LeafSheetItem(
                            icon = R.drawable.ic_palette,
                            title = stringResource(appThemeLabel(theme)),
                            onClick = {
                                persist { it.copy(appTheme = theme) }
                                sheet = SettingsSheet.NONE
                            },
                            trailing = checkMark(selected)
                        )
                    }
                }
            }
            SettingsSheet.LANGUAGE -> LeafSheetOverlay(onDismiss = { sheet = SettingsSheet.NONE }) {
                LeafSheetSurface(
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_sheet_sub)
                ) {
                    LeafLocale.supported.forEach { tag ->
                        val selected = LeafLocale.normalize(settings.language) == tag
                        LeafSheetItem(
                            icon = R.drawable.ic_document,
                            title = LeafLocale.displayName(tag),
                            onClick = { chooseLanguage(tag) },
                            trailing = checkMark(selected)
                        )
                    }
                }
            }
            SettingsSheet.ZOOM -> LeafOverlayZoomSheet(
                zoom = settings.zoom,
                onCommit = { value -> persist { it.copy(zoom = value) } },
                onDismiss = { sheet = SettingsSheet.NONE }
            )
            SettingsSheet.SPEC_COVERAGE -> LeafSheetOverlay(onDismiss = { sheet = SettingsSheet.NONE }) {
                val items = stringArrayResource(R.array.settings_coverage_items)
                LeafSheetSurface(title = stringResource(R.string.settings_spec_coverage)) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = LeafType.supporting,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = item,
                                style = LeafType.body,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        when (pendingClear) {
            null -> Unit
            SettingsClear.RECENTS -> LeafConfirmDialogOverlay(
                title = stringResource(R.string.settings_dialog_clear_recents_title),
                body = stringResource(R.string.settings_dialog_clear_recents_body),
                confirmLabel = stringResource(R.string.settings_dialog_clear_recents_confirm),
                onConfirm = { pendingClear = null; vm.clear(SettingsClear.RECENTS) },
                onDismiss = { pendingClear = null }
            )
            SettingsClear.SEARCH -> LeafConfirmDialogOverlay(
                title = stringResource(R.string.settings_dialog_clear_search_title),
                body = stringResource(R.string.settings_dialog_clear_search_body),
                confirmLabel = stringResource(R.string.settings_dialog_clear_search_confirm),
                onConfirm = { pendingClear = null; vm.clear(SettingsClear.SEARCH) },
                onDismiss = { pendingClear = null }
            )
            SettingsClear.POSITIONS -> LeafConfirmDialogOverlay(
                title = stringResource(R.string.settings_dialog_clear_positions_title),
                body = stringResource(R.string.settings_dialog_clear_positions_body),
                confirmLabel = stringResource(R.string.settings_dialog_clear_positions_confirm),
                onConfirm = { pendingClear = null; vm.clear(SettingsClear.POSITIONS) },
                onDismiss = { pendingClear = null }
            )
            SettingsClear.HIGHLIGHTS -> LeafConfirmDialogOverlay(
                title = stringResource(R.string.settings_dialog_clear_highlights_title),
                body = pluralStringResource(
                    R.plurals.settings_dialog_clear_highlights_body,
                    highlightTotal,
                    highlightTotal
                ),
                confirmLabel = stringResource(R.string.settings_dialog_clear_highlights_confirm),
                onConfirm = { pendingClear = null; vm.clear(SettingsClear.HIGHLIGHTS) },
                onDismiss = { pendingClear = null }
            )
            SettingsClear.RESET -> LeafConfirmDialogOverlay(
                title = stringResource(R.string.settings_dialog_reset_title),
                body = stringResource(R.string.settings_dialog_reset_body),
                confirmLabel = stringResource(R.string.settings_dialog_reset_confirm),
                onConfirm = { pendingClear = null; vm.clear(SettingsClear.RESET) },
                onDismiss = { pendingClear = null }
            )
        }

        snack?.let { current ->
            // Keyed so posting the same message twice restarts the timer (§6.7).
            key(current.id) {
                LeafSnackbar(
                    message = stringResource(current.textRes),
                    onDismiss = { vm.dismissSnack() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }
        }
    }
}

/** The Default zoom sheet (§6.5): a 70–250 % slider with Reset; the value persists when the drag ends. */
@Composable
private fun LeafOverlayZoomSheet(
    zoom: Float,
    onCommit: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(zoom) { mutableFloatStateOf(zoom) }
    LeafSheetOverlay(onDismiss = onDismiss) {
        LeafSheetSurface(
            title = stringResource(R.string.settings_zoom_sheet_title),
            subtitle = stringResource(R.string.settings_zoom_sheet_sub)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${(draft * 100).toInt()}%",
                    style = LeafType.body,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = draft,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onCommit(draft) },
                    valueRange = LeafSettings.ZOOM_MIN..LeafSettings.ZOOM_MAX
                )
                TextButton(onClick = { draft = 1f; onCommit(1f) }) {
                    Text(stringResource(R.string.settings_zoom_reset))
                }
            }
        }
    }
}

/** The trailing check on the selected item of a sheet (§4.6). */
private fun checkMark(selected: Boolean): (@Composable () -> Unit)? =
    if (selected) {
        { LeafCheck() }
    } else {
        null
    }

private fun appThemeLabel(theme: AppTheme): Int = when (theme) {
    AppTheme.LIGHT -> R.string.settings_theme_light
    AppTheme.DARK -> R.string.settings_theme_dark
    AppTheme.SYSTEM -> R.string.settings_theme_system
}

private fun readingThemeLabel(theme: ReadingTheme): Int = when (theme) {
    ReadingTheme.PAPER -> R.string.reader_theme_paper
    ReadingTheme.SEPIA -> R.string.reader_theme_sepia
    ReadingTheme.NIGHT -> R.string.reader_theme_night
    ReadingTheme.OLED -> R.string.reader_theme_oled
    ReadingTheme.AUTO -> R.string.reader_theme_auto
}

@Composable
private fun TabLimitSettingsRow(value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(R.drawable.ic_tab_new)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = stringResource(R.string.settings_maximum_open_tabs),
                style = LeafType.body,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_maximum_open_tabs_desc),
                style = LeafType.supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

/** The Default reading theme row: five swatch tiles, Paper · Sepia · Night · OLED · Auto (§6.5). */
@Composable
private fun ReadingThemeRow(selected: ReadingTheme, onSelect: (ReadingTheme) -> Unit) {
    val dark = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            text = stringResource(R.string.settings_reading_theme),
            style = LeafType.body,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_reading_theme_desc),
            style = LeafType.supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReadingTheme.entries.forEach { theme ->
                val isSelected = theme == selected
                val palette = theme.resolve(dark)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 40.dp)
                            .background(palette.surface, RoundedCornerShape(LeafShape.m))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(LeafShape.m)
                            )
                    )
                    Text(
                        text = stringResource(readingThemeLabel(theme)),
                        style = LeafType.supporting,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/** One settings row: a 36 dp tonal chip, label, supporting line and an optional trailing value. */
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
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                .semantics { heading() }
        )
        content()
    }
}

@Composable
private fun SettingsIconChip(icon: Int) {
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
}

@Composable
private fun SettingsDivider(show: Boolean) {
    if (show) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/** A toggle row: the whole row flips the switch (§6.5: every row is a single tap target). */
@Composable
private fun SettingsSwitchRow(
    icon: Int,
    label: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    divider: Boolean = true
) {
    SettingsDivider(divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(icon)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(text = label, style = LeafType.body, color = MaterialTheme.colorScheme.onSurface)
            if (!description.isNullOrEmpty()) {
                Text(text = description, style = LeafType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A navigation row: tapping opens a sheet or cycles a value; the trailing text shows the current value. */
@Composable
private fun SettingsNavRow(
    icon: Int,
    label: String,
    description: String?,
    value: String,
    onClick: () -> Unit,
    divider: Boolean = true
) {
    SettingsDivider(divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(icon)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(text = label, style = LeafType.body, color = MaterialTheme.colorScheme.onSurface)
            if (!description.isNullOrEmpty()) {
                Text(text = description, style = LeafType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = LeafType.supporting,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp).size(18.dp)
        )
    }
}

/** A destructive row: the trailing action label is the action it performs, and it always confirms. */
@Composable
private fun SettingsActionRow(
    icon: Int,
    label: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    divider: Boolean = true
) {
    SettingsDivider(divider)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(icon)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(text = label, style = LeafType.body, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = LeafType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = actionLabel,
            style = LeafType.chipText,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** A read-only row (About). */
@Composable
fun SettingsRow(
    icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    divider: Boolean = true
) {
    SettingsDivider(divider)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconChip(icon)
        Text(
            text = label,
            style = LeafType.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = LeafType.supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}
