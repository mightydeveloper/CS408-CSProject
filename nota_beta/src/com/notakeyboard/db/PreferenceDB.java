package com.notakeyboard.db;

import com.notakeyboard.util.PrintLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This is a key-value pair database where each key represents
 * a user preference of different type.
 */
public class PreferenceDB extends BaseDB {
  public PreferenceDB(Context context) {
    super(context);
  }

  /**
   * Insert a key-value pair for preference into the database.
   * @param key key
   * @param value value
   */
  public void put(String key, String value) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();

      if (value == null) {
        value = "";
      }

      String where = "key = '" + key + "'";
      Cursor cursor = db.query(PREFERENCES_TABLE, new String[]{"key"}, where, null, null, null, null);

      ContentValues values = new ContentValues();
      if (cursor.getCount() > 0) {
        values.put("value", value);
        db.update(PREFERENCES_TABLE, values, where, null);
      } else {
        values.put("key", key);
        values.put("value", value);
        db.insert(PREFERENCES_TABLE, null, values);
      }

      cursor.close();
    } catch (Exception e) {
      PrintLog.error(PreferenceDB.class, e);
    }

    db.setTransactionSuccessful();
    db.endTransaction();
    db.close();
  }

  /**
   * Retrieve the value of key.
   * @param key key
   * @return the value in string
   */
  public String get(String key) {
    String result = "";
    SQLiteDatabase db = this.getReadableDatabase();
    try {
      db.beginTransaction();

      String where = "key = '" + key + "'";
      Cursor cursor = db.query(PREFERENCES_TABLE, new String[]{"value"}, where, null, null, null, null);
      if (cursor.getCount() > 0) {
        cursor.moveToFirst();

        String value;
        if (cursor.getString(0) != null) {
          value = cursor.getString(0).trim();
          if (!value.equals("")) {
            result = value;
          }
        }
      }

      cursor.close();
    } catch (Exception e) {
      PrintLog.error(PreferenceDB.class, e);
    }

    db.setTransactionSuccessful();
    db.endTransaction();
    db.close();
    return result;
  }
}
