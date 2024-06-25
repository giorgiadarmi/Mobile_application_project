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
        holder.bind(session, environmentDataMap[session.id])
    }

    override fun getItemCount(): Int {
        return sessions.size
    }

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val sessionNameTextView: TextView = itemView.findViewById(R.id.sessionNameTextView)
        private val sessionDetailsTextView: TextView = itemView.findViewById(R.id.sessionDetailsTextView)
        private val environmentDataTextView: TextView = itemView.findViewById(R.id.environmentDataTextView)

        fun bind(session: Session, environmentData: EnvironmentData?) {
            sessionNameTextView.text = session.name
            sessionDetailsTextView.text = "Date of Creation: ${session.date_of_creation}, Active: ${if (session.active) "Yes" else "No"}, Session Type: ${session.session_type}"

            if (environmentData != null) {
                environmentDataTextView.text = "Temperature: ${environmentData.temperature}, Humidity: ${environmentData.humidity}, Pressure: ${environmentData.pressure}, Latitude: ${environmentData.latitude}, Longitude: ${environmentData.longitude}, Date of Measurement: ${environmentData.date_of_measurement}"
            } else {
                environmentDataTextView.text = "No environment data available"
            }
        }
    }
}
