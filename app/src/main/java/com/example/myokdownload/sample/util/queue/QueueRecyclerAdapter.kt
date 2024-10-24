package com.example.myokdownload.sample.util.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myokdownload.databinding.ItemQueueBinding

class QueueRecyclerAdapter: RecyclerView.Adapter<QueueRecyclerAdapter.QueueViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        return QueueViewHolder(ItemQueueBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    class QueueViewHolder(binding: ItemQueueBinding): RecyclerView.ViewHolder(binding.root) {
        var nameTv = binding.nameTv
        var priorityTv = binding.priorityTv
        var prioritySb = binding.prioritySb
        var statusTv = binding.statusTv
        var progressBar = binding.progressBar
    }
}