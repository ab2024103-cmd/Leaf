package app.leaf.reader.feature.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.leaf.reader.R
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.PageMetrics
import app.leaf.reader.core.format.PdfTileGrid
import app.leaf.reader.core.format.RenderTile
import app.leaf.reader.core.model.ReadingTheme
import app.leaf.reader.core.model.ScrollDir
import app.leaf.reader.core.ui.theme.LocalReadingPalette
import app.leaf.reader.core.ui.theme.LeafType
import app.leaf.reader.core.ui.theme.ReadingPalette
import app.leaf.reader.core.ui.theme.currentReadingPalette
import app.leaf.reader.core.ui.theme.resolve
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    documentId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    viewModel: ReaderViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val activity = view.context as? Activity

    LaunchedEffect(documentId, viewModel) { viewModel.openDocument(documentId) }
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
            state.isFullScreen -> viewModel.toggleFullScreen()
            else -> viewModel.closeReader(onClose)
        }
    }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = remember(state.readingTheme, systemDark) { state.readingTheme.resolve(systemDark) }
    val keyboardModifier = modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && event.key == Key.Escape) {
            when {
                state.sheet != ReaderSheet.NONE -> viewModel.setSheet(ReaderSheet.NONE)
                state.isFullScreen -> viewModel.toggleFullScreen()
                else -> viewModel.closeReader(onClose)
            }
            true
        } else {
            false
        }
    }.focusable()
    CompositionLocalProvider(LocalReadingPalette provides palette) {
        ReaderContent(
            state = state,
            viewModel = viewModel,
            palette = palette,
            onClose = onClose,
            modifier = keyboardModifier
        )
    }
}

@Composable
private fun ReaderContent(
    state: ReaderContentState,
    viewModel: ReaderViewModel,
    palette: ReadingPalette,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(palette.surfaceAlt)) {
        when {
            state.isLoading -> ReaderLoading()
            state.error != null -> ReaderError(state.error) { viewModel.closeReader(onClose) }
            state.document != null && state.engine != null -> {
                Column(Modifier.fillMaxSize()) {
                    if (!state.isFullScreen) ReaderAppBar(state, onBack = { viewModel.closeReader(onClose) }, onLayout = { viewModel.setSheet(ReaderSheet.VIEW_LAYOUT) })
                    if (!state.isFullScreen) state.textExtractionProgress?.let { ReaderExtractionProgress(it) }
                    ReaderViewport(
                        state = state,
                        engine = state.engine,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onPageTap = { if (state.isFullScreen) viewModel.toggleFullScreen() }
                    )
                    Column(Modifier.navigationBarsPadding()) {
                        ReaderFooter(
                            state = state,
                            palette = palette,
                            onJump = { viewModel.setSheet(ReaderSheet.JUMP_TO_PAGE) },
                            onWake = { if (state.isFullScreen) viewModel.toggleFullScreen() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!state.isFullScreen) {
                            ReaderToolbar(
                                state = state,
                                onLayout = { viewModel.setSheet(ReaderSheet.VIEW_LAYOUT) },
                                onZoom = viewModel::stepZoom,
                                onFullScreen = viewModel::toggleFullScreen,
                                modifier = Modifier.fillMaxWidth()
                            )
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
            onFullScreen = { viewModel.setSheet(ReaderSheet.NONE); viewModel.toggleFullScreen() }
        )
    }
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
    modifier: Modifier,
    onPageTap: () -> Unit
) {
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    Box(modifier.onGloballyPositioned { viewportBounds = it.boundsInWindow() }) {
        if (state.scrollDir == ScrollDir.VERTICAL) {
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
                        onZoom = viewModel::setZoom,
                        onPan = viewModel::updatePan,
                        onPageTap = onPageTap,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
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
                onZoom = viewModel::setZoom,
                onPan = viewModel::updatePan,
                onPageTap = onPageTap,
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
    onZoom: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onPageTap: () -> Unit,
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
                onZoom = onZoom,
                onPan = onPan,
                onPageTap = onPageTap,
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
    onZoom: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onPageTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var preview by remember(engine, pageIndex) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var previewError by remember(engine, pageIndex) { mutableStateOf(false) }
    var tileError by remember(engine, pageIndex) { mutableStateOf(false) }
    val tiles = remember(engine, pageIndex) { mutableStateMapOf<RenderTile, android.graphics.Bitmap>() }
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
    LaunchedEffect(engine, pageIndex, sourceVisibleRect != null) {
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
    LaunchedEffect(engine, pageIndex, renderTiles, zoom, sourceVisibleRect, pageWidthPx, pageHeightPx) {
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
            .background(androidx.compose.ui.graphics.Color.White)
            .pointerInput(transformState, zoom) {
                detectTapGestures(
                    onDoubleTap = { onZoom(if (zoom < 1.05f) 1.6f else 1f) },
                    onTap = { onPageTap() }
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
        }
        if ((previewError && preview == null) || tileError) {
            ReaderPageError(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f)))
        }
    }
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
            Text(
                stringResource(R.string.reader_page_of, state.pageIndex + 1, state.pageCount),
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
    }
}

@Composable
private fun ReaderToolbar(
    state: ReaderContentState,
    onLayout: () -> Unit,
    onZoom: (Int) -> Unit,
    onFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        val showZoom = maxWidth >= 380.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReaderControl(R.drawable.ic_view_layout, stringResource(R.string.reader_view_layout_title), onLayout)
            Spacer(Modifier.weight(1f))
            if (showZoom) {
                ReaderControl(R.drawable.ic_zoom_out, stringResource(R.string.reader_zoom_out), { onZoom(-1) })
                Text(
                    stringResource(R.string.reader_zoom_value, (state.zoom * 100).roundToInt()),
                    style = LeafType.supporting,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ReaderControl(R.drawable.ic_zoom_in, stringResource(R.string.reader_zoom_in), { onZoom(1) })
            }
            Spacer(Modifier.weight(1f))
            ReaderControl(
                if (state.isFullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
                stringResource(if (state.isFullScreen) R.string.reader_exit_fullscreen else R.string.reader_fullscreen),
                onFullScreen
            )
        }
    }
}

@Composable
private fun ReaderControl(icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(20.dp))
    }
}
