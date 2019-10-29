package com.chineseall.epubparser.lib.core

import com.chineseall.epubparser.lib.render.RenderItem

interface ChapterReceiver {
    fun onSuccess(paragraphs: MutableList<RenderItem>?)
    fun onFailed(msg: String?)
}