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

public class NAlertDialog extends Dialog {
	private String mTitle;
	private String mContent;
	private String mStrButton;
	private android.view.View.OnClickListener mLeftClickListener;
	private TextView mTitleView;
	private TextView mContentView;
    
	@SuppressLint("InflateParams")
	@SuppressWarnings("static-access")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		WindowManager.LayoutParams lpWinMngr = new WindowManager.LayoutParams();
		lpWinMngr.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
    // 0: completely transparent background. 1: completely blind background
		lpWinMngr.dimAmount = 0.8f;
		lpWinMngr.height = lpWinMngr.WRAP_CONTENT;
		lpWinMngr.width  = lpWinMngr.WRAP_CONTENT;
		
		getWindow().setAttributes(lpWinMngr);
		
		LayoutInflater inflater = getLayoutInflater();
		View alertLayout = inflater.inflate(R.layout.alert, null);
		setContentView(alertLayout);
		
		setLayout();
	}
	
	public NAlertDialog(Context context) {
		super(context,android.R.style.Theme_Translucent_NoTitleBar);
	}
	
	public NAlertDialog(Context context, String title, String content, String button, View.OnClickListener singleListener) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		
		mTitle = title;
		mContent = content;
		mStrButton = button;
		this.setClickListener(singleListener); 
	}
	
	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}
	
	public void setContent(String mContent) {
		this.mContent = mContent;
	}
	
	public void setStrButton(String mStrButton) {
		this.mStrButton = mStrButton;
	}
	
	private void setClickListener(View.OnClickListener single) {
		if (single != null) {
			mLeftClickListener = single;
    }
	}
	
    private void setLayout() {
      mTitleView = (TextView)findViewById(R.id.alert_set_title);
      mTitleView.setText(mTitle);

      mContentView = (TextView)findViewById(R.id.alert_set_text1);
      mContentView.setText(mContent);

			Button mButton = (Button) findViewById(R.id.left);
      mButton.setText(mStrButton);
      mButton.setOnClickListener(mLeftClickListener);
    }
}
