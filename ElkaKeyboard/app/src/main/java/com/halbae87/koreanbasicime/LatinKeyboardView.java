/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * halbae87: this project is created from Soft Keyboard Sample source
 */

package com.halbae87.koreanbasicime;

import android.content.Context;
// import android.inputmethodservice.Keyboard;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;

public class LatinKeyboardView extends KeyboardView {

    static public boolean isBlocked = false;
    static final int KEYCODE_OPTIONS = -100;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean onLongPress(Key key) {
    	/*
        if (key.codes[0] == Keyboard.KEYCODE_ALT) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
        */
        return super.onLongPress(key);
    }

    private final String TAG = "SoftKeyboard";

    @Override
    public void invalidateKey(int Keyindex)
    {
        if(isBlocked)
            invalidateAllKeys();
        else
            super.invalidateKey(Keyindex);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Log.v(TAG, "onDraw - canvas width : " + canvas.getClipBounds().width());
        //Log.v(TAG, "onDraw - canvas left : " + canvas.getClipBounds().left);
        //Log.v(TAG, "onDraw - canvas top : " + canvas.getClipBounds().top);
        if(isBlocked)
            drawLockScreen(canvas);
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
        canvas.drawText("Your keyboard is locked!", 130, canvasrect.centerY() + resized.getHeight()/2, paint);
    }
}
