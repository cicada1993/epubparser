package com.chineseall.epubparser.lib.core

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.chineseall.epubparser.lib.Kiter
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.render.RenderItem
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.http.RequestBody

@RestController
@RequestMapping("/book")
class BookController {

    @PostMapping("/openResult")
    fun openResult(
        body: RequestBody
    ): String {
        val result = body.string()
        val jsonObj = JSON.parseObject(result)
        val bookKey = jsonObj.getString("bookKey")
        val success = jsonObj.getBoolean("success")
        val data = jsonObj.getString("data")
        val receiver = Kiter.get().findOpenReceiver(bookKey)
        if (success) {
            val book = JSON.parseObject(data, OpfPackage::class.java)
            receiver?.onSuccess(book)
        } else {
            receiver?.onFailed(data)
        }
        val back = JSONObject()
        back["success"] = true
        return back.toJSONString()
    }

    @PostMapping("/chapterResult")
    fun chapterResult(
        body: RequestBody
    ): String {
        val result = body.string()
        val jsonObj = JSON.parseObject(result)
        val bookKey = jsonObj.getString("bookKey")
        val success = jsonObj.getBoolean("success")
        val data = jsonObj.getString("data")
        val receiver = Kiter.get().findChapterReceiver(bookKey)
        if (success) {
            val paragraphs = JSON.parseArray(data, RenderItem::class.java)
            receiver?.onSuccess(paragraphs)
        } else {
            receiver?.onFailed(data)
        }
        val back = JSONObject()
        back["success"] = true
        return back.toJSONString()
    }
}