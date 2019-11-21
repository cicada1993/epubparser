package com.chineseall.epubparser.lib.render

interface EffectReceiver {
    fun invalidate()

    fun drawCurPage():Boolean

    fun drawNextPage():Boolean

    fun drawPrePage():Boolean

    fun toPrePage()

    fun toNextPage()
}