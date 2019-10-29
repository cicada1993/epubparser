package com.chineseall.epubparser.lib

import android.content.Context
import android.text.TextUtils
import com.chineseall.epubparser.lib.core.ChapterReceiver
import com.chineseall.epubparser.lib.core.OpenReceiver
import com.chineseall.epubparser.lib.core.BookService
import com.chineseall.epubparser.lib.request.ChapterRequest
import com.chineseall.epubparser.lib.request.OpenRequest
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.util.NetWorkUtil
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.net.InetAddress

class Kiter private constructor() {
    private var openReceivers = HashMap<String, OpenReceiver>()
    private var chapterReceivers = HashMap<String, ChapterReceiver>()
    private var httpServer: Server? = null
    private var ip: String = "0.0.0.0"
    private var port: Int = 9696

    companion object {
        private val instance: Kiter by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { Kiter() }
        val PARSE_URL = "file:///android_asset/learnyard/index.html"
        @JvmStatic
        fun get(): Kiter {
            return instance
        }
    }

    /**
     * 打开图书
     */
    fun openBook(
        context: Context,
        bookKey: String,
        bookPath: String,
        bookUnzipPath: String,
        receiver: OpenReceiver? = null
    ) {
        if (receiver != null) {
            openReceivers[bookKey] = receiver
        }
        checkServer(context, object : ServerCallback {
            override fun onSuccess() {
                BookService.openBook(
                    context,
                    OpenRequest(ip, port, bookKey, bookPath, bookUnzipPath)
                )
            }

            override fun onFailed(msg: String?) {
                receiver?.onFailed(msg)
            }
        })

    }

    fun loadChapter(
        context: Context,
        bookKey: String,
        chapterIndex: Int,
        receiver: ChapterReceiver?
    ) {
        if (receiver != null) {
            chapterReceivers[bookKey] = receiver
        }
        checkServer(context, object : ServerCallback {
            override fun onSuccess() {
                BookService.loadChapter(
                    context,
                    ChapterRequest(
                        ip,
                        port,
                        bookKey,
                        chapterIndex
                    )
                )
            }

            override fun onFailed(msg: String?) {
                receiver?.onFailed(msg)
            }
        })
    }

    private fun checkServer(context: Context, callback: ServerCallback?) {
        try {
            val localIp = NetWorkUtil.getLocalIp(context)
            val serverRunning = httpServer?.isRunning == true
            if (serverRunning && TextUtils.equals(ip, localIp)) {
                // 服务正常
                callback?.onSuccess()
            } else {
                if (serverRunning) {
                    stopServer()
                }
                // 需要重启服务
                ip = localIp
                val address = InetAddress.getByName(ip)
                val server = AndServer.serverBuilder(context)
                    .inetAddress(address)
                    .port(port)
                    .listener(object : Server.ServerListener {
                        override fun onException(e: Exception?) {
                            LogUtil.d("http server failed")
                            callback?.onFailed(e?.message)
                        }

                        override fun onStarted() {
                            LogUtil.d("http server started")
                            callback?.onSuccess()
                        }

                        override fun onStopped() {
                            LogUtil.d("http server stopped")
                        }
                    }).build()
                server.startup()
                httpServer = server
            }
        } catch (e: Exception) {
            callback?.onFailed(e.message)
        }
    }

    fun findOpenReceiver(bookKey: String): OpenReceiver? {
        return openReceivers[bookKey]
    }

    fun findChapterReceiver(bookKey: String): ChapterReceiver? {
        return chapterReceivers[bookKey]
    }

    private fun stopServer() {
        httpServer?.run {
            if (isRunning) {
                shutdown()
            }
        }
    }

    fun release() {
        stopServer()
    }

    interface ServerCallback {
        fun onSuccess()
        fun onFailed(msg: String?)
    }
}
