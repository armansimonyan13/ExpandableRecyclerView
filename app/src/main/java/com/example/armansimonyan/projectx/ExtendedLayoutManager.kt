package com.example.armansimonyan.projectx

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.SparseArray
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

		val viewCache = SparseArray<RecyclerView.ViewHolder>(childCount)
		if (state.isPreLayout) {
			recycler.scrapList.map { viewHolder ->
				viewCache.put(viewHolder.oldPosition, viewHolder)
			}
		}

		val extraSpace = calculateExtraSpace(state, recycler)

		var position = layoutState.topPosition
		var offset = layoutState.topOffset

		while (offset < height) {
			var view = viewCache.get(position)?.itemView
			if (view == null) {
				view = recycler.getViewForPosition(position)
				addView(view)
				layoutView(view, offset)
			} else {
				addView(view)
				viewCache.remove(position)
			}

			position++
			offset += getDecoratedMeasuredHeight(view)
		}

		if (!state.isPreLayout) {
			layoutState.bottomPosition = position - 1
			layoutState.bottomOffset = offset
		}

		if (state.isPreLayout) {
//			position = layoutAppearingViews(offset, extraSpace, recycler, position)
		} else {
			layoutDisappearingViews(layoutState.topOffset, recycler)
		}

		(0 until viewCache.size()).map {
			recycler.recycleView(viewCache.valueAt(it).itemView)
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

	private fun layoutDisappearingViews(_offset: Int, recycler: RecyclerView.Recycler) {
		val list = recycler.scrapList
				.filter { !(it.itemView.layoutParams as LayoutParams).isItemRemoved }
				.sortedBy { it.layoutPosition }

		if (list.isEmpty()) {
			return
		}

		if (list[0].layoutPosition == layoutState.bottomPosition + 1) {
			var topOffset = layoutState.bottomOffset
			list.map {
				val view = it.itemView
				addDisappearingView(view)
				layoutView(view, topOffset)
				topOffset += getDecoratedMeasuredHeight(view)
			}
		} else if (list[list.lastIndex].layoutPosition == layoutState.topPosition - 1) {
			var bottomOffset = layoutState.topOffset
			list.reversed()
					.map {
						val view = it.itemView
						addDisappearingView(view)
						val decoratedHeight = getDecoratedMeasuredHeight(view)
						layoutView(view, bottomOffset - decoratedHeight)
						bottomOffset -= decoratedHeight
					}
		}
	}

	private fun layoutAppearingViews(offset: Int, extraSpace: Int, recycler: RecyclerView.Recycler, position: Int): Int {
		var nextOffset = offset
		var nextPosition = position
		while (nextOffset < height + extraSpace) {
			val view = recycler.getViewForPosition(nextPosition)
			addView(view)
			layoutView(view, nextOffset)

			nextPosition++
			nextOffset += view.measuredHeight
		}
		return nextPosition
	}

	override fun canScrollVertically(): Boolean {
		return true
	}

	override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
		var childList = mutableListOf<View>()
		(0 until childCount).map {
			childList.add(getChildAt(it))
		}

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
			childList.remove(view)
			addView(view)
			layoutView(view, offset)

			position++
			offset += getDecoratedMeasuredHeight(view)
		}

		childList.map {
			removeAndRecycleView(it, recycler)
		}

		log("topPosition: ${layoutState.topPosition}")
		log("bottomPosition: ${layoutState.bottomPosition}")
		log("topOffset: ${layoutState.topOffset}")
		log("bottomOffset: ${layoutState.bottomOffset}")

		return distance * direction
	}

	private fun layoutView(view: View, offset: Int) {
		measureChildWithMargins(view, 0, 0)
		layoutDecoratedWithMargins(view, 0, offset, getDecoratedMeasuredWidth(view), offset + getDecoratedMeasuredHeight(view))
	}

	private fun log(message: String, vararg args: Any) {
		Logger.log(message, args)
	}
}