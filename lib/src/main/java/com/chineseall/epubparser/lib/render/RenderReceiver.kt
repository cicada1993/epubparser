package com.chineseall.epubparser.lib.render

interface RenderReceiver {
    fun onPages(chapterIndex: Int, pages: MutableList<Page>)
    fun onRenderPage(index: Int)
}