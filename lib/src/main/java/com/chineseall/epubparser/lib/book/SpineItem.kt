package com.chineseall.epubparser.lib.book

/**
 * 目录项
 * [cfiBase] epubcfi
 * [href] 资源相对路径
 * [idref] 资源id
 * [index] 序号
 * [linear]
 */
class SpineItem: Comparable<SpineItem> {
    var cfiBase: String? = null
    var href: String? = null
    var idref: String? = null
    var index: Int = -1
    var linear: String? = null

    override fun compareTo(other: SpineItem): Int {
        return if (this.index > other.index) {
            1
        }else{
            -1
        }
    }
}