package com.chineseall.epubparser.lib.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.webkit.JavascriptInterface
import com.alibaba.fastjson.JSON
import com.chineseall.epubparser.lib.Kiter
import com.chineseall.epubparser.lib.request.ChapterRequest
import com.chineseall.epubparser.lib.request.BookRequest
import com.chineseall.epubparser.lib.request.SyncRequest
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.view.CommonWebView

class BookServiceDelegate(val context: Context) {
    private var monitor: TimeCostMonitor = TimeCostMonitor()
    private var webView: CommonWebView? = null
    private var webViewHandler: WebViewHandler? = null
    private var webViewThread: HandlerThread? = null
    private var cacheMsg = mutableListOf<Message>()
    private var jsReady = false

    companion object {
        val MSG_CREATE_WEBVIEW = 0x100
        val MSG_SYNC_SERVER = 0x200
        val MSG_OPEN_BOOK = 0x300
        val MSG_LOAD_CHAPTER = 0x400
        val MSG_RELEASE_WEBVIEW = 0x500
    }

    /**
     * 启动WebView
     */
    fun onCreate() {
        webViewThread = HandlerThread("BookService", Thread.MAX_PRIORITY)
        webViewThread!!.run {
            start()
            webViewHandler = WebViewHandler(looper)
            val msg = Message.obtain()
            msg.what = MSG_CREATE_WEBVIEW
            webViewHandler!!.sendMessage(msg)
        }
    }

    fun onDestroy() {
        // 销毁webview
        val msg = Message.obtain()
        msg.what = MSG_RELEASE_WEBVIEW
        webViewHandler?.sendMessage(msg)
    }

    /**
     * 同步本地服务信息
     */
    fun syncServer(request: SyncRequest?) {
        request?.run {
            sendEventToJS(MSG_SYNC_SERVER, JSON.toJSONString(this))
        }
    }

    /**
     * 打开书籍
     */
    fun openBook(request: BookRequest?) {
        request?.run {
            sendEventToJS(MSG_OPEN_BOOK, JSON.toJSONString(this))
        }
    }

    /**
     * 加载章节数据
     */
    fun loadChapter(request: ChapterRequest?) {
        request?.run {
            sendEventToJS(MSG_LOAD_CHAPTER, JSON.toJSONString(this))
        }
    }

    /**
     * 向JS发送消息
     */
    private fun sendEventToJS(msgType: Int, param: String?) {
        val msg = Message.obtain()
        msg.what = msgType
        msg.obj = param
        if (jsReady) {
            webViewHandler?.sendMessage(msg)
        } else {
            cacheMsg.add(msg)
        }
    }

    /**
     * 同步httServer信息 并 发送缓存区的消息
     */
    private fun flushEvent() {
        for (msg in cacheMsg) {
            webViewHandler?.sendMessage(msg)
        }
        cacheMsg.clear()
    }


    /**
     * 执行js方法
     *
     * @param funcName
     * @param param
     * @param todo
     */
    private fun invokeJSFunction(
        funcName: String,
        param: String?,
        todo: Runnable? = null
    ) {
        val sb = StringBuilder()
        sb.append("javascript:")
        sb.append("window:jsBridge")
        sb.append(".")
        sb.append(funcName)
        if (param != null) {
            sb.append("(\'")
            sb.append(param)
            sb.append("\')")
        } else {
            sb.append("(")
            sb.append(")")
        }
        webView?.evaluateJavascript(sb.toString()) { value ->
            LogUtil.d("返回结果：$value")
            todo?.run()
        }
    }


    inner class BookJSBridge {
        @JavascriptInterface
        fun onLife(lifeType: Int) {
            LogUtil.d("onLife:$lifeType")
            if (lifeType == 1) {
                // React Component已经创建 JSBridge对象也已经创建 此时可以进行交互
                monitor.onKey(WEBVIEW_INIT).onEnd()
                jsReady = true
                flushEvent()
            }
        }
    }

    inner class WebViewHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_CREATE_WEBVIEW -> {
                    LogUtil.d("create webview")
                    webView = CommonWebView(context)
                    webView?.run {
                        addJavascriptInterface(BookJSBridge(), "platform")
                        monitor.onKey(WEBVIEW_INIT).onStart()
                        loadUrl(Kiter.PARSE_URL)
                    }
                }
                MSG_SYNC_SERVER -> {
                    LogUtil.d("sync server")
                    val param = msg.obj as String?
                    invokeJSFunction("syncServer", param)
                }
                MSG_OPEN_BOOK -> {
                    LogUtil.d("open book by js")
                    val param = msg.obj as String?
                    invokeJSFunction("openBook", param)
                }
                MSG_LOAD_CHAPTER -> {
                    LogUtil.d("load chapter by js")
                    val param = msg.obj as String?
                    invokeJSFunction("loadChapter", param)
                }
                MSG_RELEASE_WEBVIEW -> {
                    LogUtil.d("release webview")
                    webViewThread?.quitSafely()
                    webView?.run {
                        stopLoading()
                        // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
                        settings.javaScriptEnabled = false
                        clearHistory()
                        this.removeAllViews()
                        destroy()
                    }
                }
            }
        }
    }

}