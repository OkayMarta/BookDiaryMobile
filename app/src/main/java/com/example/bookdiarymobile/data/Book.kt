package com.example.bookdiarymobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books_table")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val author: String,
    val genre: String,
    val description: String,
    val coverImagePath: String?,

    val status: BookStatus,
    val dateAdded: Long,
    val dateRead: Long?,
    val rating: Int?,
    val isFavorite: Boolean = false
)