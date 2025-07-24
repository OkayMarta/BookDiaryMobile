package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bookdiarymobile.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- КРОК 1: НАЛАШТУВАННЯ НАВІГАЦІЇ ---

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Зв'язуємо нижнє меню з NavController
        bottomNavView.setupWithNavController(navController)


        // --- КРОК 2: ЛОГІКА ВИДИМОСТІ ДЛЯ FAB ---

        // Знаходимо нашу плаваючу кнопку дії (FAB) по її ID
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)

        // Додаємо слухача, який буде реагувати на кожну зміну екрана
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Перевіряємо ID екрана, на який ми перейшли
            when (destination.id) {
                // Якщо це екран "Read" або "To Read"
                R.id.readFragment, R.id.toReadFragment -> {
                    fab.visibility = View.VISIBLE // Показати кнопку
                }
                // Для всіх інших екранів
                else -> {
                    fab.visibility = View.GONE // Сховати кнопку
                }
            }
        }
    }
}