package ru.usfeu.fittrackergemini

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText // Импорт для EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity

class GenderSelectionActivity : ComponentActivity() {

    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var weightEditText: EditText // Объявление EditText
    private lateinit var activityLevelRadioGroup: RadioGroup // Объявление RadioGroup для активности
    private lateinit var confirmGenderButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gender_selection)

        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        weightEditText = findViewById(R.id.weightEditText) // Инициализация EditText
        activityLevelRadioGroup = findViewById(R.id.activityLevelRadioGroup) // Инициализация RadioGroup для активности
        confirmGenderButton = findViewById(R.id.confirmGenderButton)

        confirmGenderButton.setOnClickListener {
            // 1. Определяем выбранный пол
            val selectedGender: String = when (genderRadioGroup.checkedRadioButtonId) {
                R.id.radioMale -> "male"
                R.id.radioFemale -> "female"
                else -> {
                    Toast.makeText(this, "Пожалуйста, выберите Ваш пол.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 2. Получаем вес пользователя
            val weightText = weightEditText.text.toString()
            if (weightText.isBlank()) {
                Toast.makeText(this, "Пожалуйста, введите Ваш вес.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userWeight = weightText.toFloatOrNull()
            if (userWeight == null || userWeight <= 0) {
                Toast.makeText(this, "Пожалуйста, введите корректный вес.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Определяем выбранный коэффициент активности
            val activityCoefficient: Float = when (activityLevelRadioGroup.checkedRadioButtonId) {
                R.id.radioLowActivity -> 1.1f
                R.id.radioModerateActivity -> 1.3f
                R.id.radioHighActivity -> 1.5f
                else -> {
                    Toast.makeText(this, "Пожалуйста, выберите уровень активности.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 4. Рассчитываем суточную норму калорий
            val baseCaloriesPerHour: Float = if (selectedGender == "male") {
                1.0f // 1 ккал на кг веса в час для мужчин
            } else {
                0.9f // 0.9 ккал на кг веса в час для женщин
            }

            val dailyCalorieGoal = (baseCaloriesPerHour * userWeight * 24 * activityCoefficient).toInt()

            // 5. Сохраняем все данные в SharedPreferences
            val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("user_gender", selectedGender)
            editor.putFloat("user_weight", userWeight) // Сохраняем вес
            editor.putFloat("activity_coefficient", activityCoefficient) // Сохраняем коэффициент активности
            editor.putInt("daily_calorie_goal", dailyCalorieGoal) // Сохраняем рассчитанную норму калорий
            editor.putBoolean("gender_selected", true) // Флаг, что параметры пола/веса/активности выбраны
            editor.apply()

            Toast.makeText(this, "Параметры сохранены. Норма калорий: $dailyCalorieGoal ккал.", Toast.LENGTH_LONG).show()

            // 6. Переходим на StepGoalActivity
            val intent = Intent(this, StepGoalActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}