package com.example.baristasimulator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baristasimulator.ui.MyFirstSensorAppTheme
//l'image cocktail_idle a été généréee par chatgpt avec l'image shaker en reference

//We create the mainactivity as extension of CompenentActivity similar
//to previous exercise with Jetpack compose.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyFirstSensorAppTheme { SensorsScreen() } }
    }

}

@Composable // dit que c une partie de linterface
fun SensorsScreen() {
    val context = LocalContext.current
    // "Remember" stores the value across recompositions of a composable function.
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Get sensors (may be null on some devices )
    // "?" declares the type(sensor) as nullable
    val lightSensor : Sensor? = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    val proximitySensor : Sensor? = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    val accelerometerSensor: Sensor? = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    // Garder les valeurs recues
    // "by" property delegation. we access the variable as light, instead of light.value
    // "mutableStateOf" This is a state variable — if it changes, the sytem recomposes any UI elements that depend on it.
    var light by remember { mutableStateOf<Float?>(null) }
    var proximity by remember { mutableStateOf<Float?>(null) }
    var accelerometer by remember { mutableStateOf<List<Float>?>(null) }
    var shakerBackground by remember { mutableStateOf(R.drawable.cocktail_idle) }
    var shakeCounter by remember { mutableStateOf(0) }
    val backgroundOffsetY by rememberInfiniteTransition().animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse)
    )

    // Check if an object is near the proximity sensor
    fun checkProximity(value: Float?): Boolean {
        if (value == null || proximitySensor == null) return false

        return value >= 0f && value < minOf(3f, proximitySensor.maximumRange)//max = 1 cm par test emulateur
    }

    // Describe the light level in the bar
    fun lightMessage(value: Float?): String {
        if (value == null) return "Luminosité : en attente"

        return if (value < 1000f) { "Ambiance : bar sombre" } else { "Ambiance : bar éclairé" }
    }

    fun shake(values: List<Float>?, previousBackground: Int): Int {
        if (values == null || values.size < 3) return R.drawable.cocktail_idle

        val verticalMovement = kotlin.math.abs(values[1] - 9.8)
        shakeCounter = if (verticalMovement > 3) {
            (shakeCounter + 1).coerceAtMost(10)
        } else {
            (shakeCounter - 1).coerceAtLeast(0)
        }

        return when {
            shakeCounter >= 3 -> R.drawable.cocktail_shaker_bg
            shakeCounter == 0 -> R.drawable.cocktail_idle
            else -> previousBackground
        }
    }

    // Register/unregister a single listener
    //fonction fournie par composo donc pas besoin de la declarer
    // We use DisposableEffect, for side effects that need to be cleaned up after the keys change or if the composable leaves the Composition
    // More information about side effects on https://developer.android.com/develop/ui/compose/side-effects
    DisposableEffect(lightSensor, proximitySensor, accelerometerSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_LIGHT -> light = event.values.firstOrNull()
                    Sensor.TYPE_PROXIMITY -> proximity = event.values.firstOrNull()
                    Sensor.TYPE_ACCELEROMETER -> {
                        accelerometer = event.values.toList()
                        shakerBackground = shake(accelerometer, shakerBackground)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }


        //register the listener in SensorManager dans disposableeffect
        fun reg(s: Sensor?, name: String) {
            if (s != null){ //existence du capteur
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
        reg(accelerometerSensor, "Accelerometer")

        onDispose { sensorManager.unregisterListener(listener) }
    }

    // "&&" : les deux conditions doivent être vraies en même temps
    val canShake = checkProximity(proximity) && shakerBackground == R.drawable.cocktail_shaker_bg
    val displayedBackground = if (canShake) R.drawable.cocktail_shaker_bg else R.drawable.cocktail_idle

    // UI
    // For more information about layout using composable, see Lecture 3
    // https://developer.android.com/courses/pathways/android-basics-compose-unit-1-pathway-3
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(displayedBackground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer {

                translationY = if (canShake) backgroundOffsetY else 0f
                scaleY = if (canShake) 1.1f else 1f
            },
            contentScale = ContentScale.Crop
        ) // Lien: https://www.pexels.com/photo/close-up-of-a-cocktail-with-foam-17571849/
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Règles", fontSize = 26.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
            Text("Pour shaker la boisson: \n1) tiens le téléphone à deux mains \n 2) secoue-le de haut en bas.\n Les 2 doivent être fait au même temps", fontSize = 22.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
            Text(text = if (checkProximity(proximity)) { "\n2eme main détectée" } else { "\nAucun objet proche détecté" },
                fontSize = 22.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)))
            Text(text = if (canShake) { "Le verre est actuellement en mouvement." } else { "Le verre est actuellement à l'arrêt." },
                fontSize = 22.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Sensors:", fontSize = 18.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)))
            Text("Light: $light lx", fontSize = 18.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)))
            Text(lightMessage(light), fontSize = 18.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)))
            Text("Proximité: ${proximity?.let { String.format("%.1f cm", it) } ?: "-- cm"}", fontSize = 18.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)))
            Text("Acceleromètre: ${accelerometer?.joinToString(", ") { String.format("%.1f", it) } ?: "--"} m/s²", fontSize = 18.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)))
        }
    }
}