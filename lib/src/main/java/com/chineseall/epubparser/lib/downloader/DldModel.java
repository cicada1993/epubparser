package com.chineseall.epubparser.lib.downloader;

import android.text.TextUtils;

import com.chineseall.epubparser.lib.util.LogUtil;
import com.chineseall.epubparser.lib.util.MD5Util;

import java.io.File;

/**
 * 任务基础数据模型
 */
public class DldModel {
    public final String url;  // 下载地址
    public final String folder;// 文件保存目录z
    public final String name;// 文件名
    public final String filePath; // 文件路径
    public final String key; // model的唯一标识

    public DldModel(Builder builder) {
        this.url = builder.url;
        if (TextUtils.isEmpty(builder.folder)) {
            this.folder = DldManager.folderPath;
        } else {
            this.folder = builder.folder;
        }
        if (TextUtils.isEmpty(builder.key)) {
            this.key = MD5Util.getMD5Code(this.url + "@dir" + this.folder);
        } else {
            this.key = builder.key;
        }
        if (TextUtils.isEmpty(builder.name)) {
            this.name = this.key;
        } else {
            this.name = builder.name;
        }
        this.filePath = this.folder + File.separator + this.name;
    }


    public static class Builder {
        String key;
        String url;
        String folder;
        String name;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder folder(String filePath) {
            this.folder = filePath;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public DldModel build() {
            if (TextUtils.isEmpty(url)) {
                LogUtil.d("download url can not be empty");
                return null;
            }
            return new DldModel(this);
        }
    }
}
