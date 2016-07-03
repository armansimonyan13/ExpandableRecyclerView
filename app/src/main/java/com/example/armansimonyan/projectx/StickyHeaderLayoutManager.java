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
		while (true) {
			View view = recycler.getViewForPosition(lastVisiblePosition);
			measureChildWithMargins(view, 0, 0);
			int childHeight = view.getMeasuredHeight();
			lastItemBottomOffset += childHeight;
			if (lastItemBottomOffset > getHeight() - getPaddingBottom()) {
				break;
			}
			lastVisiblePosition++;
		}

		fill(recycler, state, 1);
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
		log("getChildCount(): " + getChildCount());
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

		fill(recycler, state, direction);
		log();
		return scroll;
	}

	private void log(String message) {
		Log.d(StickyHeaderLayoutManager.class.getSimpleName() + ">>>", message);
	}

	private void log() {
		log("State>>>\n" +
				"firstItemTopOffset: " + firstItemTopOffset + "\n" +
				"lastItemBottomOffset: " + lastItemBottomOffset + "\n" +
				"firstVisiblePosition: " + firstVisiblePosition + "\n" +
				"lastVisiblePosition: " + lastVisiblePosition + "\n" +
				"childCount: " + getChildCount()
		);
	}
}
