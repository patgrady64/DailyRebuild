package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_meal_ingredients",

    foreignKeys = [
        ForeignKey(
            entity = SavedMeal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = FoodProduct::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],

    indices = [
        Index(value = ["mealId"]),
        Index(value = ["productId"])
    ]
)
data class SavedMealIngredient(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val mealId: Long,

    val productId: Long,

    /*
     * LABEL_SERVINGS:
     *
     * amount = 4 means four nutrition-label servings.
     *
     * MEASURED_AMOUNT:
     *
     * amount = 2 means two slices, tablespoons,
     * patties, cups, and so on.
     */
    val amountMode: String,

    val amount: Double,

    /*
     * Preserves ingredient display order.
     */
    val sortOrder: Int = 0
)

object MealAmountMode {

    const val LABEL_SERVINGS =
        "LABEL_SERVINGS"

    const val MEASURED_AMOUNT =
        "MEASURED_AMOUNT"
}