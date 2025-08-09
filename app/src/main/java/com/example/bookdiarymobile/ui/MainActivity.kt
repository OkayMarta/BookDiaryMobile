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

/**
 * Головна і єдина Activity у додатку.
 *
 * Вона виступає як контейнер (host) для `NavHostFragment` і керує основними елементами UI,
 * які є спільними для багатьох екранів, такими як `Toolbar`, `BottomNavigationView`
 * та `FloatingActionButton` (FAB). Також відповідає за динамічне керування
 * видимістю цих елементів та зміну заголовка в Toolbar залежно від поточного фрагмента.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** Контролер, що керує навігацією між фрагментами в межах `NavHostFragment`. */
    private lateinit var navController: NavController
    /**
     * Конфігурація для `ActionBar`, яка визначає, які екрани є "верхньорівневими".
     * На цих екранах кнопка "назад" (стрілка) не відображається.
     */
    private lateinit var appBarConfiguration: AppBarConfiguration
    /** Посилання на `TextView` у кастомному `Toolbar` для динамічної зміни заголовка. */
    private var toolbarTitleTextView: TextView? = null

    /**
     * Викликається при створенні Activity. Відповідає за ініціалізацію UI,
     * `SplashScreen`, `Toolbar`, `NavController` та налаштування слухачів.
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Ініціалізує та відображає стартовий екран (Splash Screen) перед тим, як буде показано основний контент.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ініціалізація Toolbar та встановлення її як ActionBar для Activity.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Вимкнення стандартного заголовка, оскільки використовується кастомний TextView.
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbarTitleTextView = findViewById(R.id.toolbar_title_text)

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        // Знаходження NavHostFragment та отримання з нього NavController.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Створення конфігурації AppBarConfiguration, де перелічені всі екрани верхнього рівня.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.readFragment, R.id.favoritesFragment, R.id.toReadFragment,
                R.id.backupFragment, R.id.statsFragment
            )
        )
        // Зв'язування ActionBar з NavController, що автоматично оновлює заголовок та кнопку "назад".
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Зв'язування BottomNavigationView з NavController.
        bottomNavView.setupWithNavController(navController)

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        // Обробник натискання на FloatingActionButton (FAB).
        fab.setOnClickListener {
            val currentDestinationId = navController.currentDestination?.id
            // Визначає поточний екран і, залежно від нього, виконує навігацію
            // до екрану додавання книги, передаючи відповідний статус (`READ` або `TO_READ`).
            val args = bundleOf(
                "book_status" to if (currentDestinationId == R.id.readFragment) "READ" else "TO_READ",
                "title" to "Add Book"
            )
            when (currentDestinationId) {
                R.id.readFragment -> navController.navigate(R.id.action_readFragment_to_addEditBookFragment, args)
                R.id.toReadFragment -> navController.navigate(R.id.action_toReadFragment_to_addEditBookFragment, args)
            }
        }

        // Слухач змін поточного екрану навігації для динамічного керування UI.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevelDestination = appBarConfiguration.topLevelDestinations.contains(destination.id)

            // Приховує BottomNavigationView на всіх екранах, що не є верхньорівневими.
            if (!isTopLevelDestination || destination.id == R.id.addEditBookFragment) {
                bottomNavView.visibility = View.GONE
            } else {
                bottomNavView.visibility = View.VISIBLE
            }

            // Показує FAB лише на екранах ReadFragment та ToReadFragment.
            when (destination.id) {
                R.id.readFragment, R.id.toReadFragment -> fab.visibility = View.VISIBLE
                else -> fab.visibility = View.GONE
            }

            // Скидає заголовок Toolbar до стандартного, якщо поточний екран не є екраном деталей.
            if (destination.id != R.id.bookDetailFragment) {
                toolbarTitleTextView?.text = "Book Diary"
            }
        }
    }

    /**
     * Обробляє натискання на системну кнопку "назад" (стрілка в Toolbar).
     * Передає керування `NavController` для здійснення переходу на попередній екран.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}