package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_meals"
)
data class SavedMeal(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * Examples:
     *
     * PBJ Sandwich
     * Four Hot Dogs
     * Four Hamburgers
     */
    val name: String,

    val isFavorite: Boolean = false,

    val createdAt: Long =
        System.currentTimeMillis(),

    val updatedAt: Long =
        System.currentTimeMillis()
)