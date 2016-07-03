package com.example.armansimonyan.projectx;

import java.util.List;

/**
 * @author armansimonyan
 */
public class GroupItem {
	private String name;
	private List<ChildItem> items;
	private boolean isExpanded;

	public GroupItem(String name, List<ChildItem> items) {
		this.name = name;
		this.items = items;
	}

	public String getName() {
		return name;
	}

	public List<ChildItem> getItems() {
		return this.items;
	}

	public boolean isExpanded() {
		return isExpanded;
	}

	public void setIsExpanded(boolean isExpanded) {
		this.isExpanded = isExpanded;
	}
}
