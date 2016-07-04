package com.example.armansimonyan.projectx;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * @author armansimonyan
 */
public class StickyHeaderLayoutManager extends RecyclerView.LayoutManager {
	private static final int DIRECTION_UP = 1;
	private static final int DIRECTION_DOWN = -1;

	private int firstItemTopOffset;
	private int lastItemBottomOffset;
	private int firstVisiblePosition;
	private int lastVisiblePosition;

	private int currentStickyHeaderPosition;
	private int currentStickyHeaderTopOffset;
	private int nextStickyHeaderPosition;
	private int nextStickyHeaderTopOffset;

	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public boolean canScrollVertically() {
		return true;
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (state.isPreLayout()) {
			return;
		}

		firstVisiblePosition = lastVisiblePosition = 0;
		firstItemTopOffset = lastItemBottomOffset = 0;
		currentStickyHeaderPosition = 0;
		currentStickyHeaderTopOffset = 0;
		nextStickyHeaderPosition = 0;
		while (true) {
			View view = recycler.getViewForPosition(lastVisiblePosition);
			measureChildWithMargins(view, 0, 0);
			int childHeight = view.getMeasuredHeight();
			if (nextStickyHeaderPosition == 0 && lastVisiblePosition != firstVisiblePosition && getItemViewType(view) == MainActivity.Adapter.GROUP_TYPE) {
				nextStickyHeaderPosition = lastVisiblePosition;
				nextStickyHeaderTopOffset = lastItemBottomOffset;
			}
			lastItemBottomOffset += childHeight;
			if (lastItemBottomOffset > getHeight() - getPaddingBottom()) {
				break;
			}
			lastVisiblePosition++;
		}

		log();
		fill(recycler, state, 1);
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		int direction = dy > 0 ? DIRECTION_UP : DIRECTION_DOWN;

		int scroll = dy;
		if (direction == DIRECTION_UP) {
			int oldLastItemBottomOffset = lastItemBottomOffset -= dy;
			while (lastItemBottomOffset < getHeight()) {
				if (lastVisiblePosition == getItemCount() - 1) {
					lastItemBottomOffset = getHeight();
					scroll = lastItemBottomOffset - oldLastItemBottomOffset;
					break;
				} else {
					lastVisiblePosition++;
					View view = recycler.getViewForPosition(lastVisiblePosition);
					measureChildWithMargins(view, 0, 0);
					lastItemBottomOffset += view.getMeasuredHeight();
				}
			}
		} else {
			int oldFirstItemTopOffset = firstItemTopOffset -= dy;
			while (firstItemTopOffset > 0) {
				if (firstVisiblePosition == 0) {
					firstItemTopOffset = 0;
					scroll = firstItemTopOffset - oldFirstItemTopOffset;
					break;
				} else {
					firstVisiblePosition--;
					View view = recycler.getViewForPosition(firstVisiblePosition);
					measureChildWithMargins(view, 0, 0);
					firstItemTopOffset -= view.getMeasuredHeight();
				}
			}
		}

		if (direction == DIRECTION_UP) {
			nextStickyHeaderTopOffset -= scroll;
			View view = recycler.getViewForPosition(currentStickyHeaderPosition);
			addView(view);
			measureChildWithMargins(view, 0, 0);
			removeAndRecycleView(view, recycler);
			currentStickyHeaderTopOffset = 0;
			if (currentStickyHeaderTopOffset + view.getMeasuredHeight() > nextStickyHeaderTopOffset) {
				currentStickyHeaderTopOffset = nextStickyHeaderTopOffset - view.getMeasuredHeight();
			}
			if (currentStickyHeaderTopOffset + view.getMeasuredHeight() < 0) {
				currentStickyHeaderPosition = nextStickyHeaderPosition;
				currentStickyHeaderTopOffset = nextStickyHeaderTopOffset;
				int bottom = currentStickyHeaderTopOffset;
				for (int i = currentStickyHeaderPosition + 1; i < getItemCount(); i++) {
					View nextView = recycler.getViewForPosition(i);
					addView(nextView);
					measureChildWithMargins(nextView, 0, 0);
					bottom += nextView.getMeasuredHeight();
					removeAndRecycleView(nextView, recycler);
					if (getItemViewType(nextView) == MainActivity.Adapter.GROUP_TYPE) {
						nextStickyHeaderPosition = i;
						nextStickyHeaderTopOffset = bottom;
						break;
					}
				}
			}
		} else {
			if (currentStickyHeaderPosition > firstVisiblePosition) {
				loop:
				for (int i = currentStickyHeaderPosition - 1; i >= 0; i--) {
					View previousView = recycler.getViewForPosition(i);
					if (getItemViewType(previousView) == MainActivity.Adapter.GROUP_TYPE) {
						nextStickyHeaderPosition = currentStickyHeaderPosition;
						currentStickyHeaderPosition = i;
						int bottom = firstItemTopOffset;
						for (int j = firstVisiblePosition; j < getItemCount(); j++) {
							View view = recycler.getViewForPosition(j);
							addView(view);
							measureChildWithMargins(view, 0, 0);
							bottom += view.getMeasuredHeight();
							removeAndRecycleView(view, recycler);
							if (j == nextStickyHeaderPosition - 1) {
								nextStickyHeaderTopOffset = bottom;
								if (nextStickyHeaderTopOffset > previousView.getMeasuredHeight()) {
									currentStickyHeaderTopOffset = 0;
								} else {
									currentStickyHeaderTopOffset = nextStickyHeaderTopOffset - previousView.getMeasuredHeight();
								}
								break loop;
							}
						}
					}
				}
			} else {
				nextStickyHeaderTopOffset -= scroll;
				int limit = 0;
				if (firstVisiblePosition == 0 && firstItemTopOffset == 0) {
					for (int i = 0; i < getItemCount(); i++) {
						View view = recycler.getViewForPosition(i);
						addView(view);
						measureChildWithMargins(view, 0, 0);
						limit += view.getMeasuredHeight();
						removeView(view);
						if (i == nextStickyHeaderPosition - 1) {
							nextStickyHeaderTopOffset = limit;
							break;
						}
					}
				}
				View view = recycler.getViewForPosition(currentStickyHeaderPosition);
				addView(view);
				measureChildWithMargins(view, 0, 0);
				if (nextStickyHeaderTopOffset > view.getMeasuredHeight()) {
					currentStickyHeaderTopOffset = 0;
				} else {
					currentStickyHeaderTopOffset = nextStickyHeaderTopOffset - view.getMeasuredHeight();
				}
				removeView(view);
			}
		}

		fill(recycler, state, direction);
		log();
		return scroll;
	}

