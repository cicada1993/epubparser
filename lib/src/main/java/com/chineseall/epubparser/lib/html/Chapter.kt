package com.chineseall.epubparser.lib.html

import com.chineseall.epubparser.lib.render.RenderContext

class Chapter {
    var bookKey: String? = null
    // 章节编号
    var chapterIndex: Int? = null
    // 渲染列表
    var renderBlocks: MutableList<RenderBlock>? = null

    fun render(renderContext: RenderContext) {
        renderContext.onNewPage()
        renderContext.onChapterStart(chapterIndex ?: -1)
        renderBlocks?.let { blocks ->
            for (block in blocks) {
                block.render(renderContext)
            }
        }
        renderContext.onChapterEnd(chapterIndex ?: -1)
    }
}