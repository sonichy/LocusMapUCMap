package com.hty.LocusMapUCMap;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MenuActivity extends Activity {
	TextView textView_copyright;
	Button button_map, button_history, button_server, button_set, button_about, button_quit, button_nomap;
	int i = 0;
	String upload_server = "";
	SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		Log.e(Thread.currentThread().getStackTrace()[2] + "", "MenuActivity.onCreate()");
		MainApplication.getInstance().addActivity(this);
		// MainApplication.setwfn("");

		textView_copyright = (TextView) findViewById(R.id.textView_copyright);
		Calendar date = Calendar.getInstance();
		textView_copyright.setText("© Copyright 2018-" + date.get(Calendar.YEAR) + " 海天鹰版权所有");

		button_nomap = (Button) findViewById(R.id.button_nomap);
		button_map = (Button) findViewById(R.id.button_map);
		button_history = (Button) findViewById(R.id.button_history);
		button_server = (Button) findViewById(R.id.button_server);
		button_set = (Button) findViewById(R.id.button_set);
		button_about = (Button) findViewById(R.id.button_about);
		button_quit = (Button) findViewById(R.id.button_quit);

		button_nomap.setOnClickListener(new ButtonListener());
		button_map.setOnClickListener(new ButtonListener());
		button_history.setOnClickListener(new ButtonListener());
		button_server.setOnClickListener(new ButtonListener());
		button_set.setOnClickListener(new ButtonListener());
		button_about.setOnClickListener(new ButtonListener());
		button_quit.setOnClickListener(new ButtonListener());

		// int width = 370;
		// button_nomap.getLayoutParams().width = width;
		// button_map.getLayoutParams().width = width;
		// button_history.getLayoutParams().width = width;
		// button_server.getLayoutParams().width = width;
		// button_set.getLayoutParams().width = width;
		// button_about.getLayoutParams().width = width;
		// button_quit.getLayoutParams().width = width;

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_nomap:
				startActivity(new Intent(MenuActivity.this, NoMapActivity.class));
				break;
			case R.id.button_map:
				startActivity(new Intent(MenuActivity.this, CurrentLocus.class));
				break;
			case R.id.button_history:
				startActivity(new Intent(MenuActivity.this, GPXListActivity.class));
				break;
			case R.id.button_server:
				upload_server = sharedPreferences.getString("uploadServer", "http://sonichy.gearhostpreview.com/locusmap/add.php");
				String surl = upload_server.substring(0, upload_server.lastIndexOf("/"));
				Intent intent = new Intent();
				intent.setData(Uri.parse(surl));
				intent.setAction(Intent.ACTION_VIEW);
				MenuActivity.this.startActivity(intent);
				break;
			case R.id.button_set:
				startActivity(new Intent(MenuActivity.this, SettingActivity.class));
				break;
			case R.id.button_about:
				new AlertDialog.Builder(MenuActivity.this)
						.setIcon(R.drawable.ic_launcher)
						.setTitle("轨迹地图UCMap版  V1.4")
						.setMessage("利用UCMap提供的地图、定位、绘图和手机的GPS功能绘制、记录位移轨迹，查看记录的轨迹，合并轨迹，上传GPS数据到服务器。\n作者：海天鹰\nE-mail：sonichy@163.com\nQQ：84429027\n源码：https://github.com/sonichy/LocusMapUCMap")
						.setPositiveButton("确定", null).show();
				break;
			case R.id.button_quit:
				MainApplication.getInstance().exit();
				break;
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "结束");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int item_id = item.getItemId();
		switch (item_id) {
		case 0:
			Log.e("MainApplication", "exit()");
			MainApplication.getInstance().exit();
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		String mode = MainApplication.getMode();
		Drawable drawable = getResources().getDrawable(R.drawable.record);
		drawable.setBounds(0, 0, 80, 80);// 这一步必须要做,否则不会显示.
		if (mode.equals("")) {
			button_nomap.setEnabled(true);
			button_nomap.setCompoundDrawables(null, null, null, null);
			button_map.setEnabled(true);
			button_map.setCompoundDrawables(null, null, null, null);
		} else if (mode.equals("nomap")) {
			button_nomap.setEnabled(true);
			button_map.setEnabled(false);
			button_nomap.setCompoundDrawables(drawable, null, null, null);// 左
			// button_nomap.setCompoundDrawables(null, null, drawable, null);
		} else if (mode.equals("map")) {
			button_nomap.setEnabled(false);
			button_map.setEnabled(true);
			button_map.setCompoundDrawables(drawable, null, null, null);
			// button_map.setCompoundDrawables(null, null, drawable, null);
		}
	}
}
