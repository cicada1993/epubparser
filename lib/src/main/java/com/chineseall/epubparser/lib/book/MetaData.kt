package com.chineseall.epubparser.lib.book

/**
 * 从opf文件中metadata标签内容
 * [creator]作者
 * [description]描述
 * [direction]
 * [flow]
 * [identifier]一般指ISBN编号
 * [language]语言类型
 * [layout]
 * [media_active_class]
 * [modified_date]修订日期
 * [orientation]
 * [pubdate]出版日期
 * [rights]所有权
 * [title]一般指书名
 * [viewport]
 */
class MetaData {
    var creator: String? = null
    var description: String? = null
    var direction: Any? = null
    var flow: String? = null
    var identifier: String? = null
    var language: String? = null
    var layout: String? = null
    var media_active_class: String? = null
    var modified_date: String? = null
    var orientation: String? = null
    var pubdate: String? = null
    var publisher: String? = null
    var rights: String? = null
    var title: String? = null
    var viewport: String? = null
}