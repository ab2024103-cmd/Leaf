package app.leaf.reader.feature.reader

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed as isKeyCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.leaf.reader.R
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.EpubDocumentEngine
import app.leaf.reader.core.format.PageMetrics
import app.leaf.reader.core.format.PdfTileGrid
import app.leaf.reader.core.format.RenderTile
import app.leaf.reader.core.model.DocType
import app.leaf.reader.core.model.Highlight
import app.leaf.reader.core.model.HighlightColor
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.model.Typeface as ReadingTypeface
import app.leaf.reader.core.ui.theme.HighlightSwatches
import app.leaf.reader.core.ui.theme.LocalReadingPalette
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.theme.ReadingPalette
import app.leaf.reader.core.ui.theme.currentReadingPalette
import app.leaf.reader.core.ui.theme.resolve
import app.leaf.reader.core.ui.util.quantityText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    documentId: String,
    initialPage: Int? = null,
    initialFindQuery: String? = null,
    onClose: () -> Unit,
    openTabCount: Int = 1,
    onOpenTabs: () -> Unit = {},
    onPickNewTab: () -> Unit = {},
    modifier: Modifier = Modifier.fillMaxSize(),
    viewModel: ReaderViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val activity = view.context as? Activity

    LaunchedEffect(documentId, initialPage, initialFindQuery, viewModel) {
        viewModel.openDocument(documentId, initialPage, initialFindQuery)
    }
    DisposableEffect(activity, state.isFullScreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            if (state.isFullScreen) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            onDispose { }
        }
    }

    BackHandler(enabled = state.sheet == ReaderSheet.NONE) {
        when {
            state.highlightMenuExpanded -> viewModel.dismissHighlightMenu()
            state.findBarVisible -> viewModel.closeFindBar()
            state.isFullScreen -> viewModel.toggleFullScreen()
            else -> viewModel.leaveReader(onClose)
        }
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.leaveReader {} }
    }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = remember(state.readingTheme, systemDark) { state.readingTheme.resolve(systemDark) }
    val keyboardModifier = modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyUp) {
            false
        } else {
            val command = event.isKeyCtrlPressed || event.isMetaPressed
            when {
                event.key == Key.Escape -> {
                    when {
                        state.sheet != ReaderSheet.NONE -> viewModel.setSheet(ReaderSheet.NONE)
                        state.highlightMenuExpanded -> viewModel.dismissHighlightMenu()
                        state.findBarVisible -> viewModel.closeFindBar()
                        state.isFullScreen -> viewModel.toggleFullScreen()
                        else -> viewModel.leaveReader(onClose)
                    }
                    true
                }
                command && state.sheet == ReaderSheet.NONE && event.key == Key.B -> {
                    viewModel.toggleBookmarkCurrentPage()
                    true
                }
                command && state.sheet == ReaderSheet.NONE && event.key == Key.Z && event.isShiftPressed -> {
                    viewModel.redoAnnotation()
                    true
                }
                command && state.sheet == ReaderSheet.NONE && event.key == Key.Z -> {
                    viewModel.undoAnnotation()
                    true
                }
                command && state.sheet == ReaderSheet.NONE && event.key == Key.Y -> {
                    viewModel.redoAnnotation()
                    true
                }
                command && state.sheet == ReaderSheet.NONE && event.key == Key.T -> {
                    onPickNewTab()
                    true
                }
                else -> false
            }
        }
    }.focusable()
    CompositionLocalProvider(LocalReadingPalette provides palette) {
        ReaderContent(
            state = state,
            viewModel = viewModel,
            palette = palette,
            onClose = onClose,
            openTabCount = openTabCount,
            onOpenTabs = onOpenTabs,
            onPickNewTab = onPickNewTab,
            modifier = keyboardModifier
        )
    }
}

private class ReaderImageProvider {
    var capture: (() -> android.graphics.Bitmap?)? = null
}

private fun captureView(view: View?): android.graphics.Bitmap? {
    val target = view ?: return null
    if (target.width <= 0 || target.height <= 0) return null
    val bitmap = runCatching {
        android.graphics.Bitmap.createBitmap(target.width, target.height, android.graphics.Bitmap.Config.ARGB_8888)
    }.getOrNull() ?: return null
    return runCatching {
        target.draw(android.graphics.Canvas(bitmap))
        bitmap
    }.getOrElse {
        if (!bitmap.isRecycled) bitmap.recycle()
        null
    }
}

