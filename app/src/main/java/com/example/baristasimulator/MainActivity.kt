package com.example.baristasimulator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import kotlinx.coroutines.delay
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
    // tata :(est de type) toto
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

    // compteur des shakes
    var consecutiveShakes by remember { mutableStateOf(0) }
    var waitingForReturn by remember { mutableStateOf(false) }
    var lastShakeTime by remember { mutableStateOf(0L) }
    var lastCountedShakeTime by remember { mutableStateOf(-250L) }

    val backgroundOffsetY by rememberInfiniteTransition().animateFloat( //valeurs de la trnaslation
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(350), //repetition de l'animation + reverse ave cudréee de 350 millisecondes
            RepeatMode.Reverse)
    )

    // meme chose mais on prepare une variation qu isera appliquée a la teinte plus bas
    val rainbowHue by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart)
    )

    // Check si obj proximity sensor
    fun checkProximity(value: Float?): Boolean {
        if (value == null || proximitySensor == null) return false //ou

        return value >= 0f && value < minOf(3f, proximitySensor.maximumRange)//max = 1 cm par test emulateur
    }

    // Descrit lumière extérieur du bar
    fun lightMessage(value: Float?): String {
        if (value == null) return "Luminosité : en attente"

        return if (value < 1000f) { "Ambiance : bar sombre" } else { "Ambiance : bar éclairé" }
    }

    fun shake(values: List<Float>?): Int {
        if (values == null || values.size < 3) return R.drawable.cocktail_idle

        val verticalMovement = kotlin.math.abs(values[1] - 9.8)
        shakeCounter = if (verticalMovement > 1) {
            (shakeCounter + 1).coerceAtMost(10)
        } else {
            (shakeCounter - 1).coerceAtLeast(0) // bornes
        }

        return if (shakeCounter >= 1) {
            R.drawable.cocktail_shaker_bg
        } else {
            R.drawable.cocktail_idle
        }
    }

    // Reset les shakes quand erreur
    fun resetStreak() {
        consecutiveShakes = 0
        waitingForReturn = false
        lastShakeTime = 0L
        lastCountedShakeTime = -250L


    }

    // en gros on mets aller ensuite bloucle qui attend un autre aller donc ca fait aller retour
    fun countShake(movement: Float, now: Long) {
        if (lastShakeTime != 0L && now - lastShakeTime > 1200L) resetStreak() //1200 trop long c ciao

        if (movement > 1f && !waitingForReturn) {
            waitingForReturn = true
            lastShakeTime = now
        } else if (movement < -1f && waitingForReturn) {

            if (now - lastCountedShakeTime >= 250L) {//  on évite les dubles omptages
                consecutiveShakes += 1
                lastCountedShakeTime = now
            }
            waitingForReturn = false
            lastShakeTime = now
        }
    }

    // permets petit pause ensuite reset, tout en long car c'est une duréee en très petit valeur
    LaunchedEffect(Unit) {
        while (true) {
            delay(100L)
            if (lastShakeTime != 0L && SystemClock.elapsedRealtime() - lastShakeTime > 1200L) {
                resetStreak()
                shakeCounter = 0
                shakerBackground = R.drawable.cocktail_idle
            }
        }
    }

    // Register/unregister a single listener
    //fonction fournie par composo donc pas besoin de la declarer
    // We use DisposableEffect, for side effects that need to be cleaned up after the keys change or if the composable leaves the Composition
    // More information about side effects on https://developer.android.com/develop/ui/compose/side-effects
    DisposableEffect(lightSensor, proximitySensor, accelerometerSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { // c'est ici qu il faut mettre les actiosn en reaction a un changenet de mesure
                when (event.sensor.type) {
                    Sensor.TYPE_LIGHT -> light = event.values.firstOrNull()
                    Sensor.TYPE_PROXIMITY -> {
                        proximity = event.values.firstOrNull()
                        if (!checkProximity(proximity)) resetStreak()


                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        accelerometer = event.values.toList()
                        shakerBackground = shake(accelerometer)//detect image
                        if (checkProximity(proximity)) { //count or not?
                            countShake(event.values[1] - 9.8f, SystemClock.elapsedRealtime())
                        } else {
                            resetStreak()
                        }
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

    // les 2 variables avec et sans les 2 mains(canshake ) et 1 main showasaker  /  && -> and
    val canShake = checkProximity(proximity) && shakerBackground == R.drawable.cocktail_shaker_bg
    val showShaker =  shakerBackground == R.drawable.cocktail_shaker_bg

    // Les variantes bleues ont été générées avec ChatGPT à partir des images d'origine
    // choisit dark background
    val displayedBackground = if ((light ?: 0f) < 1000f) {
        if (showShaker) R.drawable.cocktail_shaker_blue else R.drawable.cocktail_idle_blue
    } else {
        if (showShaker) R.drawable.cocktail_shaker_bg else R.drawable.cocktail_idle
    }

    // Ajoute le multicouleur rainbowhue fait evoluer la prem val CAD la teinte(boucle)
    val rainbowFilter = if (consecutiveShakes >= 30) { //colorfilter filtre sur limage et blendmode = blendmode
        ColorFilter.tint(Color.hsv(rainbowHue, 1f, 1f).copy(alpha = 0.55f), BlendMode.SrcAtop)
    } else {
        null
    }

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
            colorFilter = rainbowFilter,//parametre de image  le if est dans rainbowfilter
            modifier = Modifier.fillMaxSize().graphicsLayer { //graphics layer  applique les transfo a l'element
                translationY = if (canShake) backgroundOffsetY else 0f // mouvement haut-bas def plus haut
                scaleY = if (canShake) 1.1f else 1f// echele y
            },
            contentScale = ContentScale.Crop
        ) // Lien: https://www.pexels.com/photo/close-up-of-a-cocktail-with-foam-17571849/
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // montre message si  proximity and movement are detected together
            if (canShake) {
                Text("KEEP SHAKINGGG", fontSize = 30.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
            } else {
                // Show the rules when the shake is stopped
                Text("Règles", fontSize = 26.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
                Text("Pour shaker la boisson: \n1) tiens le téléphone à deux mains \n 2) secoue-le de haut en bas.\n Les 2 doivent être fait au même temps", fontSize = 22.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
                Text(text = if (checkProximity(proximity)) { "\n 2ème main prête à shaker" } else { "\n 2ème main pas a l'horizon" },
                    fontSize = 22.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)

            }
            Text("Shakes : $consecutiveShakes", fontSize = 22.sp, color = Color.White, fontFamily = FontFamily(Font(R.font.impact)), textAlign = TextAlign.Center)
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