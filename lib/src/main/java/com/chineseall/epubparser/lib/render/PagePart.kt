package com.chineseall.epubparser.lib.render

import android.text.SpannableString

class PagePart(
    val ss: SpannableString,
    val rawStart: Int,
    val rawEnd: Int,
    var pageIndex: Int = -1
) {
    var curChapterIndex: Int = -1
    var curBlockIndex: Int? = -1
    var curSectionIndex: Int? = -1

    fun isSameSection(pagePart: PagePart): Boolean {
        val waked = curChapterIndex != -1 && curBlockIndex != -1 && curSectionIndex != -1
        return waked &&
                curChapterIndex == pagePart.curChapterIndex &&
                curBlockIndex == pagePart.curBlockIndex &&
                curSectionIndex == pagePart.curSectionIndex
    }
}
