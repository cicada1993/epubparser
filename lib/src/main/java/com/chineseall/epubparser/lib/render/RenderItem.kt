package com.chineseall.epubparser.lib.render

class RenderItem {
    var itemType: String? = null
    var className: String? = null
    var style: String? = null
    var type: String? = null

    var content: String? = null

    var src: String? = null
    var active: String? = null
    var alt: String? = null

    var children: MutableList<RenderItem>? = null

    fun render(sb: StringBuilder) {
        if (itemType == "text") {
            // 文本
            sb.append(content)
        } else if (itemType == "image") {
            // 图片
            sb.append(src)
        } else if (itemType == "container") {
            // 容器
            children?.let {
                for (item in it) {
                    item.render(sb)
                }
            }
            sb.append("\n")
        }
    }
}