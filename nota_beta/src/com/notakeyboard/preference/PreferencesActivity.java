package com.notakeyboard.preference;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import com.notakeyboard.R;
import com.notakeyboard.Define;
import com.notakeyboard.util.PrintLog;
import com.notakeyboard.db.BaseDB;
import com.notakeyboard.db.PreferenceDB;
import com.notakeyboard.keyboard.NIMService;
import com.notakeyboard.obj.Pref;
import com.notakeyboard.preference.adapter.PreferenceAdapter;
import com.notakeyboard.util.NAlertDialogWithCancel;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

public class PreferencesActivity extends Activity {
  private PreferenceDB prefDB;
  private ArrayList<Pref> data;
  private ListView listView;
  private PreferenceAdapter preferenceAdapter;
  private NAlertDialogWithCancel mCustomDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.only_listview);

    init();
    bindUIComponents();
    bindListeners();
  }

  private void init() {
    prefDB = new PreferenceDB(this);

    parsePreferenceXml();
  }

  /**
   * parse the list of setting selections and add them to list
   * the list will be used for binding listeners for each setting buttons
   */
  private void parsePreferenceXml() {
    data = new ArrayList<>();

    XmlPullParser parser = getResources().getXml(R.xml.prefs);
    try {
      while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (parser.getEventType() == XmlPullParser.START_TAG) {
          String name = parser.getName();

          if (name.contains("Screen")) {
            parser.next();
            continue;
          }

          if (name.contains("Category")) {
            data.add(new Pref(parser.getAttributeValue(0)));
          } else if (name.contains("CheckBox")) {
            data.add(new Pref(name, parser.getAttributeValue(1), parser.getAttributeValue(0), parser.getAttributeValue(2)));
          } else if (name.contains("Preference")) {
            data.add(new Pref(name, parser.getAttributeValue(2), parser.getAttributeValue(0), parser.getAttributeValue(3)));
          }
        }
        parser.next();
      }
    } catch (Exception e) {
      PrintLog.error(PreferencesActivity.class, e);
    }
  }

  private void bindUIComponents() {
    listView = (ListView) findViewById(R.id.listView);

    preferenceAdapter = new PreferenceAdapter(this, data);
    listView.setAdapter(preferenceAdapter);
  }

  /**
   * Bind listeners for all preference buttons.
   * Some may start a new activity while some may trigger characteristics of the keyboard.
   */
  private void bindListeners() {
    // bind to each setting options a listener that makes changes to settings of this app
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View v, int index, long id) {
        Pref pref = data.get(index);

        if (pref.getType() == null) {
          return;
        }

        // clickable buttons
        switch (pref.getKey()) {
          case "select_layout":
            if (!isTrainingNow()) {
              resetAlert(R.string.keyboard_layout_warning);
            }
            break;
          case "key_height_pref":
            if (!isTrainingNow()) {
              resetAlert(R.string.keyboard_outfit_warning);
            }
            break;
          case "train_time":
            setTrainTime();
            break;
          case "map_reset":
            if (!isTrainingNow()) {
              resetAlert(R.string.data_reset_warning);
            }
            break;
          case "select_language":
            if (!isTrainingNow()) {
              resetAlert(R.string.select_language_warning);
            }
            break;
        }

        // check box
        if (pref.getType().contains("CheckBox")) {
          boolean isChecked = prefDB.get(pref.getKey()).equals("false");

          switch (pref.getKey()) {
            case "vibrate":
              NIMService.isVibrateOn = isChecked;
              break;
            case "sound":
              NIMService.isSoundOn = isChecked;
              break;
            case "double_touch_shift":
              break;
            case "yellow_key":
              isChecked = !prefDB.get(pref.getKey()).equals("true");
              break;
          }

          prefDB.put(pref.getKey(), String.valueOf(isChecked));
          preferenceAdapter.notifyDataSetChanged();
        }
      }
    });
  }

  /**
   * Notifies training status.
   *
   * @return true if training is in progress.
   */
  private boolean isTrainingNow() {
    if (Define.isTrainingNow) {
      Toast.makeText(PreferencesActivity.this, R.string.cannot_enter_due_to_train_alert,
          Toast.LENGTH_SHORT).show();
      return true;
    }

    return false;
  }

  private void resetAlert(final int alertString) {
    mCustomDialog = new NAlertDialogWithCancel(PreferencesActivity.this,
        PreferencesActivity.this.getString(R.string.notice),
        PreferencesActivity.this.getString(alertString),
        PreferencesActivity.this.getString(R.string.cancel),
        PreferencesActivity.this.getString(R.string.confirm),
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mCustomDialog.dismiss();
          }
        },
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (alertString == R.string.keyboard_outfit_warning) {

            } else if (alertString == R.string.keyboard_layout_warning) {

            } else if (alertString == R.string.data_reset_warning) {
              resetMap();
            } else if (alertString == R.string.select_language_warning) {

            }
            mCustomDialog.dismiss();
          }
        });
    mCustomDialog.setCanceledOnTouchOutside(true);
    mCustomDialog.show();
  }

  private void resetMap() {
    System.out.println("==============RESETMAP===========");
    if (Define.isTrainingNow) {
      Toast.makeText(this, R.string.already_train_now_alert, Toast.LENGTH_SHORT).show();
      return;
    }

    new Thread(new Runnable() {
      @Override
      public void run() {
        Define.isTrainingNow = true;

        BaseDB db = new BaseDB(PreferencesActivity.this);
        db.getWritableDatabase();
        db.resetForKeySpecChange();

        PreferenceDB prefDB = new PreferenceDB(PreferencesActivity.this);
        for (int i = 0; i < Define.NUM_OF_NKEYBOARD_TYPE; i++) {
          prefDB.put(Define.SPEC_DB_KEY(i), "");
        }

        Define.isTrainingNow = false;
      }
    }).start();

    Toast.makeText(this, R.string.map_reset_successfully, Toast.LENGTH_SHORT).show();
  }

  /**
   * Sets the keyboard training time.
   */
  private void setTrainTime() {
    String hour = prefDB.get("train_time_hour");
    String min = prefDB.get("train_time_minute");

    if (hour.isEmpty()) {
      hour = "4";
    }
    if (min.isEmpty()) {
      min = "0";
    }

    new TimePickerDialog(PreferencesActivity.this, new TimePickerDialog.OnTimeSetListener() {
      @Override
      public void onTimeSet(TimePicker view, int hour, int min) {
        prefDB.put("train_time_hour", Integer.toString(hour));
        prefDB.put("train_time_minute", Integer.toString(min));

        preferenceAdapter.notifyDataSetInvalidated();
      }
    }, Integer.parseInt(hour), Integer.parseInt(min), false).show();
  }
}
