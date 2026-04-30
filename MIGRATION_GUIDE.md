# ToMega POS — Database Migration Guide

## ⚠️ CRITICAL: Never lose user data

User sales, products, and expenses are extremely sensitive.
**NEVER** use `fallbackToDestructiveMigration()` — it deletes all data.

---

## How to safely add a new feature that changes the database

### Step 1 — Decide what changes
Examples:
- Adding a new column to an existing table
- Adding a new table
- Renaming a column (requires table copy — see below)

### Step 2 — Bump the version number

In `AppDatabase.kt`, change:
```kotlin
@Database(version = 2, ...)   // old
@Database(version = 3, ...)   // new
```

### Step 3 — Write the migration

Add a new `Migration` object in `AppDatabase.kt`:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ADD a column (safe — existing rows get the default value)
        database.execSQL(
            "ALTER TABLE sales ADD COLUMN discount REAL NOT NULL DEFAULT 0.0"
        )
    }
}
```

### Step 4 — Register the migration

In `getDatabase()`, add it to `addMigrations()`:
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

### Step 5 — Update the APK version

In `app/build.gradle`:
```groovy
versionCode 2        // increment by 1
versionName "1.1"    // update display version
```

### Step 6 — Update Firebase Remote Config

In Firebase Console → Remote Config:
- `latest_version_code` → set to new versionCode (e.g. 2)
- `latest_version_name` → set to new versionName (e.g. "1.1")
- `apk_download_url`    → URL where the new APK is hosted
- `update_message`      → what changed (e.g. "New discount feature added")
- `force_update`        → false (unless it's a critical security fix)

Users will see a non-blocking "Update available" dialog next time they open the app.

---

## Common migration patterns

### Add a column
```kotlin
database.execSQL(
    "ALTER TABLE products ADD COLUMN barcode TEXT"
)
```

### Add a new table
```kotlin
database.execSQL("""
    CREATE TABLE IF NOT EXISTS customers (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        phone TEXT,
        createdAt INTEGER NOT NULL DEFAULT 0
    )
""")
```

### Rename a column (SQLite workaround — copy table)
```kotlin
// 1. Create new table with correct column name
database.execSQL("""
    CREATE TABLE sales_new (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        productId INTEGER NOT NULL,
        productName TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        sellingPrice REAL NOT NULL,
        costPrice REAL NOT NULL,
        totalAmount REAL NOT NULL,
        totalCost REAL NOT NULL,
        profit REAL NOT NULL,
        transactionId TEXT NOT NULL,
        saleDate INTEGER NOT NULL,
        newColumnName TEXT NOT NULL DEFAULT ''
    )
""")
// 2. Copy all data
database.execSQL("""
    INSERT INTO sales_new SELECT id, productId, productName, quantity,
    sellingPrice, costPrice, totalAmount, totalCost, profit,
    transactionId, saleDate, '' FROM sales
""")
// 3. Drop old table
database.execSQL("DROP TABLE sales")
// 4. Rename new table
database.execSQL("ALTER TABLE sales_new RENAME TO sales")
```

---

## Version history

| Version | Change | Migration |
|---------|--------|-----------|
| 1 | Initial schema | — |
| 2 | Added `stockQuantity` to products | MIGRATION_1_2 |

*(Add new rows here as you make changes)*

---

## Update delivery

Updates are delivered automatically via GitHub Releases.
See `UpdateManager.kt` for implementation details.

### How to release a new version

1. **Bump version** in `app/build.gradle`:
   ```groovy
   versionCode 2        // increment by 1 each release
   versionName "1.1"    // semantic version
   ```

2. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   APK is at: `app/build/outputs/apk/debug/app-debug.apk`

3. **Create a GitHub Release**:
   - Go to: https://github.com/markped1/small-business-app/releases
   - Click **"Draft a new release"**
   - **Tag**: `v1.1`  ← must match versionName (with or without "v")
   - **Title**: `ToMega POS v1.1`
   - **Description**: what changed (shown to users in the update dialog)
   - **Attach** the APK file
   - Click **"Publish release"**

4. **All installed apps** will see the update dialog on next launch.

The user sees a non-blocking dialog:
> "🔄 Update Available — v1.1. [release notes]. Download size: X MB. Install now or later?"

They can dismiss it and keep working. The update downloads in the background and prompts to install when done.
