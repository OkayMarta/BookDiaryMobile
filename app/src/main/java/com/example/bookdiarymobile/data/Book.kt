package com.example.bookdiarymobile.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val author: String,
    val genre: String,
    val description: String,

    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String?,

    val status: BookStatus,

    @ColumnInfo(name = "date_added")
    val dateAdded: Long,

    @ColumnInfo(name = "date_read")
    val dateRead: Long?,

    val rating: Int?,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)