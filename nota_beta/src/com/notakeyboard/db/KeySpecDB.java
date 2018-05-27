package com.notakeyboard.db;

import java.util.ArrayList;
import java.util.List;

import com.notakeyboard.Define;
import com.notakeyboard.util.PrintLog;
import com.notakeyboard.keyboard.NKeyboard;
import com.notakeyboard.keyboard.NKeyboard.NKey;
import com.notakeyboard.obj.KeySpec;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class KeySpecDB extends BaseDB {
  private Context context;

  public KeySpecDB(Context context) {
    super(context);
    this.context = context;
  }

  public void insert(NKeyboard keyboard, int type) {
    try {
      this.getWritableDatabase();
      ContentValues values = new ContentValues();
      List<NKey> Keys = keyboard.getNKeys();
      for (NKey key : Keys) {
        values.put("type", type);
        values.put("keycode", key.codes[0]);
        values.put("x", key.x);
        values.put("y", key.y);
        values.put("h", key.height);
        values.put("w", key.width);

        db.insert(KEY_SPEC_TABLE, null, values);
      }

      new PreferenceDB(context).put(Define.SPEC_DB_KEY(type), Long.toString(System.currentTimeMillis()));
    } catch (Exception e) {
      PrintLog.error(KeySpecDB.class, e);
    } finally {
      this.close();
    }
  }

  public ArrayList<KeySpec> getKeySpecs(int type) {
    ArrayList<KeySpec> result = new ArrayList<>();

    try {
      this.getReadableDatabase();

      String where = "type = '" + type + "'";
      Cursor cursor = db.query(KEY_SPEC_TABLE, new String[]{"keycode, x, y, h, w"}, where, null, null, null, null);
      if (cursor.getCount() > 0) {
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
          result.add(new KeySpec(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4)));
          cursor.moveToNext();
        }
      }
      cursor.close();
    } catch (Exception e) {
      PrintLog.error(KeySpecDB.class, e);
    } finally {
      this.close();
    }
    return result;
  }
}
