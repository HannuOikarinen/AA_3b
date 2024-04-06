package com.example.aa

//Import statements for Android, Compose, and Google Location and Maps services.
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.aa.ui.theme.AATheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {

    //Define location permissions required by the app.
    private val permission = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    //Llocation client and location callback.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    //Check if location updates are needed.
    private var locationRequired: Boolean = false

    //Continue location updates when app resumes.
    override fun onResume() {
        super.onResume()
        if (locationRequired) {
            startLocationUpdates()
        }
    }

    //Stop location updates when app pauses.
    override fun onPause() {
        super.onPause()
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }

    //Configure and start receiving location updates.
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    //onCreate lifecycle callback to set up the UI and initialize location services.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initialize Google Maps
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST){

        }

        //Initialize the fused location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {

            //Remember the current location
            var currentLocation by remember {
                mutableStateOf(LatLng(0.toDouble(),0.toDouble()))
            }

            //Remember camera position for map
            val cameraPosition = rememberCameraPositionState{
                position = CameraPosition.fromLatLngZoom(
                    currentLocation, 10f
                )
            }

            //Remember and manage camera position
            var cameraPositionState by remember {
                mutableStateOf(cameraPosition)
            }

            //Callback that updates the location for UI
            locationCallback = object: LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    for (location in p0.locations) {
                        currentLocation = LatLng(location.latitude, location.longitude)

                        cameraPositionState = CameraPositionState(
                            position = CameraPosition.fromLatLngZoom(
                                currentLocation, 10f
                            )
                        )
                    }
                }
            }

            //Apply theme
            AATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //Display the location screen with the current location and camera position.
                    LocationScreen(this@MainActivity, currentLocation, cameraPositionState)
                }
            }
        }
    }

    
    @Composable
    private fun LocationScreen(context: Context, currentLocation: LatLng, cameraPositionState: CameraPositionState) {

        //Request for more permissions if needed
        val launchMultiplePermissions = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissionMaps ->
            val areGranted = permissionMaps.values.reduce {acc, next -> acc && next}
            if (areGranted) {
                locationRequired = true
                startLocationUpdates()
                Toast.makeText(context, "Permission Granted!", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(context, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            //Display the map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(
                        position = currentLocation
                    ),
                    title = "You",
                    snippet = "You're here!!!"
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                //Current coordinates
                Text(text = "Your location: ${currentLocation.latitude}/${currentLocation.longitude}")
                //Get location and check for permissions
                Button(onClick = {
                    if (permission.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }) {
                        startLocationUpdates()
                    } else {
                        launchMultiplePermissions.launch(permission)
                    }
                }) {
                    Text(text = "Get your location")
                }
            }
        }
    }
}
