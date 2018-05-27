package com.notakeyboard.keyboard;

import java.util.List;

import com.notakeyboard.R;
import com.notakeyboard.Define;
import com.notakeyboard.db.PrefData;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.view.Surface;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

/**
 * Nota Keyboard - used to define automata, layout, height, and types.
 * One of the keyboard instance will be shown to NKeyboardView.
 */
public class NKeyboard extends Keyboard {
  public static final int KEYCODE_NOTAKEY = -9999999;  // key with nota symbol
  public static final int QUICK_SYMBOLS = -9999998;
  public static final int ZZZ = -9999997;  // 'ㅋㅋㅋ'
  public static final int KEYCODE_DOT = 46;  // '.'
  public static final int KEYCODE_SPACE = 32;  // ' '
  public static final int KEYCODE_DONE = 10;  // '확인' 버튼

  private int keyboardType;
  private int xmlLayoutResId;

  private Context context;

  private Key enterKey;
  private Key shiftKey;
  private Key deleteKey;
  private Key togglelangKey;
  private Key spaceKey;
  private Key symKey;
  private Key notaKey;
  private int KbdTheme = 1;

  public NKeyboard(Context context, int xmlLayoutResId) {
    super(context, xmlLayoutResId);

    this.context = context;
    this.xmlLayoutResId = xmlLayoutResId;
    keyboardType = getTypeFromXmlId(xmlLayoutResId);

    // get user's preference for the keyboard
    PrefData prefData = new PrefData(context);
    KbdTheme = prefData.themeNum;

    setSymKeysLabel();
    changeKeyHeight(prefData.keyHeightTypeStr);
  }

  /**
   * Determine the keyboard type from xml key layout file.
   */
  private int getTypeFromXmlId(int xmlLayoutResId) {
    int type;
    switch (xmlLayoutResId) {
      case (R.xml.qwerty):
      case (R.xml.qwerty_en):
        type = Define.QWERTY;
        break;
      case (R.xml.korean):
        type = Define.KOREAN;
        break;
      case (R.xml.korean_shift):
        type = Define.KOREAN_SHIFT;
        break;
      case (R.xml.qwerty_num):
      case (R.xml.qwerty_num_en):
        type = Define.QWERTY;
        break;
      case (R.xml.korean_num):
        type = Define.KOREAN;
        break;
      case (R.xml.korean_shift_num):
        type = Define.KOREAN_SHIFT;
        break;
      default:
        type = -1;
        break;
    }

    return type;
  }

  int getXmlId() {
    return this.xmlLayoutResId;
  }

  /**
   * Returns base type for this keyboard.
   * It ignores whether the screen is portrait or not.
   * @return keyboard base type
   */
  public int getKeyboardBaseType() {
    return keyboardType;
  }

  /**
   * Returns this keyboard's type number.
   * @param context current context
   * @return keyboard type
   */
  public int getNKeyboardType(Context context) {
    if (keyboardType == -1) {
      return keyboardType;
    }

    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    if (windowManager != null &&
        windowManager.getDefaultDisplay().getRotation() != Surface.ROTATION_0) {
      // not Portrait
      return keyboardType + 3;  // return landscape type
    }
    return keyboardType;
  }

  /**
   * Get keyboard type in terms of string.
   * @param keyboardType keyboard type
   * @return string representation of keyboard type
   */
  public static String getNKeyboardTypeStr(int keyboardType) {
    String keyboardTypeStr;
    switch (keyboardType) {
      case Define.QWERTY:
        keyboardTypeStr = "QWERTY";
        break;
      case Define.KOREAN_SHIFT:
        keyboardTypeStr = "KOREAN_SHIFT";
        break;
      case Define.KOREAN:
        keyboardTypeStr = "KOREAN";
        break;
      default:
        keyboardTypeStr = "Unknown";
        break;
    }

    return keyboardTypeStr;
  }

  /**
   * Get keys for this keyboard.
   * @return list of nota keys
   */
  @SuppressWarnings("unchecked")
  public List<NKey> getNKeys() {
    return (List<NKey>) (List<? extends Key>) super.getKeys();
  }

