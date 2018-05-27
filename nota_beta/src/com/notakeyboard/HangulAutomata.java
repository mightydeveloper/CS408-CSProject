package com.notakeyboard;

import java.util.Arrays;

/**
 * 한글 오토마타
 */
public class HangulAutomata {
  // 초성 유니코드 목록
  private static final int[] PREF_CHO = {
      12593, 12594, 12596, 12599, 12600, 12601, 12609, 12610,
      12611, 12613, 12614, 12615, 12616, 12617, 12618, 12619,
      12620, 12621, 12622};

  // 중성 유니코드 목록
  private static final int[] PREF_JUNG = {
      12623, 12624, 12625, 12626, 12627, 12628, 12629, 12630,
      12631, 12632, 12633, 12634, 12635, 12636, 12637, 12638,
      12639, 12640, 12641, 12642, 12643};

  // 종성 유니코드 목록
  private static final int[] PREF_JONG = {
      12593, 12594, 12595, 12596, 12597, 12598, 12599, 12601,
      12602, 12603, 12604, 12605, 12606, 12607, 12608, 12609,
      12610, 12612, 12613, 12614, 12615, 12616, 12618, 12619,
      12620, 12621, 12622};

  private static final int HANGUL_START = -1;
  private static final int HANGUL_CHO = 0;  // 초성만 입력된 상태 : ㅂ
  private static final int HANGUL_JUNG = 1;  // 중성(모음)까지 입력된 상태 : ㅓ, 버
  private static final int HANGUL_JONG = 2;
  private static final int HANGUL_DJUNG = 3;
  private static final int HANGUL_DJONG = 4;
  private static final int HANGUL_FINISH1 = 5;
  private static final int HANGUL_FINISH2 = 6;

  private int mCurrentState = HANGUL_START;
  private int mHangulCharBuffer[] = new int[5];
  private int previousHangulCharBuffer[] = new int[5];
  private int mWorkingChar;

  private int automataType;
  private long jong_ssang_time;
  private long cho_ssang_time;
  private long jung_time;
  private int current_cho;
  private int current_jong;
  private int previous_jong;
  private boolean pull_jongseong;
  private boolean pull_djongseong;

  public static final int OUTPUT_CHAR_UNAVAILABLE = -1;  // 커서가 이 칸에 존재할 수 없음을 의미
  public static final int OUTPUT_CHAR_SET_COMPOSING = 0;  // 깜빡깜빡하면서 '작성중'임을 나타낸다

  //constructor
  public HangulAutomata(int automataType) {
    this.automataType = automataType;
    reset();
    mCurrentState = HANGUL_START;
  }

  /**
   * Resets the hangul character buffer and the state of automata.
   */
  public void reset() {
    Arrays.fill(mHangulCharBuffer, -1);
    mWorkingChar = -1;
  }

  public void reset_previous() {
    Arrays.fill(previousHangulCharBuffer, -1);
  }

  public void stateReset() {
    mCurrentState = HANGUL_START;
  }

  public int getBuffer() {
    return mWorkingChar;
  }

  public int getState() {
    return mCurrentState;
  }

  /**
   * 초성 인덱스
   * @param primaryCode keycode
   * @return chosung index
   */
  private int getChoseongIndex(int primaryCode) {
    for (int i = 0; i < PREF_CHO.length; i++) {
      if (primaryCode == PREF_CHO[i])
        return i;
    }
    return -1;
  }

  /**
   * 중성 인덱스
   * @param primaryCode keycode
   * @return jungsung index
   */
  private int getJungseongIndex(int primaryCode) {
    for (int i = 0; i < PREF_JUNG.length; i++) {
      if (primaryCode == PREF_JUNG[i])
        return i;
    }
    return -1;
  }

  /**
   * 종성 인덱스
   * @param primaryCode keycode
   * @return jongsung index
   */
  private int getJongseongIndex(int primaryCode) {
    for (int i = 0; i < PREF_JONG.length; i++) {
      if (primaryCode == PREF_JONG[i])
        return i;
    }
    return -1;
  }

  /**
   * 중성 1과 중성 2를 복중성으로 만들 수 있는지를 보고
   * 만들 수 있다면 그 결과를 뱉는다.
   * @param first 중성 1
   * @param second 중성 2
   * @return 복중성 또는 0 (복중성 생성 불가)
   */
  private int getJungseongPair(int first, int second) {
    switch (first) {
      case 12631: // ㅗ
        switch (second) {
          case 12623: // ㅏ
            return 12632;  // ㅘ
          case 12624: // ㅐ
            return 12633;  // ㅙ
          case 12643: // ㅣ
            return 12634;  // ㅚ
          default:
            return 0;
        }
      case 12636: //Woo
        switch (second) {
          case 12627: //Uh
            return 12637;
          case 12628: //EH
            return 12638;
          case 12643: //EE
            return 12639;
          default:
            return 0;
        }
      case 12641: //EU
        switch (second) {
          case 12643: //EE
            return 12642;
          default:
            return 0;
        }
      default:
        return 0;
    }
  }

  /**
   * 종성 1과 종성 2를 합쳐 복종성으로 만들 수 있는지를 보고,
   * 만들 수 있다면 그 결과를 뱉어낸다.
   * @param first 종성 1
   * @param second 종성 2
   * @return 복종성 또는 0 (복종성 생성 불가)
   */
  private int getJongseongPair(int first, int second) {
    switch (first) {
      case 12593: // ㄱ
        switch (second) {
          case 12613: // ㄲ
            return 12595;
          default:
            break;
        }
        break;
      case 12596: //s
        switch (second) {
          case 12616: //q
            return 12597;
          case 12622: //g
            return 12598;
          default:
            break;
        }
        break;
      case 12601: //f
        switch (second) {
          case 12593: //r
            return 12602;
          case 12609: //a
            return 12603;
          case 12610: //q
            return 12604;
          case 12613: //t
            return 12605;
          case 12620: //x
            return 12606;
          case 12621: //v
            return 12607;
          case 12622: //g
            return 12608;
          default:
            break;
        }
        break;
      case 12610: //q
        switch (second) {
          case 12613: //t
            return 12612;
          default:
            break;
        }
        break;
      default:
        break;
    }
    return 0;
  }

