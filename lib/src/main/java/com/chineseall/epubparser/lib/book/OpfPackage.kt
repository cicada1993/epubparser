package com.chineseall.epubparser.lib.book

import com.chineseall.epubparser.lib.html.BookPlot

/**
 * opf文件内容
 * [bookKey] 图书唯一标识
 * [container] META-INF/container.xml文件信息
 * [metadata] opf文件中metadata标签内容
 * [coverPath] 封面路径
 * [navPath]
 * [ncxPath] opf文件中spine标签指明的ncx文件路径
 * [spine] 解析ncx得到的目录列表
 * [uniqueIdentifier]资源唯一编号
 */
class OpfPackage{
    var bookPlot: BookPlot? = null
    var container: Container? = null
    var metadata: MetaData? = null
    var coverPath: String? = null
    var navPath: String? = null
    var ncxPath: String? = null
    var spine: MutableList<SpineItem>? = null
    var uniqueIdentifier: String? = null
}