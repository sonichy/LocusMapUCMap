package com.hty.LocusMapUCMap;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Vector;

import org.jeo.vector.BasicFeature;
import org.jeo.vector.Feature;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import cn.creable.ucmap.openGIS.UCCoordinateFilter;
import cn.creable.ucmap.openGIS.UCFeatureLayer;
import cn.creable.ucmap.openGIS.UCFeatureLayerListener;
import cn.creable.ucmap.openGIS.UCMapView;
import cn.creable.ucmap.openGIS.UCMarker;
import cn.creable.ucmap.openGIS.UCMarkerLayer;
import cn.creable.ucmap.openGIS.UCRasterLayer;
import cn.creable.ucmap.openGIS.UCStyle;
import cn.creable.ucmap.openGIS.UCVectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class HistoryLocus extends Activity implements UCFeatureLayerListener, LocationListener {

	TextView textView_history, textView_marker;
	CheckBox checkBox_follow;
	SeekBar seekBar;

	UCMapView mapView;
	private MeasureTool mTool = null;
	private UCCoordinateFilter filter_WGS_to_GCJ, filter_BD_to_GCJ;
	private LocationManager locationManager;
	PathAnalysisTool paTool;
	UCRasterLayer gLayer;
	UCMarkerLayer mlayer;
	UCVectorLayer vlayer;
	int type, index = 0, dd = 1;
	BitmapDrawable BD_start, BD_end, BD_marker;
	GeometryFactory GF = new GeometryFactory();
	Coordinate coord0;
	Coordinate[] coords;
	double lgt, ltt;
	String filename = "";
	ImageButton imageButton_location, imageButton_backward, imageButton_forward, imageButton_play, imageButton_pause, imageButton_stop;
	int position;
	UCMarker marker;
	boolean flag_stop = true, flag_pause = false;
	Geometry geo1, geo2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Android7分享崩溃：https://blog.csdn.net/xiaoyu940601/article/details/54406725
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//			StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//			StrictMode.setVmPolicy(builder.build());
//		}
		MainApplication.getInstance().addActivity(this);
		position = getIntent().getIntExtra("position", 0);

		UCMapView.setTileScale(0.5f);
		setContentView(R.layout.activity_history);
		textView_history = findViewById(R.id.textView_history);
		textView_marker = findViewById(R.id.textView_marker);
		checkBox_follow = findViewById(R.id.checkBoxFollow);
		imageButton_location = findViewById(R.id.imageButton_location);
		imageButton_location.setOnClickListener(new ButtonListener());
		imageButton_backward = findViewById(R.id.imageButton_backward);
		imageButton_backward.setOnClickListener(new ButtonListener());
		imageButton_forward = findViewById(R.id.imageButton_forward);
		imageButton_forward.setOnClickListener(new ButtonListener());
		imageButton_play = findViewById(R.id.imageButton_play);
		imageButton_play.setOnClickListener(new ButtonListener());
		imageButton_pause = findViewById(R.id.imageButton_pause);
		imageButton_pause.setOnClickListener(new ButtonListener());
		imageButton_stop = findViewById(R.id.imageButton_stop);
		imageButton_stop.setOnClickListener(new ButtonListener());
		seekBar = findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				textView_marker.setText(progress + "%");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// 分段显示
				if (seekBar.getProgress() > 0 && seekBar.getProgress() < 100) {
					if (geo1 != null) {
						vlayer.remove(geo1);
					}
					if (geo2 != null) {
						vlayer.remove(geo2);
					}
					int bp = coords.length * seekBar.getProgress() / 100;
					Coordinate[] coords1 = new Coordinate[bp];
					Coordinate[] coords2 = new Coordinate[coords.length - bp];
					for (int i = 0; i < coords.length; i++) {
						if (i < bp)
							coords1[i] = coords[i];
						else
							coords2[i - bp] = coords[i];
					}
					geo1 = GF.createLineString(coords1);
					vlayer.addLine(geo1, 2, 0xFFFF0000);
					geo2 = GF.createLineString(coords2);
					vlayer.addLine(geo2, 2, 0xFF0000FF);
					mapView.refresh();
				}
			}
		});

		mapView = findViewById(R.id.mapView_history);
		mapView.setBackgroundColor(0xFFFFFFFF);
		mapView.addScaleBar();
		mapView.rotation(false);

		filter_WGS_to_GCJ = new UCCoordinateFilter() {
			@Override
			public double[] to(double x, double y) {
				double[] result = new double[2];
				Gps gps = PositionUtil.gps84_To_Gcj02(y, x);
				result[0] = gps.getWgLon();
				result[1] = gps.getWgLat();
				return result;
			}

			@Override
			public double[] from(double x, double y) {
				double[] result = new double[2];
				Gps gps = PositionUtil.gcj_To_Gps84(y, x);
				result[0] = gps.getWgLon();
				result[1] = gps.getWgLat();
				return result;
			}
		};

		filter_BD_to_GCJ = new UCCoordinateFilter() {
			@Override
			public double[] to(double x, double y) {
				// 百度坐标系(BD-09)转火星坐标系(GCJ-02)
				double[] result = new double[2];
				double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
				x = x - 0.0065;
				y = y - 0.006;
				double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
				double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
				result[0] = z * Math.cos(theta);
				result[1] = z * Math.sin(theta);
				return result;
			}

			@Override
			public double[] from(double x, double y) {
				double[] result = new double[2];
				result[0] = x;
				result[1] = y;
				return result;
			}
		};

		String dir = Environment.getExternalStorageDirectory().getPath();
		gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir + "/cacheGoogleMapM.db");
		// mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=p&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}",
		// 0, 20, dir+"/cacheGoogleMapP.db");
		// mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=y&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}",
		// 0, 20, dir+"/cacheGoogleMapY.db");
		// mapView.setLayerVisible(1, false);
		// mapView.setLayerVisible(2, false);

		// 以下三行的效果是添加一个矢量图层叠加到google图层上
		if (new File(dir + "/bjshp/_polyline.shp").exists()) {
			UCFeatureLayer layer = mapView.addFeatureLayer(this);
			layer.loadShapefile(dir + "/bjshp/铁路_polyline.shp");
			// , 30, 2,
			// "#FFF1EAB5",
			// "#00000000");
			layer.setStyle(new UCStyle(null, null, 0, 0, 3, 4, "#FF000000", 8, "#FFFFFFFF"));
		}

		mapView.addLocationLayer();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mapView.moveTo(116.383333, 39.9, 5000);
		mapView.postDelayed(new Runnable() {
			@Override
			public void run() {
				mapView.refresh();
			}
		}, 0);

		BD_start = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_start);
		BD_end = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_end);
		BD_marker = (BitmapDrawable) getResources().getDrawable(R.drawable.marker);
	}

	class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.imageButton_location:
				mapView.moveTo(coord0.x, coord0.y, mapView.getScale());
				break;
			case R.id.imageButton_backward:
				dd = -1;
				break;
			case R.id.imageButton_forward:
				dd = 1;
				break;
			case R.id.imageButton_play:
				flag_stop = false;
				flag_pause = false;
				imageButton_play.setVisibility(View.GONE);
				imageButton_stop.setVisibility(View.VISIBLE);
				imageButton_pause.setVisibility(View.VISIBLE);
				new Thread(new MoveThread()).start();
				break;
			case R.id.imageButton_pause:
				flag_pause = !flag_pause;
				break;
			case R.id.imageButton_stop:
				flag_stop = true;
				imageButton_play.setVisibility(View.VISIBLE);
				imageButton_stop.setVisibility(View.GONE);
				imageButton_pause.setVisibility(View.GONE);
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "线路图");
		menu.add(0, 1, 1, "地形图");
		menu.add(0, 2, 2, "卫星图");
		menu.add(0, 3, 3, "开始路径分析");
		menu.add(0, 4, 4, "结束路径分析");
		menu.add(0, 5, 5, "测距离");
		menu.add(0, 6, 6, "测面积");
		menu.add(0, 7, 7, "停止测量");
		menu.add(0, 8, 8, "BD09转GCJ02");
		menu.add(0, 9, 9, "还原坐标");
		menu.add(0, 10, 10, "删除");
		menu.add(0, 11, 11, "分享");
		menu.add(0, 12, 12, "重命名");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == 0) {
			if (type != 0) {
				mapView.deleteLayer(gLayer);
				String dir = Environment.getExternalStorageDirectory().getPath();
				gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir
						+ "/cacheGoogleMapM.db");
				mapView.moveLayer(gLayer, 0);
				mapView.refresh();
				type = 0;
			}
			// mapView.setLayerVisible(0, true);
			// mapView.setLayerVisible(1, false);
			// mapView.setLayerVisible(2, false);
			// mapView.refresh();
		} else if (id == 1) {
			if (type != 1) {
				mapView.deleteLayer(gLayer);
				String dir = Environment.getExternalStorageDirectory().getPath();
				gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=p&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir
						+ "/cacheGoogleMapP.db");
				mapView.moveLayer(gLayer, 0);
				mapView.refresh();
				type = 1;
			}
		} else if (id == 2) {
			if (type != 2) {
				mapView.deleteLayer(gLayer);
				String dir = Environment.getExternalStorageDirectory().getPath();
				gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=y&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir
						+ "/cacheGoogleMapY.db");
				mapView.moveLayer(gLayer, 0);
				mapView.refresh();
				type = 2;
			}
		} else if (id == 3) {
			if (mTool != null) {
				mTool.stop();
				mTool = null;
			}
			if (paTool == null) {
				paTool = new PathAnalysisTool(mapView, BD_start.getBitmap(), BD_end.getBitmap());
				paTool.start();
			}
			return true;
		} else if (id == 4) {
			if (paTool != null) {
				paTool.end();
				paTool = null;
			}
			return true;
		} else if (id == 5) {
			if (paTool != null) {
				paTool.end();
				paTool = null;
			}
			if (mTool == null) {
				BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_poi);
				mTool = new MeasureTool(mapView, bd.getBitmap(), 0);
				mTool.start();
			}
		} else if (id == 6) {
			if (paTool != null) {
				paTool.end();
				paTool = null;
			}
			if (mTool == null) {
				BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_poi);
				mTool = new MeasureTool(mapView, bd.getBitmap(), 1);
				mTool.start();
			}
		} else if (id == 7) {
			if (mTool != null)
				mTool.stop();
			mTool = null;
			mapView.refresh();
		} else if (id == 8) {
			mapView.setCoordinateFilter(filter_BD_to_GCJ);
			mapView.refresh();
		} else if (id == 9) {
			mapView.setCoordinateFilter(filter_WGS_to_GCJ);
			mapView.refresh();
		} else if (id == 10) {
			new AlertDialog.Builder(HistoryLocus.this).setIcon(R.drawable.warn).setTitle("删除操作").setMessage("此步骤不可还原，确定删除\n" + filename + " ？")
					.setPositiveButton("是", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String ttext;
							// Log.e(" MainApplication.getfn()", MainApplication.getfn());
							// Log.e("MainApplication.getrfn()", MainApplication.getwfn());
							if (!filename.equals(MainApplication.getwfn())) {
								ttext = filename + " 已删除！";
								RWXML.del(filename);
								Intent intent = new Intent(HistoryLocus.this, GPXListActivity.class);
								intent.putExtra("position", position);
								startActivity(intent);
							} else {
								ttext = "此文件正在写入数据，请先结束行程！";
							}
							Toast.makeText(getApplicationContext(), ttext, Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton("否", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		} else if (id == 11) {
			String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filepath)));
			intent.setType("*/*");
			startActivity(Intent.createChooser(intent, "分享 " + filename));
		} else if (id == 12) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("重命名");
			final EditText editText = new EditText(this);
			editText.setText(filename.subSequence(0, filename.indexOf(".")));
			builder.setView(editText);
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Field field = null;
					try {
						// 通过反射获取dialog中的私有属性mShowing
						field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
						field.setAccessible(true);// 设置该属性可以访问
					} catch (Exception e) {

					}
					String fp = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/";
					File file = new File(fp);
					if (!file.exists()) {
						// field.set(dialog, false);
						editText.setError("目录不存在！");
					} else {
						String filepath_old = fp + filename;
						String filename_new = editText.getText().toString() + ".gpx";
						String filepath_new = fp + filename_new;
						File file_new = new File(filepath_new);
						if (file_new.exists()) {
							editText.setError("文件已存在！");
							try {
								field.set(dialog, false);
								dialog.dismiss();
							} catch (Exception e) {
							}
						} else {
							File file_old = new File(filepath_old);
							if (file_old.renameTo(file_new)) {
								Toast.makeText(HistoryLocus.this, filename + "\n重命名为\n" + filename_new + "\n成功", Toast.LENGTH_SHORT).show();
								try {
									field.set(dialog, true);
									dialog.dismiss();
								} catch (Exception e) {
								}
								finish();
								Intent intent = getIntent();
								intent.putExtra("filename", filename_new);
								intent.putExtra("position", position);
								//MainApplication.setfn(filename_new);
								startActivity(intent);
							}
						}
					}
				}
			});
			builder.setNegativeButton("取消", null);
			builder.create().show();
		}
		return super.onOptionsItemSelected(item);
	}

	static Feature feature(String id, Object... values) {
		Feature current = new BasicFeature(id, Arrays.asList(values));
		return current;
	}

	@Override
	public boolean onItemLongPress(UCFeatureLayer layer, Feature feature, double distance) {
		if (mTool != null)
			return true;
		if (distance > 30)
			return false;
		Toast.makeText(getBaseContext(), "长按了\n" + feature + " distance=" + distance, Toast.LENGTH_SHORT).show();
		Vector<Feature> features = new Vector<Feature>();
		features.add(feature);
		mapView.getMaskLayer().setData(features, 30, 2, "#88FF0000", "#88FF0000");
		mapView.refresh();
		return true;
	}

	@Override
	public boolean onItemSingleTapUp(UCFeatureLayer layer, Feature feature, double distance) {
		if (mTool != null)
			return true;
		if (distance > 30)
			return false;
		Toast.makeText(getBaseContext(), "点击了\n" + feature + " distance=" + distance, Toast.LENGTH_SHORT).show();
		Vector<Feature> features = new Vector<Feature>();
		features.add(feature);
		mapView.getMaskLayer().setData(features, 30, 2, "#88FF0000", "#88FF0000");
		mapView.refresh();
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		lgt = location.getLongitude();
		ltt = location.getLatitude();
		if (checkBox_follow.isChecked() && flag_stop)
			mapView.moveTo(lgt, ltt, mapView.getScale());
		mapView.setLocationPosition(lgt, ltt, location.getAccuracy());
		mapView.moveLayer(gLayer, 0);
		mapView.refresh();
	}

	@Override
	public void onProviderDisabled(String arg0) {

	}

	@Override
	public void onProviderEnabled(String arg0) {

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.e("onNewIntent", "onNewIntent");
		setIntent(intent); // must store the new intent unless getIntent() will return the old one
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.e("onResume", "onResume");

		mapView.setCoordinateFilter(filter_WGS_to_GCJ);
		enableAvailableProviders();
		Intent intent = getIntent();
		String filename1 = intent.getStringExtra("filename");
		//String fp = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/";
		//File file = new File(fp + filename1);
		if (!filename.equals(filename1) || filename1.equals(MainApplication.getwfn())) {
			filename = filename1;
			if (vlayer != null) {
				mapView.deleteLayer(vlayer);
			}
			vlayer = mapView.addVectorLayer();
			if (mlayer != null) {
				mapView.deleteLayer(mlayer);
			}
			mlayer = mapView.addMarkerLayer(null);
			// filename = MainApplication.getfn();
			coords = RWXML.read(filename);
			textView_history.setText(MainApplication.getmsg() + "\n" + coords.length + " 个点");

			geo1 = GF.createLineString(coords);
			// vlayer.addLine(geo1, 2, 0xFFFF0000);
			// geo2 = GF.createLineString(coords);
			vlayer.addLine(geo1, 2, 0xFF0000FF);

			mlayer.addBitmapItem(BD_start.getBitmap(), coords[0].x, coords[0].y, "", "");
			mlayer.addBitmapItem(BD_end.getBitmap(), coords[coords.length - 1].x, coords[coords.length - 1].y, "", "");
			marker = mlayer.addBitmapItem(BD_marker.getBitmap(), coords[0].x, coords[0].y, "", "");
			coord0 = coords[coords.length / 2];
			mapView.moveTo(coords[coords.length / 2].x, coords[coords.length / 2].y, mapView.getScale());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.e("onPause", "onPause");
		if (locationManager != null)
			locationManager.removeUpdates(this);
		flag_pause = true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(HistoryLocus.this, GPXListActivity.class));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void enableAvailableProviders() {
		if (locationManager == null)
			return;
		locationManager.removeUpdates(this);
		for (String provider : locationManager.getProviders(true)) {
			if (LocationManager.GPS_PROVIDER.equals(provider) || LocationManager.NETWORK_PROVIDER.equals(provider)) {
				locationManager.requestLocationUpdates(provider, 0, 0, this);
			}
		}
	}

	public class MoveThread implements Runnable {
		int i = 0;

		int geti() {
			return i;
		}

		@Override
		public void run() {
			while (!flag_stop) {
				if (!flag_pause) {
					if (i < coords.length && i >= 0) {
						marker.setXY(coords[i].x - 0.0001, coords[i].y + 0.0007);
						mlayer.refresh();
						// if (checkBox_follow.isChecked())
						// mapView.moveTo(coords[i].x, coords[i].y,
						// mapView.getScale());// 崩溃
						mapView.refresh();

						Message message = new Message();
						message.what = 0;
						Bundle bundle = new Bundle();
						bundle.putString("marker", i + "");
						message.setData(bundle);
						handle.sendMessage(message);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						i += dd;
					} else {
						flag_stop = true;
					}
				}
			}
		}
	}

	private final Handler handle = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				Bundle bundle = msg.getData();
				textView_marker.setText(bundle.getString("marker"));
				break;
			}
		}
	};

}