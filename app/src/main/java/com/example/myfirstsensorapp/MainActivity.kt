package com.example.myfirstsensorapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstsensorapp.ui.theme.MyFirstSensorAppTheme


//We create the mainactivity as extension of CompenentActivity similar
//to previous exercise with Jetpack compose.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyFirstSensorAppTheme {  SensorsScreen() }}
    }

}

@Composable
fun SensorsScreen() {
    val context = LocalContext.current
    // "Remember" stores the value across recompositions of a composable function.
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Get sensors (may be null on some devices )
    // "?" declares the type as nullable
    val lightSensor : Sensor? = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    val proximitySensor : Sensor? = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    val pressureSensor: Sensor? = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) }

    // Values to show
    // "by" property delegation. we access the variable as light, instead of light.value
    // "mutableStateOf" This is a state variable — if it changes, the sytem recomposes any UI elements that depend on it.
    var light by remember { mutableStateOf<Float?>(null) }
    var proximity by remember { mutableStateOf<Float?>(null) }
    var pressure by remember { mutableStateOf<Float?>(null) }

    // Register/unregister a single listener
    // We use DisposableEffect, for side effects that need to be cleaned up after the keys change or if the composable leaves the Composition
    // More information about side effects on https://developer.android.com/develop/ui/compose/side-effects
    DisposableEffect(lightSensor, proximitySensor, pressureSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_LIGHT -> light = event.values.firstOrNull()
                    Sensor.TYPE_PROXIMITY -> proximity = event.values.firstOrNull()
                    Sensor.TYPE_PRESSURE -> pressure = event.values.firstOrNull()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }


        //register the listener in SensorManager
        fun reg(s: Sensor?, name: String) {
            if (s != null){
                sensorManager.registerListener(
                    listener,
                    s,
                    SensorManager.SENSOR_DELAY_UI
                )
                Log.d("Sensors", "$name sensor registered")
            } else {
                Log.w("Sensors", "$name sensor not available on this device")
            }
        }

        reg(lightSensor, "Light Sensor")
        reg(proximitySensor, "Proximity Sensor")
        reg(pressureSensor, "Pressure Sensor")

        onDispose { sensorManager.unregisterListener(listener) }
    }

    // UI
    // For more information about layout using composable, see Lecture 3
    // https://developer.android.com/courses/pathways/android-basics-compose-unit-1-pathway-3
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sensors", fontSize = 22.sp)
            Text("Light: $light lx", fontSize = 18.sp)
            //using Elvis operator and formating the string. More stable interface,
            // no null information appear on the screen.
            //Text("Light: ${light?.let { String.format("%.1f lx", it) } ?: "-- lx"}", fontSize = 18.sp)
            Text("Proximity: $proximity-- cm", fontSize = 18.sp)
            //Text("Proximity: ${proximity?.let { String.format("%.1f cm", it) } ?: "-- cm"}", fontSize = 18.sp)
            Text("Pressure: $pressure? hPa", fontSize = 18.sp)
            //Text("Pressure: ${pressure?.let { String.format("%.1f hPa", it) } ?: "-- hPa"}", fontSize = 18.sp)
        }
    }
}
