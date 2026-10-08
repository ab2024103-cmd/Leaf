package app.leaf.reader.screenshots

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import app.leaf.reader.FIXED_NOW
import app.leaf.reader.core.format.DocumentEngine
import app.leaf.reader.core.format.DocumentEngineFactory
import app.leaf.reader.core.format.MatchRect
import app.leaf.reader.core.format.PageMetrics
import app.leaf.reader.core.format.RenderTile
import app.leaf.reader.core.format.TextRun
import app.leaf.reader.core.util.LeafClock
import app.leaf.reader.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** Test graph keeps the production database and ViewModel, with deterministic page art. */
class ReaderScreenshotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReaderScreenshotApplication)
            modules(
                appModule,
                module {
                    single<DocumentEngineFactory> { ScreenshotPdfEngineFactory() }
                    single<LeafClock> { LeafClock { FIXED_NOW } }
                }
            )
        }
    }
}

private class ScreenshotPdfEngineFactory : DocumentEngineFactory {
    override suspend fun open(uri: String): DocumentEngine = ScreenshotPdfEngine()
}

private class ScreenshotPdfEngine : DocumentEngine {
    override val pageCount: Int = 6

    override suspend fun pageMetrics(pageIndex: Int) = PageMetrics(widthPoints = 612, heightPoints = 792)

    override suspend fun renderPreview(pageIndex: Int, maxEdgePixels: Int): Bitmap = pageBitmap(306, 396, pageIndex)

    override suspend fun renderTile(pageIndex: Int, scale: Float, tile: RenderTile): Bitmap =
        pageBitmap(tile.width, tile.height, pageIndex, tile.left, tile.top)

    override suspend fun pageText(pageIndex: Int) = listOf(TextRun("Reader page ${pageIndex + 1}"))

    override suspend fun findInPage(pageIndex: Int, query: String) = emptyList<MatchRect>()

    override suspend fun extractAllText(onProgress: suspend (completed: Int, total: Int) -> Unit): List<List<TextRun>> =
        (0 until pageCount).map { page ->
            onProgress(page + 1, pageCount)
            listOf(TextRun("Reader screenshot page ${page + 1}"))
        }

    override fun close() = Unit

    private fun pageBitmap(width: Int, height: Int, page: Int, left: Int = 0, top: Int = 0): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(52, 69, 59); strokeWidth = 2f }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(43, 122, 91) }
        if (left == 0 && top == 0) {
            canvas.drawRect(width * 0.12f, height * 0.10f, width * 0.88f, height * 0.18f, accent)
        }
        for (line in 0 until 12) {
            val y = ((line + 2) * 36f - top).coerceAtLeast(0f)
            if (y in 0f..height.toFloat()) canvas.drawLine(width * 0.12f, y, width * (0.80f - (line % 3) * 0.04f), y, ink)
        }
        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(92, 106, 99); textSize = 18f }
        canvas.drawText("PAGE ${page + 1}", (width * 0.12f).coerceAtMost(width - 12f), (height - 18f).coerceAtLeast(20f), pagePaint)
        return bitmap
    }
}
