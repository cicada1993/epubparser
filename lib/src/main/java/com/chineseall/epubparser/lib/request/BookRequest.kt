package com.chineseall.epubparser.lib.request

import java.io.Serializable

class BookRequest(
    val bookKey: String,
    val bookUrl: String,
    val zipped: Boolean,
    val backUrl:String
) : Serializable