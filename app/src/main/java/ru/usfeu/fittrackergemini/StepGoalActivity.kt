package ru.usfeu.fittrackergemini

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity

class StepGoalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_goal)

        val stepGoalRadioGroup: RadioGroup = findViewById(R.id.stepGoalRadioGroup)
        val saveGoalButton: Button = findViewById(R.id.saveGoalButton)

        saveGoalButton.setOnClickListener {
            val selectedGoal: Int = when (stepGoalRadioGroup.checkedRadioButtonId) {
                R.id.radio3000_5000 -> 5000
                R.id.radio5000_7000 -> 7000
                R.id.radio6000_8000 -> 8000
                R.id.radio12000_15000 -> 15000
                else -> 8000 // Значение по умолчанию, если ничего не выбрано
            }

            val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("daily_step_goal", selectedGoal)
            editor.putBoolean("step_goal_set", true) // ИЗМЕНЕНО: Теперь устанавливаем этот флаг
            editor.apply()

            Toast.makeText(this, "Цель по шагам сохранена: $selectedGoal", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}