package com.android.music.utils;

import android.os.Build;

public class Utils {
	public static boolean isJB()
	{
		return Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN;
	}
}
