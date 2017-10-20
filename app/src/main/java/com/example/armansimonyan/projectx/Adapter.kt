package com.example.armansimonyan.projectx

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Created by armansimonyan.
 */

class Adapter(context: Context, val data: List<GroupItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), View.OnClickListener {

	companion object {
		@JvmField
		val GROUP_TYPE: Int = 0
		@JvmField
		val CHILD_TYPE: Int = 1
	}

	private var inflater: LayoutInflater = LayoutInflater.from(context)

	override fun getItemCount(): Int {
		var count = 0
		for (groupItem in data) {
			count++
			if (groupItem.isExpanded) {
				count += groupItem.items.size
			}
		}
		return count
	}

	override fun getItemViewType(position: Int): Int = when (getItem(position)) {
		is GroupItem -> GROUP_TYPE
		is ChildItem -> CHILD_TYPE
		else -> throw IllegalStateException("Unknown type")
	}

	private fun getItem(position: Int) : Any {
		var i = 0
		for (groupItem in data) {
			if (i == position) {
				return groupItem
			}
			i++
			if (groupItem.isExpanded) {
				for (childItem in groupItem.items) {
					if (i == position) {
						return childItem
					}
					i++
				}
			}
		}
		throw IllegalStateException("Item not found for position $position")
	}

	override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			GROUP_TYPE -> GroupViewHolder(inflater.inflate(R.layout.view_item_group, parent, false))
			CHILD_TYPE -> ChildViewHolder(inflater.inflate(R.layout.view_item_child, parent, false))
			else -> throw IllegalStateException("Unknown view type $viewType")
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) =
			if (holder is GroupViewHolder) {
				holder.imageView.setOnClickListener(this)
				holder.imageView.tag = holder
				val groupItem = getItem(position) as GroupItem
				holder.textView.text = groupItem.name
				if (groupItem.isExpanded) {
					holder.imageView.rotation = 90F
				} else {
					holder.imageView.rotation = 0F
				}
			} else if (holder is ChildViewHolder) {
				holder.itemView.setOnClickListener(this)
				holder.itemView.tag = holder
				holder.textView.text = (getItem(position) as ChildItem).name
			} else {
				throw IllegalArgumentException("Holder is of unrecognized type: ${holder!!::class.java}")
			}

	override fun onClick(v: View?) {
		if (v == null) return
		val holder: Any = v.tag
		if (holder is GroupViewHolder) {
			log("adapterPosition: ${holder.adapterPosition}")
			log("layoutPosition: ${holder.layoutPosition}")
			val adapterPosition = holder.adapterPosition
			val item: Any = getItem(adapterPosition)
			if (item is GroupItem) {
				if (item.isExpanded) {
					item.isExpanded = false
					holder.imageView.rotation = 0F
					notifyItemRangeRemoved(adapterPosition + 1, item.items.size)
				} else {
					item.isExpanded = true
					holder.imageView.rotation = 90F
					notifyItemRangeInserted(adapterPosition + 1, item.items.size)
				}
			}
		}
	}

	class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var textView : TextView = itemView.findViewById(R.id.text)
		var imageView : ImageView = itemView.findViewById(R.id.arrow)
	}

	class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var textView : TextView = itemView.findViewById(R.id.text)
	}

	@Suppress("unused")
	fun log(message: String, vararg args: Any) {
		Log.d("Adapter", String.format(message, args))
	}
}