package com.example.bookdiarymobile.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    private var toolbarTitleTextView: TextView? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Викликаємо метод для ініціалізації Splash Screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbarTitleTextView = findViewById(R.id.toolbar_title_text)

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.readFragment, R.id.favoritesFragment, R.id.toReadFragment,
                R.id.backupFragment, R.id.statsFragment
            )
        )
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

            if (!isTopLevelDestination || destination.id == R.id.addEditBookFragment) {
                bottomNavView.visibility = View.GONE
            } else {
                bottomNavView.visibility = View.VISIBLE
            }

            when (destination.id) {
                R.id.readFragment, R.id.toReadFragment -> fab.visibility = View.VISIBLE
                else -> fab.visibility = View.GONE
            }

            if (destination.id != R.id.bookDetailFragment) {
                toolbarTitleTextView?.text = "Book Diary"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}