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
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

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

    private var temperature: String = "--"
    private var humidity: String = "--"
    private var pressure: String = "--"


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

        temperatureValue.text = "Temperature: --"
        humidityValue.text = "Humidity: --"
        pressureValue.text = "Pressure: --"
        latitudeValue.text = "Latitude: --"
        longitudeValue.text = "Longitude: --"
        dateOfMeasurementValue.text = "Date of Measurement: --"

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
                    updateLocationOnMap(location)
                    updateSensorInfo(location)
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

        val selectedSessionType = sessionTypeSpinner.selectedItem.toString()
        sessionInfo.text = """
            ID: $sessionId
            Name: ${selectedSessionType}
            Date of Creation: $sessionStartDate
            Date of End: $sessionStartDate
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
        sessionInfo.text = sessionInfoText.replace("Date of End: $sessionStartDate", "Date of End: $sessionEndDate")
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        stopLocationUpdates()
        stopSensorUpdates()
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
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(latLng).title("Current Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
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
            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentTime = sdf.format(Date())

                temperature = main.getString("temp") + "°C"
                humidity = main.getString("humidity") + "%"
                pressure = main.getString("pressure") + " hPa"

                // Update sensor info views
                temperatureValue.text = "Temperature: $temperature"
                humidityValue.text = "Humidity: $humidity"
                pressureValue.text = "Pressure: $pressure"
                dateOfMeasurementValue.text = "Date of Measurement: $currentTime"

            } catch (e: Exception) {
                Log.e("FetchWeatherTask", "Error parsing JSON", e)
            }
        }
    }


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
    }

    private fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        Log.d("SensorData", "Sensor type: ${event.sensor.type}, Values: ${event.values.joinToString()}")
        runOnUiThread {
            when (event.sensor.type) {
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    temperatureValue.text = "Temperature: ${event.values[0]}°C"
                }
                Sensor.TYPE_RELATIVE_HUMIDITY -> {
                    humidityValue.text = "Humidity: ${event.values[0]}%"
                }
                Sensor.TYPE_PRESSURE -> {
                    pressureValue.text = "Pressure: ${event.values[0]} hPa"
                }
            }
            dateOfMeasurementValue.text = "Date of Measurement: $currentTime"
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non necessario per questo esempio
    }


    private fun updateSensorInfo(location: Location) {
        latitudeValue.text = "Latitude: ${location.latitude}"
        longitudeValue.text = "Longitude: ${location.longitude}"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        val defaultLocation = LatLng(37.7749, -122.4194)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (isSessionActive) {
            startLocationUpdates()
            startSensorUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (isSessionActive) {
            stopLocationUpdates()
            stopSensorUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }
}
