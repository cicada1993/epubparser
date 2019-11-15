package com.chineseall.epubparser.lib.css

/**
 * Represents a CSS rule: a selector followed by a declaration block
 */
class RuleCss {
    var selector: String? = null // 选择器
    var nodes: MutableList<DeclCss>? = null // 样式列表
}