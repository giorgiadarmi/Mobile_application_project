package com.example.mobile_application_project

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobile_application_project.ui.EnvironmentData
import com.example.mobile_application_project.ui.Session
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class SessionActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    private lateinit var database: DatabaseReference
    private lateinit var sessionTypeSpinner: Spinner
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var sessionInfo: TextView
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var sensorInfoLayout: LinearLayout

    private lateinit var temperatureValue: TextView
    private lateinit var humidityValue: TextView
    private lateinit var pressureValue: TextView
    private lateinit var latitudeValue: TextView
    private lateinit var longitudeValue: TextView
    private lateinit var dateOfMeasurementValue: TextView
    private lateinit var numberOfStepTextView: TextView
    private lateinit var totalDistanceTextView: TextView
    private lateinit var averagePaceTextView: TextView

    private var temperature: String = "--"
    private var humidity: String = "--"
    private var pressure: String = "--"
    private var stepCount = 0
    private var totalDistance = 0.0
    private var startTime: Long = 0
    private lateinit var lastLocation: Location

    private var sessionId = 0
    private lateinit var sessionStartDate: String
    private lateinit var sessionEndDate: String
    private var isSessionActive = false

    private val sessionTypes = arrayOf(
        "Outdoor Running", "Indoor Running", "Gym", "Calisthenics",
        "Outdoor Training", "Swimming"
    )

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var sensorManager: SensorManager
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private var pressureSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)
        database = FirebaseDatabase.getInstance().reference
        sessionTypeSpinner = findViewById(R.id.session_type_spinner)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        sessionInfo = findViewById(R.id.session_info)
        mapView = findViewById(R.id.map_view)
        sensorInfoLayout = findViewById(R.id.sensor_info_layout)

        temperatureValue = findViewById(R.id.temperature_value)
        humidityValue = findViewById(R.id.humidity_value)
        pressureValue = findViewById(R.id.pressure_value)
        latitudeValue = findViewById(R.id.latitude_value)
        longitudeValue = findViewById(R.id.longitude_value)
        dateOfMeasurementValue = findViewById(R.id.date_of_measurement_value)
        numberOfStepTextView = findViewById(R.id.number_of_step_value)
        totalDistanceTextView = findViewById(R.id.total_distance_value)
        averagePaceTextView = findViewById(R.id.average_pace_value)

        temperatureValue.text = "Temperature: --"
        humidityValue.text = "Humidity: --"
        pressureValue.text = "Pressure: --"
        latitudeValue.text = "Latitude: --"
        longitudeValue.text = "Longitude: --"
        dateOfMeasurementValue.text = "Date of Measurement: --"
        numberOfStepTextView.text = "Number of steps: --"
        totalDistanceTextView.text = "Total distance: -- meters"
        averagePaceTextView.text = "Average pace: -- km/h"

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        sessionTypeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sessionTypes
        )

        startButton.setOnClickListener {
            startSession()
        }

        stopButton.setOnClickListener {
            stopSession()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (isSessionActive) {
                        updateLocationOnMap(location)
                        updateSensorInfo(location)
                        updateTrackingInfo(location)
                    }
                }
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
        }
    }

    private fun startSession() {
        sessionId++
        sessionStartDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        isSessionActive = true
        stepCount = 0
        totalDistance = 0.0
        startTime = System.currentTimeMillis()

        val selectedSessionType = sessionTypeSpinner.selectedItem.toString()
        sessionInfo.text = """
            ID: $sessionId
            Name: ${selectedSessionType}
            Date of Creation: $sessionStartDate
            Date of End:
            Active: Yes
            Creator ID: 1
            Creator: User
            Session Type: $selectedSessionType
        """.trimIndent()
        sessionInfo.visibility = View.VISIBLE
        sensorInfoLayout.visibility = View.VISIBLE
        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE
        startLocationUpdates()
        startSensorUpdates()
        FetchWeatherTask().execute()
    }

    private fun stopSession() {
        sessionEndDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        isSessionActive = false

        val sessionInfoText = sessionInfo.text.toString().replace("Active: Yes", "Active: No")
        sessionInfo.text = sessionInfoText.replace("Date of End:", "Date of End: $sessionEndDate")
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        stopLocationUpdates()
        stopSensorUpdates()
        FetchWeatherTask().execute()

        val temperature = temperatureValue.text.toString().removePrefix("Temperature: ").removeSuffix("°C")
        val humidity = humidityValue.text.toString().removePrefix("Humidity: ").removeSuffix("%")
        val pressure = pressureValue.text.toString().removePrefix("Pressure: ").removeSuffix("hPa")

        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
        val distanceInKm = totalDistance / 1000.0
        val averagePace = if (elapsedTime > 0) distanceInKm / (elapsedTime / 3600) else 0.0

        val formattedPace = DecimalFormat("0.00").format(averagePace)
        averagePaceTextView.text = "Average pace: $formattedPace km/h"

        Log.d("SensorData", "Temperature: $temperature, Humidity: $humidity, Pressure: $pressure")
        val environmentDataList = mutableListOf<EnvironmentData>()
        environmentDataList.add(
            EnvironmentData(
                temperature = temperature,
                humidity = humidity,
                pressure = pressure,
                latitude = lastLocation.latitude,
                longitude = lastLocation.longitude,
                date_of_measurement = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        )

        val session = Session(
            name = sessionTypeSpinner.selectedItem.toString(),
            date_of_creation = sessionStartDate,
            date_of_end = sessionEndDate,
            active = false,
            creator_id = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            creator = FirebaseAuth.getInstance().currentUser?.displayName ?: "User",
            avgSpeed = averagePace,
            number_of_step = stepCount,
            totalDistance = totalDistance,
            environment_data = environmentDataList,
            session_type = sessionTypeSpinner.selectedItem.toString(),
            session_type_id = sessionTypeSpinner.selectedItemPosition + 1
        )

        sendSessionData(session)
    }

    private fun sendSessionData(session: Session) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.child("Sessions").child(userId).push().setValue(session)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SessionActivity", "Session saved successfully")
                } else {
                    Log.e("SessionActivity", "Failed to save session", task.exception)
                }
            }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationOnMap(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        googleMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f))

        if (this::lastLocation.isInitialized) {
            val lastLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            googleMap.addPolyline(
                PolylineOptions()
                    .add(lastLatLng, currentLatLng)
                    .width(5f)
                    .color(ContextCompat.getColor(this, R.color.purple_700))
            )

            val distance = lastLocation.distanceTo(location)
            totalDistance += distance
            totalDistanceTextView.text = "Total distance: ${DecimalFormat("0.00").format(totalDistance)} meters"

            val stepsTaken = (distance / 0.80).toInt()
            stepCount += stepsTaken
            numberOfStepTextView.text = "Number of steps: $stepCount"
        }

        lastLocation = location
    }

    private fun updateSensorInfo(location: Location) {
        latitudeValue.text = "Latitude: ${location.latitude}"
        longitudeValue.text = "Longitude: ${location.longitude}"
        dateOfMeasurementValue.text = "Date of Measurement: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
    }

    private fun updateTrackingInfo(location: Location) {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
        val distanceInKm = totalDistance / 1000.0
        val averagePace = if (elapsedTime > 0) distanceInKm / (elapsedTime / 3600) else 0.0

        val formattedPace = DecimalFormat("0.00").format(averagePace)
        averagePaceTextView.text = "Average pace: $formattedPace km/h"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    temperature = "${it.values[0]}°C"
                    temperatureValue.text = "Temperature: $temperature"
                }
                Sensor.TYPE_RELATIVE_HUMIDITY -> {
                    humidity = "${it.values[0]}%"
                    humidityValue.text = "Humidity: $humidity"
                }
                Sensor.TYPE_PRESSURE -> {
                    pressure = "${it.values[0]} hPa"
                    pressureValue.text = "Pressure: $pressure"
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    if (it.values[0] == 1.0f) {
                        stepCount++
                        numberOfStepTextView.text = "Number of steps: $stepCount"
                    } else {

                    }
                }
                else -> {
                    Log.d("error","error")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startSensorUpdates() {
        temperatureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        humiditySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.also { stepCounter ->
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
        Log.d("SensorWeatherTask", "Temperature: $temperature, Humidity: $humidity, Pressure: $pressure")
    }

    val CITY: String = "rome,it"
    val API: String = "5becb8ce5adefe821f7ac81d7117a28c" // Use API key
    inner class FetchWeatherTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String? {
            var response: String?
            try {
                response = URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API").readText(
                    Charsets.UTF_8
                )
            } catch (e: Exception) {
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            result?.let {
                try {
                    val jsonObj = JSONObject(it)
                    val main = jsonObj.getJSONObject("main")
                    val lastTemp = main.getString("temp")
                    val lastHumidity = main.getString("humidity")
                    val lastPressure = main.getString("pressure")

                    temperatureValue.text = "Temperature: $lastTemp°C"
                    humidityValue.text = "Humidity: $lastHumidity%"
                    pressureValue.text = "Pressure: $lastPressure hPa"

                    Log.d("FetchWeatherTask", "Temperature: $lastTemp, Humidity: $lastHumidity, Pressure: $lastPressure")
                } catch (e: Exception) {
                    Log.e("FetchWeatherTask", "Error parsing weather data", e)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (isSessionActive) {
            startSensorUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (isSessionActive) {
            stopSensorUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        if (isSessionActive) {
            stopSensorUpdates()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}