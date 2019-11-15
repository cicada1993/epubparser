package com.chineseall.epubparser.lib.html

import com.chineseall.epubparser.lib.render.RenderContext
import com.chineseall.epubparser.lib.render.RenderSection
import com.chineseall.epubparser.lib.render.TextSection

// 文本节点
class TextNode(
    nodeType: String?,
    type: String?,
    className: String?,
    style: String?,
    bookPlot: BookPlot?,
    var textType: String?,
    var content: String?
) : BaseNode(nodeType, type, className, style, bookPlot) {
    override fun appendSection(renderContext: RenderContext, sections: MutableList<RenderSection>) {
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
    }
}