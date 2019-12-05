package com.chineseall.epubparser.lib.render

import android.text.Spannable
import android.text.SpannableString
import com.chineseall.epubparser.lib.html.ImageNode

// 图文区域
class ImageSection(var imageNode: ImageNode?) :
    RenderSection {

    override fun render(sectionIndex: Int, renderContext: RenderContext) {
        imageNode?.let {
            renderContext.onSection("image", sectionIndex)
            measure(renderContext, it)
        }
    }

    private fun measure(renderContext: RenderContext, node: ImageNode) {
        val renderOptions = renderContext.options
        val spacingAdd = renderOptions.spacingAdd
        val requireHeight = node.showHeight + 2 * spacingAdd
        renderContext.onSectionRequireHeight("image", requireHeight)
        val canvasHeight = renderOptions.canvasHeight
        val remainHeight = canvasHeight - renderContext.curPageContentHeight
        if (requireHeight > remainHeight) {
            renderContext.onNewPage()
        }
        val imageSB = SpannableString("图")
        imageSB.setSpan(
            node.imageSpan(renderContext),
            0,
            imageSB.length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        renderContext.onPagePart(
            PagePart(
                imageSB,
                0,
                imageSB.length - 1,
                renderContext.curPageIndex
            )
        )
    }
}