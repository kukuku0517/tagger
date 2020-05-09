package com.project.tagger.util.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.project.tagger.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.dialog_point_exchange_coupons.view.*
import kotlinx.android.synthetic.main.item_selector.view.*

class SelectorBottomSheetDialogBuilder(
        val context: Context,
        val layoutInflater: LayoutInflater){

    var _title:String = ""
    var _items = listOf<SelectorItem>()

    var _onItemClickListener:SelectorAdapter.OnItemClickListener<SelectorItem>? = null

    fun setTitle(title:String): SelectorBottomSheetDialogBuilder {
        this._title = title
        return this
    }
    fun setOnItemClickListener(onItemClickListener: SelectorAdapter.OnItemClickListener<SelectorItem>): SelectorBottomSheetDialogBuilder {
        this._onItemClickListener = onItemClickListener
        return this
    }
    fun setItems(list:List<SelectorItem>): SelectorBottomSheetDialogBuilder {
        this._items = list
        return this
    }

    fun build(): BottomSheetDialog {
        val habitBottomSheetDialog = BottomSheetDialog(context)
        val view = layoutInflater.inflate(R.layout.dialog_point_exchange_coupons, null)
        habitBottomSheetDialog.setContentView(view)

        val selectorAdapter = SelectorAdapter<SelectorItem>(context, _onItemClickListener, habitBottomSheetDialog)
        view.mTvSelectorTitle.text = _title
        view.mRvSelector.adapter = selectorAdapter
        view.mRvSelector.layoutManager = LinearLayoutManager(context)
        selectorAdapter.items = this._items
        return habitBottomSheetDialog
    }

}
class SelectorAdapter<T : SelectorItem>(
        val context: Context,
        val listener: OnItemClickListener<SelectorItem>?,
        val habitBottomSheetDialog: BottomSheetDialog
) : RecyclerView.Adapter<SelectorAdapter.SelectorViewHolder<T>>() {

    var items = listOf<T>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): SelectorViewHolder<T> {
        val view = LayoutInflater.from(context).inflate(R.layout.item_selector, parent, false)

        val holder = SelectorViewHolder<T>(view)
        holder.containerView.setOnClickListener {
            listener?.onClick(habitBottomSheetDialog, items[holder.adapterPosition], holder.adapterPosition)
        }
        return holder
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(
            holder: SelectorViewHolder<T>,
            position: Int
    ) {
        holder.bindView(items[position])
    }

    interface OnItemClickListener<T> {
        fun onClick(
                dialog: BottomSheetDialog,
                item: T,
                position: Int
        )
    }

    class SelectorViewHolder<T : SelectorItem>(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bindView(item: T) {
            containerView.mTvSelectorContent.text = item.content
        }
    }
}

data class SelectorItem(
        val content: String
)