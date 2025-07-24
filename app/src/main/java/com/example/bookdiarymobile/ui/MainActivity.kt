package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.core.os.bundleOf
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
            val currentDestinationId = navController.currentDestination?.id
            // Створюємо bundle для передачі аргументів
            val args = bundleOf(
                // Ключ "book_status" має співпадати з тим, що ми вказали в nav_graph.xml
                "book_status" to if (currentDestinationId == R.id.readFragment) {
                    "READ" // Якщо ми на екрані "Read", передаємо "READ"
                } else {
                    "TO_READ" // В іншому випадку (на екрані "To Read"), передаємо "TO_READ"
                }
            )

            when (currentDestinationId) {
                R.id.readFragment -> {
                    navController.navigate(R.id.action_readFragment_to_addEditBookFragment, args)
                }
                R.id.toReadFragment -> {
                    navController.navigate(R.id.action_toReadFragment_to_addEditBookFragment, args)
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