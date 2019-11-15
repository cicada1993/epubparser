package com.chineseall.epubparser.lib.book

import com.chineseall.epubparser.lib.css.RuleCss

/**
 * css资源文件
 */
class CssResource : Resource() {
    var rules: MutableList<RuleCss>? = null
}