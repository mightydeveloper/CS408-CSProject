package com.notakeyboard.keyboard;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.notakeyboard.R;
import com.notakeyboard.Define;
import com.notakeyboard.util.PrintLog;
import com.notakeyboard.db.PrefData;
import com.notakeyboard.db.PreferenceDB;
import com.notakeyboard.keyboard.NKeyboard.NKey;
import com.notakeyboard.preference.PreferencesActivity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.Window;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;

/**
 * The VIEW of keyboard that is responsible for displaying the keyboard
 * whenever an input event occurs.
 * 키보드의 보여지는 상태를 관리하고 키보드의 터치 입력을 처리함
 * 들어온 터치는 NKeyboard, 즉 기능을 담당하는 인스턴스에게 넘겨서 추론 과정을 거친다.
 */
public class NKeyboardView extends KeyboardView {
  private Context context;
  private NKeyboard keyboard;
  private PreferenceDB prefDBmanager;
  public boolean isBlocked = false;

  private boolean isAlreadyEntered = false;
  private boolean isLongPressed = false;
  private boolean isBoldText;
  private boolean isNOTAKey = false;
  private boolean useNumPad = true;
  private boolean isPassword = false;
  private boolean useYellowkey = false;
  private int theme = 1;
  private int layoutNum;
  private int xmlId = -1;
  private long sentence;  // indicate a unique sentence
  private SoundPool sounds;
  private int soundId;
  private Vibrator vb;

  private List<Integer> hangulPrimaryCodes = Arrays.asList(
      12610, 12616, 12599, 12593, 12613, 12635, 12629, 12625, 12624, 12628, 12609,
      12596, 12615, 12601, 12622, 12631, 12627, 12623, 12643, 12620, 12618, 12621,
      12640, 12636, 12641, 12611, 12617, 12600, 12594, 12614, 12626, 12630, 46, 12619);

  private List<Integer> englishPrimaryCodes = Arrays.asList(
      113, 119, 101, 114, 116, 121, 117, 105, 111, 112, 97, 115, 100, 102, 103,
      104, 106, 107, 108, 120, 99, 118, 98, 110, 109, 46);

  private List<Integer> SubPrimaryCodes = Arrays.asList(
      49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 33, 64, 35, 126, 47, 94, 9829, 40,
      41, 95, 45, 58, 59, 39, 63, 49, 50, 51, 52, 53, 57, 48, 44, 8734);

  private List<Integer> subPrimaryCodesNum = Arrays.asList(
      33, 64, 35, 36, 37, 94, 38, 42, 40, 41, 126, 43, 45, 215, 9829, 58,
      59, 39, 34, 95, 60, 62, 47, 44, 63, 33, 64, 35, 36, 37, 40, 41, 44, 8734);

  private List<Integer> cheonjiinPrimaryCodes = Arrays.asList(
      12643, 12685, 12641, 12593, 12596, 12599, 12610, 12613, 12616, 12615);

  private List<Integer> cheonjiinSubPrimaryCodes = Arrays.asList(
      94, 64, 35, -9999997, 9829, 42, 126, 45, 58, 47);

  // touch list that gets collected while Performance Testing or Map Testing
  private static boolean collectTouches = false;

  /**
   * Manages keyboard view.
   * @param context context
   * @param attrs attributes
   */
  public NKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;

    // retrieve vibrator from system
    vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

