package com.example.mobile_application_project

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mobile_application_project.databinding.ActivityTrackingBinding
import com.example.mobile_application_project.ui.Activity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityTrackingBinding
    private lateinit var mapView: MapView
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var tracking = false
    private var stepCount = 0
    private var totalDistance = 0.0
    private var startTime: Long = 0
    private lateinit var lastLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.startButton.setOnClickListener {
            startTracking()
        }

        binding.endButton.setOnClickListener {
            stopTracking()
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (tracking) {
                    for (location in locationResult.locations) {
                        updateLocation(location)
                    }
                }
            }
        }
    }

    private fun startTracking() {
        tracking = true
        stepCount = 0
        totalDistance = 0.0
        startTime = System.currentTimeMillis()
        binding.numberOfStepTextView.text = "Number of steps: $stepCount"
        binding.totalDistanceTextView.text = "Total distance: ${formatDistance(totalDistance)} meters"
        binding.averagePaceTextView.text = "Average pace: 0.00 km/h"

        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopTracking() {
        tracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
        val distanceInKm = totalDistance / 1000.0
        val averagePace = if (elapsedTime > 0) distanceInKm / (elapsedTime / 3600) else 0.0

        val formattedPace = DecimalFormat("0.00").format(averagePace)

        binding.averagePaceTextView.text = "Average pace: $formattedPace km/h"

        saveActivity(stepCount, totalDistance, averagePace)
    }

    private fun updateLocation(location: Location) {
        if (this::map.isInitialized) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            map.addPolyline(PolylineOptions().add(currentLatLng))

            if (stepCount > 0) {
                val distance = location.distanceTo(lastLocation)
                totalDistance += distance
                binding.totalDistanceTextView.text = "Total distance: ${formatDistance(totalDistance)} meters"
            }

            stepCount++
            binding.numberOfStepTextView.text = "Number of steps: $stepCount"
            lastLocation = location
        }
    }

    private fun saveActivity(stepCount: Int, totalDistance: Double, averagePace: Double) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("Activities").child(userId)

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val activity = Activity(date, stepCount.toString(), formatDistance(totalDistance), DecimalFormat("0.00").format(averagePace))
        ref.push().setValue(activity)
    }

    private fun formatDistance(distance: Double): String {
        return DecimalFormat("0.00").format(distance)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        map.isMyLocationEnabled = true
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
