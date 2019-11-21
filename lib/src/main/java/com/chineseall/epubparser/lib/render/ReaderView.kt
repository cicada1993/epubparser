package com.chineseall.epubparser.lib.render

import android.content.Context
import android.graphics.*
import android.graphics.Color.parseColor
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.html.Chapter
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.util.ScreenUtil
import com.chineseall.epubparser.lib.view.SerifTextView


class ReaderView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var renderContext: RenderContext? = null
    private var pages: MutableList<Page>? = null
    private var curPage = -1
    private var renderReceiver: RenderReceiver? = null
    private var curPageCanvas: Canvas? = null
    private var curPageBitmap: Bitmap? = null
    private var preORnextPageCanvas: Canvas? = null
    private var preORnextPageBitmap: Bitmap? = null
    private var effect: EffectOfSlide? = null
    private val mPaperPaint = Paint()

    init {
        mPaperPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        setWillNotDraw(false)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (effect?.onTouchEvent(event) == true) {
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPaint(mPaperPaint)
        effect?.onDraw(canvas)
    }

    fun render(book: OpfPackage?, chapter: Chapter, receiver: RenderReceiver? = null) {
        renderReceiver = receiver
        val viewWidth = width
        val viewHeight = height
        if (curPageBitmap == null) {
            curPageBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        }
        if (curPageCanvas == null) {
            curPageCanvas = Canvas(curPageBitmap!!)
        }
        if (preORnextPageBitmap == null) {
            preORnextPageBitmap =
                Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        }
        if (preORnextPageCanvas == null) {
            preORnextPageCanvas = Canvas(preORnextPageBitmap!!)
        }
        if (effect == null) {
            effect = EffectOfSlide(context)
            effect!!.effectReceiver = SimpleEffectReceiver()
        }
        effect?.config(viewWidth, viewHeight, curPageBitmap, preORnextPageBitmap)
        // 创建RenderContext
        if (renderContext == null) {
            val textPaint = TextPaint(ANTI_ALIAS_FLAG)
            textPaint.color = parseColor("#3F3737")
            textPaint.textSize = ScreenUtil.spToPx(context, 15f)
            textPaint.typeface = SerifTextView.serifTypefaces[SerifTextView.MEDIUM]
            val canvasWidth = viewWidth * 19 / 20
            val canvasHeight = viewHeight * 19 / 20
            LogUtil.d("readerview size is $canvasWidth / $canvasHeight")
            val renderOptions = RenderOptions(canvasWidth, canvasHeight, textPaint)
            renderOptions.spacingAdd = 10
            renderContext = RenderContext(context, renderOptions, book)
        }
        renderContext?.let {
            // 开始分页
            chapter.render(it)
            it.printPlot()
            pages = it.pages
            renderReceiver?.onPages(chapter.chapterIndex!!, pages!!)
            // 渲染第一页
            effect?.resetData()
            drawPage(curPageCanvas, 1)
            curPage = 1
            invalidate()
            renderReceiver?.onRenderPage(curPage)
        }
    }

    fun clear() {
        curPageCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        preORnextPageCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        curPage = -1
        invalidate()
    }


    fun drawPage(canvas: Canvas?, pageIndex: Int) {
        pages?.let {
            if (pageIndex >= 1 && pageIndex <= it.size) {
                val target = it[pageIndex - 1]
                if (target.layout == null) {
                    val ssb = SpannableStringBuilder()
                    val size = target.contentParts.size
                    for ((index, content) in target.contentParts.withIndex()) {
                        ssb.append(content)
                        if (index < size - 1) {
                            ssb.append("\n")
                        }
                    }
                    target.layout = renderContext?.createLayout(ssb)
                }
                canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                // 把内容画在正中间
                val canvasHeight = canvas?.height ?: 0
                val canvasWidth = canvas?.width ?: 0
                val renderOptions = renderContext?.options
                val contentHeight = renderOptions?.canvasHeight ?: 0
                val contentWidth = renderOptions?.canvasWidth ?: 0
                canvas?.save()
                canvas?.translate(
                    (canvasWidth - contentWidth) / 2f,
                    (canvasHeight - contentHeight) / 2f
                )
                target.layout?.draw(canvas)
                canvas?.restore()
            }
        }
    }

    fun hasNextPage(): Boolean {
        val total = pages!!.size
        val next = curPage + 1
        return total > 0 && next <= total
    }

    fun hasPrePage(): Boolean {
        val total = pages!!.size
        val pre = curPage - 1
        return total > 0 && pre >= 1
    }

    fun nextPage() {
        effect?.autoTurnPage(false)
    }

    fun prePage() {
        effect?.autoTurnPage(true)
    }

    inner class SimpleEffectReceiver : EffectReceiver {
        override fun invalidate() {
            postInvalidate()
        }

        override fun drawCurPage(): Boolean {
            drawPage(curPageCanvas, curPage)
            preORnextPageCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            return true
        }

        override fun drawNextPage(): Boolean {
            if (hasNextPage()) {
                drawPage(preORnextPageCanvas, curPage + 1)
                return true
            }
            return false
        }

        override fun drawPrePage(): Boolean {
            if (hasPrePage()) {
                drawPage(preORnextPageCanvas, curPage - 1)
                return true
            }
            return false
        }

        override fun toPrePage() {
            if (hasPrePage()) {
                curPage--
                renderReceiver?.onRenderPage(curPage)
            }
        }

        override fun toNextPage() {
            if (hasNextPage()) {
                curPage++
                renderReceiver?.onRenderPage(curPage)
            }
        }
    }
}