  private int getChoseongPair(int v) {
    switch (v) {
      case 12610:
        return 12611;
      case 12616:
        return 12617;
      case 12599:
        return 12600;
      case 12593:
        return 12594;
      case 12613:
        return 12614;
      default:
        return v;
    }
  }

  private int getJungseongPair(int v) {
    switch (v) {
      case 12624:
        return 12626;
      case 12628:
        return 12630;
      case 12627:
        return 12629;
      case 12623:
        return 12625;
      case 12636:
        return 12640;
      case 12631:
        return 12635;
      default:
        return v;
    }
  }

  private int getJongseongPair(int v) {
    switch (v) {
      case 12593:
        return 12594;
      case 12613:
        return 12614;
      default:
        return v;
    }
  }

  private int getNextCho(int v) {
    switch (v) {
      case 12593: // ㄱ
        return 12619;
      case 12594: // ㄲ
        return 12593;
      case 12596: // ㄴ
        return 12601;
      case 12599: // ㄷ
        return 12620;
      case 12600: // ㄸ
        return 12599;
      case 12601: // ㄹ
        return 12596;
      case 12609: // ㅁ
        return 12615;
      case 12610: // ㅂ
        return 12621;
      case 12611: // ㅃ
        return 12610;
      case 12613: // ㅅ
        return 12622;
      case 12614: // ㅆ
        return 12613;
      case 12615: // ㅇ
        return 12609;
      case 12616: // ㅈ
        return 12618;
      case 12617: // ㅉ
        return 12616;
      case 12618: // ㅊ
        return 12617;
      case 12619: // ㅋ
        return 12594;
      case 12620: // ㅌ
        return 12600;
      case 12621: // ㅍ
        return 12611;
      case 12622: // ㅎ
        return 12614;
      default:
        return v;
    }
  }

  private int getCheonjiinJungseong(int v1, int v2) {
    switch (v1) {
      case 12643:
        switch (v2) {
          case 12643:
            return 0;
          case 12685:
            return 12623;
          case 12641:
            return 0;
        }
      case 12685:
        switch (v2) {
          case 12643:
            return 12627;
          case 12685:
            return 65306;
          case 12641:
            return 12631;
        }
      case 12641:
        switch (v2) {
          case 12643:
            return 12642;
          case 12685:
            return 12636;
          case 12641:
            return 0;
        }
      case 12623:
        switch (v2) {
          case 12643:
            return 12624;
          case 12685:
            return 12625;
          case 12641:
            return 0;
        }
      case 12627: // ㅓ
        switch (v2) {
          case 12643: // ㅣ
            return 12628;
          case 12685: // .
            return 0;
          case 12641: // ㅡ
            return 0;
        }
      case 65306: // :
        switch (v2) {
          case 12643: // ㅣ
            return 12629;
          case 12685: // .
            return 12685;
          case 12641: // ㅡ
            return 12635;
        }
      case 12631: // ㅗ
        switch (v2) {
          case 12643: // ㅣ
            return 12634;
          case 12685: // .
            return 0;
          case 12641: // ㅡ
            return 0;
        }
      case 12642: // ㅢ
        switch (v2) {
          case 12643: // ㅣ
            return 0;
          case 12685: // .
            return -1;
          case 12641: // ㅡ
            return 0;
        }
      case 12636: // ㅜ
        switch (v2) {
          case 12643: // ㅣ
            return 12639;
          case 12685: // .
            return 12640;
          case 12641: // ㅡ
            return 0;
        }
      case 12624: // ㅐ
        return 0;
      case 12625: // ㅑ
        switch (v2) {
          case 12643: // ㅣ
            return 12626;
          case 12685: // .
            return 12623;
          case 12641: // ㅡ
            return 0;
        }
      case 12628: // ㅔ
        return 0;
      case 12629: // ㅕ
        switch (v2) {
          case 12643: // ㅣ
            return 12630;
          case 12685: // .
            return 0;
          case 12641: // ㅡ
            return 0;
        }
      case 12635: // ㅛ
        return 0;
      case 12634: // ㅚ
        switch (v2) {
          case 12643: // ㅣ
            return 0;
          case 12685: // .
            return 12632;
          case 12641: // ㅡ
            return 0;
        }
      case 12639: // ㅟ
        switch (v2) {
          case 12643: // ㅣ
            return 0;
          case 12685: // .
            return -1;
          case 12641: // ㅡ
            return 0;
        }
      case 12640: // ㅠ
        switch (v2) {
          case 12643: // ㅣ
            return 12637;
          case 12685: // .
            return 12636;
          case 12641: // ㅡ
            return 0;
        }
      case 12626: // ㅒ
        return 0;
      case 12630: // ㅖ
        return 0;
      case 12632: // ㅘ
        switch (v2) {
          case 12643: // ㅣ
            return 12633;
          case 12685: // .
            return -1;
          case 12641: // ㅡ
            return 0;
        }
      case 12637: // ㅝ
        switch (v2) {
          case 12643: // ㅣ
            return 12638;
          case 12685: // .
            return 0;
          case 12641: // ㅡ
            return 0;
        }
      case 12633: // ㅙ
        return 0;
      case 12638: // ㅞ
        return 0;
      default:
        return 0;
    }
  }

  private boolean isDJung(int v) {
    return v == 12632 || v == 12633 || v == 12634 || v == 12637
        || v == 12638 || v == 12639 || v == 12642;
  }

