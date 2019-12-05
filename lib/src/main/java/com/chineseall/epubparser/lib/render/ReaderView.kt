package com.chineseall.epubparser.lib.render

import android.content.Context
import android.graphics.*
import android.graphics.Color.parseColor
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.html.Chapter
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.util.ScreenUtil
import com.chineseall.epubparser.lib.util.ToastUtil
import com.chineseall.epubparser.lib.view.BetterLineBgSpan
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
    private var effect: EffectOfCover? = null

    init {
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
        effect?.onDraw(canvas)
    }

    fun render(
        renderContext: RenderContext,
        chapter: Chapter,
        label: PagePart? = null,
        receiver: RenderReceiver? = null
    ) {
        this.renderContext = renderContext
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
            effect = EffectOfCover(context)
            effect!!.effectReceiver = SimpleEffectReceiver()
        }
        effect?.config(viewWidth, viewHeight, curPageBitmap, preORnextPageBitmap)
        renderContext?.let {
            // 开始分页
            it.clear()
            chapter.render(it)
            it.printPlot()
            pages = it.pages
            renderReceiver?.onPages(chapter.chapterIndex!!, pages!!)
            // 渲染书签所在页 没有则默认第一页
            var initPageIndex = 1
            for (page in pages!!) {
                if (page.hasLabel(label)) {
                    initPageIndex = page.index
                    LogUtil.d("找到书签")
                    break
                }
            }
            drawPage(curPageCanvas, initPageIndex)
            curPage = initPageIndex
            invalidate()
        }
    }

    fun clear() {
        curPageCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        preORnextPageCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        curPage = -1
        effect?.resetData()
        invalidate()
    }


    fun drawPage(canvas: Canvas?, pageIndex: Int) {
        pages?.let {
            if (pageIndex >= 1 && pageIndex <= it.size) {
                val target = it[pageIndex - 1]
                if (target.layout == null) {
                    val ssb = SpannableStringBuilder()
                    val size = target.contentParts.size
                    for ((index, contentPart) in target.contentParts.withIndex()) {
                        ssb.append(contentPart.ss)
                        if (index < size - 1) {
                            ssb.append("\n")
                        }
                    }
                    ssb.setSpan(
                        BetterLineBgSpan(target),
                        0,
                        ssb.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    target.layout = renderContext?.createLayout(ssb)
                }
                canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                // 把内容画在正中间
                val viewHeight = canvas?.height ?: 0
                val viewWidth = canvas?.width ?: 0
                val renderOptions = renderContext?.options
                val contentHeight = renderOptions?.canvasHeight ?: 0
                val contentWidth = renderOptions?.canvasWidth ?: 0
                canvas?.save()
                val dx = (viewWidth - contentWidth) / 2f
                val dy = (viewHeight - contentHeight) / 2f
                target.axisOriginOffset = Pair(dx, dy)
                canvas?.translate(dx, dy)
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
        if (hasNextPage()) {
            effect?.autoTurnPage(false)
        }
    }

    fun prePage() {
        if (hasPrePage()) {
            effect?.autoTurnPage(true)
        }
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
                renderReceiver?.onRenderPage(curPage, pages!![curPage - 1].provideLabel())
            }
        }

        override fun toNextPage() {
            if (hasNextPage()) {
                curPage++
                renderReceiver?.onRenderPage(curPage, pages!![curPage - 1].provideLabel())
            }
        }


        override fun onPageClick(x: Float, y: Float) {
            pages?.let {
                if (curPage >= 1 && curPage <= it.size) {
                    val target = it[curPage - 1]
                    val touchResult = target.onPageClick(x, y)
                    touchResult.print()
                    if (touchResult.isImageWord) {
                        ToastUtil.show(context, touchResult.imageSpan?.imageNode?.alt)
                    }
                }
            }
        }

        override fun onPageLongClick(x: Float, y: Float) {
            pages?.let {
                if (curPage >= 1 && curPage <= it.size) {
                    val target = it[curPage - 1]
                    val touchResult = target.onPageLongClick(x, y)
                    touchResult.print()
                }
            }
        }
    }
}