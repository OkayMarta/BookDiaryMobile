package com.example.bookdiarymobile.utils

import android.os.Build
import android.os.Bundle
import java.io.Serializable

/**
 * Функція-розширення для Bundle, яка безпечно отримує Serializable об'єкт,
 * враховуючи відмінності між новими та старими версіями Android API.
 *
 * @param key Ключ, за яким зберігається об'єкт.
 * @param T Тип Serializable об'єкта, який очікується.
 * @return Об'єкт типу T або null, якщо його не знайдено або тип не збігається.
 */
inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Використовуємо новий, типобезпечний метод для Android 13 (API 33) і вище
        getSerializable(key, T::class.java)
    } else {
        // Використовуємо застарілий метод для старих версій
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}