@Composable
private fun ReaderContent(
    state: ReaderContentState,
    viewModel: ReaderViewModel,
    palette: ReadingPalette,
    onClose: () -> Unit,
    openTabCount: Int,
    onOpenTabs: () -> Unit,
    onPickNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val epubImageProvider = remember { ReaderImageProvider() }
    val shareCurrentDocument: () -> Unit = {
        val document = state.document
        val engine = state.engine
        if (document != null && engine != null) {
            scope.launch {
                try {
                    shareDocument(context, document, currentEngine = engine)
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.reader_share_failed, Toast.LENGTH_SHORT).show()
                } finally {
                    viewModel.setSheet(ReaderSheet.NONE)
                }
            }
        }
    }
    val sharePageImage: () -> Unit = {
        val engine = state.engine
        if (engine != null) {
            val visiblePage = epubImageProvider.capture?.invoke()
            scope.launch {
                try {
                    shareReaderPageImage(context, engine, state.pageIndex, visiblePage)
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.reader_share_failed, Toast.LENGTH_SHORT).show()
                } finally {
                    viewModel.setSheet(ReaderSheet.NONE)
                }
            }
        }
    }
    val shareSelection: () -> Unit = {
        state.selection?.text?.takeIf(String::isNotBlank)?.let { text ->
            sharePlainText(context, text)
            viewModel.setSheet(ReaderSheet.NONE)
        }
    }
    val copyFilePath: () -> Unit = {
        val path = state.document?.uri?.let { uri ->
            val parsed = Uri.parse(uri)
            when (parsed.scheme) {
                "file" -> parsed.path?.takeIf(String::isNotBlank) ?: uri
                "content" -> parsed.toString()
                else -> uri
            }
        }
        if (path != null) {
            clipboard.setText(AnnotatedString(path))
            viewModel.showCopiedMessage()
        }
        viewModel.setSheet(ReaderSheet.NONE)
    }
    val copyPageText: () -> Unit = {
        scope.launch {
            val text = viewModel.reflowPageText(state.pageIndex)
            clipboard.setText(AnnotatedString(text))
            viewModel.showCopiedMessage()
            viewModel.setSheet(ReaderSheet.NONE)
        }
    }
    Box(modifier.background(palette.surfaceAlt)) {
        when {
            state.isLoading -> ReaderLoading()
            state.error != null -> ReaderError(state.error) { viewModel.leaveReader(onClose) }
            state.document != null && state.engine != null -> {
                Column(Modifier.fillMaxSize()) {
                    if (!state.isFullScreen) {
                        ReaderAppBar(
                            state = state,
                            openTabCount = openTabCount,
                            onBack = { viewModel.leaveReader(onClose) },
                            onOpenTabs = onOpenTabs,
                            onToggleBookmark = viewModel::toggleBookmarkCurrentPage,
                            onLayout = { viewModel.setSheet(ReaderSheet.VIEW_LAYOUT) },
                            onBookmarks = { viewModel.setSheet(ReaderSheet.BOOKMARKS) },
                            onHighlights = { viewModel.setSheet(ReaderSheet.HIGHLIGHTS) },
                            onJump = { viewModel.setSheet(ReaderSheet.JUMP_TO_PAGE) },
                            onPickNewDocument = onPickNewTab,
                            onClearPageHighlights = viewModel::clearPageHighlights,
                            onShare = { viewModel.setSheet(ReaderSheet.SHARE) }
                        )
                    }
                    if (!state.isFullScreen && state.findBarVisible) {
                        ReaderFindBar(
                            state = state,
                            onQueryChange = viewModel::setFindQuery,
                            onPrevious = viewModel::previousFindMatch,
                            onNext = viewModel::nextFindMatch,
                            onClose = viewModel::closeFindBar
                        )
                    }
                    if (!state.isFullScreen) state.textExtractionProgress?.let { ReaderExtractionProgress(it) }
                    ReaderViewport(
                        state = state,
                        engine = state.engine,
                        viewModel = viewModel,
                        epubImageProvider = epubImageProvider,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onPageTap = { if (state.isFullScreen) viewModel.toggleFullScreen() },
                        onTapAnnotation = viewModel::tapPage,
                        onLongPressText = viewModel::selectTextAt,
                        onSelectionHandleDrag = viewModel::moveSelectionHandle
                    )
                    Column(Modifier.navigationBarsPadding()) {
                        ReaderFooter(
                            state = state,
                            palette = palette,
                            onJump = { viewModel.setSheet(ReaderSheet.JUMP_TO_PAGE) },
                            onToggleBookmark = viewModel::toggleBookmarkCurrentPage,
                            onWake = { if (state.isFullScreen) viewModel.toggleFullScreen() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!state.isFullScreen) {
                            val selectedHighlight = state.highlights.firstOrNull { it.id == state.selectedHighlightId }
                            if (selectedHighlight != null) {
                                HighlightContextBar(
                                    highlight = selectedHighlight,
                                    onColor = { color -> viewModel.recolorHighlight(selectedHighlight.id, color) },
                                    onRemove = { viewModel.removeHighlight(selectedHighlight.id) },
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(selectedHighlight.text))
                                        viewModel.showCopiedMessage()
                                    },
                                    onDone = { viewModel.setSelectedHighlight(null) }
                                )
                            } else {
                                ReaderToolbar(
                                    state = state,
                                    onLayout = { viewModel.setSheet(ReaderSheet.VIEW_LAYOUT) },
                                    onFind = viewModel::toggleFindBar,
                                    onZoom = viewModel::stepZoom,
                                    onFullScreen = viewModel::toggleFullScreen,
                                    onShare = { viewModel.setSheet(ReaderSheet.SHARE) },
                                    onUndo = viewModel::undoAnnotation,
                                    onRedo = viewModel::redoAnnotation,
                                    onHighlightMenu = viewModel::toggleHighlightMenu,
                                    onHighlightColor = viewModel::selectHighlightColor,
                                    onHighlights = { viewModel.setSheet(ReaderSheet.HIGHLIGHTS) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                if (state.warning != null && !state.isFullScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(state.warning, style = LeafType.supporting, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            else -> ReaderLoading()
        }
        ReaderSheetHost(
            state = state,
            onDismiss = { viewModel.setSheet(ReaderSheet.NONE) },
            onJump = { page -> viewModel.jumpToPage(page); viewModel.setSheet(ReaderSheet.NONE) },
            onDirection = viewModel::setScrollDirection,
            onTheme = viewModel::setReadingTheme,
            onZoom = viewModel::setZoom,
            onReflow = viewModel::setReflow,
            onTypeface = viewModel::setTypeface,
            onFullScreen = { viewModel.setSheet(ReaderSheet.NONE); viewModel.toggleFullScreen() },
            onShareDocument = shareCurrentDocument,
            onSharePageImage = sharePageImage,
            onShareSelection = shareSelection,
            onCopyFilePath = copyFilePath,
            onCopyPageText = copyPageText,
            onAddCurrentBookmark = viewModel::toggleBookmarkCurrentPage,
            onJumpBookmark = viewModel::jumpToBookmark,
            onDeleteBookmark = viewModel::deleteBookmark,
            onJumpHighlight = viewModel::jumpToHighlight,
            onDeleteHighlight = viewModel::removeHighlight,
            onRequestClearAllHighlights = { viewModel.setSheet(ReaderSheet.CONFIRM_CLEAR_HIGHLIGHTS) },
            onClearAllHighlights = viewModel::clearAllHighlights
        )
        state.snackbar?.let { message ->
            androidx.compose.runtime.key(message.message) {
                app.leaf.reader.core.ui.components.LeafSnackbar(
                    message = message.message,
                    actionLabel = if (message.canUndo) stringResource(R.string.action_undo) else null,
                    onAction = if (message.canUndo) viewModel::undoAnnotation else null,
                    onDismiss = viewModel::dismissSnackbar,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                )
            }
        }
    }
}

private suspend fun shareReaderPageImage(
    context: android.content.Context,
    engine: DocumentEngine,
    pageIndex: Int,
    visiblePage: android.graphics.Bitmap? = null
) {
    val image = visiblePage ?: engine.renderPreview(pageIndex, maxEdgePixels = 1600)
    val uri = try {
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(shareDir, "page-${UUID.randomUUID()}.png")
        withContext(Dispatchers.IO) {
            file.outputStream().use { output ->
                if (!image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IllegalStateException("Could not encode the page image")
                }
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } finally {
        if (visiblePage == null) engine.releaseBitmap(image) else image.recycle()
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, context.getString(R.string.reader_share_page_image), uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.reader_share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun sharePlainText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.reader_share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun ReaderLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.reader_loading), style = LeafType.supporting, color = currentReadingPalette.textMuted)
        }
    }
}

@Composable
private fun ReaderError(message: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.reader_error_title), style = LeafType.screenTitle, color = MaterialTheme.colorScheme.onSurface)
        Text(message, style = LeafType.body, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        TextButton(onClick = onClose, modifier = Modifier.padding(top = 18.dp)) {
            Text(stringResource(R.string.reader_back_to_library))
        }
    }
}

@Composable
private fun ReaderViewport(
    state: ReaderContentState,
    engine: DocumentEngine,
    viewModel: ReaderViewModel,
    epubImageProvider: ReaderImageProvider,
    modifier: Modifier,
    onPageTap: () -> Unit,
    onTapAnnotation: (Int, Float, Float) -> Unit,
    onLongPressText: (Int, Float, Float) -> Unit,
    onSelectionHandleDrag: (Boolean, Float, Float) -> Unit
) {
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    Box(modifier.onGloballyPositioned { viewportBounds = it.boundsInWindow() }) {
        when {
            state.document?.type == DocType.EPUB && engine is EpubDocumentEngine -> {
                EpubNavigatorSurface(state, engine, viewModel, epubImageProvider)
            }
            state.reflow -> {
                ReflowReaderColumn(state, viewModel)
            }
            state.scrollDir == ScrollDir.VERTICAL -> {
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = state.pageIndex.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
            )
            LaunchedEffect(state.document?.id, state.pageCount) {
                if (state.pageCount > 0) {
                    listState.scrollToItem(state.pageIndex.coerceIn(0, state.pageCount - 1))
                    if (state.scrollFraction > 0f) {
                        androidx.compose.runtime.withFrameNanos { }
                        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == state.pageIndex }
                        if (item != null) listState.scrollToItem(item.index, (item.size * state.scrollFraction).roundToInt())
                    }
                }
            }
            LaunchedEffect(listState, state.positionRequestId) {
                if (state.positionRequestId > 0 && state.pageCount > 0) {
                    val page = state.pageIndex.coerceIn(0, state.pageCount - 1)
                    listState.scrollToItem(page)
                    if (state.scrollFraction > 0f) {
                        androidx.compose.runtime.withFrameNanos { }
                        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }
                        if (item != null) listState.scrollToItem(page, (item.size * state.scrollFraction).roundToInt())
                    }
                }
            }
            LaunchedEffect(listState, state.document?.id) {
                snapshotFlow {
                    val item = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                    item?.let { it.index to if (it.size > 0) (-it.offset.toFloat() / it.size).coerceIn(0f, 1f) else 0f }
                }.collect { current ->
                    current?.let { (page, fraction) -> viewModel.updatePosition(page, fraction) }
                }
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.pageCount, key = { "pdf-page-$it" }) { page ->
                    PdfPageSlot(
                        engine = engine,
                        pageIndex = page,
                        viewportBounds = viewportBounds,
                        renderTiles = if (state.pageCount > ReaderViewModel.MAX_PAGES) {
                            page == state.pageIndex
                        } else {
                            page in (state.pageIndex - 1..state.pageIndex + 1)
                        },
                        zoom = state.zoom,
                        panX = state.panX,
                        panY = state.panY,
                        findMatches = state.findMatches,
                        activeFindIndex = state.findIndex,
                        findQuery = state.findQuery,
                        highlights = state.highlights,
                        selection = state.selection,
                        selectedHighlightId = state.selectedHighlightId,
                        highlightColor = state.highlightColor,
                        onZoom = viewModel::setZoom,
                        onPan = viewModel::updatePan,
                        onPageTap = onPageTap,
                        onAnnotationTap = { x, y -> onTapAnnotation(page, x, y) },
                        onLongPressText = { x, y -> onLongPressText(page, x, y) },
                        onSelectionHandleDrag = onSelectionHandleDrag,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }
            else -> {
                HorizontalReaderPager(
                    state = state,
                    engine = engine,
                    viewModel = viewModel,
                    viewportBounds = viewportBounds,
                    onPageTap = onPageTap
                )
            }
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun EpubNavigatorSurface(
    state: ReaderContentState,
    engine: EpubDocumentEngine,
    viewModel: ReaderViewModel,
    imageProvider: ReaderImageProvider
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return
    val manager = activity.supportFragmentManager
    val containerId = remember { View.generateViewId() }
    val containerState = remember(engine) { mutableStateOf<androidx.fragment.app.FragmentContainerView?>(null) }
    val fragmentTag = remember(engine) { "leaf-epub-${state.document?.id}" }
    val navigatorState = remember(engine) { mutableStateOf<EpubNavigatorFragment?>(null) }
    val palette = currentReadingPalette
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val selectionActionModeCallback = remember(engine, context, viewModel, navigatorState) {
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: android.view.Menu): Boolean {
                menu.add(0, EPUB_ACTION_COPY, 0, context.getString(R.string.action_copy))
                menu.add(0, EPUB_ACTION_SHARE, 1, context.getString(R.string.reader_share_selected_text))
                menu.add(0, EPUB_ACTION_HIGHLIGHT, 2, context.getString(R.string.reader_highlight_label))
                navigatorState.value?.lifecycleScope?.launch {
                    val selected = navigatorState.value?.currentSelection() ?: return@launch
                    viewModel.setEpubSelection(selected.toReaderSelection(engine))
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: android.view.Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: android.view.MenuItem): Boolean {
                val navigator = navigatorState.value ?: return false
                navigator.lifecycleScope.launch {
                    val selected = navigator.currentSelection() ?: return@launch
                    val readerSelection = selected.toReaderSelection(engine)
                    viewModel.setEpubSelection(readerSelection)
                    when (item.itemId) {
                        EPUB_ACTION_COPY -> {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as? android.content.ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText(documentSelectionLabel(state), readerSelection.text))
                            viewModel.showCopiedMessage()
                            navigator.clearSelection()
                        }
                        EPUB_ACTION_SHARE -> sharePlainText(context, readerSelection.text)
                        EPUB_ACTION_HIGHLIGHT -> viewModel.addHighlight(readerSelection, viewModel.state.value.highlightColor)
                    }
                }
                mode.finish()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) = Unit
        }
    }
    val decorationListener = remember(viewModel) {
        object : DecorableNavigator.Listener {
            override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                val highlightId = event.decoration.extras["leafId"] as? String ?: event.decoration.id
                viewModel.setSelectedHighlight(highlightId)
                return true
            }
        }
    }

    AndroidView(
        factory = { viewContext ->
            androidx.fragment.app.FragmentContainerView(viewContext).apply {
                id = containerId
                setBackgroundColor(palette.surface.toArgb())
            }.also { containerState.value = it }
        },
        update = { container -> container.setBackgroundColor(palette.surface.toArgb()) },
        modifier = Modifier.fillMaxSize()
    )
    LaunchedEffect(manager, fragmentTag, containerState.value) {
        val container = containerState.value ?: return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        var navigator = manager.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
        if (navigator == null && !manager.isStateSaved) {
            manager.fragmentFactory = engine.createFragmentFactory(
                pageIndex = state.pageIndex,
                readingTheme = state.readingTheme,
                typeface = state.typeface,
                zoom = state.zoom,
                reflow = state.reflow,
                scrollDir = state.scrollDir,
                systemDark = systemDark,
                onPageChanged = { locator -> viewModel.updatePosition(engine.pageIndexFor(locator), 0f) },
                selectionActionModeCallback = selectionActionModeCallback
            )
            manager.beginTransaction()
                .replace(container.id, EpubNavigatorFragment::class.java, Bundle(), fragmentTag)
                .commitNow()
            navigator = manager.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
        }
        navigatorState.value = navigator
    }
    DisposableEffect(manager, fragmentTag) {
        onDispose {
            runCatching {
                manager.findFragmentByTag(fragmentTag)?.let { fragment ->
                    manager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
                }
            }
            navigatorState.value = null
        }
    }
    val navigator = navigatorState.value
    DisposableEffect(navigator, imageProvider) {
        imageProvider.capture = navigator?.let { active -> { captureView(active.view) } }
        onDispose { imageProvider.capture = null }
    }
    DisposableEffect(navigator, decorationListener) {
        val decorable = navigator as? DecorableNavigator
        decorable?.addDecorationListener(EPUB_HIGHLIGHT_GROUP, decorationListener)
        onDispose { decorable?.removeDecorationListener(decorationListener) }
    }
    LaunchedEffect(navigator, state.highlights, state.selectedHighlightId, palette) {
        val decorable = navigator as? DecorableNavigator ?: return@LaunchedEffect
        val decorations = state.highlights.mapNotNull { highlight ->
            val serialized = highlight.cfiRange ?: return@mapNotNull null
            val locator = runCatching { Locator.fromJSON(JSONObject(serialized)) }.getOrNull()
                ?: return@mapNotNull null
            val tint = HighlightSwatches.getValue(highlight.color).fillFor(palette).toArgb()
            Decoration(
                id = highlight.id,
                locator = locator,
                style = Decoration.Style.Highlight(tint = tint, isActive = highlight.id == state.selectedHighlightId),
                extras = mapOf("leafId" to highlight.id)
            )
        }
        decorable.applyDecorations(decorations, EPUB_HIGHLIGHT_GROUP)
    }
    LaunchedEffect(navigator, state.positionRequestId, state.pageIndex, state.selectedHighlightId, state.highlights) {
        if (state.positionRequestId > 0) {
            val target = state.highlights.firstOrNull { it.id == state.selectedHighlightId }
                ?.cfiRange?.let { serialized ->
                    runCatching { Locator.fromJSON(JSONObject(serialized)) }.getOrNull()
                } ?: engine.locatorAt(state.pageIndex)
            navigator?.go(target)
        }
    }
    LaunchedEffect(
        navigator,
        state.readingTheme,
        state.typeface,
        state.zoom,
        state.reflow,
        state.scrollDir,
        systemDark
    ) {
        if (navigator != null) {
            engine.submitPreferences(
                navigator,
                state.readingTheme,
                state.typeface,
                state.zoom,
                state.reflow,
                state.scrollDir,
                systemDark
            )
        }
    }
    LaunchedEffect(navigator, state.findQuery, state.findIndex, state.pageIndex, state.findMatches) {
        if (navigator != null) {
            val currentHref = engine.locatorAt(state.pageIndex).href.toString()
            val activeMatch = state.findMatches.getOrNull(state.findIndex)
            val activeOccurrence = activeMatch?.takeIf {
                engine.locatorAt(it.pageIndex).href.toString() == currentHref
            }?.let {
                state.findMatches.take(state.findIndex).count { match ->
                    engine.locatorAt(match.pageIndex).href.toString() == currentHref
                }
            }
            navigator.evaluateJavascript(epubFindScript(state.findQuery, activeOccurrence))
        }
    }
}

private fun epubFindScript(query: String, activeOccurrence: Int?): String {
    val quotedQuery = JSONObject.quote(query)
    val active = activeOccurrence?.toString() ?: "-1"
    return """
        (function() {
          var q = $quotedQuery;
          document.querySelectorAll('[data-leaf-find="true"]').forEach(function(mark) {
            var parent = mark.parentNode;
            while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
            parent.removeChild(mark);
          });
          if (!q) return "";
          var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode: function(node) {
              var tag = node.parentElement && node.parentElement.tagName;
              return tag && /^(SCRIPT|STYLE|NOSCRIPT)$/.test(tag) ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT;
            }
          });
          var nodes = [], node;
          while (node = walker.nextNode()) nodes.push(node);
          var lower = q.toLocaleLowerCase(), hit = 0;
          nodes.forEach(function(textNode) {
            var source = textNode.nodeValue || "";
            var sourceLower = source.toLocaleLowerCase(), offsets = [], from = 0, found;
            while ((found = sourceLower.indexOf(lower, from)) !== -1) {
              offsets.push(found);
              from = found + Math.max(1, q.length);
            }
            for (var i = offsets.length - 1; i >= 0; i--) {
              var start = offsets[i], end = start + q.length;
              var range = document.createRange();
              range.setStart(textNode, start); range.setEnd(textNode, end);
              var mark = document.createElement("span");
              mark.setAttribute("data-leaf-find", "true");
              if (hit + i === $active) {
                mark.style.backgroundColor = "#FF8A3C";
                mark.style.color = "#FFFFFF";
              } else {
                mark.style.backgroundColor = "rgba(242,205,85,0.60)";
                mark.style.outline = "1px solid #B87919";
              }
              range.surroundContents(mark);
            }
            hit += offsets.length;
          });
          return String(hit);
        })();
    """.trimIndent()
}

private fun org.readium.r2.navigator.Selection.toReaderSelection(engine: EpubDocumentEngine): ReaderSelection =
    ReaderSelection(
        pageIndex = engine.pageIndexFor(locator),
        text = locator.text.highlight.orEmpty(),
        bounds = emptyList(),
        startWord = 0,
        endWord = 0,
        cfiRange = locator.toJSON().toString()
    )

private fun documentSelectionLabel(state: ReaderContentState): String = state.document?.name.orEmpty()

private const val EPUB_HIGHLIGHT_GROUP = "leaf-highlights"
private const val EPUB_ACTION_COPY = 0x4c01
private const val EPUB_ACTION_SHARE = 0x4c02
private const val EPUB_ACTION_HIGHLIGHT = 0x4c03

@Composable
private fun ReflowReaderColumn(state: ReaderContentState, viewModel: ReaderViewModel) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.pageIndex.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
    )
    LaunchedEffect(state.document?.id, state.pageCount) {
        if (state.pageCount > 0) {
            listState.scrollToItem(state.pageIndex.coerceIn(0, state.pageCount - 1))
            if (state.scrollFraction > 0f) {
                androidx.compose.runtime.withFrameNanos { }
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == state.pageIndex }
                if (item != null) listState.scrollToItem(item.index, (item.size * state.scrollFraction).roundToInt())
            }
        }
    }
    LaunchedEffect(listState, state.positionRequestId) {
        if (state.positionRequestId > 0 && state.pageCount > 0) {
            val page = state.pageIndex.coerceIn(0, state.pageCount - 1)
            listState.scrollToItem(page)
            if (state.scrollFraction > 0f) {
                androidx.compose.runtime.withFrameNanos { }
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == page }
                if (item != null) listState.scrollToItem(page, (item.size * state.scrollFraction).roundToInt())
            }
        }
    }
    LaunchedEffect(listState, state.document?.id) {
        snapshotFlow {
            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            item?.let { it.index to if (it.size > 0) (-it.offset.toFloat() / it.size).coerceIn(0f, 1f) else 0f }
        }.collect { current ->
            current?.let { (page, fraction) -> viewModel.updatePosition(page, fraction) }
        }
    }
    val palette = currentReadingPalette
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(palette.surfaceAlt),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(state.pageCount, key = { "reflow-page-$it" }) { page ->
            ReflowPage(state, page, viewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReflowPage(state: ReaderContentState, pageIndex: Int, viewModel: ReaderViewModel) {
    val text by produceState(initialValue = "", state.document?.id, pageIndex) {
        value = try {
            viewModel.reflowPageText(pageIndex)
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            ""
        }
    }
    val palette = currentReadingPalette
    val highlightRanges = state.highlights.filter { it.page == pageIndex && it.textRange != null }
    val localFindIndices = state.findMatches.indices.filter { state.findMatches[it].pageIndex == pageIndex }
    val findRanges = if (state.findQuery.isBlank()) emptyList() else text.findRanges(state.findQuery)
    val selection = state.selection?.takeIf { it.pageIndex == pageIndex }
    val annotated = remember(
        text,
        highlightRanges,
        localFindIndices,
        findRanges,
        state.findIndex,
        selection?.textRange,
        state.selectedHighlightId,
        state.highlightColor,
        palette
    ) {
        AnnotatedString.Builder(text).apply {
            highlightRanges.forEach { highlight ->
                val range = highlight.textRange ?: return@forEach
                val start = range.first.coerceIn(0, text.length)
                val end = (range.last + 1).coerceIn(start, text.length)
                if (end > start) {
                    val fill = HighlightSwatches.getValue(highlight.color).fillFor(palette)
                    addStyle(
                        SpanStyle(
                            background = fill,
                            textDecoration = if (highlight.id == state.selectedHighlightId) TextDecoration.Underline else null
                        ),
                        start,
                        end
                    )
                }
            }
            findRanges.forEachIndexed { index, range ->
                val start = range.first
                val end = range.last + 1
                val isCurrent = localFindIndices.getOrNull(index) == state.findIndex
                addStyle(
                    SpanStyle(
                        background = if (isCurrent) androidx.compose.ui.graphics.Color(0xFFFF8A3C)
                        else androidx.compose.ui.graphics.Color(0x99F2CD55),
                        color = if (isCurrent) androidx.compose.ui.graphics.Color.White else palette.text
                    ),
                    start,
                    end
                )
            }
            selection?.textRange?.let { range ->
                val start = range.first.coerceIn(0, text.length)
                val end = (range.last + 1).coerceIn(start, text.length)
                if (end > start) {
                    val fill = HighlightSwatches.getValue(state.highlightColor).fillFor(palette)
                    addStyle(SpanStyle(background = fill), start, end)
                }
            }
        }.toAnnotatedString()
    }
    val baseStyle = if (state.typeface == ReadingTypeface.SERIF) {
        LeafType.readingReflow
    } else {
        LeafType.readingReflow.copy(
            fontFamily = FontFamily.SansSerif,
            lineHeight = 18.sp * 1.7f
        )
    }
    val textStyle = baseStyle.copy(
        fontSize = baseStyle.fontSize * state.zoom,
        lineHeight = baseStyle.lineHeight * state.zoom,
        color = palette.text
    )
    var layout by remember(text, pageIndex) { mutableStateOf<TextLayoutResult?>(null) }
    var selectionAnchor by remember(text, pageIndex) { mutableStateOf<Int?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        BasicText(
            text = annotated,
            style = textStyle,
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth()
                .pointerInput(pageIndex, text) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null && text.isNotEmpty()) {
                            layout?.getOffsetForPosition(up.position)
                                ?.coerceIn(0, text.lastIndex)
                                ?.let { viewModel.tapReflowText(pageIndex, it) }
                        }
                    }
                }
                .pointerInput(pageIndex, text) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { position ->
                        if (text.isEmpty()) return@detectDragGesturesAfterLongPress
                        val offset = layout?.getOffsetForPosition(position)?.coerceIn(0, text.lastIndex) ?: return@detectDragGesturesAfterLongPress
                        val range = text.wordRangeAt(offset)
                        selectionAnchor = range.first
                        viewModel.selectTextRange(pageIndex, range.first, range.last + 1)
                    },
                    onDrag = { change, _ ->
                        val anchor = selectionAnchor ?: return@detectDragGesturesAfterLongPress
                        val cursor = layout?.getOffsetForPosition(change.position)?.coerceIn(0, text.length) ?: return@detectDragGesturesAfterLongPress
                        val start = minOf(anchor, cursor)
                        val end = maxOf(anchor + 1, cursor + if (cursor < anchor) 0 else 1)
                        viewModel.selectTextRange(pageIndex, start, end)
                        change.consume()
                    },
                    onDragEnd = { selectionAnchor = null },
                    onDragCancel = { selectionAnchor = null }
                )
            }
        )
        if (pageIndex < state.pageCount - 1) {
            Box(
                Modifier.fillMaxWidth().padding(top = 22.dp).height(1.dp)
                    .background(palette.rule.copy(alpha = 0.75f))
            )
        }
    }
}

