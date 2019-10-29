package com.chineseall.epubparser.lib.book

/**
 * opf文件内容
 * [bookKey] 图书唯一标识
 * [bookPath] 图书相对路径
 * [bookUnzipPath] 图书解压资源相对路径
 * [container] META-INF/container.xml文件信息
 * [resources] 除opf文件外的所有资源列表
 * [metadata] opf文件中metadata标签内容
 * [coverPath] 封面路径
 * [navPath]
 * [ncxPath] opf文件中spine标签指明的ncx文件路径
 * [spine] 解析ncx得到的目录列表
 * [uniqueIdentifier]资源唯一编号
 */
class OpfPackage {
    var bookKey: String? = null
    var bookPath: String? = null
    var bookUnzipPath: String? = null
    var container: Container? = null
    var resources: MutableList<ResourceItem>? = null
    var metadata: MetaData? = null
    var coverPath: String? = null
    var navPath: String? = null
    var ncxPath: String? = null
    var spine: MutableList<SpineItem>? = null
    var uniqueIdentifier: String? = null
}