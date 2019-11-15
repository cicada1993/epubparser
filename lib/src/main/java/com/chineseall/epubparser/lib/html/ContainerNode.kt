package com.chineseall.epubparser.lib.html

import com.chineseall.epubparser.lib.render.RenderContext
import com.chineseall.epubparser.lib.render.RenderSection

// 容器节点
class ContainerNode(
    nodeType: String?,
    type: String?,
    className: String?,
    style: String?,
    bookPlot: BookPlot?,
    var children: MutableList<BaseNode>? = null
) : BaseNode(nodeType, type, className, style, bookPlot) {

    override fun appendSection(renderContext: RenderContext, sections: MutableList<RenderSection>) {
        children?.let { nodes ->
            for (node in nodes) {
                node.appendSection(renderContext, sections)
            }
        }
    }
}