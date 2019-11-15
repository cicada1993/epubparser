package com.chineseall.epubparser.lib.core

import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.html.Chapter

open class BookReceiver {
    open fun bookSuccess(book: OpfPackage) {

    }

    open fun bookFailed(bookKey: String?, msg: String?) {

    }

    open fun chapterSuccess(chapter: Chapter) {

    }

    open fun chapterFailed(bookKey: String?, chapterIndex: Int?, msg: String?) {

    }
}