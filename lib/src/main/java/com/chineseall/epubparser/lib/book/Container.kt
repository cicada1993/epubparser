package com.chineseall.epubparser.lib.book

/**
 * META-INF/container.xml文件信息
 * [directory] container.xml文件所在目录
 * [encoding] container.xml文件编码格式
 * [packagePath] opf文件所在相对路径
 */
class Container {
    var directory: String? = null
    var encoding: String? = null
    var packagePath: String? = null
}