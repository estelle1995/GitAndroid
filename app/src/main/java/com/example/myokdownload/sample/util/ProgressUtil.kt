package com.example.myokdownload.sample.util

import android.widget.ProgressBar

object ProgressUtil {

    fun calcProgressToViewAndMark(bar: ProgressBar, offset: Long, total: Long) {
        calcProgressToViewAndMark(bar, offset, total, true)
    }

    fun updateProgressToViewWithMark(bar: ProgressBar, currentOffset: Long) {
        updateProgressToViewWithMark(bar, currentOffset, true)
    }

    private fun updateProgressToViewWithMark(
        bar: ProgressBar, currentOffset: Long,
        anim: Boolean
    ) {
        if (bar.tag == null) return

        val shrinkRate = bar.tag as Int
        val progress = ((currentOffset) / shrinkRate).toInt()

        bar.setProgress(progress, anim)
    }

    private fun calcProgressToViewAndMark(bar: ProgressBar, offset: Long, total: Long, anim: Boolean) {
        val contentLengthOnInt = reducePrecision(total)
        val shrinkRate = if (contentLengthOnInt == 0) 1 else (total / contentLengthOnInt).toInt()
        bar.tag = shrinkRate
        val progress = offset / shrinkRate

        bar.max = contentLengthOnInt
        bar.setProgress(progress.toInt(), anim)
    }

    private fun reducePrecision(origin: Long): Int {
        if (origin <= Int.MAX_VALUE) return origin.toInt()

        var shrinkRate = 10
        var result = origin;
        while (result > Integer.MAX_VALUE) {
            result /= shrinkRate;
            shrinkRate *= 5;
        }

        return result.toInt()
    }
}