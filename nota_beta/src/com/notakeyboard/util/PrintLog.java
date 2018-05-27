package com.notakeyboard.util;

import android.util.Log;

/**
 * 로깅을 위한 Helper Class.
 */
public class PrintLog {
  private static final boolean DEBUG_MOD = true;
  private static final String TAG = "NOTA";

  @SuppressWarnings("rawtypes")
  public static void debug(Class c, Object obj) {
    if (DEBUG_MOD) {
      Log.d(TAG + "_" + c.getSimpleName(), obj.toString());
    }
  }

  @SuppressWarnings("rawtypes")
  public static void debug(Class c, Exception e) {
    if (DEBUG_MOD) {
      StackTraceElement[] trace = e.getStackTrace();
      Log.d(TAG + "_" + c.getSimpleName(), e.getClass().getName() + ":" + e.getMessage());
      for (StackTraceElement aTrace : trace) {
        Log.d(TAG + "_" + c.getSimpleName(), aTrace.toString());
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public static void error(Class c, Object obj) {
    if (DEBUG_MOD) {
      Log.e(TAG + "_" + c.getSimpleName(), obj.toString());
    }
  }

  @SuppressWarnings("rawtypes")
  public static void error(Class c, Exception e) {
    if (DEBUG_MOD) {
      StackTraceElement[] trace = e.getStackTrace();
      Log.e(TAG + "_" + c.getSimpleName(), e.getClass().getName() + ":" + e.getMessage());
      for (StackTraceElement aTrace : trace) {
        Log.e(TAG + "_" + c.getSimpleName(), aTrace.toString());
      }
    }
  }
}
