package com.notakeyboard.util;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.view.View;

public class Util {
	@SuppressWarnings("deprecation")
	public static void setBackgroundDrawable(final View view, final Drawable drawable) {
		if ( drawable == null ) return;
		
		try {
			if ( Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN ) {
				view.setBackground(drawable);
			}
			else {
				view.setBackgroundDrawable(drawable);
			}
		} catch ( Exception e ) {
			PrintLog.error(Util.class, e);
		}
	}
}