  /**
   * Retrieve the key index from keycode.
   * @param nKeycode keycode
   * @return key index of array retrieved by getNKeys()
   */
  public int getIndexOfKeycode(int nKeycode) {
    List<NKey> nKeys = this.getNKeys();
    return getIndexOfKeycode(nKeycode, nKeys);
  }

  /**
   * Retrieve the key index from keycode.
   * @param nKeycode keycode
   * @param nKeys list of keyboard keys
   * @return key index of array retrieved by getNKeys()
   */
  public int getIndexOfKeycode(int nKeycode, List<NKey> nKeys) {
    for (int i = 0; i < nKeys.size(); i++) {
      if (nKeys.get(i).codes[0] == nKeycode) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Retrieve the keycode from key index;
   * @param keyIndex key index of array retrieved by getNKeys()
   * @return keycode
   */
  public int getKeycodeFromIndex(int keyIndex) {
    if (keyIndex == -1) {
      return -1;
    }
    return this.getNKeys().get(keyIndex).getKeyCode();
  }

  /**
   * Produces the key from XML file.
   * @param res resource
   * @param parent parent row
   * @param x x-coordinate
   * @param y y-coordinate
   * @param parser xml for keyboard layout
   * @return keyboard key
   */
  @Override
  protected Key createKeyFromXml(Resources res, Row parent, int x, int y,
                                 XmlResourceParser parser) {
    Key key = new NKey(res, parent, x, y, parser);
    // the most representative character of this key
    if (key.codes[0] == NKeyboard.KEYCODE_DONE) {  // enter key
      enterKey = key;
    } else if (key.codes[0] == NKeyboard.KEYCODE_SHIFT) {
      shiftKey = key;
    } else if (key.codes[0] == NKeyboard.KEYCODE_NOTAKEY) {
      notaKey = key;
    } else if (key.codes[0] == NKeyboard.KEYCODE_SPACE) {
      spaceKey = key;
    } else if (key.codes[0] == NKeyboard.KEYCODE_CANCEL) {
      symKey = key;
    } else if (key.codes[0] == NKeyboard.KEYCODE_MODE_CHANGE) {
      togglelangKey = key;
    } else if (key.codes[0] == NKeyboard.KEYCODE_DELETE) {
      deleteKey = key;
    }
    return key;
  }

  /**
   * Get keyboard XML from some information.
   * @param layoutNum layout number
   * @param type keyboard type number
   * @param usenum whether it is using number pads
   * @param lang locale language
   * @return xml of keyboard layout, or -1 if invalid combination
   */
  public static int getNKeyboardXml(int layoutNum, int type, boolean usenum, String lang) {
    if (usenum) {
      switch (type) {
        case (Define.KOREAN):
          return R.xml.korean_num;
        case (Define.KOREAN_SHIFT):
          return R.xml.korean_shift_num;
        case (Define.QWERTY):
          if (lang.contains("ko")) {
            return R.xml.qwerty_num;
          } else {
            return R.xml.qwerty_num_en;
          }
        case (Define.KOREAN_LANDSCAPE):
          return R.xml.korean_num;
        case (Define.KOREAN_SHIFT_LANDSCAPE):
          return R.xml.korean_shift_num;
        case (Define.QWERTY_LANDSCAPE):
          if (lang.contains("ko")) {
            return R.xml.qwerty_num;
          } else {
            return R.xml.qwerty_num_en;
          }
        default:
          return -1;
      }
    } else {
      switch (type) {
        case (Define.KOREAN):
          return R.xml.korean;
        case (Define.KOREAN_SHIFT):
          return R.xml.korean_shift;
        case (Define.QWERTY):
          if (lang.contains("ko")) {
            return R.xml.qwerty;
          } else {
            return R.xml.qwerty_en;
          }
        case (Define.KOREAN_LANDSCAPE):
          return R.xml.korean;
        case (Define.KOREAN_SHIFT_LANDSCAPE):
          return R.xml.korean_shift;
        case (Define.QWERTY_LANDSCAPE):
          if (lang.contains("ko")) {
            return R.xml.qwerty;
          } else {
            return R.xml.qwerty_en;
          }
        default:
          return -1;
      }
    }
  }

  @SuppressWarnings("deprecation")
  public void setSymKeysLabel() {
    try {
      int sym_shift = context.getResources().getIdentifier("th" + KbdTheme + "_sym_shift", "drawable", context.getPackageName());
      shiftKey.icon = context.getResources().getDrawable(sym_shift);
    } catch (Exception e) {
    }

    try {
      int sym_sp = context.getResources().getIdentifier("th" + KbdTheme + "_sym_sp", "drawable", context.getPackageName());
      spaceKey.icon = context.getResources().getDrawable(sym_sp);
    } catch (Exception e) {
    }

    try {
      int sym_sym = context.getResources().getIdentifier("th" + KbdTheme + "_sym_sym", "drawable", context.getPackageName());
      symKey.icon = context.getResources().getDrawable(sym_sym);
    } catch (Exception e) {
    }

    try {
      int sym_delete = context.getResources().getIdentifier("th" + KbdTheme + "_sym_delete", "drawable", context.getPackageName());
      deleteKey.icon = context.getResources().getDrawable(sym_delete);
    } catch (Exception e) {
    }

    try {
      int sym_enter = context.getResources().getIdentifier("th" + KbdTheme + "_sym_enter", "drawable", context.getPackageName());
      enterKey.icon = context.getResources().getDrawable(sym_enter);
    } catch (Exception e) {
    }
  }

  @SuppressWarnings("deprecation")
  public void changeToggleLabel(int togglestate) {
    int id;
    switch (togglestate) {
      case (Define.TOGGLE_KOR):
        id = context.getResources().getIdentifier(
            "th" + KbdTheme + "_sym_toggle_kor", "drawable", context.getPackageName());
        togglelangKey.icon = context.getResources().getDrawable(id);
        break;
      case (Define.TOGGLE_ENG):
        id = context.getResources().getIdentifier(
            "th" + KbdTheme + "_sym_toggle_eng", "drawable", context.getPackageName());
        togglelangKey.icon = context.getResources().getDrawable(id);
        break;
      default:
        break;
    }
  }

  @SuppressWarnings("deprecation")
  void changeShiftLabel(int shiftstate) {
    int id;
    switch (shiftstate) {
      case (Define.NOT_SHIFTED):
        id = context.getResources().getIdentifier(
            "th" + KbdTheme + "_sym_shift", "drawable", context.getPackageName());
        shiftKey.icon = context.getResources().getDrawable(id);
        break;
      case (Define.SHIFTED):
        id = context.getResources().getIdentifier(
            "th" + KbdTheme + "_sym_shifted", "drawable", context.getPackageName());
        shiftKey.icon = context.getResources().getDrawable(id);
        break;
      case (Define.CAPSLOCK):
        id = context.getResources().getIdentifier(
            "th" + KbdTheme + "_sym_capslock", "drawable", context.getPackageName());
        shiftKey.icon = context.getResources().getDrawable(id);
        break;
      default:
        break;
    }
  }

  /**
   * Set appropriate text to "ENTER" key.
   * (Go, Send, Find, etc.)
   */
  public void changeEnterLabel(int curEditorAction) {
    if (enterKey == null) return;
    switch (curEditorAction) {
      case EditorInfo.IME_ACTION_GO:
        enterKey.icon = null;
        enterKey.label = context.getString(R.string.enter_go);
        break;
      case EditorInfo.IME_ACTION_NEXT:
        enterKey.icon = null;
        enterKey.label = context.getString(R.string.enter_next);
        break;
      case EditorInfo.IME_ACTION_SEARCH:
        enterKey.icon = null;
        enterKey.label = context.getString(R.string.enter_search);
        break;
      case EditorInfo.IME_ACTION_SEND:
        enterKey.icon = null;
        enterKey.label = context.getString(R.string.enter_send);
        break;
      default:
        int id = context.getResources().getIdentifier(
            "th" + KbdTheme + "_sym_enter", "drawable", context.getPackageName());
        enterKey.icon = context.getResources().getDrawable(id);
        enterKey.label = null;
        break;
    }
  }

  /**
   * Change the keyboard's height from the type.
   * @param keyHeightType type number in string : "1", "2", "3", "4"
   */
  public void changeKeyHeight(String keyHeightType) {
    double heightModifier;

    switch (Integer.parseInt(keyHeightType)) {
      case 0:
        heightModifier = Define.KEY_HEIGHT_MODIFIER_0;
        break;
      case 1:
        heightModifier = Define.KEY_HEIGHT_MODIFIER_1;
        break;
      case 2:
        heightModifier = Define.KEY_HEIGHT_MODIFIER_2;
        break;
      case 3:
        heightModifier = Define.KEY_HEIGHT_MODIFIER_3;
        break;
      case 4:
        heightModifier = Define.KEY_HEIGHT_MODIFIER_4;
        break;
      default:
        heightModifier = Define.KEY_HEIGHT_MODIFIER_2;
        break;
    }

    int height = 0;
    for (Key key : getKeys()) {
      key.height = (int) Math.round(key.height * heightModifier);
      key.y = (int) Math.round(key.y * heightModifier);
      height = key.height;
    }
    setKeyHeight(height);

    /*
     * somehow adding this fixed a weird bug where
     * bottom row keys could not be pressed if keyboard height is too tall..
     * from the Keyboard source code seems like calling this
     * will recalculate some values used in keypress detection calculation
     */
    getNearestKeys(0, 0);
  }

  /**
   * Calculate the hight of this keyboard.
   * @return height
   */
  @Override
  public int getHeight() {
    int height = 0;
    int curY = -1;
    List<Key> Keys = getKeys();
    for (Key key : Keys) {
      if (curY != key.y) {
        height += key.height;
        curY = key.y;
      }
    }

    return height;
  }

  /**
   * Class defining NOTA keyboard's keys.
   */
  public static class NKey extends NKeyboard.Key {
    private boolean isCorrected = false;

    NKey(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
      super(res, parent, x, y, parser);
    }

    /**
     * Returns the most important keycode of this key.
     * @return most significant keycode
     */
    public int getKeyCode() {
      return this.codes[0];
    }

    /**
     * Determine whether the (x,y) falls into the key w.r.t the ratio
     * By thkim
     **/
    public boolean isInside(int x, int y, double ratio) {
      // the ratio determines the boundary padding to determine whether the coordinate falls
      // inside more strictly
      return this.x + (1 - ratio) * width <= x
          && this.x + ratio * width >= x
          && this.y + (1 - ratio) * height <= y
          && this.y + ratio * height >= y;
    }

    /**
     * Return appropriate label for given key.
     * @param context current execution context
     * @return key type in string
     */
    public String getKeyLabel(Context context) {
      int keyCode = getKeyCode();

      switch (keyCode) {
        case NKeyboard.KEYCODE_SHIFT:
          return context.getString(R.string.shift);
        case NKeyboard.KEYCODE_MODE_CHANGE:
          return context.getString(R.string.toggle_lang);
        case NKeyboard.KEYCODE_CANCEL:
          return context.getString(R.string.symbol);
        case NKeyboard.KEYCODE_DELETE:
          return context.getString(R.string.backspace);
        case NKeyboard.KEYCODE_DONE:
          return context.getString(R.string.enter);
        case NKeyboard.KEYCODE_SPACE:
          return context.getString(R.string.space);
        case NKeyboard.KEYCODE_NOTAKEY:
          return context.getString(R.string.NOTA_key);
        default:
          return String.valueOf((char)keyCode);
      }
    }

    public boolean isCorrected() {
      return isCorrected;
    }

    public void setCorrected(boolean isCorrected) {
      this.isCorrected = isCorrected;
    }
  }
}