    // define different sound pool according to the version
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      createRecentSoundPool();
    } else {
      createOldSoundPool();
    }

    // sound
    soundId = sounds.load(context, R.raw.click, 1); // in 2nd param u have to pass your desire ringtone

    // database
    prefDBmanager = new PreferenceDB(context);
    updatePreferences();
  }

  /**
   * set to current time so that we can distinguish when this sentence started, and that
   * the inputs belong to the same sentence if this value is equal among.
   */
  void setUniqueSentence() {
    sentence = Calendar.getInstance().getTimeInMillis();
    PrintLog.error(this.getClass(), "setUniqueSentence : " + sentence);
  }

  /**
   * Create sound pool for API level above 21.
   */
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  protected void createRecentSoundPool() {
    AudioAttributes attributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();
    sounds = new SoundPool.Builder()
        .setAudioAttributes(attributes)
        .build();
  }

  /**
   * Create sound pool for API level less than 21.
   */
  @SuppressWarnings("deprecation")
  protected void createOldSoundPool() {
    sounds = new SoundPool(5, AudioManager.STREAM_SYSTEM, 0);
  }

  /**
   * Store preference data.
   */
  public void updatePreferences() {
    PrefData prefData = new PrefData(context);
    useNumPad = prefData.isUsingNumberPad;
    theme = prefData.themeNum;
    isPassword = prefData.isPassword;
    useYellowkey = prefData.useYellowKey;
    layoutNum = prefData.layoutNum;
    isBoldText = prefData.isUsingBoldText;
  }

  /**
   * Sets the keyboard for this view.
   * @param keyboard the keyboard to use on this view
   */
  public void setKeyboard(NKeyboard keyboard) {
    super.setKeyboard(keyboard);
    this.keyboard = keyboard;
  }

  public NKeyboard getKeyboard() {
    return keyboard;
  }

  public void playTouchSound() {
    if (NIMService.isSoundOn) {
      sounds.play(soundId, 0.3f, 0.3f, 0, 0, 1);
    }
  }

  /**
   * Called upon draw.
   * @param canvas canvas
   */
  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    drawKeyboard(canvas, theme, false, keyboard);

    if(isBlocked)
    {
      drawLockScreen(canvas);
    }
  }

  public void drawLockScreen(Canvas canvas) {
    canvas.drawColor(Color.BLACK);
    Paint paint = new Paint();
    Resources res = getResources();
    Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.lock_icon_512);
    Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

    Rect canvasrect = canvas.getClipBounds();
    canvas.drawBitmap(resized, canvasrect.centerX() - resized.getWidth()/2, canvasrect.centerY() - resized.getHeight()*3/4, paint);
    paint.setColor(Color.YELLOW);
    paint.setTextSize(80);
    canvas.drawText("Your phone is locked!", 155, canvasrect.centerY() + resized.getHeight()/2, paint);
  }

  /**
   * 키보드를 화면에 그리기.
   *
   * @param canvas canvas instance
   * @param themeNum ID of user-preferred theme
   * @param isCalledforMyMap
   * @param keyboard NOTA keyboard instance
   */
  public void drawKeyboard(Canvas canvas, int themeNum, boolean isCalledforMyMap,
                           NKeyboard keyboard) {
    List<NKey> keys = keyboard.getNKeys();
    int keyboardType = keyboard.getNKeyboardType(context);
    xmlId = keyboard.getXmlId();

    boolean isPressedAndNotForMyMap;
    Rect mPadding = new Rect(0, 0, 0, 0);
    Paint paint = new Paint();
    int targetBg;

    // About Padding, don't need to change this.
    int kbdPaddingLeft = getPaddingLeft();
    int kbdPaddingTop = getPaddingTop();

    // Keyboard background color
    int labelBgPressed = getResources().getIdentifier(
        "th" + themeNum + "_sym_bg_pressed", "drawable", context.getPackageName());
    int labelBg = getResources().getIdentifier(
        "th" + themeNum + "_sym_bg", "drawable", context.getPackageName());
    int keyBgPressed = getResources().getIdentifier(
        "th" + themeNum + "_key_bg_pressed", "drawable", context.getPackageName());
    int keyBg = getResources().getIdentifier(
        "th" + themeNum + "_key_bg", "drawable", context.getPackageName());
    int keyBgNota = getResources().getIdentifier(
        "th" + themeNum + "_nota", "drawable", context.getPackageName());

    // colors
    int mKbdColorId = getResources().getIdentifier(
        "th" + themeNum + "_bg", "color", context.getPackageName());
    int mTextColorId = getResources().getIdentifier(
        "th" + themeNum + "_text", "color", context.getPackageName());
    int mLabelColorId = getResources().getIdentifier(
        "th" + themeNum + "_labeltext", "color", context.getPackageName());

    // Key size and color of each key
    int mKbdColor = context.getResources().getColor(mKbdColorId);
    int mTextColor = context.getResources().getColor(mTextColorId);
    int mLabelColor = context.getResources().getColor(mLabelColorId);
    int keyHeight = (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        Define.DEFAULT_KEY_HEIGHT,
        getResources().getDisplayMetrics());
    int mKeyTextSize = keyHeight / 3;
    int mLabelTextSize = keyHeight / 4;
    int mSubKeyTextSize = keyHeight / 6;

    // draw Background
    paint.setColor(mKbdColor);
    if (isCalledforMyMap) {
      canvas.drawRect(0, 0, keyboard.getMinWidth(), keyboard.getHeight(), paint);
    } else {
      canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    paint.setTextAlign(Paint.Align.CENTER);
    paint.setAntiAlias(true);
    paint.setDither(true);
    paint.setSubpixelText(true);

    // is keyboard displayed with bold text
    if (isBoldText) {
      paint.setTypeface(Typeface.DEFAULT_BOLD);
    } else {
      paint.setTypeface(Typeface.DEFAULT);
    }

    boolean isCorrected;
    int curKeycode;

    isCorrected = false;
    curKeycode = NKeyboard.KEYCODE_NOTAKEY;

    boolean isThisKeyCorrected;
    for (NKey key : keys) {
      paint.setColor(context.getResources().getColor(R.color.mymap_gmm_line));

      // If currently pressed key is corrected by NOTA
      // Save whether key and currently pressed key is same or not.
      // (if Same true, else false: if Corrected by NOTA, True)
      isThisKeyCorrected = isCorrected && (curKeycode == key.codes[0]);
      isPressedAndNotForMyMap = (key.pressed && !isCalledforMyMap);

      // Switch the character to uppercase if shift is pressed
      String label = (key.label == null) ? null
          : adjustCase(key.label, isCalledforMyMap).toString();
      canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);

      // This Switch
      switch (themeNum) {
        case 1:
          if (theme1(canvas, paint, key, mPadding, kbdPaddingLeft, kbdPaddingTop,
              isPressedAndNotForMyMap, isThisKeyCorrected)) {
            continue;
          }
          break;
      }

      // Draw Text or icon
      if (key.label != null) {
        // For characters, use large font. For labels like "Done", use small font.
        if ((useNumPad && key.codes[0] < 58 && key.codes[0] > 47
            && (xmlId != R.xml.numbers && xmlId != R.xml.numbers_symbol)) // Number keys
            || (key.codes[0] == NKeyboard.KEYCODE_DOT && (xmlId != R.xml.numbers && xmlId != R.xml.numbers_symbol)) // Dot Key
            || (layoutNum == Define.CHEONJIIN_LAYOUT && (key.label.length() > 2 && key.codes.length < 2)) // In case of Cheonjiin label keys
            || (layoutNum == Define.CHEONJIIN_LAYOUT && (key.codes[0] == NKeyboard.KEYCODE_DONE))
            || (layoutNum != Define.CHEONJIIN_LAYOUT && (key.label.length() > 1 && key.codes.length < 2))) { // In case of not Cheonjiin label keys

          paint.setTextSize(mLabelTextSize);
          paint.setColor(mLabelColor);

          if (isPressedAndNotForMyMap) {
            if (isThisKeyCorrected && useYellowkey) {
              // This condition should check whether the key is corrected by nota or not.
              targetBg = keyBgNota;
            } else {
              targetBg = labelBgPressed;
            }
          } else {
            targetBg = labelBg;
          }
        } else {
          paint.setTextSize(mKeyTextSize);
          paint.setColor(mTextColor);

          if (isPressedAndNotForMyMap) {
            if (isThisKeyCorrected && useYellowkey) {
              // This condition should check whether the key is corrected by nota or not.
              targetBg = keyBgNota;
            } else {
              targetBg = keyBgPressed;
            }
          } else {
            targetBg = keyBg;
          }
        }
        NinePatchDrawable dr = (NinePatchDrawable) context.getResources().getDrawable(targetBg);
        dr.setBounds(0, 0, key.width, key.height);
        dr.draw(canvas);

        // Draw the text
        canvas.drawText(label,
            (key.width - mPadding.left - mPadding.right) / 2
                + mPadding.left,
            (key.height - mPadding.top - mPadding.bottom) / 2
                + (paint.getTextSize() - paint.descent()) / 2 + mPadding.top,
            paint);

        int subkey = 0;
        paint.setTextSize(mSubKeyTextSize);

        // In the number keyboard, dot doesn't have superscript
        if (key.codes[0] != NKeyboard.KEYCODE_DOT || (xmlId != R.xml.numbers && xmlId != R.xml.numbers_symbol)) {
          if (layoutNum == Define.CHEONJIIN_LAYOUT) {
            if (cheonjiinPrimaryCodes.indexOf(key.codes[0]) != -1) {
              subkey = cheonjiinSubPrimaryCodes.get(cheonjiinPrimaryCodes.indexOf(key.codes[0]));
            }
          } else if (useNumPad) {
            if (hangulPrimaryCodes.indexOf(key.codes[0]) != -1) {
              subkey = subPrimaryCodesNum.get(hangulPrimaryCodes.indexOf(key.codes[0]));
            } else if (englishPrimaryCodes.indexOf(key.codes[0]) != -1) {
              subkey = subPrimaryCodesNum.get(englishPrimaryCodes.indexOf(key.codes[0]));
            }
          } else {
            if (hangulPrimaryCodes.indexOf(key.codes[0]) != -1) {
              subkey = SubPrimaryCodes.get(hangulPrimaryCodes.indexOf(key.codes[0]));
            } else if (englishPrimaryCodes.indexOf(key.codes[0]) != -1) {
              subkey = SubPrimaryCodes.get(englishPrimaryCodes.indexOf(key.codes[0]));
            }
          }

          // draw 'ㅋㅋㅋ' when this key is pressed
          if (subkey == -9999997) {
            canvas.drawText("ㅋㅋㅋ", key.width * 12 / 16, key.height * 2 / 7, paint);
          } else {
            canvas.drawText(
                String.valueOf((char) subkey), key.width * 12 / 16, key.height * 2 / 7, paint);
          }
        }
      } else if (key.icon != null) {
        if (isPressedAndNotForMyMap) {
          if (isThisKeyCorrected && useYellowkey) {
            // This condition should check whether the key is corrected by nota or not.
            targetBg = keyBgNota;
          } else {
            targetBg = labelBgPressed;
          }
        } else {
          targetBg = labelBg;
        }

        NinePatchDrawable dr = (NinePatchDrawable) context.getResources().getDrawable(targetBg);
        dr.setBounds(0, 0, key.width, key.height);
        dr.draw(canvas);

        // Draw Icon
        if (layoutNum == Define.CHEONJIIN_LAYOUT
            || keyboardType == -1
            || (!isNOTAKey || !(key.codes[0] == NKeyboard.KEYCODE_SPACE))) {
          final int drawableX = (key.width - mPadding.left - mPadding.right
              - key.icon.getIntrinsicWidth()) / 2 + mPadding.left;
          final int drawableY = (key.height - mPadding.top - mPadding.bottom
              - key.icon.getIntrinsicHeight()) / 2 + mPadding.top;
          canvas.translate(drawableX, drawableY);

          key.icon.setBounds(0, 0, key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
          key.icon.draw(canvas);

          canvas.translate(-drawableX, -drawableY);
        } else {
          String frequentTypo = prefDBmanager.get(
              String.format("%s_%s", "frequentTypo", keyboardType));

          if (frequentTypo.isEmpty()) {
            frequentTypo = context.getString(R.string.no_typo_alert);
          } else {
            frequentTypo = String.format("%s: %s", context.getString(R.string.most_frequent_typo), frequentTypo);
          }

          paint.setColor(mLabelColor);
          canvas.drawText(frequentTypo,
              (key.width - mPadding.left - mPadding.right) / 2 + mPadding.left,
              (key.height - mPadding.top - mPadding.bottom) / 2
                  + (paint.getTextSize() - paint.descent()) / 2 + mPadding.top,
              paint);
        }
      }
      canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
    }
  }

  /* Should return boolean variable whether the key is set by theme function*/
  @SuppressWarnings("deprecation")
  private boolean theme1(Canvas canvas, Paint paint, NKey key, Rect mPadding,
                         int kbdPaddingLeft, int kbdPaddingTop, boolean isPressedAndNotForMyMap,
                         boolean isThisKeyCorrected) {
    if (key.codes[0] != NKeyboard.KEYCODE_DELETE)
      return false;

    int keyWidth = key.width;
    int keyHeight = key.height;

    if (isPressedAndNotForMyMap) {
      if (isThisKeyCorrected && useYellowkey) { // This condition should check whether the key is corrected by nota or not.
        paint.setColor(context.getResources().getColor(R.color.th1_backspace_shadow));
        canvas.drawCircle(keyWidth / 2, keyHeight * (1 / 2f - 1 / 50f), keyHeight * 5 / 13, paint);
        paint.setColor(context.getResources().getColor(R.color.th1_backspace_nota));
        canvas.drawCircle(keyWidth / 2, keyHeight * (1 / 2f + 1 / 50f), keyHeight * 5 / 13, paint);
      } else {
        paint.setColor(context.getResources().getColor(R.color.th1_backspace_shadow));
        canvas.drawCircle(keyWidth / 2, keyHeight * (1 / 2f - 1 / 50f), keyHeight * 5 / 13, paint);
        paint.setColor(context.getResources().getColor(R.color.th1_backspace_pressed));
        canvas.drawCircle(keyWidth / 2, keyHeight * (1 / 2f + 1 / 50f), keyHeight * 5 / 13, paint);
      }
    } else {
      paint.setColor(context.getResources().getColor(R.color.th1_backspace_shadow));
      canvas.drawCircle(keyWidth / 2, keyHeight * (1 / 2f + 1 / 50f), keyHeight * 5 / 13, paint);
      paint.setColor(context.getResources().getColor(R.color.default_blue));
      canvas.drawCircle(keyWidth / 2, keyHeight * (1 / 2f - 1 / 50f), keyHeight * 5 / 13, paint);
    }

    // Draw Icon
    final int drawableX = (keyWidth - mPadding.left - mPadding.right
        - key.icon.getIntrinsicWidth()) / 2 + mPadding.left;
    final int drawableY = (keyHeight - mPadding.top - mPadding.bottom
        - key.icon.getIntrinsicHeight()) / 2 + mPadding.top;
    canvas.translate(drawableX, drawableY);
    key.icon.setBounds(0, 0, key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
    key.icon.draw(canvas);
    canvas.translate(-drawableX, -drawableY);
    canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
    return true;
  }

  /**
   * 만일 쉬프트 키를 눌렀으면 모든 키를 대문자로.
   *
   * @param label key label
   * @param isCalledforMyMap called for drawing MyMap (key boundaries)
   * @return 대문자로 변경된 레이블
   */
  private CharSequence adjustCase(CharSequence label, boolean isCalledforMyMap) {
    if (!isCalledforMyMap) {
      if (keyboard.isShifted() && label != null && label.length() < 3
          && Character.isLowerCase(label.charAt(0))) {
        label = label.toString().toUpperCase();
      }
    }
    return label;
  }

  /**
   * 길게 눌렀을 때 효과를 처리하고 입력도 함께 처리해준다.
   *
   * @param key key instance
   * @return boolean of whether the long press happened
   */
  @Override
  protected boolean onLongPress(Key key) {
    return false;
    /*
    if (collectTouches) {
      // do not show any effect of long presses during performance test
      return false;
    }

    isLongPressed = true;
    if (handleLongPress(key)) {
      vibrateOrPlaySound();
      return true;
    } else {
      return super.onLongPress(key);
    }*/
  }

  /**
   * 길게 눌렀을 때 입력 처리
   *
   * @param key key instance
   * @return true if properly processed
   */
  protected boolean handleLongPress(Key key) {
    int subkey;
    if (layoutNum == Define.CHEONJIIN_LAYOUT) {
      if (cheonjiinPrimaryCodes.indexOf(key.codes[0]) != -1) {
        subkey = cheonjiinSubPrimaryCodes.get(
            cheonjiinPrimaryCodes.indexOf(key.codes[0]));
        getOnKeyboardActionListener().onKey(subkey, null);
        return true;
      }
    }

    if (key.codes[0] == 12619 ||
        (key.codes[0] == NKeyboard.KEYCODE_DOT
            && (xmlId == R.xml.numbers || xmlId == R.xml.numbers_symbol))) {
      return false;
    } else if (hangulPrimaryCodes.indexOf(key.codes[0]) != -1) {
      if (useNumPad) {
        subkey = subPrimaryCodesNum.get(hangulPrimaryCodes.indexOf(key.codes[0]));
      } else {
        subkey = SubPrimaryCodes.get(hangulPrimaryCodes.indexOf(key.codes[0]));
      }
    } else if (englishPrimaryCodes.indexOf(key.codes[0]) != -1) {
      if (useNumPad) {
        subkey = subPrimaryCodesNum.get(englishPrimaryCodes.indexOf(key.codes[0]));
      } else {
        subkey = SubPrimaryCodes.get(englishPrimaryCodes.indexOf(key.codes[0]));
      }
    } else if (key.codes[0] == NKeyboard.KEYCODE_NOTAKEY) {
      // if NotaKey long pressed
      handleNotaKeyLongPress(key);
      return true;
    } else {
      return false;
    }
    getOnKeyboardActionListener().onKey(subkey, null);
    return true;
  }

  //set popup for calling setting and statistics.
  private void handleNotaKeyLongPress(Key key) {
    CharSequence[] tempitems;
    if (Define.isTrainingNow) {
      tempitems = new CharSequence[]{context.getString(R.string.call_setting), context.getString(R.string.cannot_enter_due_to_train_alert)};
    } else {
      tempitems = new CharSequence[]{context.getString(R.string.call_setting), context.getString(R.string.call_statistics)};
    }

    final CharSequence[] items = tempitems;
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(context.getString(R.string.NOTAKeyboard));
    builder.setItems(items, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int item) {
        switch (item) {
          case (0):
            Intent callPerfereceIntent = new Intent(context, PreferencesActivity.class);
            callPerfereceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callPerfereceIntent);
            break;
        }
      }
    });
    AlertDialog alert = builder.create();
    alert.setCancelable(true);
    alert.setCanceledOnTouchOutside(true);
    Window mWindow = alert.getWindow();
    WindowManager.LayoutParams params = mWindow.getAttributes();
    params.token = this.getWindowToken();
    params.type = LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    mWindow.setAttributes(params);
    mWindow.addFlags(LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    alert.show();
  }

  @Override
  protected void swipeRight() {
  }

  @Override
  protected void swipeLeft() {
  }

  @Override
  protected void swipeUp() {
  }

  @Override
  protected void swipeDown() {
  }

  public void toggleIsNOTAKey() {
    isNOTAKey = !isNOTAKey;
  }

  /**
   * Vibrate or play sound upon different options.
   */
  private void vibrateOrPlaySound() {
    long vibrateTime = Define.VIBRATE_DEFAULT;
    if (NIMService.isVibrateOn) {
      vb.vibrate(vibrateTime);
    }
    // sound
    playTouchSound();
  }

  /**
   * Event listener that listens to a keyboard touch event.
   * @param me motion event that contains information about the motion
   * @return the result of touch event
   */
  @Override
  public boolean onTouchEvent(MotionEvent me) {
    // Not a registered keyboard
    int keyboardType = keyboard.getNKeyboardType(context);
    if (keyboardType == -1) {
      if (me.getAction() == MotionEvent.ACTION_DOWN) {
        vibrateOrPlaySound();
      }
      return super.onTouchEvent(me);
    }

    // get time and coordinates
    long touchEventTime = me.getEventTime();
    int touchX = (int) (me.getX() - getPaddingLeft());
    int touchY = (int) (me.getY() - getPaddingTop());
    float centerCoordinates[] = {touchX, touchY};
    float finalX = touchX;
    float finalY = touchY;
    int currentAction = me.getAction();
    int currentMetaState = me.getMetaState();
    int action = me.getAction();

    // send the touch event
    return super.onTouchEvent(MotionEvent.obtain(
        touchEventTime, touchEventTime, action, finalX, finalY, currentMetaState));
  }
}
