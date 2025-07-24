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

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavView.setupWithNavController(navController)

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)

        fab.setOnClickListener {
            // Перевіряємо, на якому екрані ми знаходимось
            when (navController.currentDestination?.id) {
                R.id.readFragment -> {
                    // Якщо на екрані "Read", виконуємо дію для переходу з нього
                    navController.navigate(R.id.action_readFragment_to_addEditBookFragment)
                }
                R.id.toReadFragment -> {
                    // Якщо на екрані "To Read", виконуємо дію для переходу з нього
                    navController.navigate(R.id.action_toReadFragment_to_addEditBookFragment)
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Додаємо наш новий екран до списку, де треба ховати FAB та нижнє меню
                R.id.addEditBookFragment -> {
                    fab.visibility = View.GONE
                    bottomNavView.visibility = View.GONE
                }
                R.id.readFragment, R.id.toReadFragment -> {
                    fab.visibility = View.VISIBLE
                    bottomNavView.visibility = View.VISIBLE
                }
                else -> {
                    fab.visibility = View.GONE
                    bottomNavView.visibility = View.VISIBLE
                }
            }
        }
    }
}