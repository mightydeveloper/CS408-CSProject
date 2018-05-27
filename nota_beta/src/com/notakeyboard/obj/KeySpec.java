package com.notakeyboard.obj;

public class KeySpec {
	private int keycode;
	private int x;
	private int y;
	private int h;
	private int w;
	
	public KeySpec(int keycode, int x, int y, int h, int w) {
		this.keycode = keycode;
		this.x  = x;
		this.y  = y;
		this.h  = h;
		this.w  = w;
	}
	
	public int getKeycode() { return keycode; }
	public int getX() { return x; }
	public int getY() { return y; }
	public int getH() { return h; }
	public int getW() { return w; }
	
	public boolean isInside(int x, int y, double ratio) {
		return this.x+(1-ratio)*w <= x
				&& this.x+ratio*w >= x
				&& this.y+(1-ratio)*h <= y
				&& this.y+ratio*h >= y;
	}
	public int[] getCenter(){
		int[] center = {x+w/2,y+h/2};
		return center;
	}
}
