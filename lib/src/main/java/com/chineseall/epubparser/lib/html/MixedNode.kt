package com.chineseall.epubparser.lib.html

class MixedNode {
    // 公共字段
    var nodeType: String? = null
    var type: String? = null
    var className: String? = null
    var style: String? = null
    var bookPlot: BookPlot? = null

    // 文本节点字段
    var textType: String? = null
    var content: String? = null


    // 图片节点字段
    var src: String? = null
    var active: String? = null
    var alt: String? = null
    var href: String? = null
    var base64Data: String? = null
    var width: Int = 0
    var height: Int = 0

    // 容器节点字段
    var children: MutableList<MixedNode>? = null

    // 转换后
    private var transed = false
    var realNode: BaseNode? = null

    // 转换根节点
    fun transSelf(): BaseNode? {
        if (!transed) {
            when (nodeType) {
                NODE_TEXT -> {
                    realNode = TextNode(
                        nodeType,
                        type,
                        className,
                        type,
                        bookPlot,
                        textType,
                        content
                    )
                }
                NODE_IMAGE -> {
                    realNode = ImageNode(
                        nodeType,
                        type,
                        className,
                        style,
                        bookPlot,
                        src,
                        active,
                        alt,
                        href,
                        base64Data,
                        width,
                        height
                    )
                }
                NODE_CONTAINER -> {
                    val containerNode =
                        ContainerNode(nodeType, type, className, style, bookPlot)
                    containerNode.children = transChildren(containerNode)
                    realNode = containerNode
                }
            }
            transed = true
        }
        return realNode
    }

    // 转换子节点
    private fun transChildren(parent: ContainerNode): MutableList<BaseNode>? {
        return children?.let { mixedChildren ->
            val realChildren = mutableListOf<BaseNode>()
            var pre: BaseNode? = null
            for (mixedChild in mixedChildren) {
                val realChild = mixedChild.transSelf()
                pre?.next = realChild
                realChild?.pre = pre
                realChild?.parent = parent
                pre = realChild
                if (realChild != null) {
                    realChildren.add(realChild)
                }
            }
            return realChildren
        }
    }
}

const val NODE_TEXT = "text"
const val NODE_IMAGE = "image"
const val NODE_CONTAINER = "container"