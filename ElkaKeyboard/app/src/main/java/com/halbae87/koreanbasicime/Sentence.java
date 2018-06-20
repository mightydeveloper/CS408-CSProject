package com.halbae87.koreanbasicime;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

/**
 * Created by jaejun on 2018-04-30.
 */

public class Sentence extends Activity {
    CharSequence[] possibleTexts = {
            "햇빛이 선명하게 나뭇잎을 핥고 있었다",
            "옛날에 금잔디 동산에 메기같이",
            "링딩동 링딩동 링디기딩 디기딩딩동",
            "하늘을 우러러 한 점 부끄럼이 없기를",
            "동해물과 백두산이 마르고 닳도록",
            "일 더하기 일은 귀요미",
            "흔들리지 않고 피는 꽃이 어디 있으랴",
            "피카츄 라이츄 파이리 꼬부기",
            "으르렁 으르렁 으르렁 대",
            "개울가에 올챙이 한마리",
            "나는 너의 영원한 형제야",
            "너와 나의 연결고리",
            "하얗게 피어난 얼음 꽃 하나가",
            "손이가요 손이가 새우깡에 손이가",
            "빠빠라빠빠라빠 삐삐리빠삐코",
            "바람 불어와 내 맘 흔들면",
            "여보세요 나야 거기 잘 지내니",
            "앞뒤가 똑같은 전화번호",
            "자기의 일은 스스로 하자",
            "태평양을 건너 대서양을 건너",
    };
    TextView givenSentence;
    EditText userInput;
    Random random = new Random();
    CharSequence targetSentece;
    boolean isNormallyFinished = false;

    public CharSequence getRandomSentence() {
        int randomIndex = random.nextInt(possibleTexts.length);
        CharSequence randomText = possibleTexts[randomIndex];

        targetSentece = randomText;
        return randomText;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sentence);
        WindowManager.LayoutParams  layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags  = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount  = 0.7f;
        getWindow().setAttributes(layoutParams);
        //getWindow().getAttributes().width   = (int)(cGameManager.getInstance().getDisplayMetrics().widthPixels * 0.9);
        //getWindow().getAttributes().height  = (int)(cGameManager.getInstance().getDisplayMetrics().heightPixels * 0.4);
        getWindow().getAttributes().width = 900;
        getWindow().getAttributes().height = 900;

        givenSentence = (TextView) findViewById(R.id.givenSentence);
        userInput = (EditText) findViewById(R.id.userInput);

        userInput.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        givenSentence.setText(getRandomSentence());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(!isNormallyFinished)
            LatinKeyboardView.isBlocked = true;
    }

    public void clickCheck(View view) {
        //Toast.makeText(this, userInput.getText(), Toast.LENGTH_SHORT).show();
        String inputTxt = userInput.getText().toString();
        String givenTxt = targetSentece.toString();
        System.out.println(inputTxt);
        System.out.println(givenTxt);
        System.out.println(inputTxt.length());
        System.out.println(givenTxt.length());
        System.out.println(inputTxt.equals(givenTxt));
        if(inputTxt.equals(givenTxt)) {
            System.out.println("DONE");
            isNormallyFinished = true;
            finish();
        }
    }
}
