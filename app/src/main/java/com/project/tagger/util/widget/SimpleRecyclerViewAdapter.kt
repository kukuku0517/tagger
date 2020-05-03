package com.project.tagger.util.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer


class SimpleRecyclerViewAdapter<T>(
    val context: Context,
    val provider: RecyclerProvider<T>
) : RecyclerView.Adapter<SimpleRecyclerViewAdapter<T>.BaseViewHolder>() {

    init {
        provider.adapter = this
    }

    abstract class RecyclerProvider<T> {
        var adapter: SimpleRecyclerViewAdapter<T>? = null

        var items: List<T> = listOf()
            set(value) {
                val oldValue = field
                field = value
                adapter?.let { setItem(it, oldValue, value) }
            }

        abstract fun getLayoutId(): Int
        abstract fun getItemCount(): Int
        abstract fun getItem(position: Int): T
        abstract fun onBindView(containerView: View, item: T)
        abstract fun onClick(adapterPosition: Int)
        abstract fun setItem(
            adapter: SimpleRecyclerViewAdapter<T>,
            oldItems: List<T>,
            newItems: List<T>
        )
    }

    inner class BaseViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun onBindView(item: T) {
            provider.onBindView(containerView, item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val rootView: View = LayoutInflater.from(context)
            .inflate(provider.getLayoutId(), parent, false)
        val holder = BaseViewHolder(rootView)
        holder.containerView.setOnClickListener {
            provider.onClick(holder.adapterPosition)
        }
        return holder
    }

    override fun getItemCount(): Int {
        return provider.getItemCount()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.onBindView(provider.getItem(position))
    }


}

