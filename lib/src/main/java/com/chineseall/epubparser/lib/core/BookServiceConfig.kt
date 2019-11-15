package com.chineseall.epubparser.lib.core

import android.content.Context
import com.chineseall.epubparser.lib.util.FileUtil
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.AssetsWebsite
import com.yanzhenjie.andserver.framework.website.FileBrowser

@Config
class BookServiceConfig : WebConfig {
    override fun onConfig(context: Context?, delegate: WebConfig.Delegate?) {
        if (context != null && delegate != null) {
            // 配置静态资源文件浏览服务
            // 相对sd卡根目录下的文件
            delegate.addWebsite(FileBrowser(FileUtil.getSDCardRootPath()))
            // 相对sd卡目录Android/data/包名/files 下的文件
            delegate.addWebsite(FileBrowser(FileUtil.getSDCardAppFilePath(context)))
            // 相对于应用内部 data/data/包名下的文件
            delegate.addWebsite(FileBrowser(FileUtil.getMemoryAppDataPath(context)))
            // 配置assets静态资源服务
            delegate.addWebsite(AssetsWebsite(context, "/learnyard"))
        }
    }
}