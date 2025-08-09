package com.example.bookdiarymobile.utils

import android.os.Build
import android.os.Bundle
import java.io.Serializable

/**
 * Функція-розширення для [Bundle], яка забезпечує сумісний та типобезпечний
 * спосіб отримання [Serializable] об'єктів.
 *
 * Ця функція абстрагує відмінності в API для отримання серіалізованих даних
 * між різними версіями Android. Вона використовує сучасний, типобезпечний метод
 * `getSerializable(key, Class)` на Android 13 (Tiramisu, API 33) і вище, та застарілий
 * метод з ручним приведенням типу на старіших версіях.
 *
 * @param T Очікуваний тип [Serializable] об'єкта. `reified` дозволяє
 *          отримати доступ до типу `T` під час виконання.
 * @param key Ключ, за яким об'єкт збережено в [Bundle].
 * @return Десеріалізований об'єкт типу [T] або `null`, якщо об'єкт
 *         за вказаним ключем не знайдено або його тип не відповідає очікуваному.
 */
inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Використовуємо новий, типобезпечний метод для Android 13 (API 33) і вище
        getSerializable(key, T::class.java)
    } else {
        // Використовуємо застарілий метод для старих версій і робимо безпечне приведення типу
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}