  /**
   * 한글 조합하기
   * Creates an hangul character (in unicode) from the buffer
   * that contains chosung, jungsung, and jongsung information.
   *
   * The buffer must have length 5 and have the shape of:
   * hangulBuffer = {chosung, jungsung1, jungsung2, jongsung1, jongsung2}
   * @param hangulBuffer hangul buffer
   * @return unicode of hangul character
   */
  private int makeHangul(int[] hangulBuffer) {
    if (hangulBuffer.length != 5) {
      return -1;
    }

    int[] buffer = new int[3];

    int jungTemp = getJungseongPair(hangulBuffer[1], hangulBuffer[2]);
    int jongTemp = getJongseongPair(hangulBuffer[3], hangulBuffer[4]);

    buffer[0] = hangulBuffer[0];

    if (jungTemp != 0) {
      buffer[1] = jungTemp;
    } else {
      buffer[1] = hangulBuffer[1];
    }

    if (jongTemp != 0) {
      buffer[2] = jongTemp;
    } else {
      buffer[2] = hangulBuffer[3];
    }

    if (buffer[0] == -1 && buffer[1] != -1) {
      return buffer[1];
    } else if (buffer[0] != -1 && buffer[1] == -1 && buffer[2] == -1) {
      return buffer[0];
    } else if (buffer[0] == -1 && buffer[2] == -1) {
      return -1;
    } else {
      return 0xAC00 + getChoseongIndex(buffer[0]) * 588 + getJungseongIndex(buffer[1]) * 28 + ((-1 != buffer[2]) ? (getJongseongIndex(buffer[2]) + 1) : 0);
    }
  }

  /**
   * Character input으로부터 output character를 유추한다. 산정 방식은 오토마타 종류에 따라 다르다.
   * @param inputChar input character
   * @return output character
   */
  public int[] hangulAutomata(int inputChar) {
    int[] outputChar;

    switch (automataType) {
      case Define.CHEONJIIN_AUTOMATA:
        outputChar = cheonjiinAutomata(inputChar);
        break;
      case Define.DANMOEUM_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA:
        // 쌍자음 더블클릭으로 입력 on이고 단모음 키보드 사용 시의 오토마타
      case Define.QWERTY_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA:
        // 쌍자음 더블클릭으로 입력 on이고 한글쿼티 키보드 사용 시의 오토마타
        outputChar = danmoeumSsangjaeumDoubleClickAutomata(inputChar);
        break;
      case Define.DANMOEUM_SSANGJAEUM_LONG_TOUCH_AUTOMATA:
        // 쌍자음 더블클릭으로 입력 off이고 단모음 키보드 사용 시의 오토마타 (default이면 이것으로 설정)
      case Define.DEFAULT_QWERTY_AUTOMATA:
        // 일반 한글쿼티 오토마타 (default)
      default:
        outputChar = defaultQwertyAutomata(inputChar);
        break;
    }

    return outputChar;
  }

