package com.example.armansimonyan.projectx;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
	public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
		super.onAdapterChanged(oldAdapter, newAdapter);
		removeAllViews();
	}

	@Override
	public boolean canScrollVertically() {
		return true;
	}

	@Override
	public boolean supportsPredictiveItemAnimations() {
		return true;
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (getChildCount() == 0) {
			firstVisiblePosition = 0;
			firstItemTopOffset = 0;
		}

		int extraSpace = 0;
		if (state.isPreLayout()) {
			for (int i = 0; i < getChildCount(); i++) {
				View view = getChildAt(i);
				RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
				if (layoutParams.isItemRemoved()) {
					extraSpace += view.getMeasuredHeight();
				}
			}
		}

		nextStickyHeaderPosition = -1;
		int tempNextStickyHeaderPosition = currentStickyHeaderPosition + 1;
		while (true) {
			View nextView = findViewByPosition(tempNextStickyHeaderPosition);
			if (nextView == null) {
				nextView = recycler.getViewForPosition(tempNextStickyHeaderPosition);
			}
			if (getItemViewType(nextView) == Adapter.GROUP_TYPE) {
				nextStickyHeaderPosition = tempNextStickyHeaderPosition;
				break;
			}
			tempNextStickyHeaderPosition++;
		}

		if (currentStickyHeaderPosition == nextStickyHeaderPosition - 1 && currentStickyHeaderTopOffset == 0) {
			firstVisiblePosition = currentStickyHeaderPosition;
			firstItemTopOffset = 0;
		}

		lastVisiblePosition = firstVisiblePosition;
		lastItemBottomOffset = firstItemTopOffset;
		while (true) {
			View lastVisibleView = findViewByPosition(lastVisiblePosition);
			if (lastVisibleView == null) {
				lastVisibleView = recycler.getViewForPosition(lastVisiblePosition);
			}
			if (lastVisiblePosition == nextStickyHeaderPosition) {
				nextStickyHeaderTopOffset = lastItemBottomOffset;
			}
			measureChildWithMargins(lastVisibleView, 0, 0);
			int lastVisibleViewHeight = lastVisibleView.getMeasuredHeight();
			lastItemBottomOffset += lastVisibleViewHeight;
			if (lastItemBottomOffset > getHeight() - getPaddingBottom() + extraSpace) {
				break;
			}
			lastVisiblePosition++;
		}

		log();
		fill(recycler, state, DIRECTION_UP);

		if (!state.isPreLayout()) {
			List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
			List<View> disappearingViews = new ArrayList<>();
			for (RecyclerView.ViewHolder viewHolder : scrapList) {
				RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) viewHolder.itemView.getLayoutParams();
				if (!layoutParams.isItemRemoved()) {
					disappearingViews.add(viewHolder.itemView);
				}
			}

			int topOffset = lastItemBottomOffset;
			for (View view : disappearingViews) {
				addDisappearingView(view);

				measureChildWithMargins(view, 0, 0);
				layoutDecoratedWithMargins(view, 0, topOffset, view.getMeasuredWidth(), topOffset + view.getMeasuredHeight());
				topOffset += view.getMeasuredHeight();
			}
		}
	}

	@Override
	public void onLayoutCompleted(RecyclerView.State state) {
		View view = findViewByPosition(currentStickyHeaderPosition);
		detachView(view);
		attachView(view);
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
					if (getItemViewType(nextView) == Adapter.GROUP_TYPE) {
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
					if (getItemViewType(previousView) == Adapter.GROUP_TYPE) {
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
						removeAndRecycleView(view, recycler);
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
				removeAndRecycleView(view, recycler);
			}
		}

		fill(recycler, state, direction);
		log();
		return scroll;
	}

	private void fill(RecyclerView.Recycler recycler, RecyclerView.State state, int direction) {
		log("fill: start");

		detachAndScrapAttachedViews(recycler);

		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(i);
			log("child at: " + i + ", layoutPosition: " + ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition() + ", adapterPosition: " + ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewAdapterPosition());
		}

		for (int i = currentStickyHeaderPosition + 1; i < firstVisiblePosition; i++) {
			recycler.recycleView(recycler.getViewForPosition(i));
		}

		if (direction == DIRECTION_UP) {
			int bottom = lastItemBottomOffset;
			int nextPosition = lastVisiblePosition;
			while (true) {
				View view = recycler.getViewForPosition(nextPosition);
				log("View: position: " + nextPosition + ", text: " + ((TextView) ((ViewGroup) view).getChildAt(0)).getText());
				if (getItemViewType(view) == Adapter.GROUP_TYPE) {
					view.setBackgroundColor(Color.GREEN);
				} else {
					view.setBackgroundColor(Color.YELLOW);
				}
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
				if (getItemViewType(view) == Adapter.GROUP_TYPE) {
					view.setBackgroundColor(Color.GREEN);
				} else {
					view.setBackgroundColor(Color.YELLOW);
				}
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

		View view = findViewByPosition(currentStickyHeaderPosition);
		if (view != null) {
			detachView(view);
			attachView(view);
		} else {
			view = recycler.getViewForPosition(currentStickyHeaderPosition);
			addView(view);
		}
		log("Sticky Header View: position: " + currentStickyHeaderPosition + ", text: " + ((TextView) ((ViewGroup) view).getChildAt(0)).getText());
		view.setBackgroundColor(Color.RED);
		measureChildWithMargins(view, 0, 0);
		layoutDecoratedWithMargins(view, 0, currentStickyHeaderTopOffset, view.getMeasuredWidth(), currentStickyHeaderTopOffset + view.getMeasuredHeight());

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
