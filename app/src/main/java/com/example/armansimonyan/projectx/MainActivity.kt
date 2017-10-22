package com.example.armansimonyan.projectx

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem

import java.util.ArrayList

class MainActivity : AppCompatActivity() {
	private lateinit var adapter: Adapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val data = ArrayList<GroupItem>()
		(0..9).map { group ->
			val items = (0..6).map { child -> ChildItem("Child $child of Group $group") }
			val groupItem = GroupItem("Group $group", items)
			groupItem.isExpanded = true
			data.add(groupItem)
		}

		val recyclerView = findViewById<RecyclerView>(R.id.recycler)
		recyclerView.layoutManager = ExtendedLayoutManager()
		val itemAnimator = DefaultItemAnimator()
		itemAnimator.removeDuration = 1000
		itemAnimator.addDuration = 1000
		itemAnimator.moveDuration = 1000
		recyclerView.itemAnimator = itemAnimator
		adapter = Adapter(this, data)
		recyclerView.adapter = adapter
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		adapter.toggle()
		return true
	}
}
