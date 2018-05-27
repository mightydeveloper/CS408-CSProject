package com.notakeyboard.keyboard;

import com.notakeyboard.R;
import com.notakeyboard.Define;
import com.notakeyboard.HangulAutomata;
import com.notakeyboard.db.BaseDB;
import com.notakeyboard.db.KeySpecDB;
import com.notakeyboard.db.PrefData;
import com.notakeyboard.db.PreferenceDB;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * NOTA Input Method Service Class
 */
public class NIMService extends InputMethodService
    implements KeyboardView.OnKeyboardActionListener {
  public static final int KEYCODE_ENTER = 10;
  public static boolean isVibrateOn;
  public static boolean isSoundOn;
  // whether currentKeyboard mvns is out-dated.
  private static boolean newMvns = false;
  private static HangulAutomata hangulAutomata;

  private NKeyboard qwerty;  // english qwerty keyboard
  private NKeyboard korean;  // korean keyboard
  private NKeyboard korean_shift;
  private NKeyboard symbols1;
  private NKeyboard symbols2;
  private NKeyboard numbers;
  private NKeyboard numbers_symbol;
  private NKeyboard currentKeyboard;
  private NKeyboard previous;
  private NKeyboardView inputView;
  private PreferenceDB prefDB;

  private boolean isCapsLock = false;
  private int curEditorAction;
  private int cur_sym_shortcut;
  private int layoutNum;
  private StringBuilder cheonjiinTextBuilder;
  private String lang;

  /**
   * Creates an InputMethodService. It stays alive until onDestroy() is called.
   */
  @Override
  public void onCreate() {
    super.onCreate();
    PrefData prefData = new PrefData(this);
    prefDB = new PreferenceDB(this);

    isVibrateOn = prefData.isVibrateOn;
    isSoundOn = prefData.isSoundOn;
    lang = prefData.lang;
    String defLang = prefData.defaultLang;  // default language
    if (defLang.isEmpty()) {
      prefDB.put("def_lang", lang);
    } else if (!lang.equals(defLang)) {
      resetDB();
    }

    cheonjiinTextBuilder = new StringBuilder();
    cur_sym_shortcut = -1;

    // sets language settings
    setLanguage();

    layoutNum = prefData.layoutNum;
    setAutomatas(layoutNum, prefData.doubleTouchShiftOn);

    // create different types of keyboards
    createKeyboards(prefData);
  }

  @Override
  public void onInitializeInterface() {}

  private void saveKeySpec(NKeyboard keyboard) {
    if (prefDB.get(Define.SPEC_DB_KEY(keyboard.getNKeyboardType(this))).isEmpty()) {
      new KeySpecDB(this).insert(keyboard, keyboard.getNKeyboardType(this));
    }
  }

  @SuppressLint("InflateParams")
  @Override
  public View onCreateInputView() {
    inputView = (NKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
    inputView.setOnKeyboardActionListener(this);
    return inputView;
  }

  /**
   * Sets the language from language settings.
   */
  private void setLanguage() {
    // language settings
  }

  /**
   * Set automata configurations from the layout.
   * @param layoutNum layout number
   */
  private void setAutomatas(int layoutNum, boolean doubleTouchShift) {
    // set automata according to the layout
    switch (layoutNum) {
      case Define.CHEONJIIN_LAYOUT:
        hangulAutomata = new HangulAutomata(Define.CHEONJIIN_AUTOMATA);
        break;
      case Define.DANMOEUM_LAYOUT:
        hangulAutomata = new HangulAutomata(Define.DANMOEUM_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA);
        break;
      case Define.QWERTY_LAYOUT:
      default:
        if (doubleTouchShift) {
          hangulAutomata = new HangulAutomata(Define.QWERTY_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA);
        } else {
          hangulAutomata = new HangulAutomata(Define.DEFAULT_QWERTY_AUTOMATA);
        }
    }
  }

  /**
   * Create keyboards according to preferences.
   */
  private void createKeyboards(PrefData prefData) {
    // number_pad on top or not.
    boolean useNumPad = prefData.isUsingNumberPad;
    boolean isLangKorean = lang.contains("ko");

    // create keyboard by parsing XML according to the layout id
    if (useNumPad) {
      if (isLangKorean) {
        qwerty = new NKeyboard(this, R.xml.qwerty_num);
      } else {
        qwerty = new NKeyboard(this, R.xml.qwerty_num_en);
      }

      switch (layoutNum) {
        case Define.QWERTY_LAYOUT:
        default:
          korean = new NKeyboard(this, R.xml.korean_num);
          korean_shift = new NKeyboard(this, R.xml.korean_shift_num);
      }

      if (isLangKorean) {
        symbols1 = new NKeyboard(this, R.xml.symbols1_num);
        symbols2 = new NKeyboard(this, R.xml.symbols2_num);
      } else {
        symbols1 = new NKeyboard(this, R.xml.symbols1_num_en);
        symbols2 = new NKeyboard(this, R.xml.symbols2_num_en);
      }
    } else {  // not using number pad
      if (lang.contains("ko")) {
        qwerty = new NKeyboard(this, R.xml.qwerty);
      } else {
        qwerty = new NKeyboard(this, R.xml.qwerty_en);
      }

      switch (layoutNum) {
        case Define.QWERTY_LAYOUT:
        default:
          korean = new NKeyboard(this, R.xml.korean);
          korean_shift = new NKeyboard(this, R.xml.korean_shift);
          break;
      }

      if (isLangKorean) {
        symbols1 = new NKeyboard(this, R.xml.symbols1);
        symbols2 = new NKeyboard(this, R.xml.symbols2);
      } else {
        symbols1 = new NKeyboard(this, R.xml.symbols1_en);
        symbols2 = new NKeyboard(this, R.xml.symbols2_en);
      }
    }

    numbers = new NKeyboard(this, R.xml.numbers);
    numbers_symbol = new NKeyboard(this, R.xml.numbers_symbol);
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);

    saveKeySpec(qwerty);
    saveKeySpec(korean);
    saveKeySpec(korean_shift);

    // reset automata states
    hangulAutomata.reset();
    hangulAutomata.stateReset();

    curEditorAction = attribute.imeOptions
        & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);

    prefDB.put("isPassword", "false");
    switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
      case InputType.TYPE_CLASS_NUMBER:
      case InputType.TYPE_CLASS_DATETIME:
      case InputType.TYPE_CLASS_PHONE:
        currentKeyboard = numbers;
        break;
      case InputType.TYPE_CLASS_TEXT:
        if (lang.contains("ko")) {
          currentKeyboard = korean;
        } else {
          currentKeyboard = qwerty;
        }

        if (inputView != null && inputView.getKeyboard() != null) {
          if ((inputView.getKeyboard().getNKeyboardType(this) == Define.QWERTY) ||
              (inputView.getKeyboard().getNKeyboardType(this) == Define.QWERTY_LANDSCAPE)) {
            currentKeyboard = qwerty;
          }
        }

        // save whether the user is typing in password
        int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
            || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
          prefDB.put("isPassword", "true");
          currentKeyboard = qwerty;
        }

        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            || variation == InputType.TYPE_TEXT_VARIATION_URI) {
          //email, uri
          currentKeyboard = qwerty;
        }
        break;
      default:
        currentKeyboard = qwerty;
    }

    // configure the text to show for 'enter' key
    changeEnterLabel();

  }


  /**
   * Called whenever the user touches on a new input and the keyboard pops up.
   * @param attribute  contains editor info, especially the input types
   * @param restarting true if we are restarting input on the same text field as before
   */
  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    super.onStartInputView(attribute, restarting);

    inputView.setKeyboard(this.currentKeyboard);
    inputView.invalidate();
    inputView.updatePreferences();
    inputView.setUniqueSentence();  // indicate a new start of a sentence
  }

  /**
   * Called on pressing a key.
   * @param primaryCode primary key code
   * @param keyCodes the list of key codes
   */
  @Override
  public void onKey(int primaryCode, int[] keyCodes) {
    NKeyboard current = inputView.getKeyboard();

    if(inputView.isBlocked)
      unblockKeyboard();

    if (current == qwerty) {
      handleQwerty(primaryCode);
    } else if (current == korean || current == korean_shift) {
      handleKorean(primaryCode);
    } else if (current == symbols1 || current == symbols2) {
      handleSymbol(primaryCode);
    } else if (current == numbers || current == numbers_symbol) {
      handleNumber(primaryCode);
    }
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    if (inputView != null) {
      inputView.setKeyboard(korean);
    }
  }

  private void handleQwerty(int primaryCode) {
    if (primaryCode == NKeyboard.KEYCODE_NOTAKEY) { // If NOTA key toggle isNOTAKey
      inputView.toggleIsNOTAKey();
      inputView.invalidate();
      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_SHIFT) {
      if (inputView.isShifted()) {
        if (isCapsLock) {
          isCapsLock = false;
          inputView.setShifted(false);
          qwerty.changeShiftLabel(Define.NOT_SHIFTED);
        } else {
          isCapsLock = true;
          qwerty.changeShiftLabel(Define.CAPSLOCK);
        }
      } else {
        inputView.setShifted(!inputView.isShifted());
        qwerty.changeShiftLabel(Define.SHIFTED);
      }

      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_DELETE) {
      if (getCurrentInputConnection().getSelectedText(0) != null) {
        getCurrentInputConnection().commitText("", 1);
      }

      getCurrentInputConnection().deleteSurroundingText(1, 0);
      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_CANCEL) {
      previous = qwerty;
      if (lang.contains("ko")) {
        symbols1.changeToggleLabel(Define.TOGGLE_ENG);
        symbols2.changeToggleLabel(Define.TOGGLE_ENG);
      }
      inputView.setShifted(false);
      isCapsLock = false;
      qwerty.changeShiftLabel(Define.NOT_SHIFTED);
      inputView.setKeyboard(symbols1);
      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_MODE_CHANGE) {
      inputView.setShifted(false);
      isCapsLock = false;
      qwerty.changeShiftLabel(Define.NOT_SHIFTED);
      inputView.setKeyboard(korean);
      return;
    }

    if (primaryCode == KEYCODE_ENTER) {
      handleEnter();
      inputView.setShifted(false);
      isCapsLock = false;
      qwerty.changeShiftLabel(Define.NOT_SHIFTED);
      return;
    }

    // If text block is selected, delete it before entering text
    if (getCurrentInputConnection().getSelectedText(0) != null) {
      getCurrentInputConnection().commitText("", 1);
    }

    if (inputView.isShifted()) primaryCode = Character.toUpperCase(primaryCode);
    getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);

    if (inputView.isShifted() && !isCapsLock) {
      inputView.setShifted(false);
      qwerty.changeShiftLabel(Define.NOT_SHIFTED);
    }
  }

  /**
   * Handle Korean inputs.
   * @param primaryCode primary keycode of input key
   */
  private void handleKorean(int primaryCode) {
    if (getCurrentInputConnection().getTextBeforeCursor(10, 0) == null) {
      commitHangul();
    } else if (getCurrentInputConnection().getTextBeforeCursor(10, 0).equals("")) {
      commitHangul();
    }

    NKeyboard current = inputView.getKeyboard();

    if (primaryCode == NKeyboard.KEYCODE_NOTAKEY) {  // If NOTA key toggle isNOTAKey
      inputView.toggleIsNOTAKey();
      inputView.invalidate();
      return;
    }

    // on shift
    if (primaryCode == NKeyboard.KEYCODE_SHIFT) {
      if (current == korean) {
        inputView.setKeyboard(korean_shift);
        korean_shift.changeShiftLabel(Define.SHIFTED);
        inputView.setShifted(true);
      } else {
        inputView.setKeyboard(korean);
        korean.changeShiftLabel(Define.NOT_SHIFTED);
        inputView.setShifted(false);
      }
      return;
    }

    // return to original keyboard as soon as key is pressed during shift keyboard
    if (current == korean_shift) {
      inputView.setKeyboard(korean);
      inputView.setShifted(false);
    }

    // on delete
    if (primaryCode == NKeyboard.KEYCODE_DELETE) {
      int deletedHangulPrimaryCode = hangulAutomata.delete();

      if (getCurrentInputConnection().getSelectedText(0) != null) {
        commitHangul();
        getCurrentInputConnection().commitText("", 1);
      } else if (deletedHangulPrimaryCode == -1) {
        // 뒤로 가야함
        if (layoutNum == Define.CHEONJIIN_LAYOUT && cheonjiinTextBuilder.length() != 0) {
          cheonjiinTextBuilder.deleteCharAt(cheonjiinTextBuilder.length() - 1);
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
          commitHangul();
        } else {
          getCurrentInputConnection().deleteSurroundingText(1, 0);  // 뒤로 빠지면서 제거
        }
      } else if (deletedHangulPrimaryCode == 0) {
        // 제자리에서 빈칸 만들기
        if (layoutNum == Define.CHEONJIIN_LAYOUT && cheonjiinTextBuilder.length() != 0) {
          cheonjiinTextBuilder.deleteCharAt(cheonjiinTextBuilder.length() - 1);
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
          commitHangul();
        } else {
          getCurrentInputConnection().commitText("", 0);  // 제자리에서 아무것도 없는 글자가 됨
        }
      } else {
        // 한 글자에서 자모를 하나씩 제거
        if (layoutNum == Define.CHEONJIIN_LAYOUT && cheonjiinTextBuilder.length() != 0) {
          // 천지인
          cheonjiinTextBuilder.setCharAt(
              cheonjiinTextBuilder.length() - 1, (char) deletedHangulPrimaryCode);
          getCurrentInputConnection().setComposingText(
              cheonjiinTextBuilder.toString(), 1);
        } else {
          // not 천지인
          getCurrentInputConnection().setComposingText(
              String.valueOf((char) deletedHangulPrimaryCode), 1);
        }
      }
      return;
    }

    // on cancel
    if (primaryCode == NKeyboard.KEYCODE_CANCEL) {
      commitHangul();
      previous = korean;
      symbols1.changeToggleLabel(Define.TOGGLE_KOR);
      symbols2.changeToggleLabel(Define.TOGGLE_KOR);
      inputView.setKeyboard(symbols1);
      return;
    }

    // on keycode mode change
    if (primaryCode == NKeyboard.KEYCODE_MODE_CHANGE) {
      commitHangul();
      inputView.setKeyboard(qwerty);
      return;
    }

    // on enter
    if (primaryCode == KEYCODE_ENTER) {
      commitHangul();
      handleEnter();
      return;
    }

    // If text block is selected, delete it before entering text
    if (getCurrentInputConnection().getSelectedText(0) != null) {
      commitHangul();
      getCurrentInputConnection().commitText("", 1);
    }

    // handle hangul with automata
    int[] hangulPrimaryCode = hangulAutomata.hangulAutomata(primaryCode);

    if (hangulPrimaryCode[1] == -1) {
      // 두번째 한글 버퍼 (작성중 글자)가 없을 때
      if (hangulPrimaryCode[0] == NKeyboard.KEYCODE_SPACE && cheonjiinTextBuilder.length() != 0) {
        commitHangul();
        return;
      } else if (hangulPrimaryCode[0] == NKeyboard.QUICK_SYMBOLS) {
        // 퀵심볼
        if (cur_sym_shortcut == -1) {
          commitHangul();
          cheonjiinTextBuilder.append('.');
          cur_sym_shortcut = 46;
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
        } else if (cur_sym_shortcut == 46) {
          cheonjiinTextBuilder.setCharAt(0, ',');
          cur_sym_shortcut = 44;
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
        } else if (cur_sym_shortcut == 44) {
          cheonjiinTextBuilder.setCharAt(0, '?');
          cur_sym_shortcut = 63;
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
        } else if (cur_sym_shortcut == 63) {
          cheonjiinTextBuilder.setCharAt(0, '!');
          cur_sym_shortcut = 33;
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
        } else if (cur_sym_shortcut == 33) {
          cheonjiinTextBuilder.setCharAt(0, '.');
          cur_sym_shortcut = 46;
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
        }
        return;
      } else if (hangulPrimaryCode[0] == NKeyboard.ZZZ) {
        // ㅋㅋㅋ 연속 입력
        commitHangul();
        getCurrentInputConnection().commitText("ㅋㅋㅋ", 0);
        return;
      }

      commitHangul();
      getCurrentInputConnection().commitText(String.valueOf((char) hangulPrimaryCode[0]), 1);
    } else {
      // 두번째 한글 버퍼가 존재할 때
      if (cur_sym_shortcut != -1) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
          ic.finishComposingText();
          cur_sym_shortcut = -1;
          cheonjiinTextBuilder.delete(0, cheonjiinTextBuilder.length());
        }
      }

      if (hangulPrimaryCode[0] == 0 && layoutNum == Define.CHEONJIIN_LAYOUT) {
        if (cheonjiinTextBuilder.length() < 2) {
          cheonjiinTextBuilder.append((char) hangulPrimaryCode[1]);
        } else {
          cheonjiinTextBuilder.deleteCharAt(cheonjiinTextBuilder.length() - 1);
          cheonjiinTextBuilder.setCharAt(
              cheonjiinTextBuilder.length() - 1, (char) hangulPrimaryCode[1]);
        }
        getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
      } else if (hangulPrimaryCode[1] == 0) {
        // 두번째 버퍼가 empty string일 때
        if (layoutNum == Define.CHEONJIIN_LAYOUT) {
          // 천지인의 경우
          if (cheonjiinTextBuilder.length() == 0) {
            cheonjiinTextBuilder.append((char) hangulPrimaryCode[0]);
          } else {
            cheonjiinTextBuilder.setCharAt(cheonjiinTextBuilder.length() - 1, (char) hangulPrimaryCode[0]);
          }
          getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
        } else {
          // non 천지인
          getCurrentInputConnection().setComposingText(
              String.valueOf((char) hangulPrimaryCode[0]), 1);
        }
      } else {
        // 두번째 버퍼에 내용물이 존재할 때
        if (layoutNum == Define.CHEONJIIN_LAYOUT) {
          // 천지인
          if (hangulPrimaryCode[1] > 100000000) {
            cheonjiinTextBuilder.setCharAt(
                cheonjiinTextBuilder.length() - 2, (char) hangulPrimaryCode[0]);
            cheonjiinTextBuilder.setCharAt(
                cheonjiinTextBuilder.length() - 1, (char) (hangulPrimaryCode[1] - 100000000));

            getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
          } else {
            if (cheonjiinTextBuilder.length() == 0) {
              cheonjiinTextBuilder.append((char) hangulPrimaryCode[0]);
            } else {
              cheonjiinTextBuilder.setCharAt(cheonjiinTextBuilder.length() - 1, (char) hangulPrimaryCode[0]);
            }
            cheonjiinTextBuilder.append((char) hangulPrimaryCode[1]);
            getCurrentInputConnection().setComposingText(cheonjiinTextBuilder.toString(), 1);
          }
        } else {
          // non 천지인
          // 이전 버퍼 내용을 입력하고, 그 다음 칸을 작성중인 칸으로 설정
          getCurrentInputConnection().commitText(
              String.valueOf((char) hangulPrimaryCode[0]), 1);
          getCurrentInputConnection().setComposingText(
              String.valueOf((char) hangulPrimaryCode[1]), 1);
        }
      }
    }
  }

  private void handleSymbol(int primaryCode) {
    if (primaryCode == NKeyboard.KEYCODE_NOTAKEY) {
      return;  // If NOTA key do nothing
    }

    if (primaryCode == NKeyboard.KEYCODE_SHIFT) {
      NKeyboard current = inputView.getKeyboard();
      if (current == symbols1) {
        inputView.setKeyboard(symbols2);
      } else {
        inputView.setKeyboard(symbols1);
      }
      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_DELETE) {
      if (getCurrentInputConnection().getSelectedText(0) != null) {
        getCurrentInputConnection().commitText("", 1);
      }
      getCurrentInputConnection().deleteSurroundingText(1, 0);
      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_MODE_CHANGE) {
      if (previous == korean || previous == korean_shift) {
        inputView.setKeyboard(qwerty);
        return;
      } else if (previous == qwerty) {
        inputView.setKeyboard(korean);
        return;
      }
    }

    if (primaryCode == NKeyboard.KEYCODE_CANCEL) {
      inputView.setKeyboard(previous);
      return;
    }

    if (primaryCode == KEYCODE_ENTER) {
      handleEnter();
      return;
    }

    // If a text block is selected, delete it before entering text
    if (getCurrentInputConnection().getSelectedText(0) != null) {
      getCurrentInputConnection().commitText("", 1);
    }

    getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
  }

  private void handleNumber(int primaryCode) {
    NKeyboard current = inputView.getKeyboard();

    if (primaryCode == NKeyboard.KEYCODE_NOTAKEY) return;  //If NOTA key do nothing

    if (primaryCode == NKeyboard.KEYCODE_DELETE) {
      if (getCurrentInputConnection().getSelectedText(0) != null) {
        getCurrentInputConnection().commitText("", 1);
      }

      getCurrentInputConnection().deleteSurroundingText(1, 0);
      return;
    }

    if (primaryCode == NKeyboard.KEYCODE_CANCEL) {
      if (current == numbers) {
        inputView.setKeyboard(numbers_symbol);
      } else if (current == numbers_symbol) {
        inputView.setKeyboard(numbers);
      }
      return;
    }

    if (primaryCode == KEYCODE_ENTER) {
      handleEnter();
      return;
    }

    // If text block is selected, delete it before entering text
    if (getCurrentInputConnection().getSelectedText(0) != null) {
      getCurrentInputConnection().commitText("", 1);
    }

    getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
  }

  /**
   * 엔터키 입력시
   */
  private void handleEnter() {
    // indicate the end of this sentence, as well as the start of the new sentence
    inputView.setUniqueSentence();

    // Identified EditorAction: 1~7, Unidentified(0) and else: just send '\n'
    // start a new line unless it has predefined action - such as 'search' or 'send'
    if (curEditorAction < 8 && curEditorAction > 0) {
      getCurrentInputConnection().performEditorAction(curEditorAction);
    } else {
      getCurrentInputConnection().commitText(String.valueOf("\n"), 1);
    }
  }

  /**
   * Set appropriate text to "ENTER" key.
   * (Go, Send, Find, etc.)
   */
  private void changeEnterLabel() {
    NKeyboard[] Keyboards = {
        qwerty, korean, korean_shift, symbols1, symbols2, numbers, numbers_symbol
    };

    for (NKeyboard keyboard : Keyboards) {
      keyboard.changeEnterLabel(curEditorAction);
    }
  }

  /**
   * Commits hangul and resets automata.
   */
  private void commitHangul() {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
      // finish active composing text - but leaves the text as-is
      ic.finishComposingText();
      cur_sym_shortcut = -1;
      cheonjiinTextBuilder.delete(0, cheonjiinTextBuilder.length());

      // reset the automata
      hangulAutomata.reset();
      hangulAutomata.stateReset();
    }

    blockKeyboard();
  }

  public void blockKeyboard() {
    inputView.isBlocked = true;
    inputView.invalidate();
  }

  public void unblockKeyboard() {
    inputView.isBlocked = false;
    inputView.invalidate();
  }

  @Override
  public void onViewClicked(boolean focusChanged) {
    if (!focusChanged) {
      commitHangul();
    }
  }

  @Override
  public void onPress(int primaryCode) {
    inputView.setPreviewEnabled(false);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  private void resetDB() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        BaseDB db = new BaseDB(NIMService.this);
        db.getWritableDatabase();
        db.resetForKeySpecChange();

        for (int i = 0; i < Define.NUM_OF_NKEYBOARD_TYPE; i++) {
          prefDB.put(Define.SPEC_DB_KEY(i), "");
        }
      }
    }).start();
  }


  @Override
  public void onRelease(int primaryCode) {
  }

  @Override
  public void onText(CharSequence text) {
  }

  @Override
  public void swipeLeft() {
  }

  @Override
  public void swipeRight() {
  }

  @Override
  public void swipeDown() {
  }

  @Override
  public void swipeUp() {
  }
}
