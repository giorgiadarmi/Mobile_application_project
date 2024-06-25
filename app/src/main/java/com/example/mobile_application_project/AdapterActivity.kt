package com.example.mobile_application_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.ui.Activity

class AdapterActivity : ListAdapter<Activity, AdapterActivity.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = getItem(position)
        holder.bind(activity)
    }

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)
        private val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)
        private val paceTextView: TextView = itemView.findViewById(R.id.paceTextView)

        fun bind(activity: Activity) {
            dateTextView.text = "Date: ${activity.data}"
            stepsTextView.text = "Number of Steps: ${activity.numberOfStep}"
            distanceTextView.text = "Total Distance: ${activity.totalDistance} km"
            paceTextView.text = "Average Pace: ${activity.averagePace} km/h"
        }
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<Activity>() {
        override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
            return oldItem.data == newItem.data &&
                    oldItem.numberOfStep == newItem.numberOfStep &&
                    oldItem.totalDistance == newItem.totalDistance &&
                    oldItem.averagePace == newItem.averagePace
        }
    }
}
