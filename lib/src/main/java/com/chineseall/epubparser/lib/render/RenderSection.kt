package com.chineseall.epubparser.lib.render

interface RenderSection {
    fun render(sectionIndex: Int, renderContext: RenderContext)
}