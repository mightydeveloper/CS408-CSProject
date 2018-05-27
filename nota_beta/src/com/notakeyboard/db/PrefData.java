package com.notakeyboard.db;

import android.content.Context;

import com.notakeyboard.Define;

import java.util.Locale;

/**
 * Preference data access object.
 */
public class PrefData {
  private Context context;
  public boolean isUsingNumberPad;
  public int layoutNum;
  public String layoutStr;
  public String lang;
  public String defaultLang;
  public String theme;
  public int themeNum = 1;  // default value
  public String keyHeightTypeStr;
  public boolean useYellowKey;
  public boolean isPassword;
  public boolean isUsingBoldText;
  public boolean isVibrateOn = true;
  public boolean isSoundOn = true;
  public boolean isAlarmSet = false;
  public boolean doubleTouchShiftOn = false;

  public PrefData(Context context) {
    this.context = context;
    this.getPreferenceOptions();
  }

  /**
   * Get preference options.
   */
  public void getPreferenceOptions() {
    PreferenceDB prefDB = new PreferenceDB(context);

    isUsingNumberPad = !prefDB.get("number_pad").equals("false");
    lang = prefDB.get("lang");
    if (lang.isEmpty()) {
      lang = Locale.getDefault().getLanguage();
    }
    defaultLang = prefDB.get("def_lang");

    layoutStr = prefDB.get("layout");
    layoutNum = Define.QWERTY_LAYOUT;
    if (!layoutStr.isEmpty()) {
      layoutNum = Integer.valueOf(layoutStr);
    }

    // theme preferences
    theme = prefDB.get("theme");
    if (!theme.isEmpty()) {
      themeNum = Integer.parseInt(theme);
    }

    // key height choice
    keyHeightTypeStr = prefDB.get("key_height_pref");
    if (keyHeightTypeStr.isEmpty()) {
      keyHeightTypeStr = "2";
    }

    // keyboard option
    doubleTouchShiftOn = prefDB.get("double_touch_shift").equals("true");

    // want to use yellow key?
    useYellowKey = prefDB.get("yellow_key").equals("true");

    // is it password?
    isPassword = prefDB.get("isPassword").equals("true");

    // is keyboard displayed with bold text?
    isUsingBoldText = prefDB.get("bold_text").equals("true");

    // vibration and sound preferences
    isVibrateOn = !prefDB.get("vibrate").equals("false");
    isSoundOn = !prefDB.get("sound").equals("false");
    isAlarmSet = prefDB.get("isAlarmSet").equals("true");
  }
}
