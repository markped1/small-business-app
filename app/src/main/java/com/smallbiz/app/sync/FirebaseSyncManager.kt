package com.smallbiz.app.sync

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smallbiz.app.data.model.Expense
import com.smallbiz.app.data.model.Sale

/**
 * Syncs sales and expenses to Firebase Realtime Database so the store
 * manager can view live data from their own phone without touching the
 * sales device.
 *
 * Firebase data structure:
 *   businesses/
 *     {businessId}/
 *       info/   { name, address, currency }
 *       sales/  { auto-key: sale fields }
 *       expenses/ { auto-key: expense fields }
 */
object FirebaseSyncManager {

    private val db by lazy { FirebaseDatabase.getInstance() }
    private var businessId: String = "default"
    val isEnabled: Boolean = true

    /** Called at app start on the sales device. */
    fun init(businessName: String, adminPin: String) {
        businessId = (businessName + adminPin)
            .hashCode().toString().replace("-", "n")
    }

    /** Called on the viewer phone when connecting to a remote business. */
    fun initWithId(id: String) {
        businessId = id
    }

    fun getBusinessId(): String = businessId

    // ── References ───────────────────────────────────────────────────────────
    private fun salesRef()    = db.getReference("businesses/$businessId/sales")
    private fun expensesRef() = db.getReference("businesses/$businessId/expenses")
    private fun infoRef()     = db.getReference("businesses/$businessId/info")

    // ── Push a completed customer transaction ─────────────────────────────────
    fun pushSales(sales: List<Sale>) {
        val ref = salesRef()
        sales.forEach { sale ->
            val key = ref.push().key ?: return@forEach
            ref.child(key).setValue(
                mapOf(
                    "productName"   to sale.productName,
                    "quantity"      to sale.quantity,
                    "sellingPrice"  to sale.sellingPrice,
                    "costPrice"     to sale.costPrice,
                    "totalAmount"   to sale.totalAmount,
                    "totalCost"     to sale.totalCost,
                    "profit"        to sale.profit,
                    "transactionId" to sale.transactionId,
                    "saleDate"      to sale.saleDate
                )
            )
        }
    }

    // ── Push a single expense ─────────────────────────────────────────────────
    fun pushExpense(expense: Expense) {
        val key = expensesRef().push().key ?: return
        expensesRef().child(key).setValue(
            mapOf(
                "description"  to expense.description,
                "amount"       to expense.amount,
                "category"     to expense.category,
                "expenseDate"  to expense.expenseDate
            )
        )
    }

    // ── Push business info (name, address, currency) ──────────────────────────
    fun pushBusinessInfo(name: String, address: String, currency: String) {
        infoRef().setValue(mapOf(
            "name"     to name,
            "address"  to address,
            "currency" to currency
        ))
    }

    // ── Live listener for the viewer phone ────────────────────────────────────
    fun listenForSales(
        onData: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ) {
        salesRef().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    @Suppress("UNCHECKED_CAST")
                    child.value as? Map<String, Any>
                }
                onData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

    fun listenForExpenses(
        onData: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ) {
        expensesRef().addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    @Suppress("UNCHECKED_CAST")
                    child.value as? Map<String, Any>
                }
                onData(list)
            }
            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }
}
