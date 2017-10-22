package com.example.armansimonyan.projectx

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * Created by armansimonyan
 */
class ExtendedLayoutManager : RecyclerView.LayoutManager() {

	class LayoutParams : RecyclerView.LayoutParams {
		constructor(width: Int, height: Int) : super(width, height)

		constructor(lp: LayoutParams) : super(lp)

		constructor(lp: ViewGroup.MarginLayoutParams) : super(lp)

		constructor(lp: ViewGroup.LayoutParams?) : super(lp)

		constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
	}

	override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
		return LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
	}

	override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): RecyclerView.LayoutParams {
		return when (lp) {
			is LayoutParams -> LayoutParams(lp)
			is ViewGroup.MarginLayoutParams -> LayoutParams(lp)
			else -> LayoutParams(lp)
		}
	}

	override fun generateLayoutParams(c: Context?, attrs: AttributeSet?): RecyclerView.LayoutParams {
		return LayoutParams(c, attrs)
	}

	override fun supportsPredictiveItemAnimations(): Boolean {
		return true
	}

	override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
		detachAndScrapAttachedViews(recycler)

		var extraSpace = 0
		if (state.isPreLayout) {
			recycler.scrapList
					.filter { (it.itemView.layoutParams as LayoutParams).isItemRemoved }
					.map {
						extraSpace += it.itemView.measuredHeight
					}
		}

		var position = 0;
		var offset = 0

		while (offset < height) {
			val view = recycler.getViewForPosition(position)
			addView(view)
			layoutView(view, offset)

			position++
			offset += view.measuredHeight
		}

		if (state.isPreLayout) {
			while (offset < height + extraSpace) {
				val view = recycler.getViewForPosition(position)
				addView(view)
				layoutView(view, offset)

				position++
				offset += view.measuredHeight
			}
		} else {
			var disappearingViewOffset = offset
			recycler.scrapList
					.filter { !(it.itemView.layoutParams as LayoutParams).isItemRemoved }
					.reversed()
					.map {
						val view = it.itemView
						addDisappearingView(view)
						layoutView(view, disappearingViewOffset)

						disappearingViewOffset += view.measuredHeight
					}
		}

		log("Drawn $position items")
	}

	private fun layoutView(view: View, offset: Int) {
		measureChildWithMargins(view, 0, 0)
		layoutDecoratedWithMargins(view, 0, offset, view.measuredWidth, offset + view.measuredHeight)
	}

	private fun log(message: String, vararg args: Any) {
		Logger.log(message, args)
	}
}