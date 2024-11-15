package com.example.myokdownload.sample

import android.os.Bundle
import com.example.myokdownload.R

class MainActivity : BaseListActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun titleRes(): Int {
        return R.string.app_name
    }

    override fun setupAdapter(holder: ItemsHolder?) {
        holder?.addItem(
            R.string.single_download_title, R.string.single_download_desc,
            SingleActivity::class.java
        )
        holder?.addItem(
            R.string.each_block_progress_title, R.string.each_block_progress_desc,
            EachBlockProgressActivity::class.java
        )
        holder?.addItem(
            R.string.queue_download_title, R.string.queue_download_desc,
            QueueActivity::class.java
        )
        holder!!.addItem(
            R.string.bunch_download_title, R.string.bunch_download_desc,
            BunchActivity::class.java
        )
//        holder.addItem(R.string.task_manager_title, R.string.task_manager_desc,
//                ManagerActivity.class);
        //        holder.addItem(R.string.task_manager_title, R.string.task_manager_desc,
//                ManagerActivity.class);
//        holder!!.addItem(
//            R.string.title_content_uri, R.string.content_uri_desc,
//            ContentUriActivity::class.java
//        )
//        holder!!.addItem(
//            R.string.title_notification, R.string.notification_desc,
//            NotificationActivity::class.java
//        )
//        holder.addItem(R.string.comprehensive_case_title, R.string.comprehensive_case_desc,
//                ComprehensiveActivity.class);
    }
}