package com.example.myokdownload.sample.util.queue

import android.content.Context
import com.example.myokdownload.dowload.DownloadContext
import com.example.myokdownload.dowload.DownloadContextListener
import com.example.myokdownload.dowload.DownloadTask
import java.io.File

class QueueController {
    private val taskList = arrayListOf<DownloadTask>()
    private var context: DownloadContext? = null
    private val listener = QueueListener()
    private var queueDir: File? = null

    fun initTasks(context: Context, listener: DownloadContextListener) {
    }
}