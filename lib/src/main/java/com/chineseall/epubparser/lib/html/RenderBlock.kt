package com.chineseall.epubparser.lib.html

import com.chineseall.epubparser.lib.render.RenderContext
import com.chineseall.epubparser.lib.render.RenderSection

class RenderBlock {
    var blockIndex: Int? = null
    var blockNode: MixedNode? = null

    fun createSections(renderContext: RenderContext): MutableList<RenderSection> {
        val sections = mutableListOf<RenderSection>()
        val realRootNode = blockNode?.transSelf()
        realRootNode?.appendSection(renderContext, sections)
        return sections
    }

    fun render(renderContext: RenderContext) {
        renderContext.onBlock(blockIndex ?: -1)
        val sections = createSections(renderContext)
        for ((index, section) in sections.withIndex()) {
            section.render(index, renderContext)
        }
    }
}