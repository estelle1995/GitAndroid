package com.example.myokdownload.sample.util

import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.example.myokdownload.dowload.core.Util
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo

object EachBlockProgressUtil {
    private const val TAG: String = "EachBlockProgressUtil"

    fun updateProgress(progressBar: ProgressBar, currentOffset: Long) {
        ProgressUtil.updateProgressToViewWithMark(progressBar, currentOffset)
    }

    fun getSpeedTv(
        blockIndex: Int,
        block0SpeedTv: TextView?,
        block1SpeedTv: TextView?,
        block2SpeedTv: TextView?,
        block3SpeedTv: TextView?
    ): TextView? {
        return if (blockIndex == 0) {
            block0SpeedTv
        } else if (blockIndex == 1) {
            block1SpeedTv
        } else if (blockIndex == 2) {
            block2SpeedTv
        } else if (blockIndex == 3) {
            block3SpeedTv
        } else {
            null
        }
    }

    fun getProgressBar(blockIndex: Int, block0Pb: ProgressBar, block1Pb: ProgressBar, block2Pb: ProgressBar, block3Pb: ProgressBar): ProgressBar? {
        return when (blockIndex) {
            0 -> block0Pb
            1 -> block1Pb
            2 -> block2Pb
            3 -> block3Pb
            else -> null
        }
    }

    fun initProgress(info: BreakpointInfo, taskPb: ProgressBar,
                     block0Pb: ProgressBar, block1Pb: ProgressBar,
                     block2Pb: ProgressBar, block3Pb: ProgressBar) {


        // task
        ProgressUtil.calcProgressToViewAndMark(
            taskPb,
            info.totalOffset, info.totalLength
        )


        // blocks
        val blockCount = info.blockCount
        for (blockIndex in 0 until blockCount) {
            val blockInfo = info.getBlock(blockIndex)
            if (blockIndex == 0) {
                ProgressUtil.calcProgressToViewAndMark(
                    block0Pb, blockInfo.currentOffset,
                    blockInfo.contentLength
                )
            } else if (blockIndex == 1) {
                ProgressUtil.calcProgressToViewAndMark(
                    block1Pb, blockInfo.currentOffset,
                    blockInfo.contentLength
                )
            } else if (blockIndex == 2) {
                ProgressUtil.calcProgressToViewAndMark(
                    block2Pb, blockInfo.currentOffset,
                    blockInfo.contentLength
                )
            } else if (blockIndex == 3) {
                ProgressUtil.calcProgressToViewAndMark(
                    block3Pb, blockInfo.currentOffset,
                    blockInfo.contentLength
                )
            } else {
                Log.w(
                    TAG,
                    "no more progress to display block: $blockInfo"
                )
            }
        }
    }

    fun initTitle(info: BreakpointInfo, taskTitleTv: TextView,
                         block0TitleTv: TextView, block1TitleTv: TextView,
                         block2TitleTv: TextView, block3TitleTv: TextView) {
        // task
        assembleTitleToView("Task", 0, info.totalLength, taskTitleTv)

        //blocks
        val blockCount = info.blockCount;
        for (blockIndex in 0 until blockCount) {
            val blockInfo: BlockInfo = info.getBlock(blockIndex)
            if (blockIndex == 0) {
                assembleTitleToView(
                    "Block0",
                    blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                    block0TitleTv
                )
            } else if (blockIndex == 1) {
                assembleTitleToView(
                    "Block1",
                    blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                    block1TitleTv
                )
            } else if (blockIndex == 2) {
                assembleTitleToView(
                    "Block2",
                    blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                    block2TitleTv
                )
            } else if (blockIndex == 3) {
                assembleTitleToView(
                    "Block3",
                    blockInfo.getRangeLeft(), blockInfo.getRangeRight(),
                    block3TitleTv
                )
            } else {
                Log.w(
                    TAG,
                    "no more title view to display block: $blockInfo"
                )
            }
        }
    }

    fun assembleTitleToView(prefix: String, rangeLeft: Long, rangeRight: Long, titleTv: TextView) {
        val readableRangeLeft = Util.humanReadableBytes(rangeLeft, true)
        val readableRangeRight = Util.humanReadableBytes(rangeRight, true)

        val readableRange = "($readableRangeLeft~$readableRangeRight)"
        val title = prefix + readableRange
        titleTv.text = title
    }
}