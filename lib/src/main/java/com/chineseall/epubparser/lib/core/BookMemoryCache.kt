package com.chineseall.epubparser.lib.core

import android.util.SparseArray
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.html.Chapter

class BookMemoryCache {
    var book: OpfPackage? = null
    var chapters: SparseArray<Chapter>? = null

    fun cacheChapter(chapter: Chapter) {
        if (chapters == null) {
            chapters = SparseArray()
        }
        val chapterIndex = chapter.chapterIndex!!
        chapters!!.put(chapterIndex, chapter)
    }
}