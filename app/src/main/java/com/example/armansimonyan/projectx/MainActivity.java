package com.example.armansimonyan.projectx;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

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

		RecyclerView recyclerView = findViewById(R.id.recycler);
		recyclerView.setLayoutManager(new StickyHeaderLayoutManager());
		DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
		itemAnimator.setRemoveDuration(1000);
		itemAnimator.setAddDuration(1000);
		itemAnimator.setMoveDuration(1000);
		recyclerView.setItemAnimator(itemAnimator);
		recyclerView.setAdapter(new Adapter(this, data));
	}

}
