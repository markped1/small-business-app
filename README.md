# SmallBiz POS

A complete Android Point-of-Sale app for small businesses.

## Features

### Sales Screen
- Visual product grid with images — usable by anyone, including those who can't read
- `+` / `−` quantity buttons per item
- Live cart with running total
- One-tap sale confirmation

### Admin Panel (PIN-protected)
- Add/edit products with name, selling price, cost price, stock quantity, and photo
- Manage expenses (rent, transport, etc.)
- Business name & address setup — each business gets its own identity

### Reports
- **Daily**, **Weekly**, and **Monthly** reports showing:
  - Gross Sales
  - Cost of Goods Sold
  - Gross Profit
  - Expenses
  - **Net Profit** (colour-coded green/red)

### Daily Stock Report
- Per-product breakdown: Opening Stock → Sold Today → Remaining
- Value of remaining stock (purchase value, sales value, profit in stock)
- Today's revenue and profit per product
- Overall summary totals

## Tech Stack
- **Language**: Kotlin
- **Database**: Room (SQLite)
- **Architecture**: MVVM (ViewModel + LiveData)
- **UI**: Material Design 3, ViewBinding
- **Images**: Glide

## Requirements
- Android 7.0+ (API 24)
- Android Studio Hedgehog or newer

## Build
```bash
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## First Launch
On first launch the app asks for:
1. Business name
2. Business address
3. Admin PIN (4+ digits)

This can be updated anytime from the Admin panel → Settings.
