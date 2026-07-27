package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SavedMealWithIngredients(

    @Embedded
    val meal: SavedMeal,

    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val ingredients:
    List<SavedMealIngredient>
)