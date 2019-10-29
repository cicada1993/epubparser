package com.chineseall.epubparser.lib.core

import android.content.Context
import com.chineseall.epubparser.lib.util.FileUtil
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.FileBrowser

@Config
class BookServiceConfig : WebConfig {
    override fun onConfig(context: Context?, delegate: WebConfig.Delegate?) {
        // 配置图书静态资源服务
        val filePath = FileUtil.getAppCachePath(context)
        val fileBrowser = FileBrowser(filePath)
        delegate?.addWebsite(fileBrowser)
    }
}