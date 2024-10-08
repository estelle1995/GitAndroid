package com.example.myokdownload

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import com.example.myokdownload.databinding.ActivitySingleBinding
import com.example.myokdownload.dowload.DownloadTask
import com.example.myokdownload.dowload.SpeedCalculator
import com.example.myokdownload.dowload.StatusUtil
import com.example.myokdownload.dowload.core.Util
import com.example.myokdownload.dowload.core.cause.EndCause
import com.example.myokdownload.kotlin_enhance.enqueue4WithSpeed
import com.example.myokdownload.kotlin_enhance.spChannel
import com.example.myokdownload.util.DemoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

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
        task = DownloadTask.Builder(url, parentFile)
            .setFilename(filename)
            .setMinIntervalMillisCallbackProcess(16)
            .setPassIfAlreadyCompleted(false)
            .build()
    }

    private fun initStatus() {
        task?.let {
            val status = StatusUtil.getStatus(it)
            if (status == StatusUtil.Status.COMPLETED) {
                binding.progressBar.progress = binding.progressBar.max;
            }
            binding.statusTv.text = status.toString()
            StatusUtil.getCurrentInfo(it)?.let { info ->
                DemoUtil.calcProgressToView(binding.progressBar, info.totalOffset, info.totalLength)
            }
        }
    }

    private fun initAction() {
        binding.actionTv.setText(R.string.start)
        binding.actionView.setOnClickListener {
            task?.let {
                if (it.tag != null) {
                    it.cancel()
                } else {
                    // to start
                    binding.actionTv.setText(R.string.cancel)
                    startTask()
                    it.tag = "mark-task-started"
                }
            }
        }
    }

    private fun startTask() {
        var totalLength: Long = 0
        var readableTotalLength: String? = null
        task?.enqueue4WithSpeed(
            onTaskStart = { binding.statusTv.setText(R.string.task_start) },
            onInfoReadyWithSpeed = { _, info, _, _ ->
                binding.statusTv.setText(R.string.info_ready)
                totalLength = info.totalLength
                readableTotalLength = Util.humanReadableBytes(totalLength, true)
                DemoUtil.calcProgressToView(binding.progressBar, info.totalOffset, totalLength)
            },
            // First way to show progress.
//            onProgressWithSpeed = { _, currentOffset, taskSpeed ->
//                val readableOffset = Util.humanReadableBytes(currentOffset, true)
//                val progressStatus = "$readableOffset/$readableTotalLength"
//                val speed = taskSpeed.speed()
//                val progressStatusWithSpeed = "$progressStatus($speed)"
//                statusTv.text = progressStatusWithSpeed
//                DemoUtil.calcProgressToView(progressBar, currentOffset, totalLength)
//            },
            onConnectStart = { _, blockIndex, _ ->
                val status = "Connect End $blockIndex"
                binding.statusTv.text = status
            }
        ) { task, cause, realCause, taskSpeed ->
            val statusWithSpeed = cause.toString() + " " + taskSpeed.averageSpeed()
            binding.statusTv.text = statusWithSpeed
            binding.actionTv.setText(R.string.start)
            // remove mark
            task.tag = null
            if (cause == EndCause.COMPLETED) {
                val realMd5 = fileToMD5(task.file!!.absolutePath)
                if (!realMd5!!.equals("f836a37a5eee5dec0611ce15a76e8fd5", ignoreCase = true)) {
                    Log.e(TAG, "file is wrong because of md5 is wrong $realMd5")
                }
            }
            realCause?.let {
                Log.e(TAG, "download error", it)
            }
        }

        // Second way to show progress.
        val speedCalculator = SpeedCalculator()
        CoroutineScope(Dispatchers.Main).launch {
            var lastOffset = 0L
            task?.spChannel()?.consumeEach { dp ->
                val increase = when (lastOffset) {
                    0L -> 0L
                    else -> dp.currentOffset - lastOffset
                }
                lastOffset = dp.currentOffset
                speedCalculator.downloading(increase)
                val readableOffset = Util.humanReadableBytes(dp.currentOffset, true)
                val progressStatus = "$readableOffset/$readableTotalLength"
                val speed = speedCalculator.speed()
                val progressStatusWithSpeed = "$progressStatus($speed)"
                binding.statusTv.text = progressStatusWithSpeed
                DemoUtil.calcProgressToView(binding.progressBar, dp.currentOffset, totalLength)
            }
        }
    }

    override fun titleRes(): Int {
        return R.string.single_download_title
    }


    companion object {

        private const val TAG = "SingleActivity"
        fun fileToMD5(filePath: String): String? {
            var inputStream: InputStream? = null
            try {
                inputStream = FileInputStream(filePath)
                val buffer = ByteArray(1024)
                val digest = MessageDigest.getInstance("MD5")
                var numRead = 0
                while (numRead != -1) {
                    numRead = inputStream.read(buffer)
                    if (numRead > 0) {
                        digest.update(buffer, 0, numRead)
                    }
                }
                val md5Bytes = digest.digest()
                return convertHashToString(md5Bytes)
            } catch (ignored: Exception) {
                return null
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "file to md5 failed", e)
                    }
                }
            }
        }

        @SuppressLint("DefaultLocale")
        private fun convertHashToString(md5Bytes: ByteArray): String = StringBuffer().apply {
            md5Bytes.forEach { byte ->
                append(((byte.toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
        }.toString().toUpperCase()
    }
}