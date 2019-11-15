package com.chineseall.epubparser.lib.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color.parseColor
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.os.Build
import android.text.*
import android.util.AttributeSet
import android.view.View
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.html.Chapter
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.util.ScreenUtil
import com.chineseall.epubparser.lib.util.ToastUtil
import com.chineseall.epubparser.lib.view.SerifTextView


class ReaderView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var layout: StaticLayout? = null
    private lateinit var renderContext: RenderContext
    private var pages: MutableList<Page>? = null
    private var curPage = -1
    private var renderReceiver: RenderReceiver? = null

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.save()
        layout?.draw(canvas)
        canvas?.restore()
    }

    fun render(book: OpfPackage?, chapter: Chapter, receiver: RenderReceiver? = null) {
        this.renderReceiver = receiver
        // 创建RenderContext
        val textPaint = TextPaint(ANTI_ALIAS_FLAG)
        textPaint.color = parseColor("#3F3737")
        textPaint.textSize = ScreenUtil.spToPx(context, 13f)
        textPaint.typeface = SerifTextView.serifTypefaces[SerifTextView.REGULAR]
        val canvasWidth = width
        val canvasHeight = height
        LogUtil.d("readerview size is $canvasWidth / $canvasHeight")
        val renderOptions = RenderOptions(canvasWidth, canvasHeight, textPaint)
        renderOptions.spacingAdd = 10
        renderContext = RenderContext(context, renderOptions, book)
        // 开始分页
        chapter.render(renderContext)
        renderContext.printPlot()
        pages = renderContext.pages
        renderReceiver?.onPages(chapter.chapterIndex!!, pages!!)
        // 渲染第一页
        showPage(1)
    }

    fun clear() {
        layout = createLayout(renderContext, SpannableStringBuilder(""))
        invalidate()
    }

    fun showPage(pageIndex: Int) {
        pages?.let {
            if (pageIndex >= 1 && pageIndex <= it.size) {
                curPage = pageIndex
                val target = it[pageIndex - 1]
                val ssb = SpannableStringBuilder()
                for (content in target.contentParts) {
                    ssb.append(content)
                    ssb.append("\n")
                }
                layout = createLayout(renderContext, ssb)
                renderReceiver?.onRenderPage(target, curPage)
                invalidate()
            }
        }
    }

    fun nextPage() {
        val total = pages!!.size
        val next = curPage + 1
        if (next > total) {
            ToastUtil.show(context, "已经是最后一页")
        } else {
            showPage(next)
        }
    }

    fun prePage() {
        val pre = curPage - 1
        if (pre < 1) {
            ToastUtil.show(context, "已经是第一页")
        } else {
            showPage(pre)
        }
    }

    private fun createLayout(
        renderContext: RenderContext,
        ssb: SpannableStringBuilder
    ): StaticLayout {
        val renderOptions = renderContext.options
        val spacingAdd = renderOptions.spacingAdd
        val textPaint = renderOptions.textPaint
        val layout: StaticLayout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 6.0之后使用Build模式
            val builder = StaticLayout.Builder.obtain(
                ssb,
                0,
                ssb.length,
                textPaint,
                width
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setLineSpacing(spacingAdd.toFloat(), 1.0f)
                .setIncludePad(false)
                .setEllipsizedWidth(0)
                .setEllipsize(null)
                .setMaxLines(Integer.MAX_VALUE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            }
            layout = builder.build()
        } else {
            layout = StaticLayout(
                ssb,
                textPaint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                spacingAdd.toFloat(),
                false
            )
        }
        return layout
    }
}