	private void fill(RecyclerView.Recycler recycler, RecyclerView.State state, int direction) {
		detachAndScrapAttachedViews(recycler);

		if (direction == DIRECTION_UP) {
			int bottom = lastItemBottomOffset;
			int nextPosition = lastVisiblePosition;
			while (true) {
				View view = recycler.getViewForPosition(nextPosition);
				addView(view);
				measureChildWithMargins(view, 0, 0);
				int top = bottom - view.getMeasuredHeight();
				layoutDecoratedWithMargins(view, 0, top, view.getMeasuredWidth(), bottom);
				if (top <= 0) {
					firstVisiblePosition = nextPosition;
					firstItemTopOffset = top;
					break;
				}
				bottom = top;
				nextPosition--;
			}
		} else {
			int top = firstItemTopOffset;
			int nextPosition = firstVisiblePosition;
			while (true) {
				View view = recycler.getViewForPosition(nextPosition);
				addView(view);
				measureChildWithMargins(view, 0, 0);
				int bottom = top + view.getMeasuredHeight();
				layoutDecoratedWithMargins(view, 0, top, view.getMeasuredWidth(), bottom);
				if (bottom >= getHeight()) {
					lastVisiblePosition = nextPosition;
					lastItemBottomOffset = bottom;
					break;
				}
				top = bottom;
				nextPosition++;
			}
		}

		View view = recycler.getViewForPosition(currentStickyHeaderPosition);
		addView(view);
		measureChildWithMargins(view, 0, 0);
		layoutDecoratedWithMargins(view, 0, currentStickyHeaderTopOffset, view.getMeasuredWidth(), currentStickyHeaderTopOffset + view.getMeasuredHeight());
		MainActivity.GroupViewHolder groupViewHolder = (MainActivity.GroupViewHolder) view.getTag();
		log("stickyHeaderArrowRotation: " + groupViewHolder.imageView.getRotation());

		log("getChildCount(): " + getChildCount());
		log("scrapListSize: " + recycler.getScrapList().size());
		log("scrapList: " + recycler.getScrapList());
	}

	private void log(String message) {
		Log.d(StickyHeaderLayoutManager.class.getSimpleName() + ">>>", message);
	}

	private void log() {
		log("State>>>\n" +
				"firstVisiblePosition: " + firstVisiblePosition + "\n" +
				"firstItemTopOffset: " + firstItemTopOffset + "\n" +
				"lastVisiblePosition: " + lastVisiblePosition + "\n" +
				"lastItemBottomOffset: " + lastItemBottomOffset + "\n" +
				"currentStickyHeaderPosition: " + currentStickyHeaderPosition + "\n" +
				"currentStickyHeaderTopOffset: " + currentStickyHeaderTopOffset + "\n" +
				"nextStickyHeaderPosition: " + nextStickyHeaderPosition + "\n" +
				"nextStickyHeaderTopOffset: " + nextStickyHeaderTopOffset + "\n"
		);
	}
}
