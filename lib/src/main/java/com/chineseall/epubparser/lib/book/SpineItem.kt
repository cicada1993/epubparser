package com.chineseall.epubparser.lib.book

/**
 * 目录项
 * [canonical]
 * [cfiBase] epubcfi
 * [href] 资源相对路径
 * [idref] 资源id
 * [index] 序号
 * [linear]
 * [url] 资源url
 */
class SpineItem: Comparable<SpineItem> {
    var canonical: String? = null
    var cfiBase: String? = null
    var href: String? = null
    var idref: String? = null
    var index: Int = -1
    var linear: Boolean? = null
    var url: String? = null

    override fun compareTo(other: SpineItem): Int {
        return if (this.index > other.index) {
            1
        }else{
            -1
        }
    }
}