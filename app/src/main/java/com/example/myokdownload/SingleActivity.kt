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
        initAction()
    }

    var task: DownloadTask? = null;

    private fun initTask() {
        val filename = "single-test"
        val url = "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk"
        val parentFile = DemoUtil.getParentFile(this)
//        task = DownloadTask.Builder(url, parentFile)
//            .setFilename(filename)
//            .setMinIntervalMillisCallbackProcess(16)
//            .setPassIfAlreadyCompleted(false)
//            .build()
    }

    private fun initStatus() {
        task?.let {
//            val status = StatusUtil.getStatus(it)
//            if (status == StatusUtil.Status.COMPLETED) {
//                binding.progressBar.progress = binding.progressBar.max;
//            }
//            binding.statusTv.text = status.toString()
//            StatusUtil.getCurrentInfo(it)?.let { info ->
//                DemoUtil.calcProgressToView(binding.progressBar, info.totalOffset, info.totalLength)
//            }
        }
    }

    private fun initAction() {
//        binding.actionTv.setText(R.string.start)
//        binding.actionView.setOnClickListener {
//            task?.let {
//                if (it.tag != null) {
//                    it.cancel()
//                }
//            }
//        }
    }

    override fun titleRes(): Int {
        return R.string.single_download_title
    }
}