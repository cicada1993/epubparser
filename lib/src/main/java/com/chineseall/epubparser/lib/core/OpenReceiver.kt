package com.chineseall.epubparser.lib.core

import com.chineseall.epubparser.lib.book.OpfPackage

interface OpenReceiver {
    fun onSuccess(book: OpfPackage?)
    fun onFailed(msg: String?)
}