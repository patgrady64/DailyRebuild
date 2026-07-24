package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FoodDao {

    /*
     * Used later when editing an existing saved product.
     */
    @Upsert
    suspend fun saveProduct(
        product: FoodProduct
    )

    /*
     * Creates a new product and returns its generated database ID.
     */
    @Insert
    suspend fun addProduct(
        product: FoodProduct
    ): Long

    @Query(
        """
        SELECT *
        FROM food_products
        WHERE id = :productId
        LIMIT 1
        """
    )
    suspend fun getProductById(
        productId: Long
    ): FoodProduct?

    @Query(
        """
        SELECT *
        FROM food_products
        WHERE barcode = :barcode
        LIMIT 1
        """
    )
    suspend fun getProductByBarcode(
        barcode: String
    ): FoodProduct?

    @Query(
        """
        SELECT *
        FROM food_products
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getAllProducts():
            List<FoodProduct>

    @Query(
        """
        SELECT *
        FROM food_products
        WHERE isFavorite = 1
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getFavoriteProducts():
            List<FoodProduct>

    @Insert
    suspend fun addFoodEntry(
        entry: FoodLogEntry
    ): Long

    @Query(
        """
        SELECT *
        FROM food_log_entries
        WHERE date = :date
        ORDER BY createdAt ASC
        """
    )
    suspend fun getEntriesForDate(
        date: String
    ): List<FoodLogEntry>

    @Delete
    suspend fun deleteFoodEntry(
        entry: FoodLogEntry
    )

    @Query(
        """
        DELETE FROM food_log_entries
        WHERE id = :entryId
        """
    )
    suspend fun deleteFoodEntryById(
        entryId: Long
    )
}