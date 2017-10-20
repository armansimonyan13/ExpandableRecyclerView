package com.example.armansimonyan.projectx

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView

import java.util.ArrayList

class MainActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val data = ArrayList<GroupItem>()
		for (i in 0..99) {
			val items = ArrayList<ChildItem>()
			for (j in 0..6) {
				val childItem = ChildItem("Child $j of Group $i")
				items.add(childItem)
			}
			val groupItem = GroupItem("Group " + i, items)
			groupItem.isExpanded = true
			data.add(groupItem)
		}

		val recyclerView = findViewById<RecyclerView>(R.id.recycler)
		recyclerView.layoutManager = LayoutManager()
		val itemAnimator = DefaultItemAnimator()
		itemAnimator.removeDuration = 1000
		itemAnimator.addDuration = 1000
		itemAnimator.moveDuration = 1000
		recyclerView.itemAnimator = itemAnimator
		recyclerView.adapter = Adapter(this, data)
	}

}