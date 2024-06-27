package com.example.mobile_application_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobile_application_project.ui.EnvironmentData
import com.example.mobile_application_project.ui.Session

class SessionsAdapter : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    private var sessions: List<Session> = emptyList()
    private var environmentDataMap: MutableMap<Int, EnvironmentData> = mutableMapOf()

    fun setSessions(sessions: List<Session>) {
        this.sessions = sessions
        notifyDataSetChanged()
    }

    fun setEnvironmentData(sessionId: Int, environmentData: EnvironmentData) {
        environmentDataMap[sessionId] = environmentData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        val environmentData = environmentDataMap[session.id]
        holder.bind(session, environmentData)
    }

    override fun getItemCount(): Int {
        return sessions.size
    }

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val sessionNameTextView: TextView = itemView.findViewById(R.id.sessionNameTextView)
        private val sessionDetailsTextView: TextView = itemView.findViewById(R.id.sessionDetailsTextView)
        private val environmentDataTextView: TextView = itemView.findViewById(R.id.environmentDataTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)
        private val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)
        private val paceTextView: TextView = itemView.findViewById(R.id.paceTextView)

        fun bind(session: Session, environmentData: EnvironmentData?) {
            sessionNameTextView.text = session.name
            sessionDetailsTextView.text = getSessionDetails(session)
            setEnvironmentData(environmentData)
        }

        private fun getSessionDetails(session: Session): String {
            val sessionType = when (session.session_type_id) {
                1 -> "Outdoor Running"
                2 -> "Indoor Running"
                3 -> "Gym"
                4 -> "Calisthenics"
                5 -> "Outdoor Training"
                6 -> "Swimming"
                else -> "Unknown Activity"
            }

            return "Type of Activity: $sessionType\n" +
                    "Date of Creation: ${session.date_of_creation}\n" +
                    "Active: ${if (session.active) "Yes" else "No"}"
        }

        private fun setEnvironmentData(environmentData: EnvironmentData?) {
            if (environmentData != null) {
                environmentDataTextView.text = "Environment Data:\n" +
                        "Temperature: ${environmentData.temperature}\n" +
                        "Humidity: ${environmentData.humidity}\n" +
                        "Pressure: ${environmentData.pressure}\n" +
                        "Latitude: ${environmentData.latitude}\n" +
                        "Longitude: ${environmentData.longitude}\n" +
                        "Date of Measurement: ${environmentData.date_of_measurement}"
            } else {
                environmentDataTextView.text = "No environment data available"
            }
        }
    }
}
