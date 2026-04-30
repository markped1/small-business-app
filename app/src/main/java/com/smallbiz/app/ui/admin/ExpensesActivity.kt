package com.smallbiz.app.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smallbiz.app.data.model.Expense
import com.smallbiz.app.databinding.ActivityExpensesBinding
import com.smallbiz.app.databinding.DialogAddExpenseBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Expenses"

        expenseAdapter = ExpenseAdapter(
            onEdit = { expense -> showAddEditDialog(expense) },
            onDelete = { expense ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Expense")
                    .setMessage("Delete \"${expense.description}\"?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteExpense(expense) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@ExpensesActivity)
            adapter = expenseAdapter
        }

        viewModel.allExpenses.observe(this) { expenses ->
            expenseAdapter.submitList(expenses)
            val total = expenses.sumOf { it.amount }
            binding.tvTotalExpenses.text = "Total: ${CurrencyFormatter.format(total)}"
        }

        binding.fabAddExpense.setOnClickListener { showAddEditDialog(null) }
    }

    private fun showAddEditDialog(expense: Expense?) {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)

        expense?.let {
            dialogBinding.etExpenseDescription.setText(it.description)
            dialogBinding.etExpenseAmount.setText(it.amount.toString())
            dialogBinding.etExpenseCategory.setText(it.category)
        }

        AlertDialog.Builder(this)
            .setTitle(if (expense == null) "Add Expense" else "Edit Expense")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val desc = dialogBinding.etExpenseDescription.text.toString().trim()
                val amountStr = dialogBinding.etExpenseAmount.text.toString().trim()
                val category = dialogBinding.etExpenseCategory.text.toString().trim().ifEmpty { "General" }

                if (desc.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (expense == null) {
                    viewModel.insertExpense(Expense(description = desc, amount = amount, category = category))
                    Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateExpense(expense.copy(description = desc, amount = amount, category = category))
                    Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
