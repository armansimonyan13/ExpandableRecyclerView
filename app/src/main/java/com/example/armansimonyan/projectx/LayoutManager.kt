package com.example.armansimonyan.projectx

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import java.util.ArrayList

/**
 * @author armansimonyan
 */
class LayoutManager : RecyclerView.LayoutManager() {

	private var firstItemTopOffset: Int = 0
	private var lastItemBottomOffset: Int = 0
	private var firstVisiblePosition: Int = 0
	private var lastVisiblePosition: Int = 0

	private var currentStickyHeaderPosition: Int = 0
	private var currentStickyHeaderTopOffset: Int = 0
	private var nextStickyHeaderPosition: Int = 0
	private var nextStickyHeaderTopOffset: Int = 0

	override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
		return RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
	}

	override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
		super.onAdapterChanged(oldAdapter, newAdapter)
		removeAllViews()
	}

	override fun canScrollVertically(): Boolean {
		return true
	}

	override fun supportsPredictiveItemAnimations(): Boolean {
		return true
	}

	override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
		if (recycler == null || state == null) {
			return
		}
		_onLayoutChildren(recycler, state)
	}

	private fun _onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
		if (state.isPreLayout) {
			handleFirstPass(recycler, state)
		} else {
			handleSecondPass(recycler, state)
		}
	}

	private fun handleFirstPass(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
		nextStickyHeaderPosition = calculateNextStickyHeaderPosition(recycler, this, currentStickyHeaderPosition)

		if (currentStickyHeaderPosition == nextStickyHeaderPosition - 1 && currentStickyHeaderTopOffset == 0) {
			firstVisiblePosition = currentStickyHeaderPosition
			firstItemTopOffset = 0
		}

		var extraSpace = 0
		for (i in 0 until childCount) {
			val view = getChildAt(i)
			val layoutParams = view.layoutParams as RecyclerView.LayoutParams
			if (layoutParams.isItemRemoved) {
				extraSpace += view.measuredHeight
			}
		}
		calculateLastVisiblePositionAndLastItemBottomOffset(recycler, extraSpace)

		log()
		fill(recycler, state, DIRECTION_UP)
	}

	private fun handleSecondPass(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
		nextStickyHeaderPosition = calculateNextStickyHeaderPosition(recycler, this, currentStickyHeaderPosition)

		if (currentStickyHeaderPosition == nextStickyHeaderPosition - 1 && currentStickyHeaderTopOffset == 0) {
			firstVisiblePosition = currentStickyHeaderPosition
			firstItemTopOffset = 0
		}

		var extraSpace = 0
		calculateLastVisiblePositionAndLastItemBottomOffset(recycler, extraSpace)

		log()
		fill(recycler, state, DIRECTION_UP)

		val scrapList = recycler.scrapList
		val disappearingViews = ArrayList<View>()
		for (viewHolder in scrapList) {
			val layoutParams = viewHolder.itemView.layoutParams as RecyclerView.LayoutParams
			if (!layoutParams.isItemRemoved) {
				disappearingViews.add(viewHolder.itemView)
			}
		}

		var topOffset = lastItemBottomOffset
		for (view in disappearingViews) {
			addDisappearingView(view)

			measureChildWithMargins(view, 0, 0)
			layoutDecoratedWithMargins(view, 0, topOffset, view.measuredWidth, topOffset + view.measuredHeight)
			topOffset += view.measuredHeight
		}
	}

	private fun calculateLastVisiblePositionAndLastItemBottomOffset(recycler: RecyclerView.Recycler, extraSpace: Int) {
		lastVisiblePosition = firstVisiblePosition
		lastItemBottomOffset = firstItemTopOffset
		while (true) {
			var lastVisibleView: View? = findViewByPosition(lastVisiblePosition)
			if (lastVisibleView == null) {
				lastVisibleView = recycler.getViewForPosition(lastVisiblePosition)
			}
			if (lastVisiblePosition == nextStickyHeaderPosition) {
				nextStickyHeaderTopOffset = lastItemBottomOffset
			}
			measureChildWithMargins(lastVisibleView!!, 0, 0)
			val lastVisibleViewHeight = lastVisibleView.measuredHeight
			lastItemBottomOffset += lastVisibleViewHeight
			if (lastItemBottomOffset > height - paddingBottom + extraSpace) {
				break
			}
			lastVisiblePosition++
		}
	}

	override fun onLayoutCompleted(state: RecyclerView.State?) {
		val view = findViewByPosition(currentStickyHeaderPosition)
		detachView(view)
		attachView(view)
	}

	override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
		if (recycler == null || state == null) {
			return 0
		}
		return _scrollVerticallyBy(dy, recycler, state)
	}

	private fun _scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State) : Int {
		val direction = if (dy > 0) DIRECTION_UP else DIRECTION_DOWN

		var scroll = dy
		if (direction == DIRECTION_UP) {
			lastItemBottomOffset -= dy
			val oldLastItemBottomOffset = lastItemBottomOffset
			while (lastItemBottomOffset < height) {
				if (lastVisiblePosition == itemCount - 1) {
					lastItemBottomOffset = height
					scroll = lastItemBottomOffset - oldLastItemBottomOffset
					break
				} else {
					lastVisiblePosition++
					val view = recycler.getViewForPosition(lastVisiblePosition)
					measureChildWithMargins(view, 0, 0)
					lastItemBottomOffset += view.measuredHeight
				}
			}
		} else {
			firstItemTopOffset -= dy
			val oldFirstItemTopOffset = firstItemTopOffset
			while (firstItemTopOffset > 0) {
				if (firstVisiblePosition == 0) {
					firstItemTopOffset = 0
					scroll = firstItemTopOffset - oldFirstItemTopOffset
					break
				} else {
					firstVisiblePosition--
					val view = recycler.getViewForPosition(firstVisiblePosition)
					measureChildWithMargins(view, 0, 0)
					firstItemTopOffset -= view.measuredHeight
				}
			}
		}

		if (direction == DIRECTION_UP) {
			nextStickyHeaderTopOffset -= scroll
			val view = recycler.getViewForPosition(currentStickyHeaderPosition)
			addView(view)
			measureChildWithMargins(view, 0, 0)
			removeAndRecycleView(view, recycler)
			currentStickyHeaderTopOffset = 0
			if (currentStickyHeaderTopOffset + view.measuredHeight > nextStickyHeaderTopOffset) {
				currentStickyHeaderTopOffset = nextStickyHeaderTopOffset - view.measuredHeight
			}
			if (currentStickyHeaderTopOffset + view.measuredHeight < 0) {
				currentStickyHeaderPosition = nextStickyHeaderPosition
				currentStickyHeaderTopOffset = nextStickyHeaderTopOffset
				var bottom = currentStickyHeaderTopOffset
				for (i in currentStickyHeaderPosition + 1 until itemCount) {
					val nextView = recycler.getViewForPosition(i)
					addView(nextView)
					measureChildWithMargins(nextView, 0, 0)
					bottom += nextView.measuredHeight
					removeAndRecycleView(nextView, recycler)
					if (getItemViewType(nextView) == Adapter.GROUP_TYPE) {
						nextStickyHeaderPosition = i
						nextStickyHeaderTopOffset = bottom
						break
					}
				}
			}
		} else {
			if (currentStickyHeaderPosition > firstVisiblePosition) {
				loop@ for (i in currentStickyHeaderPosition - 1 downTo 0) {
					val previousView = recycler.getViewForPosition(i)
					if (getItemViewType(previousView) == Adapter.GROUP_TYPE) {
						nextStickyHeaderPosition = currentStickyHeaderPosition
						currentStickyHeaderPosition = i
						var bottom = firstItemTopOffset
						for (j in firstVisiblePosition until itemCount) {
							val view = recycler.getViewForPosition(j)
							addView(view)
							measureChildWithMargins(view, 0, 0)
							bottom += view.measuredHeight
							removeAndRecycleView(view, recycler)
							if (j == nextStickyHeaderPosition - 1) {
								nextStickyHeaderTopOffset = bottom
								if (nextStickyHeaderTopOffset > previousView.measuredHeight) {
									currentStickyHeaderTopOffset = 0
								} else {
									currentStickyHeaderTopOffset = nextStickyHeaderTopOffset - previousView.measuredHeight
								}
								break@loop
							}
						}
					}
				}
			} else {
				nextStickyHeaderTopOffset -= scroll
				var limit = 0
				if (firstVisiblePosition == 0 && firstItemTopOffset == 0) {
					for (i in 0 until itemCount) {
						val view = recycler.getViewForPosition(i)
						addView(view)
						measureChildWithMargins(view, 0, 0)
						limit += view.measuredHeight
						removeAndRecycleView(view, recycler)
						if (i == nextStickyHeaderPosition - 1) {
							nextStickyHeaderTopOffset = limit
							break
						}
					}
				}
				val view = recycler.getViewForPosition(currentStickyHeaderPosition)
				addView(view)
				measureChildWithMargins(view, 0, 0)
				if (nextStickyHeaderTopOffset > view.measuredHeight) {
					currentStickyHeaderTopOffset = 0
				} else {
					currentStickyHeaderTopOffset = nextStickyHeaderTopOffset - view.measuredHeight
				}
				removeAndRecycleView(view, recycler)
			}
		}

		fill(recycler, state, direction)
		log()
		return scroll
	}

	private fun fill(recycler: RecyclerView.Recycler, state: RecyclerView.State, direction: Int) {
		log("fill: start")

		detachAndScrapAttachedViews(recycler)

		logAllChildren()

		for (i in currentStickyHeaderPosition + 1 until firstVisiblePosition) {
			recycler.recycleView(recycler.getViewForPosition(i))
		}

		if (direction == DIRECTION_UP) {
			var bottom = lastItemBottomOffset
			var nextPosition = lastVisiblePosition
			while (true) {
				val view = recycler.getViewForPosition(nextPosition)
				log("View: position: " + nextPosition + ", text: " + ((view as ViewGroup).getChildAt(0) as TextView).text)
				if (getItemViewType(view) == Adapter.GROUP_TYPE) {
					view.setBackgroundColor(Color.GREEN)
				} else {
					view.setBackgroundColor(Color.YELLOW)
				}
				addView(view)
				measureChildWithMargins(view, 0, 0)
				val top = bottom - view.getMeasuredHeight()
				layoutDecoratedWithMargins(view, 0, top, view.getMeasuredWidth(), bottom)
				if (top <= 0) {
					firstVisiblePosition = nextPosition
					firstItemTopOffset = top
					break
				}
				bottom = top
				nextPosition--
			}
		} else {
			var top = firstItemTopOffset
			var nextPosition = firstVisiblePosition
			while (true) {
				val view = recycler.getViewForPosition(nextPosition)
				if (getItemViewType(view) == Adapter.GROUP_TYPE) {
					view.setBackgroundColor(Color.GREEN)
				} else {
					view.setBackgroundColor(Color.YELLOW)
				}
				addView(view)
				measureChildWithMargins(view, 0, 0)
				val bottom = top + view.measuredHeight
				layoutDecoratedWithMargins(view, 0, top, view.measuredWidth, bottom)
				if (bottom >= height) {
					lastVisiblePosition = nextPosition
					lastItemBottomOffset = bottom
					break
				}
				top = bottom
				nextPosition++
			}
		}

		var view: View? = findViewByPosition(currentStickyHeaderPosition)
		if (view != null) {
			detachView(view)
			attachView(view)
		} else {
			view = recycler.getViewForPosition(currentStickyHeaderPosition)
			addView(view)
		}
		log("Sticky Header View: position: " + currentStickyHeaderPosition + ", text: " + ((view as ViewGroup).getChildAt(0) as TextView).text)
		view.setBackgroundColor(Color.RED)
		measureChildWithMargins(view, 0, 0)
		layoutDecoratedWithMargins(view, 0, currentStickyHeaderTopOffset, view.measuredWidth, currentStickyHeaderTopOffset + view.measuredHeight)

		log("getChildCount(): " + childCount)
		log("scrapListSize: " + recycler.scrapList.size)
		log("scrapList: " + recycler.scrapList)
	}

	private fun logAllChildren() {
		for (i in 0 until childCount) {
			val view = getChildAt(i)
			log("child at: " + i + ", layoutPosition: " + (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition + ", adapterPosition: " + (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition)
		}
	}

	private fun log(message: String = "State>>>\n" +
			"firstVisiblePosition: " + firstVisiblePosition + "\n" +
			"firstItemTopOffset: " + firstItemTopOffset + "\n" +
			"lastVisiblePosition: " + lastVisiblePosition + "\n" +
			"lastItemBottomOffset: " + lastItemBottomOffset + "\n" +
			"currentStickyHeaderPosition: " + currentStickyHeaderPosition + "\n" +
			"currentStickyHeaderTopOffset: " + currentStickyHeaderTopOffset + "\n" +
			"nextStickyHeaderPosition: " + nextStickyHeaderPosition + "\n" +
			"nextStickyHeaderTopOffset: " + nextStickyHeaderTopOffset + "\n") {
		Log.d(LayoutManager::class.java.simpleName + ">>>", message)
	}

	companion object {
		private val DIRECTION_UP = 1
		private val DIRECTION_DOWN = -1
	}
}

fun calculateNextStickyHeaderPosition(recycler: RecyclerView.Recycler, layoutManager: LayoutManager, currentStickyHeaderPosition: Int) : Int {
	var nextStickyHeaderPosition = -1
	var tempNextStickyHeaderPosition = currentStickyHeaderPosition + 1
	while (true) {
		var nextView: View? = layoutManager.findViewByPosition(tempNextStickyHeaderPosition)
		if (nextView == null) {
			nextView = recycler.getViewForPosition(tempNextStickyHeaderPosition)
		}
		if (layoutManager.getItemViewType(nextView) == Adapter.GROUP_TYPE) {
			nextStickyHeaderPosition = tempNextStickyHeaderPosition
			break
		}
		tempNextStickyHeaderPosition++
	}
	return nextStickyHeaderPosition
}