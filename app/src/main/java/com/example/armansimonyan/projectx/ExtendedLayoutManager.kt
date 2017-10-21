package com.example.armansimonyan.projectx

import android.support.v7.widget.RecyclerView

/**
 * Created by armansimonyan
 */
class ExtendedLayoutManager : RecyclerView.LayoutManager() {

	class LayoutParams(width: Int, height: Int) : RecyclerView.LayoutParams(width, height)

	override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
		return LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
	}

	override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
		detachAndScrapAttachedViews(recycler)

		var position = 0;
		var offset = 0

		while (offset < height) {
			val view = recycler.getViewForPosition(position)
			addView(view)
			measureChildWithMargins(view, 0, 0)
			layoutDecoratedWithMargins(view, 0, offset, view.measuredWidth, offset + view.measuredHeight)

			position++
			offset += view.measuredHeight
		}

		log("Drawn $position items")
	}

	private fun log(message: String, vararg args: Any) {
		Logger.log(message, args)
	}
}