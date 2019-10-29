package com.chineseall.epubparser.lib.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.chineseall.epubparser.lib.request.ChapterRequest
import com.chineseall.epubparser.lib.request.OpenRequest
import com.chineseall.epubparser.lib.util.LogUtil

/**
 * 图书服务
 */
class BookService : Service() {
    private var delegate: BookServiceDelegate? = null

    companion object {
        const val TAG = "BookService"
        const val OPEN_REQUEST = "openRequest"
        const val CHAPTER_REQUEST = "chapterRequest"
        const val ACTION_OPEN_BOOK = "openBook"
        const val ACTION_LOAD_CHAPTER = "loadChapter"

        fun openBook(context: Context, openRequest: OpenRequest) {
            val intent = Intent(context, BookService::class.java)
            intent.action = ACTION_OPEN_BOOK
            intent.putExtra(OPEN_REQUEST, openRequest)
            context.startService(intent)
        }

        fun loadChapter(context: Context, chapterRequest: ChapterRequest) {
            val intent = Intent(context, BookService::class.java)
            intent.action = ACTION_LOAD_CHAPTER
            intent.putExtra(CHAPTER_REQUEST, chapterRequest)
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        setForeground()
        if (delegate == null) {
            delegate = BookServiceDelegate(this)
        }
        delegate?.onCreate()
    }

    private fun setForeground() {
        val pid = android.os.Process.myPid()
        val notiManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notiChannel = NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_HIGH)
            notiChannel.enableLights(false)
            notiChannel.enableVibration(false)
            notiChannel.vibrationPattern = LongArray(0)
            notiChannel.setSound(null, null)
            notiManager.createNotificationChannel(notiChannel)
        }
        val notiBuilder = Notification.Builder(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notiBuilder.setChannelId(TAG)
        }
        notiBuilder.setVibrate(LongArray(0))
        notiBuilder.setSound(null)
        val notification = notiBuilder.build()
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        notification.flags = notification.flags or Notification.FLAG_FOREGROUND_SERVICE
        startForeground(pid, notification)
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_OPEN_BOOK -> {
                val openRequest = intent.getSerializableExtra(OPEN_REQUEST) as OpenRequest?
                delegate?.openBook(openRequest)
            }
            ACTION_LOAD_CHAPTER -> {
                val chapterRequest = intent.getSerializableExtra(CHAPTER_REQUEST) as ChapterRequest?
                delegate?.loadChapter(chapterRequest)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d("BookService destroyed")
        delegate?.onDestroy()
    }

}