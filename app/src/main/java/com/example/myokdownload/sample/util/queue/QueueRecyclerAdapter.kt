package com.example.myokdownload.sample.util.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.myokdownload.databinding.ItemQueueBinding

class QueueRecyclerAdapter(private val controller: QueueController): RecyclerView.Adapter<QueueRecyclerAdapter.QueueViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = ItemQueueBinding.inflate(LayoutInflater.from(parent.context))
        binding.root.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        return QueueViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return controller.size()
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        controller.bind(holder, position)
    }

    class QueueViewHolder(binding: ItemQueueBinding): RecyclerView.ViewHolder(binding.root) {
        var nameTv = binding.nameTv
        var priorityTv = binding.priorityTv
        var prioritySb = binding.prioritySb
        var statusTv = binding.statusTv
        var progressBar = binding.progressBar
    }
}