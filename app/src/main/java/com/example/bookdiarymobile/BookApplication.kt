package com.example.bookdiarymobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Основний клас додатку, що успадковується від Application.
 * Анотація @HiltAndroidApp запускає генерацію коду Hilt.
 */
@HiltAndroidApp
class BookApplication : Application() {
    // Hilt ствоює БД і репозиторій...
}