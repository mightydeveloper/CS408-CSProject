package com.notakeyboard.keyboard;

/**
 * Abstract class that represents a single touch on the keyboard.
 */
abstract public class Touch {
  public int x;  // x-coordinate
  public int y;  // y-coordinate
  public int keycode;  // character that this touch represents
  public int keyboardType;  // keyboard type in which this touch is made

  public void setKeycode(int keycode) {
    this.keycode = keycode;
  }
}
