package com.chineseall.epubparser.lib.html

import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import com.chineseall.epubparser.lib.render.ImageSection
import com.chineseall.epubparser.lib.render.RenderContext
import com.chineseall.epubparser.lib.render.RenderSection
import com.chineseall.epubparser.lib.render.TextSection
import com.chineseall.epubparser.lib.util.BitmapUtil
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.view.BetterImageSpan

// 图片节点
class ImageNode(
    nodeType: String?,
    type: String?,
    className: String?,
    style: String?,
    bookPlot: BookPlot?,
    var src: String?,
    var active: String?,
    var alt: String?,
    var href: String?,
    var base64Data: String?,
    var width: Int = 0,
    var height: Int = 0
) : BaseNode(nodeType, type, className, style, bookPlot) {
    var decoded = false
    var imageDrawable: Drawable? = null
    var showWidth = 0
    var showHeight = 0
    fun decode(renderContext: RenderContext) {
        if (!decoded) {
            try {
                val metaData = BitmapUtil.decodeFromBase64Data(base64Data)
                width = metaData.dimensions!!.first
                height = metaData.dimensions!!.second
                decoded = true
            } catch (e: Exception) {
                LogUtil.d(href)
            }
        }
    }

    fun isWord(): Boolean {
        return width < 80 && height < 80 && (pre is TextNode || next is TextNode)
    }

    fun imageSpan(renderContext: RenderContext): BetterImageSpan {
        decode(renderContext)
        imageDrawable = BitmapUtil.drawableFromBase64Data(renderContext.context, base64Data)
        showWidth = imageDrawable!!.intrinsicWidth
        showHeight = imageDrawable!!.intrinsicHeight
        val renderOptions = renderContext.options
        var horiOff = 0
        if (isWord()) {
            val textPaint = renderOptions.textPaint
            showWidth = textPaint.textSize.toInt()
            showHeight = showWidth * height / width
        } else {
            val canvasWidth = renderOptions.canvasWidth
            val canvasHeight = renderOptions.canvasHeight
            if (showWidth > canvasWidth * 4 / 5) {
                showWidth = canvasWidth * 4 / 5
            }
            showHeight = showWidth * height / width
            if (showHeight > canvasHeight * 4 / 5) {
                showHeight = canvasHeight * 4 / 5
                showWidth = showHeight * width / height
            }
            horiOff = (canvasWidth - showWidth) / 2
        }
        imageDrawable!!.setBounds(horiOff, 0, showWidth + horiOff, showHeight)
        val imageSpan = BetterImageSpan(
            imageDrawable,
            BetterImageSpan.normalizeAlignment(ImageSpan.ALIGN_CENTER)
        )
        imageSpan.imageNode = this
        return imageSpan
    }

    override fun newSection(): Boolean {
        return super.newSection() || !isWord()
    }

    override fun appendSection(renderContext: RenderContext, sections: MutableList<RenderSection>) {
        decode(renderContext)
        if (isWord()) {
            // 当行内文字处理
            if (newSection() || sections.isEmpty()) {
                // 新建
                val textSection = TextSection()
                textSection.appendNode(this)
                sections.add(textSection)
            } else {
                // 追加
                val tailSection = sections[0]
                if (tailSection is TextSection) {
                    tailSection.appendNode(this)
                }
            }
        } else {
            sections.add(ImageSection(this))
        }
    }
}