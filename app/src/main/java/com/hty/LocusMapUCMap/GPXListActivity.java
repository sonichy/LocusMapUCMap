package com.hty.LocusMapUCMap;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class GPXListActivity extends ListActivity {
	ListView listView;
	int position = 0;
	ArrayList<HashMap<String, Object>> list_file = new ArrayList<>();
	SimpleAdapter adapter;
	SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	final String dir = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainApplication.getInstance().addActivity(this);
		setTitle("轨迹列表");
		adapter = new SimpleAdapter(this, list_file, R.layout.item, new String[]{ "icon", "name", "size", "time" }, new int[]{ R.id.imageView_icon, R.id.textView_name, R.id.textView_size, R.id.textView_time });
		//getList();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		String title = ((TextView) info.targetView.findViewById(R.id.textView_name)).getText().toString();
		menu.setHeaderTitle(title);
		menu.add(0, 0, 0, "分享");
		menu.add(0, 1, 1, "删除");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final String filename = ((TextView) info.targetView.findViewById(R.id.textView_name)).getText().toString();
		switch (item.getItemId()) {
			case 0:
				String filepath = dir + filename;
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filepath)));
				intent.setType("*/*");
				startActivity(Intent.createChooser(intent, "分享 " + filename));
				break;
			case 1:
				position = info.position;
				new AlertDialog.Builder(GPXListActivity.this).setIcon(R.drawable.warn).setTitle("删除操作").setMessage("此步骤不可还原，确定删除\n" + filename + " ？")
						.setPositiveButton("是", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String ttext;
								if (!filename.equals(MainApplication.getwfn())) {
									RWXML.del(filename);
									Intent intent = getIntent();
									finish();
									overridePendingTransition(0, 0);
									startActivity(intent);
									ttext = filename + " 已删除";
									setSelection(position);
								} else {
									ttext = filename + " 正在写入数据，禁止删除！";
								}
								Toast.makeText(getApplicationContext(), ttext, Toast.LENGTH_SHORT).show();
							}
						}).setNegativeButton("否", null).show();
				break;
			default:
				break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(GPXListActivity.this, MenuActivity.class));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getList();
	}

	class ComparatorTimeDesc implements Comparator<File> {
		public int compare(File f1, File f2) {
			long diff = f2.lastModified() - f1.lastModified();
			if (diff > 0)
				return 1;
			else if (diff == 0)
				return 0;
			else
				return -1;
		}
	}

	void getList() {
		list_file.clear();
		File file = new File(dir);
		File[] files;
		files = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return name.endsWith(".gpx");
			}
		});
		Arrays.sort(files, new ComparatorTimeDesc());
		for (File file1 : files) {
			HashMap<String, Object> listItem = new HashMap<>();
			String suffix = file1.getName().substring(file1.getName().lastIndexOf(".")+1).toLowerCase();
			if (suffix.equals("gpx")) {
				listItem.put("icon", R.drawable.gpx);
			} else {
				listItem.put("icon", R.drawable.file);
			}
			listItem.put("name", file1.getName());
			listItem.put("size",  Formatter.formatFileSize(this, file1.length()));
			listItem.put("time", SDF.format(new Date(file1.lastModified())));
			list_file.add(listItem);
		}

		setListAdapter(adapter);
		listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String filename = ((TextView) arg1.findViewById(R.id.textView_name)).getText().toString();
				// Log.e("filename", filename);
				// Log.e("MainApplication.getrfn()", MainApplication.getrfn());
				position = arg2;
				if (filename.toLowerCase().endsWith(".gpx")) {
					if (RWXML.read(filename) != null) {
						Intent intent = new Intent(GPXListActivity.this, HistoryLocus.class);
						intent.putExtra("filename", filename);
						intent.putExtra("position", position);
						//MainApplication.setfn(filename);
						startActivity(intent);
					} else {
						String ttext;
						if (!filename.equals(MainApplication.getwfn())) {
							RWXML.del(filename);
							ttext = filename + "是空文档，自动删除！";
							Intent intent = getIntent();
							finish();
							overridePendingTransition(0, 0);
							startActivity(intent);
							setSelection(position);
						} else {
							ttext = filename + "刚创建，即将写入数据！";
						}
						Toast.makeText(getApplicationContext(), ttext, Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		registerForContextMenu(getListView());
		setSelection(position);
	}

}