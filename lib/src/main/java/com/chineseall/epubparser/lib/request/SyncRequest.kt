package com.chineseall.epubparser.lib.request

import java.io.Serializable

class SyncRequest(
    val ip: String,
    val port: Int
) : Serializable
