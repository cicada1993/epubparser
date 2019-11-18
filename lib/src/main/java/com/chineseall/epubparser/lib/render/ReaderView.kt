package com.chineseall.epubparser.lib.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color.parseColor
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
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
        layout = renderContext.createLayout(SpannableStringBuilder(""))
        invalidate()
    }

    fun showPage(pageIndex: Int) {
        pages?.let {
            if (pageIndex >= 1 && pageIndex <= it.size) {
                curPage = pageIndex
                val target = it[pageIndex - 1]
                val ssb = SpannableStringBuilder()
                val size = target.contentParts.size
                for ((index, content) in target.contentParts.withIndex()) {
                    ssb.append(content)
                    if (index < size - 1) {
                        ssb.append("\n")
                    }
                }
                layout = renderContext.createLayout(ssb)
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
}