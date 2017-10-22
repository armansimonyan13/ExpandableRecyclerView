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

	private val UP = 1
	private val DOWN = -1

	class LayoutState {
		/**
		 * Adapter position of top visible item view
		 */
		var topPosition = 0

		/**
		 * Adapter position of bottom visible item view
		 */
		var bottomPosition = 0

		/**
		 * Top offset of top visible item view
		 */
		var topOffset = 0

		/**
		 * Bottom offset of bottom visible item view
		 */
		var bottomOffset = 0
	}

	private var layoutState = LayoutState()

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

		val extraSpace = calculateExtraSpace(state, recycler)

		var position = layoutState.topPosition
		var offset = layoutState.topOffset

		while (offset < height) {
			val view = recycler.getViewForPosition(position)
			addView(view)
			layoutView(view, offset)

			position++
			offset += view.measuredHeight
		}

		layoutState.bottomPosition = position - 1
		layoutState.bottomOffset = offset

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
		detachAndScrapAttachedViews(recycler)

		val direction = if (dy > 0) UP else DOWN

		var distance = Math.abs(dy)

		if (direction == UP) {
			// Calculate layoutState.bottomOffset and layoutState.bottomPosition
			var bottomOffset = layoutState.bottomOffset - distance
			var bottomPosition = layoutState.bottomPosition
			while (true) {
				val view = recycler.getViewForPosition(bottomPosition)

				measureChildWithMargins(view, 0, 0)

				val decoratedMeasuredHeight = getDecoratedMeasuredHeight(view)
				val top = bottomOffset - decoratedMeasuredHeight
				val bottom = bottomOffset
				if (top < height && bottom >= height) {
					layoutState.bottomOffset = bottom
					layoutState.bottomPosition = bottomPosition
					break
				} else {
					bottomPosition++
					if (bottomPosition >= itemCount) {
						distance = height - layoutState.bottomOffset
						layoutState.bottomPosition = itemCount - 1
						layoutState.bottomOffset = height
						break
					}
					val nextView = recycler.getViewForPosition(bottomPosition)
					measureChildWithMargins(nextView, 0, 0)
					bottomOffset += getDecoratedMeasuredHeight(nextView)
				}
			}

			// Calculate layoutState.topOffset and layoutState.topPosition
			bottomOffset = layoutState.bottomOffset
			bottomPosition = layoutState.bottomPosition
			while (true) {
				val view = recycler.getViewForPosition(bottomPosition)

				measureChildWithMargins(view, 0, 0)

				val decoratedMeasuredHeight = getDecoratedMeasuredHeight(view)
				val top = bottomOffset - decoratedMeasuredHeight
				val bottom = bottomOffset
				if (top <= 0 && bottom > 0) {
					layoutState.topOffset = top
					layoutState.topPosition = bottomPosition
					break
				} else {
					bottomOffset -= decoratedMeasuredHeight
					bottomPosition--
				}
			}
		} else {
			// Calculate layoutState.topOffset and layoutState.topPosition
			var topOffset = layoutState.topOffset + distance
			var topPosition = layoutState.topPosition
			while (true) {
				val view = recycler.getViewForPosition(topPosition)

				measureChildWithMargins(view, 0, 0)

				val top = topOffset
				val bottom = top + getDecoratedMeasuredHeight(view)
				if (top <= 0 && bottom > 0) {
					layoutState.topOffset = topOffset
					layoutState.topPosition = topPosition
					break
				} else {
					topPosition--
					if (topPosition < 0) {
						distance = -layoutState.topOffset
						layoutState.topOffset = 0
						layoutState.topPosition = 0
						break
					}
					val previousView = recycler.getViewForPosition(topPosition)

					measureChildWithMargins(previousView, 0 ,0)

					topOffset -= getDecoratedMeasuredHeight(previousView)
				}
			}

			// Calculate layoutState.bottomOffset and layoutState.bottomPosition
			topOffset = layoutState.topOffset
			topPosition = layoutState.topPosition
			while (true) {
				val view = recycler.getViewForPosition(topPosition)

				measureChildWithMargins(view, 0, 0)

				val top = topOffset
				val bottom = top + getDecoratedMeasuredHeight(view)
				if (top < height && bottom >= height) {
					layoutState.bottomOffset = topOffset
					layoutState.bottomPosition = topPosition
					break
				} else {
					topPosition++
					topOffset = bottom
				}
			}
		}

		var position = layoutState.topPosition
		var offset = layoutState.topOffset
		while (offset < height && position < itemCount) {
			val view = recycler.getViewForPosition(position)
			addView(view)
			layoutView(view, offset)

			position++
			offset += getDecoratedMeasuredHeight(view)
		}

		log("topPosition: ${layoutState.topPosition}")
		log("bottomPosition: ${layoutState.bottomPosition}")
		log("topOffset: ${layoutState.topOffset}")
		log("bottomOffset: ${layoutState.bottomOffset}")

		return distance * direction
	}

	private fun layoutView(view: View, offset: Int) {
		measureChildWithMargins(view, 0, 0)
		layoutDecoratedWithMargins(view, 0, offset, view.measuredWidth, offset + view.measuredHeight)
	}

	private fun log(message: String, vararg args: Any) {
//		Logger.log(message, args)
	}
}