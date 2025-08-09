package com.example.bookdiarymobile.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Основний клас бази даних додатку, побудований на основі Room.
 *
 * Цей клас є абстрактним і наслідується від [RoomDatabase]. Він слугує точкою
 * доступу до бази даних та визначає, які сутності (entities) вона містить,
 * а також версію схеми.
 *
 * @property entities Список класів-сутностей, що входять до бази даних. В цьому випадку, це лише [Book].
 * @property version Версія бази даних. Збільшується при зміні схеми для міграції.
 * @property exportSchema Визначає, чи потрібно експортувати схему бази даних у JSON-файл.
 *                      `false` використовується для спрощення, коли автоматизовані тести міграцій не плануються.
 */
@Database(entities = [Book::class], version = 1, exportSchema = false)
abstract class BookDatabase : RoomDatabase() {

    /**
     * Надає екземпляр [BookDao].
     *
     * Room автоматично згенерує реалізацію цього методу, що дозволить
     * отримати доступ до всіх методів для роботи з таблицею книг.
     *
     * @return Екземпляр [BookDao] для виконання операцій з базою даних.
     */
    abstract fun bookDao(): BookDao

}