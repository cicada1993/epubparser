package com.chineseall.epubparser.lib.render

import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.TextUtils
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.view.BetterImageSpan

class Page(val index: Int, val contentParts: MutableList<PagePart>) {
    var layout: Layout? = null
    var axisOriginOffset: Pair<Float, Float>? = null
    var lineRects = mutableListOf<Rect>()

    /**
     * 判断该页是否包含书签
     */
    fun hasLabel(label: PagePart?): Boolean {
        if (label != null) {
            for (contentPart in contentParts) {
                if (label.isSameSection(contentPart)) {
                    val labelRawStart = label.rawStart
                    return labelRawStart >= contentPart.rawStart && labelRawStart <= contentPart.rawEnd
                }
            }
        }
        return false
    }

    fun provideLabel():PagePart {
        return contentParts[0]
    }

    fun onPageClick(rawX: Float, rawY: Float): TouchResult {
        val layoutX = rawX - (axisOriginOffset?.first ?: 0f)
        val layoutY = rawY - (axisOriginOffset?.second ?: 0f)
        return getTouchResult(layoutX, layoutY)
    }

    fun onPageLongClick(rawX: Float, rawY: Float): TouchResult {
        val layoutX = rawX - (axisOriginOffset?.first ?: 0f)
        val layoutY = rawY - (axisOriginOffset?.second ?: 0f)
        return getTouchResult(layoutX, layoutY)
    }

    fun appendLineRec(lineNum: Int, lineRect: Rect) {
        lineRects.add(lineNum, lineRect)
    }

    fun getLineLeft(lineNum: Int): Float {
        val lineStart = layout?.getLineStart(lineNum) ?: 0
        val lineText = getLineText(lineNum)
        for (i in 0 until (lineText?.length ?: 0)) {
            if (!TextUtils.equals(lineText?.subSequence(i, i + 1), "\u3000")) {
                return layout?.getPrimaryHorizontal(lineStart + i) ?: 0f
            }
        }
        return 0f
    }

    fun getLineWidth(lineNum: Int): Float {
        return layout?.getLineWidth(lineNum) ?: 0f
    }

    fun getLineText(lineNum: Int): CharSequence? {
        val text = layout?.text
        val lineStart = layout?.getLineStart(lineNum) ?: 0
        val lineEnd = layout?.getLineEnd(lineNum) ?: 0
        return text?.subSequence(lineStart, lineEnd)
    }

    fun isImageLine(lineNum: Int): Pair<Boolean, BetterImageSpan?> {
        val lineText = getLineText(lineNum)
        if (lineText is SpannableStringBuilder) {
            val imageSpans = lineText.getSpans(0, lineText.length, BetterImageSpan::class.java)
            if (imageSpans.size == 1 && !imageSpans[0].imageNode.isWord()) {
                return Pair(true, imageSpans[0])
            }
        }
        return Pair(false, null)
    }

    fun getTouchResult(layoutX: Float, layoutY: Float): TouchResult {
        val touchResult = TouchResult(layoutX, layoutY)
        var touchLineNum = -1
        for ((lineNum, lineRect) in lineRects.withIndex()) {
            if (layoutY >= lineRect.top && layoutY <= lineRect.bottom
                && layoutX >= lineRect.left && layoutX <= lineRect.right
            ) {
                touchLineNum = lineNum
                touchResult.touchLineNum = touchLineNum
                break
            }
        }
        if (touchLineNum >= 0) {
            val lineStart = layout?.getLineStart(touchLineNum) ?: 0
            val lineText = getLineText(touchLineNum)
            if (lineText is SpannableStringBuilder) {
                val imageSpans = lineText.getSpans(0, lineText.length, BetterImageSpan::class.java)
                if (imageSpans.size == 1 && !imageSpans[0].imageNode.isWord()) {
                    touchResult.isImageLine = true
                    touchResult.imageSpan = imageSpans[0]
                    touchResult.touchWord = lineText
                } else {
                    var word: CharSequence?
                    val wordPath = Path()
                    val wordRectF = RectF()
                    for (i in 0 until lineText.length) {
                        word = lineText.subSequence(i, i + 1)
                        if (!TextUtils.equals(word, "\u3000") && !TextUtils.equals(word, "\n")) {
                            // 过滤空格换行
                            layout?.getSelectionPath(lineStart + i, lineStart + i + 1, wordPath)
                            wordPath.computeBounds(wordRectF, true)
                            wordRectF.left = layout?.getPrimaryHorizontal(lineStart + i) ?: 0f
                            if (layoutX >= wordRectF.left && layoutX <= wordRectF.right) {
                                touchResult.touchWord = word
                                if (word is SpannableStringBuilder) {
                                    val imageSpans =
                                        word.getSpans(0, word.length, BetterImageSpan::class.java)
                                    if (imageSpans.size == 1 && imageSpans[0].imageNode.isWord()) {
                                        touchResult.isImageWord = true
                                        touchResult.imageSpan = imageSpans[0]
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
        return touchResult
    }

    class TouchResult(
        val layoutX: Float,
        val layoutY: Float,
        var touchLineNum: Int = -1,
        var isImageLine: Boolean = false,
        var touchWord: CharSequence? = null,
        var isImageWord: Boolean = false,
        var imageSpan: BetterImageSpan? = null
    ) {
        fun print() {
            if (touchLineNum != -1) {
                LogUtil.d(
                    "layoutX $layoutX \n " +
                            "layoutY $layoutY \n " +
                            "touchLineNum $touchLineNum \n" +
                            "isImageLine $isImageLine \n" +
                            "touchWord $touchWord \n" +
                            "isImageWord $isImageWord \n"
                )
            } else {
                LogUtil.d("未命中")
            }
        }
    }
}