  /**
   * 일반 한글쿼리 오토마타
   * 또는 쌍자음 더블클릭으로 입력 off이고 단모음 키보드 사용 시의 오토마타 (default이면 이것으로 설정)
   * @param inputChar input character
   * @return output characters
   */
  private int[] defaultQwertyAutomata(int inputChar) {
    int[] outputChar = new int[2];

    // 오토마타의 현재 상태 (state)에 따라 다른 처리
    switch (mCurrentState) {
      case HANGUL_START:  // 시작
        if (getChoseongIndex(inputChar) != -1) {
          // 입력 문자가 초성일 경우
          mCurrentState = HANGUL_CHO;
          mHangulCharBuffer[0] = inputChar;
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
        } else if (getJungseongIndex(inputChar) != -1) {
          // 입력 문자가 중성일 경우
          mCurrentState = HANGUL_JUNG;
          mHangulCharBuffer[1] = inputChar;
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
        } else {
          // 초성도 중성도 아닌 경우 올바른 한글 입력이 아니다.
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_UNAVAILABLE;
        }
        break;
      case HANGUL_CHO:  // 초성만 있는 상태. ex) ㅂ
        if (getChoseongIndex(inputChar) != -1) {
          // 초성 입력
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_CHO;
          outputChar[0] = mHangulCharBuffer[0];  // 이전 초성
          outputChar[1] = inputChar;  // 입력 초성
          reset();
          mHangulCharBuffer[0] = inputChar;  // 새로 작성
        } else if (getJungseongIndex(inputChar) != -1) {
          // 중성 입력
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_JUNG;
          mHangulCharBuffer[1] = inputChar;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
        } else {
          // 종성 입력
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_UNAVAILABLE;
        }
        break;
      case HANGUL_JUNG:  // 중성까지 입력된 상태 : ㅓ 또는 버
        if (getJongseongIndex(inputChar) != -1) {
          // 종성 입력
          if (mHangulCharBuffer[0] != -1) {
            mCurrentState = HANGUL_JONG;
            mHangulCharBuffer[3] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
          } else {
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else if (getChoseongIndex(inputChar) != -1) {
          // 초성 입력
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_CHO;
          outputChar[0] = makeHangul(mHangulCharBuffer);  // 현재까지 입력한걸 만들어서 뱉고
          outputChar[1] = inputChar;  // 새로 시작
          reset();
          mHangulCharBuffer[0] = inputChar;  // 버퍼에서도 새로 시작
        } else if (getJungseongIndex(inputChar) != -1) {
          // 중성 입력
          if (getJungseongPair(mHangulCharBuffer[1], inputChar) != 0) {
            // 복중성 가능
            mCurrentState = HANGUL_DJUNG;
            mHangulCharBuffer[2] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
          } else {
            // 중성 하나씩 따로따로
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_JUNG;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;  // 따로따로
            reset();
            mHangulCharBuffer[1] = inputChar;
          }
        } else {
          // 중성도 종성도 아닌 문자가 들어오면
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_UNAVAILABLE;
        }
        break;
      case HANGUL_JONG:  // 종성까지 입력된 경우
        if (getChoseongIndex(inputChar) != -1) {
          // 초성 입력
          if (getJongseongPair(mHangulCharBuffer[3], inputChar) != 0) {
            // 종성 1과 pair가 가능하다면
            mCurrentState = HANGUL_DJONG;
            mHangulCharBuffer[4] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
          } else {
            // pair만들기 실패
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();  // buffer reset
            mHangulCharBuffer[0] = inputChar;
          }
        } else if (getJungseongIndex(inputChar) != -1) {
          // 중성 입력
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;

          // 첫 종성을 떼어낸 후 들어온 중성과 합친다
          int temp = mHangulCharBuffer[3];
          mHangulCharBuffer[3] = -1;
          outputChar[0] = makeHangul(mHangulCharBuffer);  // 떼어넨 결과
          reset();
          mHangulCharBuffer[0] = temp;
          mHangulCharBuffer[1] = inputChar;
          outputChar[1] = makeHangul(mHangulCharBuffer);  // 새로 합친 결과
        } else {
          stateReset();
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_UNAVAILABLE;
        }
        break;
      case HANGUL_DJUNG:  // 겹중성
        if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_JUNG;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          reset();
          mHangulCharBuffer[1] = inputChar;
        } else if (getChoseongIndex(inputChar) != -1) {
          if (getJongseongIndex(inputChar) != -1) {
            if (mHangulCharBuffer[0] != -1) {
              mCurrentState = HANGUL_JONG;
              mHangulCharBuffer[3] = inputChar;
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = OUTPUT_CHAR_SET_COMPOSING;
            } else {
              mCurrentState = HANGUL_FINISH1;
              mCurrentState = HANGUL_CHO;
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = inputChar;
              reset();
              mHangulCharBuffer[0] = inputChar;
            }
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_UNAVAILABLE;
        }
        break;
      case HANGUL_DJONG:  // 겹종성
        if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;
          int temp = mHangulCharBuffer[4];
          mHangulCharBuffer[4] = -1;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          reset();
          mHangulCharBuffer[0] = temp;
          mHangulCharBuffer[1] = inputChar;
          outputChar[1] = makeHangul(mHangulCharBuffer);
        } else if (getChoseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_CHO;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          reset();
          mHangulCharBuffer[0] = inputChar;
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = OUTPUT_CHAR_UNAVAILABLE;
        }
        break;
      case HANGUL_FINISH1:
        break;
      case HANGUL_FINISH2:
        break;
      default:
        break;
    }

    return outputChar;
  }

  /**
   * 쌍자음 더블클릭으로 입력 on이고 단모음 키보드 사용 시의 오토마타
   * 또는 쌍자음 더블클릭으로 입력 on이고 한글쿼티 키보드 사용 시의 오토마타
   * @param inputChar input character
   * @return output characters
   */
  private int[] danmoeumSsangjaeumDoubleClickAutomata(int inputChar) {
    int[] outputChar = new int[2];

    switch (mCurrentState) {
      case HANGUL_START:
        if (getChoseongIndex(inputChar) != -1) {
          cho_ssang_time = System.currentTimeMillis();
          mCurrentState = HANGUL_CHO;
          mHangulCharBuffer[0] = inputChar;
          outputChar[0] = inputChar;
          outputChar[1] = 0;
        } else if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_JUNG;
          mHangulCharBuffer[1] = inputChar;
          outputChar[0] = inputChar;
          outputChar[1] = 0;
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_CHO:
        if (getChoseongIndex(inputChar) != -1) {
          if (isChoPairable(inputChar) &&
              mHangulCharBuffer[0] == inputChar &&
              (System.currentTimeMillis() - cho_ssang_time) < 250) {
            mCurrentState = HANGUL_CHO;
            cho_ssang_time = -250;
            mHangulCharBuffer[0] = getChoseongPair(inputChar);
            outputChar[0] = mHangulCharBuffer[0];
            outputChar[1] = 0;
          } else {
            cho_ssang_time = System.currentTimeMillis();
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = mHangulCharBuffer[0];
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else if (getJungseongIndex(inputChar) != -1) {
          jung_time = System.currentTimeMillis();
          mCurrentState = HANGUL_JUNG;
          cho_ssang_time = -250;
          mHangulCharBuffer[1] = inputChar;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = 0;
        } else {
          mCurrentState = HANGUL_START;
          cho_ssang_time = -250;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_JUNG:
        if (getJongseongIndex(inputChar) != -1) {
          if (mHangulCharBuffer[0] != -1) {
            jung_time = System.currentTimeMillis() - jung_time;
            jong_ssang_time = System.currentTimeMillis();
            mCurrentState = HANGUL_JONG;
            mHangulCharBuffer[3] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = 0;
          } else {
            cho_ssang_time = System.currentTimeMillis();
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else if (getChoseongIndex(inputChar) != -1) {
          cho_ssang_time = System.currentTimeMillis();
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_CHO;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          reset();
          mHangulCharBuffer[0] = inputChar;
        } else if (getJungseongIndex(inputChar) != -1) {
          if (getJungseongPair(mHangulCharBuffer[1], inputChar) != 0) {
            mCurrentState = HANGUL_DJUNG;
            mHangulCharBuffer[2] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = 0;
          } else if (isJungseongPairable(inputChar) && mHangulCharBuffer[1] == inputChar) {
            mCurrentState = HANGUL_JUNG;
            mHangulCharBuffer[1] = getJungseongPair(inputChar);
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = 0;
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_JUNG;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[1] = inputChar;
          }
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_JONG:
        if (getChoseongIndex(inputChar) != -1) {
          if (getJongseongPair(mHangulCharBuffer[3], inputChar) != 0) {
            mCurrentState = HANGUL_DJONG;
            jong_ssang_time = System.currentTimeMillis();
            mHangulCharBuffer[4] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = 0;
          } else if (isJongseongPairable(inputChar) &&
              mHangulCharBuffer[3] == inputChar &&
              (System.currentTimeMillis() - jong_ssang_time) < 250) {
            mCurrentState = HANGUL_JONG;
            mHangulCharBuffer[3] = getJongseongPair(inputChar);
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = 0;
            jong_ssang_time = -250;
          } else if (isChoPairable(inputChar) &&
              mHangulCharBuffer[3] == inputChar &&
              (System.currentTimeMillis() - jong_ssang_time) < 250) {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            mHangulCharBuffer[3] = -1;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar + 1;  // 쌍자음으로 변환
            reset();
            mHangulCharBuffer[0] = inputChar + 1;  // 쌍자음으로 변환
            jong_ssang_time = -250;
          } else {
            cho_ssang_time = System.currentTimeMillis();
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
            jong_ssang_time = -250;
          }
        } else if (getJungseongIndex(inputChar) != -1) {
          if (isSeparableWord(makeHangul(mHangulCharBuffer), inputChar) && jung_time < 250) {
            mCurrentState = HANGUL_FINISH2;
            mCurrentState = HANGUL_JUNG;
            if (mHangulCharBuffer[3] == 12594) {
              mHangulCharBuffer[3] = 12593;
            } else if (mHangulCharBuffer[3] == 12614) {
              mHangulCharBuffer[3] = 12613;
            }
            int temp = mHangulCharBuffer[3];
            outputChar[0] = makeHangul(mHangulCharBuffer);
            reset();
            mHangulCharBuffer[0] = temp;
            mHangulCharBuffer[1] = inputChar;
            outputChar[1] = makeHangul(mHangulCharBuffer);
          } else {
            mCurrentState = HANGUL_FINISH2;
            mCurrentState = HANGUL_JUNG;
            int temp = mHangulCharBuffer[3];
            mHangulCharBuffer[3] = -1;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            reset();
            mHangulCharBuffer[0] = temp;
            mHangulCharBuffer[1] = inputChar;
            outputChar[1] = makeHangul(mHangulCharBuffer);
          }
          jong_ssang_time = -250;
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
          jong_ssang_time = -250;
        }
        break;
      case HANGUL_DJUNG:
        if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_JUNG;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          reset();
          mHangulCharBuffer[1] = inputChar;
        } else if (getChoseongIndex(inputChar) != -1) {
          if (getJongseongIndex(inputChar) != -1) {
            if (mHangulCharBuffer[0] != -1) {
              jong_ssang_time = System.currentTimeMillis();
              mCurrentState = HANGUL_JONG;
              mHangulCharBuffer[3] = inputChar;
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = 0;
            } else {
              cho_ssang_time = System.currentTimeMillis();
              mCurrentState = HANGUL_FINISH1;
              mCurrentState = HANGUL_CHO;
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = inputChar;
              reset();
              mHangulCharBuffer[0] = inputChar;
            }
          } else {
            cho_ssang_time = System.currentTimeMillis();
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_DJONG:
        if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;
          int temp = mHangulCharBuffer[4];
          mHangulCharBuffer[4] = -1;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          reset();
          mHangulCharBuffer[0] = temp;
          mHangulCharBuffer[1] = inputChar;
          outputChar[1] = makeHangul(mHangulCharBuffer);
        } else if (getChoseongIndex(inputChar) != -1) {
          if (isChoPairable(inputChar) &&
              mHangulCharBuffer[4] == inputChar &&
              (System.currentTimeMillis() - jong_ssang_time) < 250) {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            mHangulCharBuffer[4] = -1;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = getChoseongPair(inputChar);
            reset();
            mHangulCharBuffer[0] = getChoseongPair(inputChar);
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
            cho_ssang_time = System.currentTimeMillis();
          }
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        jong_ssang_time = -250;
        break;
      case HANGUL_FINISH1:
        break;
      case HANGUL_FINISH2:
        break;
      default:
        break;
    }

    return outputChar;
  }

  /**
   * 천지인 오토마타
   * @param inputChar input character
   * @return output characters
   */
  private int[] cheonjiinAutomata(int inputChar) {
    int[] outputChar = new int[2];

    switch (mCurrentState) {
      case HANGUL_START:
        reset_previous();
        current_cho = -1;
        current_jong = -1;
        if (getChoseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_CHO;
          current_cho = inputChar;
          mHangulCharBuffer[0] = inputChar;
          outputChar[0] = inputChar;
          outputChar[1] = 0;
        } else if (getJungseongIndex(inputChar) != -1 || inputChar == 12685) {
          mCurrentState = HANGUL_JUNG;
          mHangulCharBuffer[1] = inputChar;
          outputChar[0] = inputChar;
          outputChar[1] = 0;
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_CHO:
        if (getChoseongIndex(inputChar) != -1) {
          if (current_cho == inputChar) {
            if (current_cho == current_jong &&
                ((previousHangulCharBuffer[3] == -1 && getJongseongIndex(getNextCho(mHangulCharBuffer[0])) != -1) || (previousHangulCharBuffer[3] != -1 && getJongseongPair(previousHangulCharBuffer[3], getNextCho(mHangulCharBuffer[0])) != 0))) {
              current_jong = inputChar;
              if (previousHangulCharBuffer[3] == -1) {
                previous_jong = inputChar;
                mCurrentState = HANGUL_JONG;
                mHangulCharBuffer[3] = getNextCho(mHangulCharBuffer[0]);
              } else {
                mCurrentState = HANGUL_DJONG;
                mHangulCharBuffer[3] = previousHangulCharBuffer[3];
                mHangulCharBuffer[4] = getNextCho(mHangulCharBuffer[0]);
              }
              mHangulCharBuffer[0] = previousHangulCharBuffer[0];
              mHangulCharBuffer[1] = previousHangulCharBuffer[1];
              outputChar[0] = 0;
              outputChar[1] = makeHangul(mHangulCharBuffer);
            } else {
              mCurrentState = HANGUL_CHO;
              current_cho = inputChar;
              mHangulCharBuffer[0] = getNextCho(mHangulCharBuffer[0]);
              outputChar[0] = mHangulCharBuffer[0];
              outputChar[1] = 0;
            }
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            current_cho = inputChar;
            outputChar[0] = mHangulCharBuffer[0];
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else if (inputChar == 12685) {
          mCurrentState = HANGUL_JUNG;
          reset_previous();
          outputChar[0] = mHangulCharBuffer[0];
          outputChar[1] = 12685;
          previousHangulCharBuffer[0] = mHangulCharBuffer[0];
          reset();
          mHangulCharBuffer[1] = 12685;
        } else if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_JUNG;
          mHangulCharBuffer[1] = inputChar;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = 0;
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_JUNG:
      case HANGUL_DJUNG:
        if (getJongseongIndex(inputChar) != -1) {
          if (mHangulCharBuffer[0] != -1) {
            mCurrentState = HANGUL_JONG;
            previous_jong = inputChar;
            current_jong = inputChar;
            mHangulCharBuffer[3] = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = 0;
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            current_cho = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
          }
        } else if (getChoseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH1;
          mCurrentState = HANGUL_CHO;
          current_cho = inputChar;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          reset();
          mHangulCharBuffer[0] = inputChar;
        } else if (getJungseongIndex(inputChar) != -1 || inputChar == 12685) {
          if (getCheonjiinJungseong(mHangulCharBuffer[1], inputChar) == -1) {
            mCurrentState = HANGUL_JUNG;
            switch (mHangulCharBuffer[1]) {
              case 12642:
                mHangulCharBuffer[1] = 12641;
                outputChar[0] = makeHangul(mHangulCharBuffer);
                outputChar[1] = 12623;
                reset();
                mHangulCharBuffer[1] = 12623;
                break;
              case 12639:
                mHangulCharBuffer[1] = 12636;
                outputChar[0] = makeHangul(mHangulCharBuffer);
                outputChar[1] = 12623;
                reset();
                mHangulCharBuffer[1] = 12623;
                break;
              case 12632:
                mHangulCharBuffer[1] = 12631;
                outputChar[0] = makeHangul(mHangulCharBuffer);
                outputChar[1] = 12623;
                reset();
                mHangulCharBuffer[1] = 12625;
                break;
              default:
                outputChar[0] = makeHangul(mHangulCharBuffer);
                outputChar[1] = inputChar;
                reset();
                mHangulCharBuffer[1] = inputChar;
                break;
            }
          } else if (getCheonjiinJungseong(mHangulCharBuffer[1], inputChar) != 0) {
            mCurrentState = HANGUL_JUNG;
            if ((mHangulCharBuffer[1] == 12685 || mHangulCharBuffer[1] == 65306)) {
              if (previousHangulCharBuffer[0] != -1 && previousHangulCharBuffer[3] == -1) {
                if (inputChar == 12685) {
                  mHangulCharBuffer[1] = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);
                  outputChar[0] = mHangulCharBuffer[1];
                  outputChar[1] = 0;
                } else {
                  mHangulCharBuffer[0] = previousHangulCharBuffer[0];
                  previousHangulCharBuffer[0] = -1;
                  mHangulCharBuffer[1] = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);
                  outputChar[0] = 0;
                  outputChar[1] = makeHangul(mHangulCharBuffer);
                }
              } else {
                if (inputChar == 12685) {
                  mHangulCharBuffer[1] = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);
                  outputChar[0] = mHangulCharBuffer[1];
                  outputChar[1] = 0;
                } else if (pull_jongseong) {
                  int temp_jung = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);

                  reset();
                  mHangulCharBuffer[0] = previousHangulCharBuffer[0];
                  mHangulCharBuffer[1] = previousHangulCharBuffer[1];
                  outputChar[0] = makeHangul(mHangulCharBuffer);
                  reset();
                  mHangulCharBuffer[0] = previousHangulCharBuffer[3];
                  mHangulCharBuffer[1] = temp_jung;
                  outputChar[1] = makeHangul(mHangulCharBuffer) + 100000000;
                } else if (pull_djongseong) {
                  int temp_jung = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);

                  reset();
                  mHangulCharBuffer[0] = previousHangulCharBuffer[0];
                  mHangulCharBuffer[1] = previousHangulCharBuffer[1];
                  mHangulCharBuffer[3] = previousHangulCharBuffer[3];
                  outputChar[0] = makeHangul(mHangulCharBuffer);
                  reset();
                  mHangulCharBuffer[0] = previousHangulCharBuffer[4];
                  mHangulCharBuffer[1] = temp_jung;
                  outputChar[1] = makeHangul(mHangulCharBuffer) + 100000000;
                } else {
                  mHangulCharBuffer[1] = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);
                  outputChar[0] = makeHangul(mHangulCharBuffer);
                  outputChar[1] = 0;
                }
              }
            } else {
              mHangulCharBuffer[1] = getCheonjiinJungseong(mHangulCharBuffer[1], inputChar);
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = 0;
            }
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_JUNG;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[1] = inputChar;
          }
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }

        if (!(mHangulCharBuffer[1] == 65306 && mHangulCharBuffer[0] == -1)) {
          pull_jongseong = false;
          pull_djongseong = false;
        }
        break;
      case HANGUL_JONG:
        if (getChoseongIndex(inputChar) != -1) {
          if (current_jong == inputChar) {
            if (getJongseongIndex(getNextCho(mHangulCharBuffer[3])) != -1) {
              mCurrentState = HANGUL_JONG;
              previous_jong = inputChar;
              current_jong = inputChar;
              mHangulCharBuffer[3] = getNextCho(mHangulCharBuffer[3]);
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = 0;
            } else {
              mCurrentState = HANGUL_FINISH1;
              mCurrentState = HANGUL_CHO;
              current_cho = inputChar;
              int temp_jong = getNextCho(mHangulCharBuffer[3]);
              mHangulCharBuffer[3] = -1;
              previousHangulCharBuffer[0] = mHangulCharBuffer[0];
              previousHangulCharBuffer[1] = mHangulCharBuffer[1];
              previousHangulCharBuffer[3] = mHangulCharBuffer[3];
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = temp_jong;
              reset();
              mHangulCharBuffer[0] = temp_jong;
            }
          } else {
            if (getJongseongPair(mHangulCharBuffer[3], inputChar) != 0) {
              mCurrentState = HANGUL_DJONG;
              current_jong = inputChar;
              mHangulCharBuffer[4] = inputChar;
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = 0;
            } else {
              mCurrentState = HANGUL_FINISH1;
              mCurrentState = HANGUL_CHO;
              current_cho = inputChar;
              if (getJongseongPair(mHangulCharBuffer[3], getNextCho(inputChar)) != 0)
                current_jong = inputChar;
              previousHangulCharBuffer[0] = mHangulCharBuffer[0];
              previousHangulCharBuffer[1] = mHangulCharBuffer[1];
              previousHangulCharBuffer[3] = mHangulCharBuffer[3];
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = inputChar;
              reset();
              mHangulCharBuffer[0] = inputChar;
            }
          }
        } else if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;
          int temp = mHangulCharBuffer[3];
          mHangulCharBuffer[3] = -1;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          reset();
          mHangulCharBuffer[0] = temp;
          mHangulCharBuffer[1] = inputChar;
          outputChar[1] = makeHangul(mHangulCharBuffer);
        } else if (inputChar == 12685) {
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          previousHangulCharBuffer[0] = mHangulCharBuffer[0];
          previousHangulCharBuffer[1] = mHangulCharBuffer[1];
          previousHangulCharBuffer[3] = mHangulCharBuffer[3];
          reset();
          mHangulCharBuffer[1] = inputChar;
          pull_jongseong = true;
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        break;
      case HANGUL_DJONG:
        if (getJungseongIndex(inputChar) != -1) {
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;
          int temp = mHangulCharBuffer[4];
          mHangulCharBuffer[4] = -1;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          reset();
          mHangulCharBuffer[0] = temp;
          mHangulCharBuffer[1] = inputChar;
          outputChar[1] = makeHangul(mHangulCharBuffer);
        } else if (inputChar == 12685) {
          mCurrentState = HANGUL_FINISH2;
          mCurrentState = HANGUL_JUNG;
          outputChar[0] = makeHangul(mHangulCharBuffer);
          outputChar[1] = inputChar;
          previousHangulCharBuffer[0] = mHangulCharBuffer[0];
          previousHangulCharBuffer[1] = mHangulCharBuffer[1];
          previousHangulCharBuffer[3] = mHangulCharBuffer[3];
          previousHangulCharBuffer[4] = mHangulCharBuffer[4];
          reset();
          mHangulCharBuffer[1] = inputChar;
          pull_djongseong = true;
        } else if (getChoseongIndex(inputChar) != -1) {
          if (current_jong == inputChar) {
            if (getJongseongPair(mHangulCharBuffer[3], getNextCho(mHangulCharBuffer[4])) != 0) {
              mCurrentState = HANGUL_DJONG;
              current_jong = inputChar;
              mHangulCharBuffer[4] = getNextCho(mHangulCharBuffer[4]);
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = 0;
            } else {
              mCurrentState = HANGUL_FINISH1;
              mCurrentState = HANGUL_CHO;
              current_cho = inputChar;
              int temp_jong = getNextCho(mHangulCharBuffer[4]);
              mHangulCharBuffer[4] = -1;
              previousHangulCharBuffer[0] = mHangulCharBuffer[0];
              previousHangulCharBuffer[1] = mHangulCharBuffer[1];
              previousHangulCharBuffer[3] = mHangulCharBuffer[3];
              outputChar[0] = makeHangul(mHangulCharBuffer);
              outputChar[1] = temp_jong;
              reset();
              mHangulCharBuffer[0] = temp_jong;
            }
          } else {
            mCurrentState = HANGUL_FINISH1;
            mCurrentState = HANGUL_CHO;
            current_cho = inputChar;
            outputChar[0] = makeHangul(mHangulCharBuffer);
            outputChar[1] = inputChar;
            reset();
            mHangulCharBuffer[0] = inputChar;
            cho_ssang_time = System.currentTimeMillis();
          }
        } else {
          mCurrentState = HANGUL_START;
          reset();
          outputChar[0] = inputChar;
          outputChar[1] = -1;
        }
        jong_ssang_time = -250;
        break;
      case HANGUL_FINISH1:
        break;
      case HANGUL_FINISH2:
        break;
      default:
        break;
    }

    return outputChar;
  }

  /**
   * 삭제
   * Handle character delete.
   * @return output character
   */
  public int delete() {
    int outputChar;

    switch (mCurrentState) {
      case HANGUL_START:
        outputChar = -1;  // cannot delete
        reset();
        break;
      case HANGUL_CHO:
        mCurrentState = HANGUL_START;
        outputChar = 0;  // empty space
        reset();
        break;
      case HANGUL_JUNG:
        if (mHangulCharBuffer[0] == -1) {
          // 단어 전체 삭제
          mCurrentState = HANGUL_START;
          outputChar = 0;
          reset();
        } else {
          mCurrentState = HANGUL_CHO;
          if (mHangulCharBuffer[1] == 12685 || mHangulCharBuffer[1] == 65306) {
            outputChar = 0;
            mHangulCharBuffer[1] = -1;
          } else {
            outputChar = mHangulCharBuffer[0];
            mHangulCharBuffer[1] = -1;
          }
        }
        break;
      case HANGUL_DJUNG:
        mCurrentState = HANGUL_JUNG;
        if (mHangulCharBuffer[0] == -1) {
          outputChar = mHangulCharBuffer[1];  // 쌍모음 첫번째
          mHangulCharBuffer[2] = -1;  // 쌍모음 두번째만 제거한다
        } else {
          mHangulCharBuffer[2] = -1;
          outputChar = makeHangul(mHangulCharBuffer);  // 자음까지 존재하면 재합성한다
        }
        break;
      case HANGUL_JONG:
        if (mHangulCharBuffer[2] == -1) {
          mCurrentState = HANGUL_JUNG;
        } else {
          mCurrentState = HANGUL_DJUNG;
        }
        mHangulCharBuffer[3] = -1;
        outputChar = makeHangul(mHangulCharBuffer);
        break;
      case HANGUL_DJONG:
        mCurrentState = HANGUL_JONG;
        mHangulCharBuffer[4] = -1;
        current_jong = previous_jong;
        outputChar = makeHangul(mHangulCharBuffer);
        break;
      default:
        mCurrentState = HANGUL_START;
        outputChar = -1;
        reset();
        break;
    }
    return outputChar;
  }

  /**
   * 한글 유니코드를 받고 분해한다.
   * Decompose a unicode character into chosung, jungsung, and jongsung keycodes.
   * @param unicodeOfChar unicode
   * @param decomposeCompoundJamo whether or not to decompose compound jamo (ㅘ, ㄹㅂ) as well
   * @return int[][] {{chosung}, {jungsung, jungsung_secondary}, {jongsung, jongsung_secondary}}
   */
  public static int[][] decomposeHangul(int unicodeOfChar, boolean decomposeCompoundJamo) {
    int[][] chojungjong = new int[3][2];
    for (int[] part : chojungjong) {
      Arrays.fill(part, -1);
    }

    if (isHangul(unicodeOfChar)) {
      int choseongindex = (((unicodeOfChar - 0xAC00) - (unicodeOfChar - 0xAC00) % 28) / 28) / 21;
      int jungseongindex = (((unicodeOfChar - 0xAC00) - (unicodeOfChar - 0xAC00) % 28) / 28) % 21;
      int jongseongindex = (unicodeOfChar - 0xAC00) % 28 - 1;

      // default values are -1
      int cho = -1;
      int jung = -1;
      int jungSecondary = -1;
      int jong = -1;
      int jongSecondary = -1;

      if (choseongindex >= 0) {
        cho = PREF_CHO[choseongindex];
      }

      if (jungseongindex >= 0) {
        jung = PREF_JUNG[jungseongindex];
      }

      if (jongseongindex >= 0) {
        jong = PREF_JONG[jongseongindex];
      }

      if (decomposeCompoundJamo) {
        // decompose compound jungsung 복중성 분해
        switch (jung) {
          case 12632:  // ㅘ
            jung = 'ㅗ';
            jungSecondary = 'ㅏ';
            break;
          case 12633:  // ㅙ
            jung = 'ㅗ';
            jungSecondary = 'ㅐ';
            break;
          case 12634:  // ㅚ
            jung = 'ㅗ';
            jungSecondary = 'ㅣ';
            break;
          case 12637:  // ㅝ
            jung = 'ㅜ';
            jungSecondary = 'ㅓ';
            break;
          case 12638:  // ㅞ
            jung = 'ㅜ';
            jungSecondary = 'ㅔ';
            break;
          case 12639: // ㅟ
            jung = 'ㅜ';
            jungSecondary = 'ㅣ';
            break;
          case 12642:  // ㅢ
            jung = 'ㅡ';
            jungSecondary = 'ㅣ';
            break;
        }

        // decompose compount jongsung 종성 복자음 분해
        switch (jong) {
          case 12595:  // ㄱㅅ
            jong = 'ㄱ';
            jongSecondary = 'ㅅ';
            break;
          case 12597:  // ㄴㅈ
            jong = 'ㄴ';
            jongSecondary = 'ㅈ';
            break;
          case 12598:  // ㄴㅎ
            jong = 'ㄴ';
            jongSecondary = 'ㅎ';
            break;
          case 12602:  // ㄹㄱ
            jong = 'ㄹ';
            jongSecondary = 'ㄱ';
            break;
          case 12603: // ㄹㅁ
            jong = 'ㄹ';
            jongSecondary = 'ㅁ';
            break;
          case 12604:  // ㄹㅂ
            jong = 'ㄹ';
            jongSecondary = 'ㅂ';
            break;
          case 12605:  // ㄹㅅ
            jong = 'ㄹ';
            jongSecondary = 'ㅅ';
            break;
          case 12606:  // ㄹㅌ
            jong = 'ㄹ';
            jongSecondary = 'ㅌ';
            break;
          case 12607:  // ㄹㅍ
            jong = 'ㄹ';
            jongSecondary = 'ㅍ';
            break;
          case 12608:  // ㄹㅎ
            jong = 'ㄹ';
            jongSecondary = 'ㅎ';
            break;
          case 12612:  // ㅂㅅ
            jong = 'ㅂ';
            jongSecondary = 'ㅅ';
            break;
        }
      }

      chojungjong[0][0] = cho;
      chojungjong[1][0] = jung;
      chojungjong[1][1] = jungSecondary;
      chojungjong[2][0] = jong;
      chojungjong[2][1] = jongSecondary;
    } else {
      chojungjong[0][0] = unicodeOfChar;
    }

    return chojungjong;
  }

  /**
   * 한글인가 아닌가
   * Determines whether a unicode is Korean.
   * @param unicode unicode
   * @return true if unicode is korean.
   */
  public static boolean isHangul(int unicode) {
    return (44032 <= unicode && unicode <= 55215);
  }

  private boolean isChoPairable(int unicode) {
    return unicode == 12610 || unicode == 12616 || unicode == 12599
        || unicode == 12593 || unicode == 12613;

  }

  private boolean isJungseongPairable(int unicode) {
    if (!(automataType == Define.QWERTY_SSANGJAEUM_DOUBLE_CLICK_AUTOMATA)) {
      if (unicode == 12627 || unicode == 12623 || unicode == 12636 || unicode == 12631)
        return true;
    }
    return unicode == 'ㅐ' || unicode == 'ㅔ';

  }

  private boolean isJongseongPairable(int unicode) {
    return unicode == 'ㄱ' || unicode == 'ㅅ';
  }

  private boolean isSeparableWord(int previousChar, int inputChar) {
    switch (previousChar) {
      case 54618:
        if (inputChar == 'ㅗ' || inputChar == 'ㅣ') {
          return true;
        }
        break;
      case 47674:
        if (inputChar == 12631) return true;
        else if (inputChar == 12643) return true;
        break;
      case 49438:
        if (inputChar == 12631) return true;
        else if (inputChar == 12643) return true;
        else if (inputChar == 12623) return true;
        break;
      case 52844:
        if (inputChar == 12631) return true;
        break;
      case 50978:
        if (inputChar == 12631) return true;
        break;
      case 51202:
        if (inputChar == 12641) return true;
        break;
      default:
    }

    return false;
  }
}