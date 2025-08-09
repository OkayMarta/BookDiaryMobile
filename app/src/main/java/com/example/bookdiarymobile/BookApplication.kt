package com.example.bookdiarymobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Основний клас додатку, який слугує точкою входу та ініціалізує Hilt.
 *
 * Анотація [@HiltAndroidApp] вмикає механізм впровадження залежностей Hilt
 * на рівні всього додатку. Вона запускає генерацію коду, який створює
 * контейнер залежностей, що буде доступний протягом усього життєвого циклу
 * додатку. Цей клас має бути зареєстрований в `AndroidManifest.xml`.
 */
@HiltAndroidApp
class BookApplication : Application()