package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Update

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

    @Update
    suspend fun updateProduct(
        product: FoodProduct
    )

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

       OR barcode = ('0' || :barcode)

       OR (
            length(:barcode) = 13
            AND substr(:barcode, 1, 1) = '0'
            AND barcode = substr(:barcode, 2)
       )

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
        WHERE isReusable = 1
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
          AND isReusable = 1
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getFavoriteProducts():
            List<FoodProduct>


    @Query(
        """
        SELECT product.*
        FROM food_products AS product
        INNER JOIN (
            SELECT productId, MAX(createdAt) AS lastLoggedAt
            FROM food_log_entries
            WHERE sourceTypeSnapshot != 'PACKAGED'
            GROUP BY productId
        ) AS recent ON recent.productId = product.id
        WHERE product.isReusable = 0
        ORDER BY recent.lastLoggedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentPreparedProducts(limit: Int = 12): List<FoodProduct>

    @Insert
    suspend fun addFoodEntry(
        entry: FoodLogEntry
    ): Long

    @Update
    suspend fun updateFoodEntry(
        entry: FoodLogEntry
    ): Int

    /*
     * Individual foods can be combined when the same food is logged
     * more than once on the same day. Saved-meal ingredients are
     * intentionally excluded because each meal log must remain intact.
     *
     * Product ID is the strongest match. The exact displayed product
     * name is a fallback for manually entered foods that may have been
     * saved as a new product record on a later entry.
     */
    @Query(
        """
        SELECT *
        FROM food_log_entries
        WHERE date = :date
          AND (mealLogId IS NULL OR TRIM(mealLogId) = '')
          AND (
                productId = :productId
                OR LOWER(TRIM(productNameSnapshot)) =
                   LOWER(TRIM(:productNameSnapshot))
              )
          AND LOWER(TRIM(unit)) = LOWER(TRIM(:unit))
          AND LOWER(TRIM(COALESCE(mealName, ''))) =
              LOWER(TRIM(COALESCE(:mealName, '')))
        ORDER BY createdAt ASC
        LIMIT 1
        """
    )
    suspend fun findMergeableIndividualEntry(
        date: String,
        productId: Long,
        productNameSnapshot: String,
        unit: String,
        mealName: String?
    ): FoodLogEntry?

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

    @Query(
        """
        SELECT *
        FROM food_log_entries
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, createdAt ASC
        """
    )
    suspend fun getEntriesBetween(
        startDate: String,
        endDate: String
    ): List<FoodLogEntry>

    @Query(
        """
        SELECT *
        FROM food_log_entries
        ORDER BY date ASC, createdAt ASC
        """
    )
    suspend fun getAllEntries(): List<FoodLogEntry>

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

    @Query(
        """
        DELETE FROM food_products
        WHERE id = :productId
        """
    )
    suspend fun deleteProductById(
        productId: Long
    )

    @Query(
        """
        DELETE FROM food_log_entries
        WHERE mealLogId = :mealLogId
        """
    )
    suspend fun deleteFoodEntriesByMealLogId(
        mealLogId: String
    )

    @Query(
        """
        DELETE FROM food_log_entries
        WHERE date = :date
        """
    )
    suspend fun deleteEntriesForDate(
        date: String
    )
}