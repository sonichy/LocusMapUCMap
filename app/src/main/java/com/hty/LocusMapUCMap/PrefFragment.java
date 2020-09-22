package com.hty.LocusMapUCMap;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	EditTextPreference EDP_upload_server;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		EDP_upload_server = (EditTextPreference) findPreference("upload_server");
		// Log.e("PrefFragment.java:18", EDP_upload_server + "");
	}

	void initTextSummary() {
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		String uploadServer = sharedPreferences.getString("uploadServer", "");
		Log.e("PrefFragment.java:24", uploadServer);
		if (uploadServer.equals("")) {
			EDP_upload_server.setSummary("http://sonichy.gearhostpreview.com/locusmap");
		} else {
			EDP_upload_server.setSummary(uploadServer);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		initTextSummary();
		Log.e("PrefFragment.java:35", key + "=" + sharedPreferences.getString(key, ""));
		EDP_upload_server.setSummary(sharedPreferences.getString(key, ""));
	}

	@Override
	public void onResume() {
		super.onResume();
		initTextSummary();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
}