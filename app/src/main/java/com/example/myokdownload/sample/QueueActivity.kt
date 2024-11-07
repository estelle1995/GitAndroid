package com.example.myokdownload.sample

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myokdownload.R
import com.example.myokdownload.databinding.ActivityQueueBinding
import com.example.myokdownload.kotlin_enhance.listener.createDownloadContextListener
import com.example.myokdownload.sample.util.queue.QueueController
import com.example.myokdownload.sample.util.queue.QueueRecyclerAdapter

class QueueActivity: BaseSampleActivity() {
    private var controller: QueueController? = null
    private var adapter: QueueRecyclerAdapter? = null

    override fun titleRes(): Int = R.string.queue_download_title

    private lateinit var binding: ActivityQueueBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initQueueActivity()
    }

    private fun initQueueActivity() {
        initController()
        initRecyclerView()
        initAction()
    }

    private fun initController() {
        val controller = QueueController()
        this.controller = controller
        controller.initTasks(this, createDownloadContextListener {
            binding.actionView.tag = null
            binding.actionTv.setText(R.string.start)
            // to cancel
            controller.stop()
            binding.serialRb.isEnabled = true
            binding.parallelRb.isEnabled = true

            binding.deleteActionView.isEnabled = true
            binding.deleteActionView.cardElevation = binding.deleteActionView.tag as Float
            binding.deleteActionTv.isEnabled = true

            adapter?.notifyDataSetChanged()
        })
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        controller?.let {
            val adapter = QueueRecyclerAdapter(it)
            this.adapter = adapter;
            binding.recyclerView.adapter = adapter
        }
    }

    private fun initAction() {
        binding.deleteActionView.setOnClickListener {
            controller?.deleteFiles()
            adapter?.notifyDataSetChanged()
        }
        binding.actionTv.setText(R.string.start)
        binding.actionView.setOnClickListener { v ->
            val started = v.tag != null

            if (started) {
                controller?.stop()
            } else {
                v.tag = Any()
                binding.actionTv.setText(R.string.cancel)

                // to start
                controller?.start(binding.serialRb.isChecked)
                adapter?.notifyDataSetChanged()

                binding.serialRb.isEnabled = false
                binding.parallelRb.isEnabled = false
                binding.deleteActionView.isEnabled = false
                binding.deleteActionView.tag = binding.deleteActionView.cardElevation
                binding.deleteActionView.cardElevation = 0f
                binding.deleteActionTv.isEnabled = false
            }
        }
    }
}