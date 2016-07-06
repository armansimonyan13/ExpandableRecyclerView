package com.example.armansimonyan.projectx;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		List<GroupItem> data = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			List<ChildItem> items = new ArrayList<>();
			for (int j = 0; j < 7; j++) {
				ChildItem childItem = new ChildItem("Child " + j + " of Group " + i);
				items.add(childItem);
			}
			GroupItem groupItem = new GroupItem("Group " + i, items);
			groupItem.setIsExpanded(true);
			data.add(groupItem);
		}

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
		recyclerView.setLayoutManager(new StickyHeaderLayoutManager());
		DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
		itemAnimator.setRemoveDuration(1000);
		itemAnimator.setAddDuration(1000);
		itemAnimator.setMoveDuration(1000);
		recyclerView.setItemAnimator(itemAnimator);
		recyclerView.setAdapter(new Adapter(this, data));
	}

	public static class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
		public static final int GROUP_TYPE = 0;
		public static final int CHILD_TYPE = 1;

		private LayoutInflater inflater;
		private List<GroupItem> data;

		public Adapter(Context context, List<GroupItem> data) {
			inflater = LayoutInflater.from(context);
			this.data = data;
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			if (viewType == GROUP_TYPE) {
				return new GroupViewHolder(inflater.inflate(R.layout.view_item_group, parent, false));
			} else if (viewType == CHILD_TYPE) {
				return new ChildViewHolder(inflater.inflate(R.layout.view_item_child, parent, false));
			} else {
				throw new IllegalArgumentException("Unknown view type: " + viewType);
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			if (holder instanceof GroupViewHolder) {
				GroupViewHolder groupViewHolder = (GroupViewHolder) holder;
				groupViewHolder.itemView.setOnClickListener(this);
				groupViewHolder.itemView.setTag(groupViewHolder);
				GroupItem groupItem = (GroupItem) getItem(position);
				groupViewHolder.textView.setText(groupItem.getName());
				if (groupItem.isExpanded()) {
					groupViewHolder.imageView.setRotation(90);
				} else {
					groupViewHolder.imageView.setRotation(0);
				}
			} else if (holder instanceof ChildViewHolder) {
				ChildViewHolder childViewHolder = (ChildViewHolder) holder;
				childViewHolder.itemView.setOnClickListener(this);
				childViewHolder.itemView.setTag(childViewHolder);
				childViewHolder.textView.setText(((ChildItem) getItem(position)).getName());
			} else {
				throw new IllegalArgumentException("Holder is of unrecognized type: " + holder.getClass());
			}
		}

		@Override
		public int getItemCount() {
			int count = 0;
			for (GroupItem groupItem : data) {
				count++;
				if (groupItem.isExpanded()) {
					count += groupItem.getItems().size();
				}
			}
			return count;
		}

		@Override
		public int getItemViewType(int position) {
			Object item = getItem(position);
			if (item instanceof GroupItem) {
				return GROUP_TYPE;
			} else if (item instanceof ChildItem) {
				return CHILD_TYPE;
			} else {
				throw new IllegalStateException("Unknown type");
			}
		}

		@Override
		public void onClick(View view) {
			Object holder = view.getTag();
			if (holder instanceof GroupViewHolder) {
				GroupViewHolder groupViewHolder = (GroupViewHolder) holder;
				log("adapterPosition: " + groupViewHolder.getAdapterPosition());
				log("layoutPosition: " + groupViewHolder.getLayoutPosition());
				int adapterPosition = groupViewHolder.getAdapterPosition();
				Object item = getItem(adapterPosition);
				if (item instanceof GroupItem) {
					GroupItem groupItem = (GroupItem) item;
					if (groupItem.isExpanded()) {
						groupItem.setIsExpanded(false);
						groupViewHolder.imageView.setRotation(0);
						notifyItemRangeRemoved(adapterPosition + 1, groupItem.getItems().size());
					} else {
						groupItem.setIsExpanded(true);
						groupViewHolder.imageView.setRotation(90);
						notifyItemRangeInserted(adapterPosition + 1, groupItem.getItems().size());
					}
				}
			}
		}

		public Object getItem(int position) {
			int i = 0;
			for (GroupItem groupItem : data) {
				if (i == position) {
					return groupItem;
				}
				i++;
				if (groupItem.isExpanded()) {
					for (ChildItem childItem : groupItem.getItems()) {
						if (i == position) {
							return childItem;
						}
						i++;
					}
				}
			}
			return null;
		}
	}

	public static class GroupViewHolder extends RecyclerView.ViewHolder {
		public TextView textView;
		public ImageView imageView;

		public GroupViewHolder(View itemView) {
			super(itemView);

			textView = (TextView) itemView.findViewById(R.id.text);
			imageView = (ImageView) itemView.findViewById(R.id.arrow);
		}
	}

	public static class ChildViewHolder extends RecyclerView.ViewHolder {
		public TextView textView;

		public ChildViewHolder(View itemView) {
			super(itemView);

			textView = (TextView) itemView.findViewById(R.id.text);
		}
	}

	private static void log(String message) {
		Log.d("MainActivity>>>: ", message);
	}

	private static void log() {
		Log.d("MainActivity>>>: \n",
				""
		);
	}
}
