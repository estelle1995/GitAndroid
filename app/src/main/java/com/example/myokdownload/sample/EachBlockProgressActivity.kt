package com.example.myokdownload.sample

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.example.myokdownload.R
import com.example.myokdownload.databinding.ActivityEachBlockProgressBinding
import com.example.myokdownload.dowload.DownloadTask
import com.example.myokdownload.dowload.SpeedCalculator
import com.example.myokdownload.dowload.StatusUtil
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo
import com.example.myokdownload.dowload.core.cause.EndCause
import com.example.myokdownload.dowload.core.listener.DownloadListener4WithSpeed
import com.example.myokdownload.dowload.core.listener.assist.Listener4SpeedAssistExtend
import com.example.myokdownload.sample.util.DemoUtil
import com.example.myokdownload.sample.util.EachBlockProgressUtil
import java.io.File

class EachBlockProgressActivity: BaseSampleActivity() {
    override fun titleRes() = R.string.each_block_progress_title

    private var task: DownloadTask? = null

    private lateinit var binding: ActivityEachBlockProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEachBlockProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        task = createTask()
        initStatus()
        initAction()
    }

    private fun initStatus() {
        val status = task?.let { StatusUtil.getStatus(it) }
        binding.statusTv.text = status.toString()
        if (status == StatusUtil.Status.COMPLETED)
            binding.taskPb.progress = binding.taskPb.max

        task?.let { StatusUtil.getCurrentInfo(it) }?.let {
            Log.d(TAG, "init status with: $it")
            EachBlockProgressUtil
                .initTitle(
                    it, binding.taskTitleTv, binding.block0TitleTv, binding.block1TitleTv, binding.block2TitleTv,
                    binding.block3TitleTv
                )
            EachBlockProgressUtil.initProgress(it, binding.taskPb, binding.block0Pb, binding.block1Pb, binding.block2Pb, binding.block3Pb)
        }
    }

    private fun initAction() {
        binding.actionTv.setText(R.string.start)

        binding.actionView.setOnClickListener {
            val started = task?.tag != null
            if (started) task?.cancel()
            else {
                binding.actionTv.setText(R.string.cancel)
                startTask()

                // mark
                task?.tag = "mark-task-started"
            }
        }

        binding.startSameTaskView.setOnClickListener {
            val started = task?.tag != null
            if (!started) return@setOnClickListener
            val task = createTask()
            task.enqueue(EachBlockProgressUtil.createSampleListener(binding.extInfoTv));
        }

        binding.startSameFileView.setOnClickListener {
            val started = task?.tag != null
            if (!started) return@setOnClickListener

            val sameFileAnotherUrlTask = createSameFileAnotherUrlTask()
            sameFileAnotherUrlTask.enqueue(EachBlockProgressUtil.createSampleListener(binding.extInfoTv))
        }
    }

    private fun createSameFileAnotherUrlTask(): DownloadTask {
        val anotherUrl =
            "http://dldir1.qq.com/weixin/android/seixin6516android1120.apk"
        return createTask(anotherUrl)
    }

    private fun startTask() {
        task?.enqueue(object: DownloadListener4WithSpeed() {
            override fun taskStart(task: DownloadTask) {
                binding.statusTv.setText(R.string.task_start)
            }

            override fun connectStart(
                task: DownloadTask,
                blockIndex: Int,
                requestHeaderFields: MutableMap<String, MutableList<String>>
            ) {
                val status = "connectStart $blockIndex $requestHeaderFields"
                binding.statusTv.text = status
            }

            override fun connectEnd(
                task: DownloadTask,
                blockIndex: Int,
                responseCode: Int,
                responseHeaderFields: MutableMap<String, MutableList<String>>
            ) {
                val status = ("connectEnd " + blockIndex + " " + responseCode + " "
                        + responseHeaderFields)
                binding.statusTv.text = status
            }

            override fun taskEnd(
                task: DownloadTask,
                cause: EndCause,
                realCause: Exception?,
                taskSpeed: SpeedCalculator
            ) {
                binding.statusTv.setText(cause.toString())
                binding.taskSpeedTv.setText(taskSpeed.averageSpeed())

                binding.actionTv.setText(R.string.start)

                // mark
                task.tag = null
            }

            override fun infoReady(
                task: DownloadTask,
                info: BreakpointInfo,
                fromBreakpoint: Boolean,
                model: Listener4SpeedAssistExtend.Listener4SpeedModel
            ) {
                EachBlockProgressUtil
                    .initTitle(
                        info, binding.taskTitleTv, binding.block0TitleTv, binding.block1TitleTv, binding.block2TitleTv,
                        binding.block3TitleTv
                    )
                EachBlockProgressUtil
                    .initProgress(info, binding.taskPb, binding.block0Pb, binding.block1Pb, binding.block2Pb, binding.block3Pb)
            }

            override fun progressBlock(
                task: DownloadTask,
                blockIndex: Int,
                currentBlockOffset: Long,
                blockSpeed: SpeedCalculator
            ) {
                val progressBar: ProgressBar? = EachBlockProgressUtil
                    .getProgressBar(blockIndex, binding.block0Pb, binding.block1Pb, binding.block2Pb, binding.block3Pb)

                if (progressBar != null) {
                    EachBlockProgressUtil.updateProgress(progressBar, currentBlockOffset)
                }

                val speedTv: TextView? = EachBlockProgressUtil.getSpeedTv(
                    blockIndex,
                    binding.block0SpeedTv, binding.block1SpeedTv, binding.block2SpeedTv, binding.block3SpeedTv
                )

                if (speedTv != null) speedTv.text = blockSpeed.speed()

            }

            override fun progress(
                task: DownloadTask,
                currentOffset: Long,
                taskSpeed: SpeedCalculator
            ) {
                binding.statusTv.setText(R.string.fetch_progress)

                EachBlockProgressUtil.updateProgress(binding.taskPb, currentOffset)
                binding.taskSpeedTv.text = taskSpeed.speed()
            }

            override fun blockEnd(
                task: DownloadTask,
                blockIndex: Int,
                info: BlockInfo?,
                blockSpeed: SpeedCalculator
            ) {
                val speedTv = EachBlockProgressUtil.getSpeedTv(
                    blockIndex,
                    binding.block0SpeedTv, binding.block1SpeedTv, binding.block2SpeedTv, binding.block3SpeedTv
                )

                if (speedTv != null) speedTv.text = blockSpeed.averageSpeed()
            }

        })
    }

    private fun createTask(): DownloadTask {
        val url =
            "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk"
        return createTask(url)
    }

    private fun createTask(url: String): DownloadTask {
        val filename = "each-block-progress-test"
        val parentFile: File = DemoUtil.getParentFile(this)
        return DownloadTask.Builder(url, parentFile)
            .setFilename(filename) // the minimal interval millisecond for callback progress
            .setMinIntervalMillisCallbackProcess(64) // ignore the same task has already completed in the past.
            .setPassIfAlreadyCompleted(false)
            .build()
    }

    companion object {
        private const val TAG: String = "EachBlockProgress"
    }
}