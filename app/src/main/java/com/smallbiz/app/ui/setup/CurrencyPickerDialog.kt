package com.smallbiz.app.ui.setup

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smallbiz.app.R
import com.smallbiz.app.utils.CurrencyItem

class CurrencyPickerDialog(
    private val currentCode: String,
    private val onSelected: (CurrencyItem) -> Unit
) : DialogFragment() {

    private val allCurrencies: List<CurrencyItem> by lazy { CurrencyItem.buildList() }
    private lateinit var adapter: CurrencyListAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_currency_picker, container, false)

        val etSearch = view.findViewById<EditText>(R.id.etCurrencySearch)
        val rvCurrencies = view.findViewById<RecyclerView>(R.id.rvCurrencies)
        val tvTitle = view.findViewById<TextView>(R.id.tvCurrencyTitle)
        tvTitle.text = "Select Currency"

        adapter = CurrencyListAdapter(currentCode) { item ->
            onSelected(item)
            dismiss()
        }

        rvCurrencies.layoutManager = LinearLayoutManager(requireContext())
        rvCurrencies.adapter = adapter
        adapter.submitList(allCurrencies)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                val filtered = if (query.isEmpty()) allCurrencies
                else allCurrencies.filter { it.searchText.contains(query) }
                adapter.submitList(filtered)
            }
        })

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class CurrencyListAdapter(
    private val selectedCode: String,
    private val onPick: (CurrencyItem) -> Unit
) : ListAdapter<CurrencyItem, CurrencyListAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_currency, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvLabel = v.findViewById<TextView>(R.id.tvCurrencyLabel)
        private val tvCode  = v.findViewById<TextView>(R.id.tvCurrencyCode)
        private val tvCheck = v.findViewById<TextView>(R.id.tvCurrencyCheck)

        fun bind(item: CurrencyItem) {
            tvLabel.text = "${item.displayLabel}"
            tvCode.text  = item.symbol
            tvCheck.visibility = if (item.code == selectedCode) View.VISIBLE else View.GONE
            itemView.setBackgroundColor(
                if (item.code == selectedCode)
                    itemView.context.getColor(R.color.primary_light)
                else
                    itemView.context.getColor(R.color.white)
            )
            itemView.setOnClickListener { onPick(item) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CurrencyItem>() {
            override fun areItemsTheSame(a: CurrencyItem, b: CurrencyItem) = a.code == b.code
            override fun areContentsTheSame(a: CurrencyItem, b: CurrencyItem) = a == b
        }
    }
}
