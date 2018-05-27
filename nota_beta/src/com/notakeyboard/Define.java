package com.notakeyboard;

public class Define {
  //about Keyboard Type
  public static final int NUM_OF_NKEYBOARD_TYPE = 6;

  public static final int QWERTY = 0;
  public static final int KOREAN = 1;
  public static final int KOREAN_SHIFT = 2;
  public static final int QWERTY_LANDSCAPE = 3;
  public static final int KOREAN_LANDSCAPE = 4;
  public static final int KOREAN_SHIFT_LANDSCAPE = 5;

  public static final int QWERTY_LAYOUT = 0;
  public static final int DANMOEUM_LAYOUT = 1;
  public static final int CHEONJIIN_LAYOUT = 2;

  //about Automata type
  public static final int DEFAULT_QWERTY_AUTOMATA = 0;
  public static final int DANMOEUM_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA = 1;
  public static final int DANMOEUM_SSANGJAEUM_LONG_TOUCH_AUTOMATA = 2;
  public static final int QWERTY_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA = 3;
  public static final int CHEONJIIN_AUTOMATA = 4;

  public static final int[] TRAINING_SQUENCE = {QWERTY, KOREAN_SHIFT, KOREAN};

  public static String SPEC_DB_KEY(int type) {
    return "spec:" + type;
  }

  //about Training
  public static final double MINIMUM_KEY_RANGE = 0.8;
  public static boolean isTrainingNow = false;
  public static boolean isTPDBopen = false;

  //about vibration
  public static final long VIBRATE_DEFAULT = 15;
  public static final long VIBRATE_NOTA = 30;

  //about Shift state
  public static final int NOT_SHIFTED = 0;
  public static final int SHIFTED = 1;
  public static final int CAPSLOCK = 2;

  //about Toggle state
  public static final int TOGGLE_KOR = 0;
  public static final int TOGGLE_ENG = 1;

  //about Key height
  public static final int DEFAULT_KEY_HEIGHT = 55;
  public static final double KEY_HEIGHT_MODIFIER_0 = 0.85;
  public static final double KEY_HEIGHT_MODIFIER_1 = 0.925;
  public static final double KEY_HEIGHT_MODIFIER_2 = 1;
  public static final double KEY_HEIGHT_MODIFIER_3 = 1.075;
  public static final double KEY_HEIGHT_MODIFIER_4 = 1.15;

  // e-mail address
  public static final String DEV_EMAIL = "dev@notakeyboard.com";
}
