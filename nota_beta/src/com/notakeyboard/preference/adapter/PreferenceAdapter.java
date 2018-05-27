package com.notakeyboard.preference.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.notakeyboard.Define;
import com.notakeyboard.R;
import com.notakeyboard.db.PreferenceDB;
import com.notakeyboard.obj.Pref;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class PreferenceAdapter extends BaseAdapter {
	private Context context;
	
	private ArrayList<Pref> data;
	private LayoutInflater layoutInflater;
	
	private String lang;
	
	public PreferenceAdapter(Context context, ArrayList<Pref> data) {
		this.context = context;

		// retrieve language settings
		lang = new PreferenceDB(context).get("lang");
		if (lang.isEmpty()) {
			lang = Locale.getDefault().getLanguage();
		}

		this.data = data;
		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	private class ViewHolder {
		TextView category;
		
		View preference;
		TextView title, summary;
		TextView info;
		CheckBox checkBox;
	}
	
	@Override
	public int getCount() { return data.size(); }
	
	@Override
	public Pref getItem(int index) { return data.get(index); }
	
	@Override
	public long getItemId(int index) { return index; }
	
	@Override
	public View getView(final int index, View view, ViewGroup parent) {
		ViewHolder holder;
		if (view == null) {
			view = layoutInflater.inflate(R.layout.preference_adapter, parent, false);
			
			holder = new ViewHolder();
			holder.category = (TextView)view.findViewById(R.id.categoryTextView);
			
			holder.preference = view.findViewById(R.id.preferenceLayout);
			holder.title = (TextView)view.findViewById(R.id.titleTextView);
			holder.summary = (TextView)view.findViewById(R.id.summaryTextView);
			holder.info = (TextView)view.findViewById(R.id.infoTextView);
			holder.checkBox = (CheckBox)view.findViewById(R.id.prefCheckBox);
			
			view.setTag(holder);
		} else {
			holder = (ViewHolder)view.getTag();
		}
		
		Pref pref = data.get(index);
		
		if ( pref.getType() != null ) {
			holder.category.setVisibility(View.GONE);
			holder.preference.setVisibility(View.VISIBLE);
			
			setEvent(pref, holder);
		} else {	// header
			holder.category.setVisibility(View.VISIBLE);
			holder.preference.setVisibility(View.GONE);
			
			holder.category.setText(context.getString(pref.getCategory()));
		}
		
		return view;
	}
	
	private void setEvent(final Pref pref, final ViewHolder holder) {
		holder.title.setText(context.getString(pref.getTitle()));
		holder.summary.setText(context.getString(pref.getSummary()));
		
		holder.title.setEnabled(true);
		holder.summary.setEnabled(true);
		holder.info.setEnabled(true);
		
		final PreferenceDB db = new PreferenceDB(context);
		
		holder.info.setVisibility(View.GONE);
		holder.checkBox.setVisibility(View.GONE);
		
		if ( pref.getType().contains("CheckBox") ) {
			holder.checkBox.setVisibility(View.VISIBLE);
			
			boolean isChecked = !db.get(pref.getKey()).equals("false");
			
			if ( pref.getKey().equals("yellow_key") ) isChecked = db.get(pref.getKey()).equals("true");
			
			if ( pref.getKey().equals("double_touch_shift") ) {
				if ( lang.contains("ko") ) {
					final PreferenceDB prefDB = new PreferenceDB(context);
					
					isChecked = db.get(pref.getKey()).equals("true");
					
					if ( prefDB.get("layout").equals(String.valueOf(Define.DANMOEUM_LAYOUT)) || prefDB.get("layout").equals(String.valueOf(Define.CHEONJIIN_LAYOUT))) {
						holder.title.setEnabled(false);
						holder.summary.setEnabled(false);
						holder.info.setEnabled(false);
						holder.checkBox.setEnabled(false);
						isChecked = false;
					} else {
						if(db.get(pref.getKey()).isEmpty()) isChecked = false;
					}
				} else {
					holder.preference.setVisibility(View.GONE);
				}
			}
			
			holder.checkBox.setChecked(isChecked);
		} else if ( pref.getKey().equals("train_time") ) {
			holder.info.setVisibility(View.VISIBLE);
			
			final PreferenceDB prefDB = new PreferenceDB(context);
			String hour = prefDB.get("train_time_hour");
			String min = prefDB.get("train_time_minute");
			
			if (hour.isEmpty()) {
				hour = "4";
			}

			if (min.isEmpty()) {
				min = "0";
			}
		
			holder.info.setText(makeTimeFormat(hour, min));
			
			if ( prefDB.get("auto_train").equals("false") ) {
				holder.title.setEnabled(false);
				holder.summary.setEnabled(false);
				holder.info.setEnabled(false);
			}
		}
		
		if ( pref.getKey().equals("select_layout") && !lang.contains("ko") ) {
			holder.preference.setVisibility(View.GONE);
		}
	}
	
	private String makeTimeFormat(String hour, String min) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
		cal.set(Calendar.MINUTE, Integer.parseInt(min));
		
		String AM_PM = cal.get(Calendar.AM_PM) == 0 ? "AM" : "PM";
		String minute = cal.get(Calendar.MINUTE) < 10 ? "0"+Integer.toString(cal.get(Calendar.MINUTE)) : Integer.toString(cal.get(Calendar.MINUTE));
		
		return String.format(Locale.US, "%s %d:%s", AM_PM, cal.get(Calendar.HOUR), minute);
	}
}
