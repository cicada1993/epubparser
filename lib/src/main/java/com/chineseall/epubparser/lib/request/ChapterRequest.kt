package com.chineseall.epubparser.lib.request

import java.io.Serializable

class ChapterRequest(
    val ip: String,
    val port: Int,
    val bookKey: String,
    val chapterIndex: Int
) : Serializable