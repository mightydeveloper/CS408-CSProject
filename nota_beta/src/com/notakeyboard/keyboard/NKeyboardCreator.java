package com.notakeyboard.keyboard;

import android.content.Context;

import com.notakeyboard.db.PrefData;

/**
 * Easy keyboard creator from keyboard type.
 * Retrieves preferences automatically.
 */
public class NKeyboardCreator {
  public static NKeyboard createKeyboard(Context context, int keyboardType) {
    // get preference information
    PrefData prefData = new PrefData(context);
    boolean useNumPad = prefData.isUsingNumberPad;
    int layoutNum = prefData.layoutNum;
    String lang = prefData.lang;

    return new NKeyboard(context,
        NKeyboard.getNKeyboardXml(layoutNum, keyboardType, useNumPad, lang));
  }
}
