package com.chineseall.epubparser.lib.render

import android.text.*
import com.chineseall.epubparser.lib.html.BaseNode
import com.chineseall.epubparser.lib.html.ImageNode
import com.chineseall.epubparser.lib.html.TextNode
import com.chineseall.epubparser.lib.util.LogUtil
import java.lang.StringBuilder
import kotlin.math.floor

// 文本区域
class TextSection : RenderSection {
    var nodes: MutableList<BaseNode>? = null
    var imageTexts = mutableListOf<ImageText>()
    var textPageParts = mutableListOf<PagePart>()
    fun appendNode(node: BaseNode) {
        if (nodes == null) {
            nodes = mutableListOf()
        }
        nodes?.add(node)
    }

    override fun render(sectionIndex: Int, renderContext: RenderContext) {
        nodes?.let {
            renderContext.onSection("text", sectionIndex)
            measure(renderContext)
            for (part in textPageParts) {
                val rawRange = IntRange(part.rawStart, part.rawEnd)
                val ss = part.ss
                // 还原图片文字
                for (imageText in imageTexts) {
                    if (rawRange.contains(imageText.rawPosition)) {
                        val imageNode = imageText.imageNode
                        val partPosition = imageText.rawPosition - part.rawStart
                        LogUtil.d("${part.rawStart} ${part.rawEnd} ${imageText.rawPosition}")
                        ss.setSpan(
                            imageNode.imageSpan(renderContext),
                            partPosition,
                            partPosition + 1,
                            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }
    }

    private fun measure(renderContext: RenderContext) {
        val totalSB = StringBuilder()
        nodes?.let {
            for ((index, node) in it.withIndex()) {
                if (index == 0) {
                    totalSB.append("\u3000\u3000")
                }
                when (node) {
                    is TextNode -> {
                        totalSB.append(node.content)
                    }
                    is ImageNode -> {
                        val imageText = ImageText(node, totalSB.length)
                        LogUtil.d("文字图片 位置 ${totalSB.length}")
                        // 图片位置暂用“图”字替代
                        totalSB.append("图")
                        imageTexts.add(imageText)
                    }
                }
            }
        }
        if (imageTexts.isNullOrEmpty()) {

        } else {
            LogUtil.d("有文字图片 范围 0 ~ ${totalSB.length - 1}")
        }
        val totalSS = SpannableString(totalSB)
        val totalPart = PagePart(totalSS, 0, totalSS.length - 1)
        divideText(renderContext, totalPart, true)
    }

    private fun divideText(
        renderContext: RenderContext,
        textPagePart: PagePart,
        total: Boolean = false
    ) {
        val renderOptions = renderContext.options
        val canvasHeight = renderOptions.canvasHeight
        val textLayout = createLayout(renderContext, textPagePart)
        val requireHeight = textLayout.height
        val requireLines = textLayout.lineCount
        if (total) {
            renderContext.onSectionRequireHeight("text", requireHeight, requireLines)
        }
        val remainHeight = canvasHeight - renderContext.curPageContentHeight
        if (requireHeight > remainHeight) {
            val ss = textPagePart.ss
            // 超出当前页剩余高度
            val avHeight = 1.0f * requireHeight / requireLines
            var remainLines = floor(remainHeight / avHeight).toInt()
            if (remainLines >= requireLines) {
                remainLines = requireLines - 1
            }
            if (remainLines <= 0) {
                renderContext.onNewPage()
                divideText(renderContext, textPagePart)
                return
            }
            // 定位在哪一行超出
            var nextPageStart = textLayout.getLineStart(remainLines)
            var curPartSS = SpannableString(ss.subSequence(0, nextPageStart))
            var curPartRawStart = textPagePart.rawStart
            var curPartRawEnd = curPartRawStart + curPartSS.length - 1
            var curPagePart = PagePart(
                curPartSS,
                curPartRawStart,
                curPartRawEnd,
                renderContext.curPageIndex
            )
            var curPartLayout = createLayout(renderContext, curPagePart)
            while (curPartLayout.height > remainHeight) {
                remainLines--
                nextPageStart = textLayout.getLineStart(remainLines)
                curPartSS = SpannableString(ss.subSequence(0, nextPageStart))
                // 计算该部分文本在整段文本中的位置
                curPartRawStart = textPagePart.rawStart
                curPartRawEnd = curPartRawStart + curPartSS.length - 1
                curPagePart = PagePart(
                    curPartSS,
                    curPartRawStart,
                    curPartRawEnd,
                    renderContext.curPageIndex
                )
                curPartLayout = createLayout(renderContext, curPagePart)
            }
            textPageParts.add(curPagePart)
            renderContext.onPagePart(curPagePart)
            // 新建一页
            renderContext.onNewPage()
            // 处理剩余的
            val remainSS = SpannableString(ss.subSequence(nextPageStart, ss.length))
            val remainPartRawStart = curPartRawEnd + 1
            val remainPartRawEnd = textPagePart.rawEnd
            val remianPagePart = PagePart(
                remainSS,
                remainPartRawStart,
                remainPartRawEnd,
                renderContext.curPageIndex
            )
            divideText(renderContext, remianPagePart)
        } else {
            textPagePart.pageIndex = renderContext.curPageIndex
            textPageParts.add(textPagePart)
            renderContext.onPagePart(textPagePart)
        }
    }

    private fun createLayout(
        renderContext: RenderContext,
        textPagePart: PagePart
    ): StaticLayout {
        return renderContext.createLayout(textPagePart.ss)
    }

    class ImageText(
        val imageNode: ImageNode,
        val rawPosition: Int
    )
}