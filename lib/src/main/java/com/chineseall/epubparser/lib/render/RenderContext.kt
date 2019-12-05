package com.chineseall.epubparser.lib.render

import android.content.Context
import android.os.Build
import android.text.*
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.util.ScreenUtil

class RenderContext(
    val context: Context,
    val options: RenderOptions,
    val book: OpfPackage?
) {
    fun setTextSize(textSize:Float){
        options.textPaint.textSize =  ScreenUtil.spToPx(context, textSize)
    }
    var curChapterIndex: Int = -1
    var curBlockIndex: Int? = -1
    var curSectionIndex: Int? = -1

    // 已渲染总页数
    var pageSum = 0
    var pages = mutableListOf<Page>()
    // 当前页
    var curPageIndex = 0
    var curPage: Page? = null
    // 当前页面内容高度
    var curPageContentHeight = 0
    var curPageContentParts = mutableListOf<SpannableString>()
    var curPageContent = SpannableStringBuilder()

    var renderPlot = StringBuilder()

    fun clear() {
        curChapterIndex = -1
        curBlockIndex = -1
        curSectionIndex = -1
        pageSum = 0
        pages.clear()
        curPageIndex = 0
        curPageContentHeight = 0
        curPageContentParts.clear()
        curPageContent.clear()
        renderPlot.clear()
    }

    fun onChapterStart(index: Int) {
        this.pages.clear()
        this.curChapterIndex = index
        renderPlot.append("渲染chapter：$index\n")
    }

    fun onChapterEnd(index: Int) {
        renderPlot.append("chapter：$index 共计 $pageSum 页")
    }

    fun onBlock(index: Int) {
        this.curBlockIndex = index
    }

    fun onSection(sectionType: String, index: Int) {
        this.curSectionIndex = index
    }

    fun onSectionRequireHeight(sectionType: String, requierHeight: Int, requireLines: Int = 0) {
        val canvasHeight = options.canvasHeight
        val remainHeight = canvasHeight - curPageContentHeight
        if (sectionType == "text") {
            renderPlot.append("渲染该段文字理论行数 $requireLines 理论高度：$requierHeight 当前剩余高度$remainHeight \n")
        } else {
            renderPlot.append("渲染该图片理论高度：$requierHeight 当前剩余高度$remainHeight\n")
        }
    }

    fun onPagePart(element: PagePart) {
        curPage?.run {
            element.curChapterIndex = curChapterIndex
            element.curBlockIndex = curBlockIndex
            element.curSectionIndex = curSectionIndex
            contentParts.add(element)
            // 自动计算当前页高度 计算高度时 两段之前应该只有一个换行符
            if (contentParts.size >= 2) {
                curPageContent.append("\n")
            }
            curPageContent.append(element.ss)
            val layout = createLayout(curPageContent)
            curPageContentHeight = layout.height
        }
    }

    fun onNewPage() {
        pageSum++
        curPageIndex++
        val newPage = Page(curPageIndex, mutableListOf())
        pages.add(newPage)
        curPage = newPage
        curPageContentParts.clear()
        curPageContentHeight = 0
        curPageContent.clear()
        renderPlot.append("目前总页数 $pageSum   当前计算页 $curPageIndex\n")
    }

    fun createLayout(
        words: CharSequence
    ): StaticLayout {
        val spacingAdd = options.spacingAdd
        val textPaint = options.textPaint
        val canvasWidth = options.canvasWidth
        val layout: StaticLayout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 6.0之后使用Build模式
            val builder = StaticLayout.Builder.obtain(
                words,
                0,
                words.length,
                textPaint,
                canvasWidth
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
                words,
                textPaint,
                canvasWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                spacingAdd.toFloat(),
                false
            )
        }
        return layout
    }

    fun printPlot() {
        LogUtil.d(renderPlot.toString())
    }
}