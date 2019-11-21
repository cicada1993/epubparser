package com.chineseall.epubparser.lib.render

import android.text.Layout
import android.text.SpannableString

class Page(val index: Int, val contentParts: MutableList<SpannableString>) {
    var layout: Layout? = null
}