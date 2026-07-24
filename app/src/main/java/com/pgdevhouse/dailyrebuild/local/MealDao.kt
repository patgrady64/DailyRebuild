package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface MealDao {

    @Insert
    suspend fun addMeal(
        meal: SavedMeal
    ): Long

    @Insert
    suspend fun addIngredient(
        ingredient: SavedMealIngredient
    ): Long

    @Insert
    suspend fun addIngredients(
        ingredients:
        List<SavedMealIngredient>
    )

    @Update
    suspend fun updateMeal(
        meal: SavedMeal
    )

    @Query(
        """
        SELECT *
        FROM saved_meals
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getAllMeals():
            List<SavedMeal>

    @Transaction
    @Query(
        """
        SELECT *
        FROM saved_meals
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getAllMealsWithIngredients():
            List<SavedMealWithIngredients>

    @Transaction
    @Query(
        """
        SELECT *
        FROM saved_meals
        WHERE id = :mealId
        LIMIT 1
        """
    )
    suspend fun getMealWithIngredients(
        mealId: Long
    ): SavedMealWithIngredients?

    @Query(
        """
        DELETE FROM saved_meals
        WHERE id = :mealId
        """
    )
    suspend fun deleteMeal(
        mealId: Long
    )
}