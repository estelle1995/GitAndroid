package com.example.myokdownload.sample

import android.os.Bundle
import com.example.myokdownload.R
import com.example.myokdownload.databinding.ActivityQueueBinding
import com.example.myokdownload.dowload.DownloadTask
import com.example.myokdownload.sample.util.queue.QueueRecyclerAdapter

class QueueActivity: BaseSampleActivity() {

    override fun titleRes(): Int = R.string.queue_download_title

    private lateinit var binding: ActivityQueueBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}