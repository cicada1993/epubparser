package com.chineseall.epubparser.lib.request

import java.io.Serializable

class OpenRequest(
    val ip: String,
    val port: Int,
    val bookKey: String,
    val bookPath: String,
    val bookUnzipPath: String
):Serializable