private fun String.findRanges(query: String): List<IntRange> {
    if (query.isBlank()) return emptyList()
    val result = mutableListOf<IntRange>()
    var start = 0
    while (start <= length - query.length) {
        val match = indexOf(query, start, ignoreCase = true)
        if (match < 0) break
        result += match until (match + query.length)
        start = match + query.length
    }
    return result
}

private fun String.wordRangeAt(index: Int): IntRange {
    if (isEmpty()) return 0..0
    var cursor = index.coerceIn(0, lastIndex)
    if (this[cursor].isWhitespace() && cursor + 1 < length && !this[cursor + 1].isWhitespace()) cursor++
    if (this[cursor].isWhitespace() && cursor > 0 && !this[cursor - 1].isWhitespace()) cursor--
    var start = cursor
    var end = cursor
    while (start > 0 && !this[start - 1].isWhitespace()) start--
    while (end + 1 < length && !this[end + 1].isWhitespace()) end++
    return start..end
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalReaderPager(
    state: ReaderContentState,
    engine: DocumentEngine,
    viewModel: ReaderViewModel,
    viewportBounds: Rect?,
    onPageTap: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.pageIndex.coerceAtLeast(0), pageCount = { state.pageCount })
    LaunchedEffect(state.document?.id, state.pageCount) {
        if (state.pageCount > 0) pagerState.scrollToPage(state.pageIndex.coerceIn(0, state.pageCount - 1))
    }
    LaunchedEffect(pagerState, state.positionRequestId) {
        if (state.positionRequestId > 0 && state.pageCount > 0) {
            pagerState.scrollToPage(state.pageIndex.coerceIn(0, state.pageCount - 1))
        }
    }
    LaunchedEffect(pagerState, state.document?.id) {
        snapshotFlow { pagerState.currentPage }.collect { page -> viewModel.updatePosition(page, 0f) }
    }
    HorizontalPager(
        state = pagerState,
        pageSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        key = { it },
        modifier = Modifier.fillMaxSize()
    ) { page ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PdfPageSlot(
                engine = engine,
                pageIndex = page,
                viewportBounds = viewportBounds,
                renderTiles = if (state.pageCount > ReaderViewModel.MAX_PAGES) {
                    page == state.pageIndex
                } else {
                    page in (state.pageIndex - 1..state.pageIndex + 1)
                },
                zoom = state.zoom,
                panX = state.panX,
                panY = state.panY,
                findMatches = state.findMatches,
                activeFindIndex = state.findIndex,
                findQuery = state.findQuery,
                highlights = state.highlights,
                selection = state.selection,
                selectedHighlightId = state.selectedHighlightId,
                highlightColor = state.highlightColor,
                onZoom = viewModel::setZoom,
                onPan = viewModel::updatePan,
                onPageTap = onPageTap,
                onAnnotationTap = { x, y -> viewModel.tapPage(page, x, y) },
                onLongPressText = { x, y -> viewModel.selectTextAt(page, x, y) },
                onSelectionHandleDrag = viewModel::moveSelectionHandle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PdfPageSlot(
    engine: DocumentEngine,
    pageIndex: Int,
    viewportBounds: Rect?,
    renderTiles: Boolean,
    zoom: Float,
    panX: Float,
    panY: Float,
    findMatches: List<ReaderFindMatch>,
    activeFindIndex: Int,
    findQuery: String,
    highlights: List<app.leaf.reader.core.model.Highlight>,
    selection: ReaderSelection?,
    selectedHighlightId: String?,
    highlightColor: app.leaf.reader.core.model.HighlightColor,
    onZoom: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onPageTap: () -> Unit,
    onAnnotationTap: (Float, Float) -> Unit,
    onLongPressText: (Float, Float) -> Unit,
    onSelectionHandleDrag: (Boolean, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var metrics by remember(engine, pageIndex) { mutableStateOf<PageMetrics?>(null) }
    var metricsError by remember(engine, pageIndex) { mutableStateOf(false) }
    LaunchedEffect(engine, pageIndex) {
        try {
            metrics = engine.pageMetrics(pageIndex)
            metricsError = false
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            metrics = null
            metricsError = true
        }
    }
    val pageMetrics = metrics
    if (pageMetrics == null) {
        if (metricsError) {
            ReaderPageError(modifier.height(300.dp))
        } else {
            Box(modifier.height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
            }
        }
    } else {
        BoxWithConstraints(modifier) {
            val density = LocalDensity.current
            val pageWidth = (maxWidth - 40.dp).coerceAtMost(720.dp).coerceAtLeast(200.dp)
            val fitScale = with(density) { pageWidth.toPx() } / pageMetrics.widthPoints
            val pageHeight = with(density) { (pageMetrics.heightPoints * fitScale).toDp() }
            PdfPageCanvas(
                engine = engine,
                pageIndex = pageIndex,
                metrics = pageMetrics,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                fitScale = fitScale,
                viewportBounds = viewportBounds,
                renderTiles = renderTiles,
                zoom = zoom,
                panX = panX,
                panY = panY,
                findMatches = findMatches,
                activeFindIndex = activeFindIndex,
                findQuery = findQuery,
                highlights = highlights,
                selection = selection,
                selectedHighlightId = selectedHighlightId,
                highlightColor = highlightColor,
                onZoom = onZoom,
                onPan = onPan,
                onPageTap = onPageTap,
                onAnnotationTap = onAnnotationTap,
                onLongPressText = onLongPressText,
                onSelectionHandleDrag = onSelectionHandleDrag,
                modifier = Modifier.width(pageWidth).height(pageHeight).align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PdfPageCanvas(
    engine: DocumentEngine,
    pageIndex: Int,
    metrics: PageMetrics,
    pageWidth: Dp,
    pageHeight: Dp,
    fitScale: Float,
    viewportBounds: Rect?,
    renderTiles: Boolean,
    zoom: Float,
    panX: Float,
    panY: Float,
    findMatches: List<ReaderFindMatch>,
    activeFindIndex: Int,
    findQuery: String,
    highlights: List<app.leaf.reader.core.model.Highlight>,
    selection: ReaderSelection?,
    selectedHighlightId: String?,
    highlightColor: app.leaf.reader.core.model.HighlightColor,
    onZoom: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onPageTap: () -> Unit,
    onAnnotationTap: (Float, Float) -> Unit,
    onLongPressText: (Float, Float) -> Unit,
    onSelectionHandleDrag: (Boolean, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val palette = currentReadingPalette
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    LaunchedEffect(engine, palette, outlineVariant) {
        engine.setReaderColors(
            background = palette.surface.toArgb(),
            surfaceAlt = palette.surfaceAlt.toArgb(),
            text = palette.text.toArgb(),
            outlineVariant = outlineVariant.toArgb()
        )
    }
    var preview by remember(engine, pageIndex, palette) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var previewError by remember(engine, pageIndex, palette) { mutableStateOf(false) }
    var tileError by remember(engine, pageIndex, palette) { mutableStateOf(false) }
    val tiles = remember(engine, pageIndex, palette) { mutableStateMapOf<RenderTile, android.graphics.Bitmap>() }
    var pageBounds by remember(engine, pageIndex) { mutableStateOf<Rect?>(null) }
    val pageWidthPx = PdfTileGrid.pagePixels(metrics, fitScale * zoom).first
    val pageHeightPx = PdfTileGrid.pagePixels(metrics, fitScale * zoom).second
    val baseWidthPx = with(density) { pageWidth.toPx() }
    val baseHeightPx = with(density) { pageHeight.toPx() }
    val panOffset = Offset(panX * baseWidthPx, panY * baseHeightPx)
    val transformState: TransformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextZoom = (zoom * zoomChange).coerceIn(0.7f, 2.5f)
        onZoom(nextZoom)
        val limit = (nextZoom - 1f).coerceAtLeast(0f) / 2f
        if (nextZoom > 1f) {
            onPan(
                (panX + panChange.x / baseWidthPx).coerceIn(-limit, limit),
                (panY + panChange.y / baseHeightPx).coerceIn(-limit, limit)
            )
        }
    }
    val sourceVisibleRect = remember(pageBounds, viewportBounds, zoom, panOffset, pageWidthPx, pageHeightPx) {
        visiblePageRegion(pageBounds, viewportBounds, zoom, panOffset, pageWidthPx, pageHeightPx)
    }
    LaunchedEffect(engine, pageIndex, sourceVisibleRect != null, palette) {
        if (sourceVisibleRect == null) {
            preview = null
            previewError = false
            return@LaunchedEffect
        }
        if (preview != null) return@LaunchedEffect
        previewError = false
        try {
            val rendered = withContext(NonCancellable) { engine.renderPreview(pageIndex) }
            if (currentCoroutineContext().isActive) preview = rendered else engine.releaseBitmap(rendered)
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            previewError = true
        }
    }
    DisposableEffect(engine, preview) {
        val leasedPreview = preview
        onDispose { leasedPreview?.let(engine::releaseBitmap) }
    }
    LaunchedEffect(engine, pageIndex, renderTiles, zoom, sourceVisibleRect, pageWidthPx, pageHeightPx, palette) {
        if (renderTiles && sourceVisibleRect != null) delay(120L)
        val requested = if (renderTiles) {
            sourceVisibleRect?.let { PdfTileGrid.visibleTiles(pageWidthPx, pageHeightPx, it) }.orEmpty()
        } else {
            emptyList()
        }
        val requestedSet = requested.toSet()
        tiles.keys.filter { it !in requestedSet }.forEach { tiles.remove(it) }
        tileError = false
        for (tile in requested) {
            if (!currentCoroutineContext().isActive) return@LaunchedEffect
            if (tile !in tiles) {
                try {
                    val rendered = withContext(NonCancellable) {
                        engine.renderTile(pageIndex, fitScale * zoom, tile)
                    }
                    if (currentCoroutineContext().isActive) tiles[tile] = rendered
                    else {
                        engine.releaseBitmap(rendered)
                        return@LaunchedEffect
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    tileError = true
                }
            }
        }
    }
    Box(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .onGloballyPositioned { pageBounds = it.boundsInWindow() }
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .pointerInput(transformState, zoom, panOffset, baseWidthPx, baseHeightPx) {
                detectTapGestures(
                    onDoubleTap = { onZoom(if (zoom < 1.05f) 1.6f else 1f) },
                    onTap = { position ->
                        val point = normalizedPagePoint(position, baseWidthPx, baseHeightPx, zoom, panOffset)
                        onPageTap()
                        onAnnotationTap(point.x, point.y)
                    },
                    onLongPress = { position ->
                        val point = normalizedPagePoint(position, baseWidthPx, baseHeightPx, zoom, panOffset)
                        onLongPressText(point.x, point.y)
                    }
                )
            }
            .pointerInput(zoom) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll && event.keyboardModifiers.isCtrlPressed) {
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (delta != 0f) onZoom((zoom * kotlin.math.exp(-delta * 0.12f)).coerceIn(0.7f, 2.5f))
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            .transformable(state = transformState, canPan = { zoom > 1f })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panOffset.x
                    translationY = panOffset.y
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            preview?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.reader_page_accessibility, pageIndex + 1),
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White))
            tiles.forEach { (tile, bitmap) ->
                androidx.compose.runtime.key(tile) {
                    DisposableEffect(engine, bitmap) {
                        onDispose { engine.releaseBitmap(bitmap) }
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (tile.left / zoom).roundToInt(),
                                    (tile.top / zoom).roundToInt()
                                )
                            }
                            .width(with(density) { (tile.width / zoom).toDp() })
                            .height(with(density) { (tile.height / zoom).toDp() })
                    )
                }
            }
            FindMatchOverlay(
                pageIndex = pageIndex,
                matches = findMatches,
                activeIndex = activeFindIndex,
                query = findQuery
            )
            AnnotationOverlay(
                pageIndex = pageIndex,
                highlights = highlights,
                selection = selection,
                selectedHighlightId = selectedHighlightId,
                highlightColor = highlightColor,
                onHandleDrag = onSelectionHandleDrag
            )
        }
        if ((previewError && preview == null) || tileError) {
            ReaderPageError(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f)))
        }
    }
}

@Composable
private fun FindMatchOverlay(
    pageIndex: Int,
    matches: List<ReaderFindMatch>,
    activeIndex: Int,
    query: String
) {
    if (matches.none { it.pageIndex == pageIndex && it.bounds.isNotEmpty() }) return
    Canvas(Modifier.fillMaxSize()) {
        val ordinaryFill = androidx.compose.ui.graphics.Color(0xFFF2CD55).copy(alpha = 0.6f)
        val ordinaryOutline = androidx.compose.ui.graphics.Color(0xFFB87919)
        val activeFill = androidx.compose.ui.graphics.Color(0xFFFF8A3C)
        matches.forEachIndexed { matchIndex, match ->
            if (match.pageIndex != pageIndex) return@forEachIndexed
            match.bounds.forEachIndexed { boundIndex, rect ->
                val left = rect.left * size.width
                val top = rect.top * size.height
                val right = rect.right * size.width
                val bottom = rect.bottom * size.height
                val width = right - left
                val height = bottom - top
                if (width <= 0f || height <= 0f) return@forEachIndexed
                val isActive = matchIndex == activeIndex
                drawRect(
                    color = if (isActive) activeFill else ordinaryFill,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
                if (isActive) {
                    if (boundIndex == 0 && query.isNotBlank()) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            textSize = height.coerceAtMost(24.dp.toPx()).coerceAtLeast(10.dp.toPx())
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            val textWidth = measureText(query)
                            if (textWidth > width - 4f && textWidth > 0f) {
                                textScaleX = ((width - 4f).coerceAtLeast(1f) / textWidth).coerceAtLeast(0.1f)
                            }
                        }
                        val baseline = top + height / 2f - (paint.ascent() + paint.descent()) / 2f
                        drawContext.canvas.nativeCanvas.drawText(query, left + 2f, baseline, paint)
                    }
                } else {
                    drawRect(
                        color = ordinaryOutline,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun normalizedPagePoint(
    position: Offset,
    baseWidth: Float,
    baseHeight: Float,
    zoom: Float,
    pan: Offset
): Offset {
    val contentLeft = (baseWidth - baseWidth * zoom) / 2f + pan.x
    val contentTop = (baseHeight - baseHeight * zoom) / 2f + pan.y
    return Offset(
        ((position.x - contentLeft) / (baseWidth * zoom).coerceAtLeast(1f)).coerceIn(0f, 1f),
        ((position.y - contentTop) / (baseHeight * zoom).coerceAtLeast(1f)).coerceIn(0f, 1f)
    )
}

private fun visiblePageRegion(
    pageBounds: Rect?,
    viewportBounds: Rect?,
    zoom: Float,
    pan: Offset,
    pageWidthPx: Int,
    pageHeightPx: Int
): RenderTile? {
    val page = pageBounds ?: return null
    val viewport = viewportBounds ?: return null
    val contentLeft = page.left + (1f - zoom) * page.width / 2f + pan.x
    val contentTop = page.top + (1f - zoom) * page.height / 2f + pan.y
    // The transformed bitmap is clipped to the unscaled page card before it reaches the viewport.
    val clipLeft = maxOf(viewport.left, page.left)
    val clipTop = maxOf(viewport.top, page.top)
    val clipRight = minOf(viewport.right, page.right)
    val clipBottom = minOf(viewport.bottom, page.bottom)
    val intersectionLeft = maxOf(clipLeft, contentLeft)
    val intersectionTop = maxOf(clipTop, contentTop)
    val intersectionRight = minOf(clipRight, contentLeft + page.width * zoom)
    val intersectionBottom = minOf(clipBottom, contentTop + page.height * zoom)
    if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) return null
    val left = ((intersectionLeft - contentLeft) * pageWidthPx / (page.width * zoom)).roundToInt().coerceIn(0, pageWidthPx - 1)
    val top = ((intersectionTop - contentTop) * pageHeightPx / (page.height * zoom)).roundToInt().coerceIn(0, pageHeightPx - 1)
    val width = (((intersectionRight - intersectionLeft) * pageWidthPx / (page.width * zoom)).roundToInt()).coerceAtLeast(1)
    val height = (((intersectionBottom - intersectionTop) * pageHeightPx / (page.height * zoom)).roundToInt()).coerceAtLeast(1)
    return RenderTile(left, top, width.coerceAtMost(pageWidthPx - left), height.coerceAtMost(pageHeightPx - top))
}

@Composable
private fun ReaderFooter(
    state: ReaderContentState,
    palette: ReadingPalette,
    onJump: () -> Unit,
    onToggleBookmark: () -> Unit,
    onWake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressDescription = stringResource(
        R.string.cd_reading_progress,
        state.percentRead,
        state.pageIndex + 1,
        state.pageCount
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surfaceAlt)
            .alpha(if (state.isFullScreen) 0.68f else 1f)
            .clickable(
                enabled = state.isFullScreen,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onWake
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onJump) {
            val pageLabel = when (state.document?.type) {
                null, DocType.PDF, DocType.TXT, DocType.DOCX ->
                    stringResource(R.string.reader_page_of, state.pageIndex + 1, state.pageCount)
                DocType.XLSX, DocType.PPTX -> state.pageLabel
                    ?: stringResource(R.string.reader_page_of, state.pageIndex + 1, state.pageCount)
                DocType.EPUB -> if (state.pageLabel == null) {
                    stringResource(R.string.reader_page_of, state.pageIndex + 1, state.pageCount)
                } else "${state.pageLabel} · ${state.pageIndex + 1}/${state.pageCount}"
            }
            Text(
                pageLabel,
                style = LeafType.supporting,
                color = palette.text
            )
        }
        Box(
            Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .semantics {
                    contentDescription = progressDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(state.percentRead / 100f, 0f..1f)
                }
        ) {
            Box(
                Modifier.fillMaxWidth((state.percentRead / 100f).coerceIn(0f, 1f))
                    .height(4.dp).background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            stringResource(R.string.reader_percent, state.percentRead),
            style = LeafType.supporting,
            color = palette.text
        )
        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(if (state.isCurrentPageBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = stringResource(
                    if (state.isCurrentPageBookmarked) R.string.reader_bookmark_remove
                    else R.string.reader_bookmark_add
                ),
                tint = if (state.isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = quantityText(R.plurals.n_bookmarks, state.bookmarks.size, state.bookmarks.size),
            style = LeafType.chipLabel,
            color = palette.textMuted,
            maxLines = 1
        )
    }
}
