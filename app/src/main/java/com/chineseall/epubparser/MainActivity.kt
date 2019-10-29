package com.chineseall.epubparser

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chineseall.epubparser.lib.Kiter
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.core.ChapterReceiver
import com.chineseall.epubparser.lib.core.OpenReceiver
import com.chineseall.epubparser.lib.downloader.DldListener
import com.chineseall.epubparser.lib.downloader.DldManager
import com.chineseall.epubparser.lib.downloader.DldModel
import com.chineseall.epubparser.lib.downloader.DldTask
import com.chineseall.epubparser.lib.render.RenderItem
import com.chineseall.epubparser.lib.util.FileUtil
import com.chineseall.epubparser.lib.util.LogUtil
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import pub.devrel.easypermissions.*
import java.lang.StringBuilder
import java.util.*

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks, View.OnClickListener {
    private val REQ_PERMISSION = 0x100
    private val REQ_SETTING = 0x200
    private lateinit var shIdET: EditText
    private lateinit var urlET: EditText
    private lateinit var downloadBtn: Button
    private lateinit var msgTV: TextView
    private lateinit var unzipBtn: Button
    private lateinit var openBtn: Button
    private lateinit var titleTV: TextView
    private lateinit var creatorTV: TextView
    private lateinit var publisherTV: TextView
    private lateinit var pubdateTV: TextView
    private lateinit var languageTV: TextView
    private lateinit var identifierTV: TextView
    private lateinit var chapterIndexET: EditText
    private lateinit var chapterContentTV: TextView
    private lateinit var chapterLoadBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndReqPermission()
    }

    fun checkAndReqPermission() {
        val denyPermissions = PermissionRegistery.getDenyPermissions(this)
        denyPermissions?.run {
            val keys = first!!
            val descs = second!!
            if (!keys.isNullOrEmpty()) {
                val keyArray = keys.toTypedArray()
                val descArray = descs.toTypedArray()
                EasyPermissions.requestPermissions(
                    PermissionRequest.Builder(this@MainActivity, REQ_PERMISSION, *keyArray)
                        .setRationale("为保证本应用正常运行，请您同意如下权限\n" + Arrays.toString(descArray))
                        .setPositiveButtonText("好的")
                        .setNegativeButtonText("退出")
                        .build()
                )
                return
            }
        }
        initPage()
    }

    /**
     * 检查并提示权限
     */
    fun checkAndNotifyPermission() {
        val denyPermissions = PermissionRegistery.getDenyPermissions(this)
        denyPermissions?.run {
            val keys = first!!
            val descs = second!!
            if (!keys.isNullOrEmpty()) {
                val descArray = descs.toTypedArray()
                if (EasyPermissions.somePermissionPermanentlyDenied(this@MainActivity, keys)) {
                    // 有权限被拒绝
                    AppSettingsDialog.Builder(this@MainActivity)
                        .setTitle("权限设置")
                        .setNegativeButton("退出")
                        .setPositiveButton("去设置")
                        .setRequestCode(REQ_SETTING)
                        .setRationale("为保证本应用正常运行，需开启如下权限\n" + Arrays.toString(descArray))
                        .build()
                        .show()
                } else {
                    checkAndReqPermission()
                }
                return
            }
        }
        initPage()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        checkAndNotifyPermission()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        checkAndNotifyPermission()
    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    override fun onRationaleDenied(requestCode: Int) {
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SETTING) {
            data?.run {
                if (hasExtra(PermissionConst.BACK_SIGN)) {
                    val backSign = data.getStringExtra(PermissionConst.BACK_SIGN)
                    if (TextUtils.equals(backSign, PermissionConst.BACK_FOR_CANCEL)) {
                        finish()
                    } else {
                        checkAndNotifyPermission()
                    }
                }
            }
        }
    }

    private var task: DldTask? = null
    private var unzipDisp: Disposable? = null
    private lateinit var bookRootDir: String
    private lateinit var bookUnzipRootDir: String
    private val bookFolder = "/book"
    private val bookUnzipFolder = "/book/unzip"
    fun initPage() {
        bookRootDir = FileUtil.getAppCachePath(this) + bookFolder + "/"
        bookUnzipRootDir = FileUtil.getAppCachePath(this) + bookUnzipFolder + "/"
        shIdET = findViewById(R.id.et_book_shId)
        urlET = findViewById(R.id.et_book_url)
        downloadBtn = findViewById(R.id.btn_download)
        msgTV = findViewById(R.id.tv_msg)
        unzipBtn = findViewById(R.id.btn_unzip)
        openBtn = findViewById(R.id.btn_open)

        titleTV = findViewById(R.id.tv_title)
        creatorTV = findViewById(R.id.tv_creator)
        publisherTV = findViewById(R.id.tv_publisher)
        pubdateTV = findViewById(R.id.tv_pubdate)
        languageTV = findViewById(R.id.tv_language)
        identifierTV = findViewById(R.id.tv_identifier)
        chapterIndexET = findViewById(R.id.et_chapter_index)
        chapterContentTV = findViewById(R.id.tv_chapter_content)
        chapterLoadBtn = findViewById(R.id.btn_load_chapter)

        downloadBtn.setOnClickListener(this)
        unzipBtn.setOnClickListener(this)
        openBtn.setOnClickListener(this)
        chapterLoadBtn.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_download -> {
                download()
            }
            R.id.btn_unzip -> {
                unzip()
            }
            R.id.btn_open -> {
                open()
            }
            R.id.btn_load_chapter -> {
                loadChapter()
            }
        }
    }

    fun download() {
        val shId = shIdET.text.toString()
        val url = urlET.text.toString()
        if (TextUtils.isEmpty(shId) || TextUtils.isEmpty(url)) {
            return
        }
        val model = DldModel.Builder().url(url).key(shId).folder(bookRootDir).build()

        task = DldManager.get().createTask(model)
        task!!.listener(
            object : DldListener {
                override fun onPending(taskId: Int) {

                }

                override fun onStart(taskId: Int) {

                }

                override fun onConnect(taskId: Int) {

                }

                override fun onProgress(taskId: Int, readBytes: Long, totalBytes: Long) {
                    downloadBtn.isEnabled = false
                    downloadBtn.text = "下载中"
                }

                override fun onComplete(taskId: Int, totalBytes: Long) {
                    downloadBtn.isEnabled = false
                    downloadBtn.text = "已下载"
                }

                override fun onPause(taskId: Int, readBytes: Long, totalBytes: Long) {
                    downloadBtn.isEnabled = true
                    downloadBtn.text = "已暂停"
                }

                override fun onError(taskId: Int, errMsg: String?) {
                    msgTV.text = errMsg
                }
            })
            .notify(true)
            .fresh(false)
            .setUp()

    }

    fun unzip() {
        task?.run {
            val model = dldModel
            val key = model.key
            val filePath = model.filePath
            val destDir = "$bookUnzipRootDir$key/"
            unzipBtn.text = "正在解压"
            unzipDisp = Observable.create(ObservableOnSubscribe<Boolean> {
                it.onNext(FileUtil.unzip(filePath, destDir))
                it.onComplete()
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (it) {
                            unzipBtn.text = "解压完成"
                        } else {
                            unzipBtn.text = "解压失败"
                        }
                    },
                    {

                    },
                    {

                    }
                )
        }
    }

    fun open() {
        LogUtil.d("打开图书")
        task?.run {
            val model = dldModel
            val bookKey = model.key
            val name = model.name
            val bookPath = "$bookFolder/$name"
            val bookUnzipPath = "$bookUnzipFolder/$bookKey/"
            Kiter.get().openBook(
                this@MainActivity,
                bookKey,
                bookPath,
                bookUnzipPath,
                object : OpenReceiver {
                    override fun onSuccess(book: OpfPackage?) {
                        LogUtil.d("打开成功")
                        fillBookInfo(book)
                    }

                    override fun onFailed(msg: String?) {
                        LogUtil.d("打开失败:$msg")
                    }
                })
        }
    }

    fun fillBookInfo(book: OpfPackage?) {
        runOnUiThread {
            book?.metadata?.let { meta ->
                titleTV.text = meta.title ?: ""
                creatorTV.text = meta.creator ?: ""
                publisherTV.text = meta.publisher ?: ""
                pubdateTV.text = meta.pubdate ?: ""
                languageTV.text = meta.language ?: ""
                identifierTV.text = meta.identifier ?: ""
            }
        }
    }

    fun loadChapter() {
        task?.run {
            val model = dldModel
            val bookKey = model.key
            val chapterIndex = chapterIndexET.text.toString().toInt()
            Kiter.get()
                .loadChapter(
                    this@MainActivity,
                    bookKey,
                    chapterIndex,
                    object : ChapterReceiver {
                        override fun onSuccess(paragraphs: MutableList<RenderItem>?) {
                            LogUtil.d("加载成功")
                            fillChapterContent(paragraphs)
                        }

                        override fun onFailed(msg: String?) {
                            LogUtil.d("加载失败:$msg")
                        }
                    })
        }
    }

    fun fillChapterContent(paragraphs: MutableList<RenderItem>?) {
        val sb = StringBuilder()
        paragraphs?.let {
            for (paragraph in it) {
                paragraph.render(sb)
            }
        }
        runOnUiThread{
            chapterContentTV.text = sb
        }
    }
}

