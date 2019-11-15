package com.chineseall.epubparser.lib.render

import android.content.Context
import android.text.SpannableString
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.util.LogUtil

class RenderContext(
    val context: Context,
    val options: RenderOptions,
    val book: OpfPackage?
) {
    var curChapterIndex: Int = -1
    var curBlockIndex: Int? = -1
    var curSectionIndex: Int? = -1

    // 已渲染总页数
    var pageSum = 0
    // 当前页
    var curPageIndex = 0

    // 当前内容高度
    var curConentHeight = 0

    var renderPlot = StringBuilder()

    var pages = mutableListOf<Page>()

    var curPage: Page? = null

    //var ssb = SpannableStringBuilder()

    fun onChapterStart(index: Int) {
        this.curChapterIndex = index
        renderPlot.append("渲染chapter：$index\n")
    }

    fun onChapterEnd(index: Int) {
        renderPlot.append("chapter：$index 共计 $pageSum 页")
    }

    fun onBlock(index: Int) {
        this.curBlockIndex = index
        // renderPlot.append("渲染block：$index\n")
    }

    fun onSection(sectionType: String, index: Int) {
        this.curSectionIndex = index
        //renderPlot.append("渲染section：$sectionType $index\n")
    }

    fun onSectionRequireHeight(sectionType: String, requierHeight: Int, requireLines: Int = 0) {
        val canvasHeight = options.canvasHeight
        val remainHeight = canvasHeight - curConentHeight
        if (sectionType == "text") {
            renderPlot.append("渲染该段文字理论行数 $requireLines 理论高度：$requierHeight 当前剩余高度$remainHeight \n")
        } else {
            renderPlot.append("渲染该图片理论高度：$requierHeight 当前剩余高度$remainHeight\n")
        }
    }

    fun onPageContent(content: SpannableString) {
        curPage?.contentParts?.add(content)
    }

    fun onNewPage() {
        pageSum++
        curPageIndex++
        val newPage = Page(curPageIndex, mutableListOf())
        pages.add(newPage)
        curPage = newPage
        curConentHeight = 0
        renderPlot.append("目前总页数 $pageSum   当前计算页 $curPageIndex\n")
    }

    fun printPlot() {
       LogUtil.d(renderPlot.toString())
    }
}