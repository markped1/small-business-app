package com.smallbiz.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smallbiz.app.data.model.Expense
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.data.model.Sale

/**
 * ToMega POS — Room Database
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * DATA SAFETY RULES — READ BEFORE MAKING ANY SCHEMA CHANGE
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * 1. NEVER use fallbackToDestructiveMigration() — it wipes all user data.
 *
 * 2. Every time you add/remove/rename a column or table:
 *    a. Increment the version number in @Database(version = X)
 *    b. Write a new Migration(oldVersion, newVersion) object below
 *    c. Add it to addMigrations(...) in getDatabase()
 *    d. Test the migration on a device that has the old version installed
 *
 * 3. Safe SQL operations for migrations:
 *    ADD column:    ALTER TABLE t ADD COLUMN col TYPE NOT NULL DEFAULT value
 *    ADD table:     CREATE TABLE IF NOT EXISTS ...
 *    RENAME table:  ALTER TABLE old RENAME TO new  (SQLite 3.25+)
 *    DROP column:   NOT supported in SQLite — create new table, copy, drop old
 *
 * 4. exportSchema = true writes a JSON snapshot of the schema to app/schemas/
 *    Commit these files to git — they are your migration audit trail.
 *
 * 5. Current version history:
 *    v1 → v2 : Added stockQuantity INTEGER to products table
 *    (add new entries here as you make changes)
 * ══════════════════════════════════════════════════════════════════════════════
 */
@Database(
    entities  = [Product::class, Sale::class, Expense::class],
    version   = 2,
    exportSchema = true   // writes schema JSON to app/schemas/ — commit to git
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ── Migration v1 → v2 ─────────────────────────────────────────────────
        // Added: stockQuantity INTEGER NOT NULL DEFAULT 0 to products
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN stockQuantity INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // ── Template for next migration ───────────────────────────────────────
        // When you need to add a column in the future, copy this template:
        //
        // val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         // Example: add a new column to sales
        //         database.execSQL(
        //             "ALTER TABLE sales ADD COLUMN discount REAL NOT NULL DEFAULT 0.0"
        //         )
        //     }
        // }
        //
        // Then add MIGRATION_2_3 to addMigrations() below AND bump version to 3.

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smallbiz_database"
                )
                    // List ALL migrations here — never skip one
                    .addMigrations(MIGRATION_1_2)
                    // NO fallbackToDestructiveMigration() — protects user data
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
