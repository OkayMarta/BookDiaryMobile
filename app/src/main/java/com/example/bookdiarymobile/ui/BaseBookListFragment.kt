package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.SortOrder
import com.example.bookdiarymobile.utils.getSerializableCompat
import kotlinx.coroutines.launch

/**
 * Тип-псевдонім (typealias) для лямбда-функції, що інфлейтить ViewBinding.
 * Це дозволяє передавати функцію для створення біндінга в конструктор базового фрагмента,
 * роблячи його повністю незалежним від конкретної реалізації ViewBinding.
 */
typealias Inflater<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

/**
 * Абстрактний базовий фрагмент для екранів, що відображають списки книг
 * (наприклад, "Прочитані", "Хочу прочитати", "Улюблені").
 *
 * Він інкапсулює спільну логіку для:
 * - Налаштування меню (пошук, сортування).
 * - Обробки результатів з екрану сортування.
 * - Підписки на оновлення списку книг від ViewModel.
 * - Налаштування `BookAdapter`.
 *
 * Це дозволяє уникнути дублювання коду в дочірніх фрагментах.
 *
 * @param VB Тип ViewBinding, що використовується у конкретному дочірньому фрагменті.
 * @param VM Тип ViewModel, що має успадковуватися від [BaseBookListViewModel].
 * @property bindingInflater Лямбда-функція для інфлейту відповідного ViewBinding.
 */
abstract class BaseBookListFragment<VB : ViewBinding, VM : BaseBookListViewModel>(
    private val bindingInflater: Inflater<VB>
) : Fragment() {

    private var _binding: VB? = null
    /**
     * Захищений доступ до ViewBinding, гарантує, що він не є null після `onViewCreated`
     * і до `onDestroyView`.
     */
    protected val binding get() = _binding!!

    /**
     * Абстрактна властивість для ViewModel. Кожен дочірній клас має надати
     * свою реалізацію ViewModel.
     */
    protected abstract val viewModel: VM

    /**
     * Абстрактний метод для навігації до екрану деталей книги.
     * Дочірній клас реалізує його, викликаючи відповідну дію Navigation Component.
     * @param bookId Ідентифікатор книги для перегляду.
     */
    protected abstract fun navigateToBookDetail(bookId: Int)

    /**
     * Абстрактний метод для навігації до екрану вибору параметрів сортування.
     */
    protected abstract fun navigateToSortOptions()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Використовуємо передану лямбду для створення конкретного ViewBinding
        _binding = bindingInflater.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Створюємо адаптер і передаємо йому лямбду для обробки кліків
        val adapter = BookAdapter { clickedBook ->
            navigateToBookDetail(clickedBook.id)
        }

        // Дочірній клас налаштує свої UI компоненти (RecyclerView) з цим адаптером
        setupUI(adapter)

        // Слухач для отримання результату з екрану сортування
        setFragmentResultListener("SORT_REQUEST") { _, bundle ->
            val newSortOrder = bundle.getSerializableCompat<SortOrder>("SORT_ORDER")
            newSortOrder?.let { viewModel.applySortOrder(it) }
        }

        // Налаштування меню (пошук, сортування)
        setupMenu()

        // Запуск корутини для спостереження за списком книг
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.books.collect { books ->
                    // Оновлюємо стан "порожнього списку"
                    updateEmptyState(books.isEmpty())
                    // Передаємо новий список в адаптер
                    adapter.submitList(books)
                }
            }
        }
    }

    /**
     * Абстрактний метод, який дочірні класи повинні реалізувати для налаштування
     * своїх UI-компонентів, зокрема RecyclerView.
     * @param adapter Адаптер, який потрібно встановити для RecyclerView.
     */
    abstract fun setupUI(adapter: BookAdapter)

    /**
     * Абстрактний метод для оновлення UI, коли список книг порожній або не порожній.
     * (наприклад, показ/приховування тексту "Список порожній").
     * @param isEmpty `true`, якщо список книг порожній.
     */
    abstract fun updateEmptyState(isEmpty: Boolean)

    /**
     * Налаштовує меню на панелі інструментів, використовуючи lifecycle-aware `MenuProvider`.
     */
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                // Відновлення стану пошукового запиту, якщо він є
                val currentQuery = viewModel.searchQuery.value
                if (currentQuery.isNotEmpty()) {
                    searchItem.expandActionView()
                    searchView.setQuery(currentQuery, false)
                    searchView.clearFocus()
                }

                // Слухач для обробки введення тексту в пошук
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = true
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.applySearchQuery(newText.orEmpty())
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Обробка натискання на кнопку сортування
                return if (menuItem.itemId == R.id.action_sort) {
                    navigateToSortOptions()
                    true
                } else {
                    false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очищення біндінга для уникнення витоків пам'яті
    }
}