package com.chineseall.epubparser.lib.html

import com.chineseall.epubparser.lib.render.RenderContext
import com.chineseall.epubparser.lib.render.RenderSection

abstract class BaseNode {
    // 节点类型
    var nodeType: String? = null
    // html标签名
    var type: String? = null
    // 标签上的class属性值
    var className: String? = null
    // 标签上的行内样式 json字符串
    var style: String? = null
    var bookPlot: BookPlot? = null

    var pre: BaseNode? = null
    var next: BaseNode? = null
    var parent: ContainerNode? = null

    constructor(
        nodeType: String?,
        type: String?,
        className: String?,
        style: String?,
        bookPlot: BookPlot?
    ) {
        this.nodeType = nodeType
        this.type = type
        this.className = className
        this.style = style
        this.bookPlot = bookPlot
    }

    open fun newSection(): Boolean {
        return this.parent == null || this.pre == null
    }

    open fun appendSection(renderContext: RenderContext, sections: MutableList<RenderSection>) {

    }
}