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

	private var mOffset = 0

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

		var extraSpace = calculateExtraSpace(state, recycler)

		var position = 0;
		var offset = mOffset

		while (offset < height) {
			val view = recycler.getViewForPosition(position)
			addView(view)
			layoutView(view, offset)

			position++
			offset += view.measuredHeight
		}

		if (state.isPreLayout) {
			position = layoutAppearingViews(offset, extraSpace, recycler, position)
		} else {
			layoutDisappearingViews(offset, recycler)
		}

		log("Drawn $position items")
	}

	private fun calculateExtraSpace(state: RecyclerView.State, recycler: RecyclerView.Recycler): Int {
		var extraSpace = 0
		if (state.isPreLayout) {
			recycler.scrapList
					.filter { (it.itemView.layoutParams as LayoutParams).isItemRemoved }
					.map {
						extraSpace += it.itemView.measuredHeight
					}
		}
		return extraSpace
	}

	private fun layoutDisappearingViews(offset: Int, recycler: RecyclerView.Recycler) {
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

	private fun layoutAppearingViews(offset: Int, extraSpace: Int, recycler: RecyclerView.Recycler, position: Int): Int {
		var offset1 = offset
		var position1 = position
		while (offset1 < height + extraSpace) {
			val view = recycler.getViewForPosition(position1)
			addView(view)
			layoutView(view, offset1)

			position1++
			offset1 += view.measuredHeight
		}
		return position1
	}

	override fun canScrollVertically(): Boolean {
		return true
	}

	override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
		mOffset -= dy

		detachAndScrapAttachedViews(recycler)

		var position = 0
		var offset = mOffset

		while (offset < height && position < itemCount) {
			val view = recycler.getViewForPosition(position)
			addView(view)
			layoutView(view, offset)

			position++
			offset += view.measuredHeight
		}

		return dy
	}

	private fun layoutView(view: View, offset: Int) {
		measureChildWithMargins(view, 0, 0)
		layoutDecoratedWithMargins(view, 0, offset, view.measuredWidth, offset + view.measuredHeight)
	}

	private fun log(message: String, vararg args: Any) {
		Logger.log(message, args)
	}
}