package com.chineseall.epubparser

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSON
import com.chineseall.epubparser.lib.Kiter
import com.chineseall.epubparser.lib.book.OpfPackage
import com.chineseall.epubparser.lib.core.BOOK_OPEN
import com.chineseall.epubparser.lib.core.BookReceiver
import com.chineseall.epubparser.lib.core.CHAPTER_LOAD
import com.chineseall.epubparser.lib.core.TimeCostMonitor
import com.chineseall.epubparser.lib.html.Chapter
import com.chineseall.epubparser.lib.render.Page
import com.chineseall.epubparser.lib.render.ReaderView
import com.chineseall.epubparser.lib.render.RenderReceiver
import com.chineseall.epubparser.lib.util.MD5Util
import pub.devrel.easypermissions.*
import java.util.*

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks, View.OnClickListener, ScanResultBus.Consumer {
    private val REQ_PERMISSION = 0x100
    private val REQ_SETTING = 0x200
    private lateinit var urlET: EditText
    private lateinit var openUnzippedBtn: Button
    private lateinit var openZippedBtn: Button
    private lateinit var bookInfoTV: TextView
    private lateinit var chapterIndexET: EditText
    private lateinit var chapterReadBtn: Button
    private lateinit var openCostTimeTV: TextView
    private lateinit var chapterCostTimeTV: TextView
    private lateinit var readerView: ReaderView
    private lateinit var chapterTitleTV: TextView
    private lateinit var readerCloseTV: TextView
    private lateinit var readerControlLayout: View
    private lateinit var pagePreTV: TextView
    private lateinit var pageNextTV: TextView
    private lateinit var pageProgressTV: TextView
    private var book: OpfPackage? = null
    private var monitor = TimeCostMonitor()
    private var totalPage = 0
    private var curPage = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndReqPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.reset -> {
                reset()
            }
            R.id.scan -> {
                scan()
            }
        }
        return true
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

    fun initPage() {
        urlET = findViewById(R.id.et_book_url)
        openUnzippedBtn = findViewById(R.id.btn_open_unzipped)
        openZippedBtn = findViewById(R.id.btn_open_zipped)
        bookInfoTV = findViewById(R.id.tv_bookinfo)

        chapterIndexET = findViewById(R.id.et_chapter_index)
        chapterReadBtn = findViewById(R.id.btn_chapter_read)
        openCostTimeTV = findViewById(R.id.tv_open_cost_time)
        chapterCostTimeTV = findViewById(R.id.tv_chapter_cost_time)

        readerView = findViewById(R.id.view_reader)
        chapterTitleTV = findViewById(R.id.tv_chapter_title)
        readerCloseTV = findViewById(R.id.tv_read_close)
        readerControlLayout = findViewById(R.id.layout_read_control)
        pagePreTV = findViewById(R.id.tv_pre_page)
        pageNextTV = findViewById(R.id.tv_next_page)
        pageProgressTV = findViewById(R.id.tv_page_progress)

        openUnzippedBtn.setOnClickListener(this)
        openZippedBtn.setOnClickListener(this)
        chapterReadBtn.setOnClickListener(this)
        readerCloseTV.setOnClickListener(this)
        pagePreTV.setOnClickListener(this)
        pageNextTV.setOnClickListener(this)
        ScanResultBus.register(this)
    }

    override fun onScanResult(result: String?) {
        urlET.setText(result)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_open_unzipped -> {
                openBook(false)
            }
            R.id.btn_open_zipped -> {
                openBook(true)
            }
            R.id.btn_chapter_read -> {
                loadChapter()
            }
            R.id.tv_read_close -> {
                stopRead()
            }
            R.id.tv_pre_page -> {
                prePage()
            }
            R.id.tv_next_page -> {
                nextPage()
            }
        }
    }

    private val bookReceiver = object : BookReceiver() {
        override fun bookSuccess(book: OpfPackage) {
            fillBookInfo(book)
        }

        override fun bookFailed(bookKey: String?, msg: String?) {

        }

        override fun chapterSuccess(chapter: Chapter) {
            fillChapterContent(chapter)
        }

        override fun chapterFailed(bookKey: String?, chapterIndex: Int?, msg: String?) {

        }
    }

    private val renderReceiver = object : RenderReceiver {
        override fun onPages(chapterIndex: Int, pages: MutableList<Page>) {
            totalPage = pages.size
        }

        override fun onRenderPage(page: Page, index: Int) {
            curPage = index
            pageProgressTV.text = "$curPage / $totalPage"
        }
    }

    fun openBook(zipped: Boolean) {
        val bookUrl = urlET.text.toString()
        if (bookUrl.isNullOrEmpty()) {

        } else {
            book = null
            monitor.onKey(BOOK_OPEN).onStart()
            Kiter.get().openServerBook(
                this,
                MD5Util.getMD5Code(bookUrl),
                bookUrl,
                zipped,
                bookReceiver
            )
        }
    }

    fun fillBookInfo(book: OpfPackage) {
        this.book = book
        runOnUiThread {
            val openGap = monitor.onKey(BOOK_OPEN).onEnd().gap()
            openCostTimeTV.text =
                if (openGap > 1000) "${String.format("%.2f", openGap / 1000f)}s" else "${openGap}ms"
            val chapterCount = book.spine?.size ?: 0
            chapterIndexET.hint = "输入章节编号（0—${chapterCount - 1}）"
            book.metadata?.let {
                bookInfoTV.text = JSON.toJSONString(it)
            }
        }
    }

    fun loadChapter() {
        book?.bookPlot?.run {
            monitor.onKey(CHAPTER_LOAD).onStart()
            val chapterIndex = chapterIndexET.text.toString().toInt()
            Kiter.get().loadChapter(
                this@MainActivity,
                bookKey!!,
                bookUrl!!,
                zipped!!,
                chapterIndex,
                bookReceiver
            )
        }
    }

    fun fillChapterContent(chapter: Chapter) {
        runOnUiThread {
            val chapterGap = monitor.onKey(CHAPTER_LOAD).onEnd().gap()
            chapterCostTimeTV.text =
                if (chapterGap > 1000) "${String.format(
                    "%.2f",
                    chapterGap / 1000f
                )}s" else "${chapterGap}ms"
            startRead(chapter)
        }
    }

    fun startRead(chapter: Chapter) {
        readerView.post {
            readerCloseTV.visibility = View.VISIBLE
            readerControlLayout.visibility = View.VISIBLE
            readerView.visibility = View.VISIBLE
            chapterTitleTV.visibility = View.VISIBLE
            val chapterIndex = chapterIndexET.text.toString().toInt()
            chapterTitleTV.text = "第${chapterIndex + 1}章"
            readerView.render(book, chapter, renderReceiver)
        }
    }


    fun nextPage() {
        readerView.nextPage()
    }

    fun prePage() {
        readerView.prePage()
    }

    fun stopRead() {
        readerCloseTV.visibility = View.INVISIBLE
        readerControlLayout.visibility = View.INVISIBLE
        readerView.visibility = View.INVISIBLE
        chapterTitleTV.visibility = View.INVISIBLE
        chapterTitleTV.text = ""
        readerView.clear()
    }

    fun reset() {
        book = null
        bookInfoTV.text = ""
        readerView.clear()
        urlET.setText("")
        chapterIndexET.setText("")
        chapterIndexET.hint = "输入章节编号"
        openCostTimeTV.text = ""
        chapterCostTimeTV.text = ""
        chapterTitleTV.text = ""
        totalPage = 0
        curPage = 0
    }

    fun scan() {
        ScanActivity.start(this)
    }
}

