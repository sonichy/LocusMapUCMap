package com.hty.LocusMapUCMap;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;

public class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	EditTextPreference EDP_upload_server;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		EDP_upload_server = (EditTextPreference) findPreference("upload_server");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("upload_server"))
			EDP_upload_server.setSummary(sharedPreferences.getString(key, "http://sonichy.gearhostpreview.com/locusmap/add.php"));
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		String upload_server = sharedPreferences.getString("uploadServer", "http://sonichy.gearhostpreview.com/locusmap/add.php");
		EDP_upload_server.setSummary(upload_server);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

}