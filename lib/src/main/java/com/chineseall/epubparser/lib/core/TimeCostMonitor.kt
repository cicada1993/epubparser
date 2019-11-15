package com.chineseall.epubparser.lib.core

import com.chineseall.epubparser.lib.util.LogUtil

class TimeCostMonitor {
    private val dataMap = HashMap<String, Data>()

    fun onKey(key: String): Data {
        var data = dataMap[key]
        if (data == null) {
            data = Data()
            dataMap[key] = data
        }
        data.onKey(key)
        return data
    }

    inner class Data {
        private var key: String? = null
        private var start: Long = 0L
        private var end: Long = 0L

        fun onKey(key: String): Data {
            this.key = key
            return this
        }

        fun onStart(): Data {
            this.start = System.currentTimeMillis()
            return this
        }

        fun onEnd(): Data {
            this.end = System.currentTimeMillis()
            LogUtil.d("key $key || cost time ${gap()}ms")
            return this
        }

        fun gap(): Long {
            return end - start
        }
    }
}

const val HTTP_SERVER_INIT = "http_server_init"
const val WEBVIEW_INIT = "webview_init"
const val BOOK_OPEN = "book_open"
const val CHAPTER_LOAD = "chapter_load"