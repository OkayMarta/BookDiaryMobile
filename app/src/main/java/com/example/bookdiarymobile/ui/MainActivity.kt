package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.bookdiarymobile.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // --- 1. СТВОРЮЄМО AppBarConfiguration ---
        // Перераховуємо тут ID всіх фрагментів з нижнього меню.
        // Це "скаже" системі, що вони є екранами верхнього рівня.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.readFragment, R.id.favoritesFragment, R.id.toReadFragment,
                R.id.backupFragment, R.id.statsFragment
            )
        )
        // --- КІНЕЦЬ НОВОГО БЛОКУ ---

        // 2. Передаємо appBarConfiguration в метод налаштування
        setupActionBarWithNavController(navController, appBarConfiguration)

        bottomNavView.setupWithNavController(navController)

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val currentDestinationId = navController.currentDestination?.id
            val args = bundleOf(
                "book_status" to if (currentDestinationId == R.id.readFragment) "READ" else "TO_READ",
                "title" to "Add Book"
            )
            when (currentDestinationId) {
                R.id.readFragment -> navController.navigate(R.id.action_readFragment_to_addEditBookFragment, args)
                R.id.toReadFragment -> navController.navigate(R.id.action_toReadFragment_to_addEditBookFragment, args)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevelDestination = appBarConfiguration.topLevelDestinations.contains(destination.id)

            // Ховаємо нижнє меню та кнопку FAB на екранах, які не є головними
            // (окрім випадків, де FAB потрібна)
            if (!isTopLevelDestination || destination.id == R.id.addEditBookFragment) {
                bottomNavView.visibility = View.GONE
            } else {
                bottomNavView.visibility = View.VISIBLE
            }

            when (destination.id) {
                R.id.readFragment, R.id.toReadFragment -> fab.visibility = View.VISIBLE
                else -> fab.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // 3. Також використовуємо appBarConfiguration тут
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}