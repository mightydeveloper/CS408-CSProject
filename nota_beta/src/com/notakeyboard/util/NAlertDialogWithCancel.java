package com.notakeyboard.util;

import com.notakeyboard.R;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class NAlertDialogWithCancel extends Dialog {
  private String mTitle;
  private String mContent;
  private String mLeftButton;
  private String mRightButton;
  private android.view.View.OnClickListener mLeftClickListener;
  private android.view.View.OnClickListener mRightClickListener;
  private Button mLButton;
  private Button mRButton;
  private TextView mTitleView;
  private TextView mContentView;

  @SuppressLint("InflateParams")
  @SuppressWarnings("static-access")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WindowManager.LayoutParams lpWinMngr = new WindowManager.LayoutParams();
    lpWinMngr.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
    lpWinMngr.dimAmount = 0.8f; // 0: completely transparent background. 1: completely blind background
    lpWinMngr.height = lpWinMngr.WRAP_CONTENT;
    lpWinMngr.width = lpWinMngr.WRAP_CONTENT;

    getWindow().setAttributes(lpWinMngr);

    LayoutInflater inflater = getLayoutInflater();
    View alertLayout = inflater.inflate(R.layout.alert_with_cancel, null);
    setContentView(alertLayout);
    setLayout();
  }

  public NAlertDialogWithCancel(Context context, String title, String content, String strLButton,
                                String strRButton, View.OnClickListener leftListener,
                                View.OnClickListener rightListener) {
    super(context, android.R.style.Theme_Translucent_NoTitleBar);

    mTitle = title;
    mContent = content;
    mLeftButton = strLButton;
    mRightButton = strRButton;
    this.setClickListener(leftListener, rightListener);
  }

  public void setTitle(String mTitle) {
    this.mTitle = mTitle;
  }

  public void setContent(String mContent) {
    this.mContent = mContent;
  }

  private void setClickListener(View.OnClickListener left, View.OnClickListener right) {
    if (left != null && right != null) {
      mLeftClickListener = left;
      mRightClickListener = right;
    }
  }

  private void setLayout() {
    mTitleView = (TextView) findViewById(R.id.alert_set_title);
    mTitleView.setText(mTitle);

    mContentView = (TextView) findViewById(R.id.alert_set_text1);
    mContentView.setText(mContent);

    mLButton = (Button) findViewById(R.id.left);
    mLButton.setText(mLeftButton);
    mLButton.setOnClickListener(mLeftClickListener);

    mRButton = (Button) findViewById(R.id.right);
    mRButton.setText(mRightButton);
    mRButton.setOnClickListener(mRightClickListener);
  }
}
