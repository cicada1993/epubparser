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
    var textPageParts = mutableListOf<TextPagePart>()
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
                val sb = part.sb
                // 还原图片文字
                for (imageText in imageTexts) {
                    if (rawRange.contains(imageText.rawPosition)) {
                        val imageNode = imageText.imageNode
                        val partPosition = imageText.rawPosition - part.rawStart
                        LogUtil.d("${part.rawStart} ${part.rawEnd} ${imageText.rawPosition}")
                        sb.setSpan(
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
        val totalPart = TextPagePart(SpannableString(totalSB), 0, totalSB.length - 1)
        divideText(renderContext, totalPart, true)
    }

    private fun divideText(
        renderContext: RenderContext,
        textPagePart: TextPagePart,
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
            val sb = textPagePart.sb
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
            var curPartSB = SpannableString(sb.subSequence(0, nextPageStart))
            var curPartRawStart = textPagePart.rawStart
            var curPartRawEnd = curPartRawStart + curPartSB.length - 1
            var curPagePart = TextPagePart(
                curPartSB,
                curPartRawStart,
                curPartRawEnd,
                renderContext.curPageIndex
            )
            var curPartLayout = createLayout(renderContext, curPagePart)
            while (curPartLayout.height > remainHeight) {
                remainLines--
                nextPageStart = textLayout.getLineStart(remainLines)
                curPartSB = SpannableString(sb.subSequence(0, nextPageStart))
                // 计算该部分文本在整段文本中的位置
                curPartRawStart = textPagePart.rawStart
                curPartRawEnd = curPartRawStart + curPartSB.length - 1
                curPagePart = TextPagePart(
                    curPartSB,
                    curPartRawStart,
                    curPartRawEnd,
                    renderContext.curPageIndex
                )
                curPartLayout = createLayout(renderContext, curPagePart)
            }
            textPageParts.add(curPagePart)
            renderContext.onPageContent(curPagePart.sb)
            // 新建一页
            renderContext.onNewPage()
            // 处理剩余的
            val remainSB = SpannableString(sb.subSequence(nextPageStart, sb.length))
            val remainPartRawStart = curPartRawEnd + 1
            val remainPartRawEnd = textPagePart.rawEnd
            val remianPagePart = TextPagePart(
                remainSB,
                remainPartRawStart,
                remainPartRawEnd,
                renderContext.curPageIndex
            )
            divideText(renderContext, remianPagePart)
        } else {
            textPagePart.pageIndex = renderContext.curPageIndex
            textPageParts.add(textPagePart)
            renderContext.onPageContent(textPagePart.sb)
        }
    }

    private fun createLayout(
        renderContext: RenderContext,
        textPagePart: TextPagePart
    ): StaticLayout {
        return renderContext.createLayout(textPagePart.sb)
    }

    class ImageText(
        val imageNode: ImageNode,
        val rawPosition: Int
    )

    class TextPagePart(
        val sb: SpannableString,
        val rawStart: Int,
        val rawEnd: Int,
        var pageIndex: Int = -1
    )

}