package ru.usfeu.fittrackergemini

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ru.usfeu.fittrackergemini.StepGoalActivity
import ru.usfeu.fittrackergemini.GenderSelectionActivity

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null

    private lateinit var stepsDisplayTextView: TextView
    private lateinit var caloriesDisplayTextView: TextView
    private lateinit var trackingStatusTextView: TextView
    private lateinit var toggleTrackingButton: Button

    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var dailyStepGoal = 8000
    private var userGender: String = ""
    private var userWeight: Float = 0f
    private var dailyCalorieGoal: Int = 2000

    private var isTrackingActive = false

    private val ACTIVITY_RECOGNITION_PERMISSION_CODE = 100
    private val NOTIFICATION_PERMISSION_CODE = 101
    private val CHANNEL_ID = "fitness_tracker_channel"

    // Пороговые значения для уведомлений (каждая 1000 шагов до 15000)
    private val stepThresholds = intArrayOf(
        1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000,
        11000, 12000, 13000, 14000, 15000
    )
    // Set для хранения уже достигнутых порогов, чтобы не отправлять уведомления повторно
    private val reachedThresholds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        createNotificationChannel()

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val genderSelected = sharedPreferences.getBoolean("gender_selected", false)
        val stepGoalSet = sharedPreferences.getBoolean("step_goal_set", false)

        if (!genderSelected) {
            val intent = Intent(this, GenderSelectionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        if (!stepGoalSet) {
            val intent = Intent(this, StepGoalActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        stepsDisplayTextView = findViewById(R.id.stepsDisplayTextView)
        caloriesDisplayTextView = findViewById(R.id.caloriesDisplayTextView)
        trackingStatusTextView = findViewById(R.id.trackingStatusTextView)
        toggleTrackingButton = findViewById(R.id.toggleTrackingButton)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        requestActivityRecognitionPermission()

        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            Toast.makeText(this, "Датчик шагов найден!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Датчик шагов не найден на этом устройстве. Функционал будет ограничен.", Toast.LENGTH_LONG).show()
        }

        loadData()

        updateTrackingState(isTrackingActive)

        stepsDisplayTextView.text = "${totalSteps.toInt()} / ${dailyStepGoal} шагов"
        caloriesDisplayTextView.text = "0 / ${dailyCalorieGoal} ккал"

        toggleTrackingButton.setOnClickListener {
            isTrackingActive = !isTrackingActive
            updateTrackingState(isTrackingActive)
            saveData()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Мотивационные уведомления FitTracker"
            val descriptionText = "Уведомления о достижении целей по шагам"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_PERMISSION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECOGNITION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение на физическую активность предоставлено.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение на физическую активность отклонено. Функционал подсчета шагов будет недоступен.", Toast.LENGTH_LONG).show()
                }
            }
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение на уведомления предоставлено.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение на уведомления отклонено. Мотивационные уведомления будут отключены.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isTrackingActive && event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSteps = event.values[0]

            if (previousTotalSteps == 0f) {
                previousTotalSteps = currentSteps
            }

            totalSteps = currentSteps - previousTotalSteps

            stepsDisplayTextView.text = "${totalSteps.toInt()} / ${dailyStepGoal} шагов"

            val caloriesBurnedFromSteps = totalSteps * 0.000735f * userWeight
            caloriesDisplayTextView.text = "${caloriesBurnedFromSteps.toInt()} / ${dailyCalorieGoal} ккал"

            checkStepThresholds(totalSteps.toInt())
        }
    }

    private fun checkStepThresholds(currentSteps: Int) {
        for (threshold in stepThresholds) {
            if (currentSteps >= threshold && !reachedThresholds.contains(threshold)) {
                sendNotification("Поздравляем!", "Вы достигли ${threshold} шагов! Отличная работа!")
                reachedThresholds.add(threshold)
                saveData()
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build())
        } else {
            Toast.makeText(this, "Не удалось отправить уведомление: разрешение не предоставлено.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required for this sensor
    }

    private fun updateTrackingState(active: Boolean) {
        isTrackingActive = active
        if (active) {
            trackingStatusTextView.text = "Статус: Активен"
            toggleTrackingButton.text = "Остановить трекинг"
            stepCounterSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            Toast.makeText(this, "Трекинг запущен!", Toast.LENGTH_SHORT).show()
        } else {
            trackingStatusTextView.text = "Статус: Неактивен"
            toggleTrackingButton.text = "Начать трекинг"
            sensorManager.unregisterListener(this)
            Toast.makeText(this, "Трекинг остановлен.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTrackingState(isTrackingActive)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        saveData()
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", previousTotalSteps)
        editor.putBoolean("is_tracking_active", isTrackingActive)
        editor.putString("reached_thresholds", reachedThresholds.joinToString(","))
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f)
        dailyStepGoal = sharedPreferences.getInt("daily_step_goal", 8000)
        userGender = sharedPreferences.getString("user_gender", "") ?: ""
        userWeight = sharedPreferences.getFloat("user_weight", 0f)
        dailyCalorieGoal = sharedPreferences.getInt("daily_calorie_goal", 2000)
        isTrackingActive = sharedPreferences.getBoolean("is_tracking_active", false)

        val savedThresholds = sharedPreferences.getString("reached_thresholds", "")
        if (!savedThresholds.isNullOrEmpty()) {
            reachedThresholds.addAll(savedThresholds.split(",").mapNotNull { it.toIntOrNull() })
        }
    }
}