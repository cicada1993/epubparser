package com.chineseall.epubparser.lib.request

import java.io.Serializable

class ChapterRequest(
    val bookKey: String,
    val bookUrl: String,
    val zipped: Boolean,
    val chapterIndex: Int,
    val backUrl:String
) : Serializable