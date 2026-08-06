package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CatalogImportDao {
    @Query("UPDATE catalog_products SET active = 0")
    suspend fun deactivateAllProducts()

    @Upsert
    suspend fun upsertProducts(products: List<CatalogProduct>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<CatalogProductRole>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItems(items: List<CatalogPantryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<CatalogImportSource>)

    @Query("DELETE FROM catalog_product_roles")
    suspend fun clearRoles()

    @Query("DELETE FROM catalog_pantry_items")
    suspend fun clearPantryItems()

    @Query("DELETE FROM catalog_import_sources")
    suspend fun clearSources()

    @Query("SELECT COUNT(*) FROM catalog_products")
    suspend fun productCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_products WHERE active = 1")
    suspend fun activeProductCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_product_roles WHERE active = 1")
    suspend fun activeRoleCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_pantry_items")
    suspend fun pantryItemCount(): Int

    @Query("SELECT MAX(importedAtEpochMillis) FROM catalog_import_sources")
    suspend fun lastImportedAt(): Long?

    @Query("SELECT productId FROM catalog_products")
    suspend fun productIds(): List<String>

    @Query("SELECT * FROM catalog_products WHERE active = 1 ORDER BY productName COLLATE NOCASE")
    suspend fun getActiveProducts(): List<CatalogProduct>

    @Query("SELECT * FROM catalog_products WHERE productType = :productType AND active = 1 ORDER BY productName COLLATE NOCASE")
    suspend fun getActiveProductsByType(productType: String): List<CatalogProduct>

    @Query(
        """
        SELECT product.*
        FROM catalog_products AS product
        INNER JOIN catalog_product_roles AS role
            ON role.productId = product.productId
        WHERE product.active = 1
          AND role.active = 1
          AND LOWER(role.role) = LOWER(:roleName)
        ORDER BY product.productName COLLATE NOCASE
        """
    )
    suspend fun getActiveProductsForRole(roleName: String): List<CatalogProduct>

    @Query("SELECT * FROM catalog_product_roles WHERE active = 1 ORDER BY productId, role COLLATE NOCASE")
    suspend fun getActiveRoles(): List<CatalogProductRole>

    @Query("SELECT * FROM catalog_pantry_items ORDER BY productId")
    suspend fun getAllPantryItems(): List<CatalogPantryItem>

    @Query("SELECT * FROM catalog_pantry_items WHERE productId = :productId LIMIT 1")
    suspend fun getPantryItem(productId: String): CatalogPantryItem?
}
