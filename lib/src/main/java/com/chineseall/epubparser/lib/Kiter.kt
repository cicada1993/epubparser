package com.chineseall.epubparser.lib

import android.content.Context
import android.text.TextUtils
import android.util.SparseArray
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.core.*
import com.chineseall.epubparser.lib.html.Chapter
import com.chineseall.epubparser.lib.request.ChapterRequest
import com.chineseall.epubparser.lib.request.BookRequest
import com.chineseall.epubparser.lib.request.SyncRequest
import com.chineseall.epubparser.lib.util.LogUtil
import com.chineseall.epubparser.lib.util.NetWorkUtil
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.net.InetAddress

class Kiter private constructor() {
    private var bookReceivers = SparseArray<BookReceiver>()
    private var bookMemoryCache = SparseArray<BookMemoryCache>()
    private var httpServer: Server? = null
    private var ip: String = "0.0.0.0"
    private var port: Int = 9696
    private var monitor = TimeCostMonitor()

    companion object {
        private val instance: Kiter by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { Kiter() }
        val PARSE_URL = "file:///android_asset/learnyard/index.html"
        @JvmStatic
        fun get(): Kiter {
            return instance
        }
    }

    fun initServer(context: Context) {
        checkServer(context)
    }

    /**
     * [bookKey] 图书唯一标识
     * [bookPath] 图书路径 可以是相对sd卡根目录 或 sd卡 或 assets目录
     * [zipped]
     */
    fun openLocalBook(
        context: Context,
        bookKey: String,
        bookPath: String,
        zipped: Boolean = true,
        receiver: BookReceiver? = null
    ) {
        val bookUrl = "http://$ip:$port${bookPath}"
        val backUrl = "http://$ip:$port$PATH_BOOK_RESULT"
        val bookKeyHash = bookKey.hashCode()
        if (receiver != null) {
            bookReceivers.put(bookKeyHash, receiver)
        }
        val cache = bookMemoryCache.get(bookKeyHash)
        val bookCache = cache?.book
        if (bookCache != null) {
            LogUtil.d("本地缓存")
            receiver?.bookSuccess(bookCache)
            return
        }
        checkServer(context, object : ServerCallback {
            override fun onSuccess() {
                BookService.openBook(
                    context,
                    BookRequest(bookKey, bookUrl, zipped, backUrl)
                )
            }

            override fun onFailed(msg: String?) {
                receiver?.bookFailed(bookKey, msg)
            }
        })
    }

    /**
     *
     */
    fun openServerBook(
        context: Context,
        bookKey: String,
        bookUrl: String,
        zipped: Boolean = true,
        receiver: BookReceiver? = null
    ) {
        val backUrl = "http://$ip:$port$PATH_BOOK_RESULT"
        val bookKeyHash = bookKey.hashCode()
        if (receiver != null) {
            bookReceivers.put(bookKeyHash, receiver)
        }
        val cache = bookMemoryCache.get(bookKeyHash)
        val bookCache = cache?.book
        if (bookCache != null) {
            LogUtil.d("本地缓存")
            receiver?.bookSuccess(bookCache)
            return
        }
        checkServer(context, object : ServerCallback {
            override fun onSuccess() {
                BookService.openBook(
                    context,
                    BookRequest(bookKey, bookUrl, zipped, backUrl)
                )
            }

            override fun onFailed(msg: String?) {
                receiver?.bookFailed(bookKey, msg)
            }
        })
    }

    fun loadChapter(
        context: Context,
        bookKey: String,
        bookUrl: String,
        zipped: Boolean = true,
        chapterIndex: Int,
        receiver: BookReceiver?
    ) {
        val backUrl = "http://$ip:$port$PATH_CHAPTER_RESULT"
        val bookKeyHash = bookKey.hashCode()
        if (receiver != null) {
            bookReceivers.put(bookKeyHash, receiver)
        }
        val cache = bookMemoryCache.get(bookKeyHash)
        val chapterCache = cache?.chapters?.get(chapterIndex)
        if (chapterCache != null) {
            LogUtil.d("本地缓存")
            receiver?.chapterSuccess(chapterCache)
            return
        }
        checkServer(context, object : ServerCallback {
            override fun onSuccess() {
                BookService.loadChapter(
                    context,
                    ChapterRequest(
                        bookKey,
                        bookUrl,
                        zipped,
                        chapterIndex,
                        backUrl
                    )
                )
            }

            override fun onFailed(msg: String?) {
                receiver?.chapterFailed(bookKey, chapterIndex, msg)
            }
        })
    }

    private fun checkServer(context: Context, callback: ServerCallback? = null) {
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
                            callback?.onFailed(e?.message)
                        }

                        override fun onStarted() {
                            BookService.syncServer(context, SyncRequest(ip, port))
                            callback?.onSuccess()
                            monitor.onKey(HTTP_SERVER_INIT).onEnd()
                        }

                        override fun onStopped() {

                        }
                    }).build()
                monitor.onKey(HTTP_SERVER_INIT).onStart()
                server.startup()
                httpServer = server
            }
        } catch (e: Exception) {
            callback?.onFailed(e.message)
        }
    }

    fun cacheBook(book: OpfPackage) {
        val bookKeyHash = book.bookPlot!!.bookKey.hashCode()
        var cache = bookMemoryCache.get(bookKeyHash)
        if (cache == null) {
            cache = BookMemoryCache()
            bookMemoryCache.put(bookKeyHash, cache)
        }
        cache.book = book
    }

    fun cacheChapter(chapter: Chapter) {
        val bookKeyHash = chapter.bookKey!!.hashCode()
        var cache = bookMemoryCache.get(bookKeyHash)
        if (cache == null) {
            cache = BookMemoryCache()
            bookMemoryCache.put(bookKeyHash, cache)
        }
        cache.cacheChapter(chapter)
    }

    fun findBookReceiver(bookKey: String): BookReceiver? {
        val bookKeyHash = bookKey.hashCode()
        return bookReceivers.get(bookKeyHash)
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
