package com.example.myokdownload

import android.os.Bundle
import com.example.myokdownload.databinding.ActivitySingleBinding
import com.example.myokdownload.dowload.DownloadTask
import com.example.myokdownload.util.DemoUtil

class SingleActivity: BaseSampleActivity() {
    private lateinit var binding: ActivitySingleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initTask()
        initStatus()
    }

    var task: DownloadTask? = null;

    private fun initTask() {
        val filename = "single-test"
        val url = "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk"
        val parentFile = DemoUtil.getParentFile(this)
        task = DownloadTask.Builder(url, parentFile)
            .setFilename(filename)
            .setMinIntervalMillisCallbackProcess(16)
            .setPassIfAlreadyCompleted(false)
            .build()
    }

    private fun initStatus() {

    }

    override fun titleRes(): Int {
        return R.string.single_download_title
    }
}