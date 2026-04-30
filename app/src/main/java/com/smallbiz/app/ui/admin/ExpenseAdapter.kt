package com.smallbiz.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smallbiz.app.data.model.Expense
import com.smallbiz.app.databinding.ItemExpenseBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val onEdit: (Expense) -> Unit,
    private val onDelete: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.tvExpenseDescription.text = expense.description
            binding.tvExpenseAmount.text = CurrencyFormatter.format(expense.amount)
            binding.tvExpenseCategory.text = expense.category
            binding.tvExpenseDate.text = sdf.format(Date(expense.expenseDate))
            binding.btnEditExpense.setOnClickListener { onEdit(expense) }
            binding.btnDeleteExpense.setOnClickListener { onDelete(expense) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(old: Expense, new: Expense) = old.id == new.id
            override fun areContentsTheSame(old: Expense, new: Expense) = old == new
        }
    }
}
