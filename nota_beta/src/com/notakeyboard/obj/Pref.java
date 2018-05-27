package com.notakeyboard.obj;

public class Pref {
	String type;
	String key;
	String title, summary;
	boolean bool;
	
	String category;
	
	public Pref(String type, String key, String title, String summary) {
		this.type = type;
		this.key = key;
		this.title = title.replaceAll("@", "");
		this.summary = summary.replaceAll("@", "");
	}
	
	public Pref(String category) {
		this.category = category.replaceAll("@", "");
	}
	
	public void setBool(boolean bool) {
		this.bool = bool;
	}
	
	public String getType() { return type; }
	public String getKey() { return key; }
	public int getTitle() { return Integer.parseInt(title); }
	public int getSummary() { return Integer.parseInt(summary); }
	public boolean getBool() { return bool; }
	
	public int getCategory() { return Integer.parseInt(category); }
}
