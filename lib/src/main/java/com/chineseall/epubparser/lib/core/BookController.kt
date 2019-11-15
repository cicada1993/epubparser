package com.chineseall.epubparser.lib.core

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.chineseall.epubparser.lib.Kiter
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.downloader.AutoAdjustThreadPool
import com.chineseall.epubparser.lib.html.Chapter
import com.chineseall.epubparser.lib.util.LogUtil
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.http.RequestBody

@RestController
class BookController {
    /**
     * 接收书籍打开结果
     */
    @PostMapping(PATH_BOOK_RESULT)
    fun bookResult(
        body: RequestBody
    ): String {
        LogUtil.d("收到书籍打开结果 当前时间${System.currentTimeMillis()}")
        val result = body.string()
        onBookResult(result)
        val back = JSONObject()
        back["success"] = true
        return back.toJSONString()
    }

    private fun onBookResult(result: String) {
        AutoAdjustThreadPool.get().execute {
            val jsonObj = JSON.parseObject(result)
            val bookKey = jsonObj.getString("bookKey")
            val success = jsonObj.getBoolean("success")
            val data = jsonObj.getString("data")
            val receiver = Kiter.get().findBookReceiver(bookKey)
            if (success) {
                val book = JSON.parseObject(data, OpfPackage::class.java)
                receiver?.bookSuccess(book)
                Kiter.get().cacheBook(book)
            } else {
                receiver?.bookFailed(bookKey, data)
            }
        }
    }

    /**
     * 接收章节内容加载结果
     */
    @PostMapping(PATH_CHAPTER_RESULT)
    fun chapterResult(
        body: RequestBody
    ): String {
        LogUtil.d("收到章节加载结果 当前时间${System.currentTimeMillis()}")
        val result = body.string()
        onChapterResult(result)
        val back = JSONObject()
        back["success"] = true
        return back.toJSONString()
    }

    private fun onChapterResult(result: String) {
        AutoAdjustThreadPool.get().execute {
            val jsonObj = JSON.parseObject(result)
            val bookKey = jsonObj.getString("bookKey")
            val success = jsonObj.getBoolean("success")
            val chapterIndex = jsonObj.getInteger("chapterIndex")
            val data = jsonObj.getString("data")
            val receiver = Kiter.get().findBookReceiver(bookKey)
            if (success) {
                val chapter = JSON.parseObject(data, Chapter::class.java)
                receiver?.chapterSuccess(chapter)
                Kiter.get().cacheChapter(chapter)
            } else {
                receiver?.chapterFailed(bookKey, chapterIndex, data)
            }
        }
    }
}

const val PATH_BOOK_RESULT = "/book/bookResult"
const val PATH_CHAPTER_RESULT = "/book/chapterResult"