package com.chineseall.epubparser

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.text.TextUtils
import com.chineseall.epubparser.lib.downloader.DldManager
import com.chineseall.epubparser.lib.util.LogUtil


class EpubApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if(isMainProcess()){
            DldManager.init(this)
        }
    }

    /**
     * 判断主进程
     *
     * @return
     */
    private fun isMainProcess(): Boolean {
        val myPid = android.os.Process.myPid()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //RunningAppProcessInfo应该是Task list有的进程吧，不然被启动的进程也就算了，怎么着最次也得是后台进程
        var myProcess: ActivityManager.RunningAppProcessInfo? = null
        val runningProcesses = activityManager.runningAppProcesses
        if (runningProcesses != null) {
            val var11 = runningProcesses.iterator()
            while (var11.hasNext()) {
                val process = var11.next() as ActivityManager.RunningAppProcessInfo
                LogUtil.d("process=" + process.processName + " pid=" + process.pid)
                if (process.pid == myPid) {
                    myProcess = process
                    break
                }
            }
        }

        return if (myProcess == null || TextUtils.isEmpty(myProcess.processName)) {
            LogUtil.d("Could not find running process for %d$myPid")
            false
        } else {
            LogUtil.d("进程名：" + myProcess.processName)
            myProcess.processName.equals(packageName, ignoreCase = true)
        }
    }
}