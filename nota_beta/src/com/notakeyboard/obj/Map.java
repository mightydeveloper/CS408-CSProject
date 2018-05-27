package com.notakeyboard.obj;

/**
 * Defines a keyboard map. The numbers are the parameters for this map.
 */
public class Map {
  public int type;
  public long time;
  public int keycode;
  public double w;
  public double mx, my;
  public double c1, c2, c4;

  public Map(int type, long time, int keycode, double w, double mx,
             double my, double c1, double c2, double c4) {
    this.type = type;
    this.time = time;
    this.keycode = keycode;

    this.w = w;
    this.mx = mx;
    this.my = my;

    this.c1 = c1;
    this.c2 = c2;
    this.c4 = c4;
  }

  public int getType() {
    return type;
  }

  public long getTime() {
    return time;
  }

  public int getKeycode() {
    return keycode;
  }

  public double getW() {
    return w;
  }

  public double[] getM() {
    return new double[] {mx, my};
  }

  public double getC1() {
    return c1;
  }

  public double getC2() {
    return c2;
  }

  public double[][] getC() {
    return new double[][] {{c1, c2}, {c2, c4}};
  }
}
