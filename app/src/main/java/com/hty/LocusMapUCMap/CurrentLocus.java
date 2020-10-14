package com.hty.LocusMapUCMap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import org.jeo.vector.Feature;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import cn.creable.ucmap.openGIS.UCCoordinateFilter;
import cn.creable.ucmap.openGIS.UCFeatureLayer;
import cn.creable.ucmap.openGIS.UCFeatureLayerListener;
import cn.creable.ucmap.openGIS.UCMapView;
import cn.creable.ucmap.openGIS.UCRasterLayer;
import cn.creable.ucmap.openGIS.UCStyle;
import cn.creable.ucmap.openGIS.UCVectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CurrentLocus extends Activity implements UCFeatureLayerListener, LocationListener {

	TextView textView_current, textView_upload;
	CheckBox checkBoxFollow;

	private UCMapView mapView;
	private MeasureTool mTool = null;
	private UCCoordinateFilter UCCF;
	private LocationManager locationManager;
	PathAnalysisTool paTool;
	UCRasterLayer gLayer;
	int type;
	BitmapDrawable BD_start, BD_end;
	UCVectorLayer vlayer;
	GeometryFactory GF = new GeometryFactory();
	double lgt, ltt, lgt0, ltt0, alt;
	float dist, lc = 0, speed;
	boolean isFirst = true;
	SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	SimpleDateFormat SDF1 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
	SimpleDateFormat SDF_time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	SimpleDateFormat SDF_date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	Date time_start;
	SharedPreferences sharedPreferences;
	String filename_gpx = "";
	DecimalFormat DF1 = new DecimalFormat("0.0");
	DecimalFormat DF2 = new DecimalFormat("0.00");
	Intent serviceForegroundIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_current);
		Log.e(Thread.currentThread().getStackTrace()[2] + "", "CurrentLocus.onCreate()");
		MainApplication.getInstance().addActivity(this);
		MainApplication.context = this;
		MainApplication.setMode("map");
		SDF_time.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		UCMapView.setTileScale(0.5f);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		textView_current = findViewById(R.id.textView_current);
		textView_upload = findViewById(R.id.textView_upload);
		checkBoxFollow = findViewById(R.id.checkBoxFollow);

		mapView = findViewById(R.id.mapView_current);
		mapView.setBackgroundColor(0xFFFFFFFF);
		mapView.addScaleBar();
		mapView.rotation(false);
		// mapView.refresh();
		if (vlayer == null)
			vlayer = mapView.addVectorLayer();

		UCCF = new UCCoordinateFilter() {
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

		mapView.setCoordinateFilter(UCCF);

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

		mapView.moveTo(116.383333, 39.9, 2000);
		mapView.postDelayed(new Runnable() {
			@Override
			public void run() {
				mapView.refresh();
			}
		}, 0);

		BD_start = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_start);
		BD_end = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_end);

		time_start = new Date();
		filename_gpx = SDF1.format(time_start) + "UC.gpx";
		MainApplication.setwfn(filename_gpx);
		RWXML.create(SDF1.format(time_start));

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, this);
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
		menu.add(0, 8, 8, "结束");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case 0:
				if (type != 0) {
					mapView.deleteLayer(gLayer);
					String dir = Environment.getExternalStorageDirectory().getPath();
					gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir + "/cacheGoogleMapM.db");
					mapView.moveLayer(gLayer, 0);
					mapView.refresh();
					type = 0;
				}
				// mapView.setLayerVisible(0, true);
				// mapView.setLayerVisible(1, false);
				// mapView.setLayerVisible(2, false);
				// mapView.refresh();
			case 1:
				if (type != 1) {
					mapView.deleteLayer(gLayer);
					String dir = Environment.getExternalStorageDirectory().getPath();
					gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=p&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir + "/cacheGoogleMapP.db");
					mapView.moveLayer(gLayer, 0);
					mapView.refresh();
					type = 1;
				}
				// mapView.setLayerVisible(0, false);
				// mapView.setLayerVisible(1, true);
				// mapView.setLayerVisible(2, false);
				// mapView.refresh();
			case 2:
				if (type != 2) {
					mapView.deleteLayer(gLayer);
					String dir = Environment.getExternalStorageDirectory().getPath();
					gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=y&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir + "/cacheGoogleMapY.db");
					mapView.moveLayer(gLayer, 0);
					mapView.refresh();
					type = 2;
				}
				// mapView.setLayerVisible(0, false);
				// mapView.setLayerVisible(1, false);
				// mapView.setLayerVisible(2, true);
				// mapView.refresh();
			case 3:
				if (mTool != null) {
					mTool.stop();
					mTool = null;
				}
				if (paTool == null) {
					paTool = new PathAnalysisTool(mapView, BD_start.getBitmap(), BD_end.getBitmap());
					paTool.start();
				}
				return true;
			case 4:
				if (paTool != null) {
					paTool.end();
					paTool = null;
				}
				return true;
			case 5:
				if (paTool != null) {
					paTool.end();
					paTool = null;
				}
				if (mTool == null) {
					BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_poi);
					mTool = new MeasureTool(mapView, bd.getBitmap(), 0);
					mTool.start();
				}
			case 6:
				if (paTool != null) {
					paTool.end();
					paTool = null;
				}
				if (mTool == null) {
					BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_poi);
					mTool = new MeasureTool(mapView, bd.getBitmap(), 1);
					mTool.start();
				}
			case 7:
				if (mTool != null)
					mTool.stop();
				mTool = null;
				mapView.refresh();
			case 8:
				MainApplication.setwfn("");
				MainApplication.setMode("");
				finish();
		}
		return super.onOptionsItemSelected(item);
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
		//Log.e(Thread.currentThread().getStackTrace()[2] + "", location.toString());
		lgt = location.getLongitude();
		ltt = location.getLatitude();
		speed = location.getSpeed();
		alt = location.getAltitude();
		if (checkBoxFollow.isChecked())
			mapView.moveTo(lgt, ltt, mapView.getScale());
		mapView.setLocationPosition(lgt, ltt, location.getAccuracy());

		if (isFirst) {
			isFirst = false;
			lgt0 = lgt;
			ltt0 = ltt;
		}

		Coordinate[] coords = new Coordinate[2];
		coords[0] = new Coordinate(lgt, ltt);
		coords[1] = new Coordinate(lgt0, ltt0);
		Geometry geo = GF.createLineString(coords);
		// 测试：速度>1显示绿色，否则显示红色。
		if (speed > 1)
			vlayer.addLine(geo, 2, 0xFF00FF00);
		else
			vlayer.addLine(geo, 2, 0xFFFF0000);
		mapView.refresh();
		// dist =
		// cn.creable.ucmap.openGIS.Arithmetic.Distance(GF.createPoint(new
		// Coordinate(lgt0, ltt0)), GF.createPoint(new Coordinate(lgt,
		// ltt)));
		float[] results = new float[1];
		Location.distanceBetween(ltt, lgt, ltt0, lgt0, results);
		dist = results[0];
		lc += dist;
		lgt0 = lgt;
		ltt0 = ltt;
		Date date = new Date();
		long duration = date.getTime() - time_start.getTime();
		if (location.getAccuracy() < 10) {// 测试：精度小于10米才记录
			RWXML.add(filename_gpx, SDF.format(date), String.valueOf(ltt), String.valueOf(lgt), String.valueOf(lc), SDF_time.format(duration));
		}
		if (sharedPreferences.getBoolean("switch_upload", false) && location.getAccuracy() < 10) {
			new Thread(t).start();
		} else {
			textView_upload.setText("");
		}
		textView_current.setText(SDF.format(time_start) + "\n经度：" + lgt + "\n纬度：" + ltt + "\n高度：" + alt + " 米\n速度：" + DF1.format(speed) + " 米/秒\n精度：" + location.getAccuracy() + " 米\n位移：" + DF2.format(dist) + " 米\n时长：" + SDF_time.format(duration) + "\n路程：" + DF2.format(lc) + " 米");
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
	protected void onPause() {
		super.onPause();
		serviceForegroundIntent = new Intent(this, ServiceNotification.class);
		serviceForegroundIntent.putExtra(ServiceNotification.EXTRA_NOTIFICATION_CONTENT, "后台记录轨迹");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceForegroundIntent);
		} else {
			startService(serviceForegroundIntent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		enableAvailableProviders();
		//mapView.setCoordinateFilter(UCCF);
		if (serviceForegroundIntent != null) {
			stopService(serviceForegroundIntent);
			serviceForegroundIntent = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(Thread.currentThread().getStackTrace()[2] + "", "onDestroy()");
		locationManager.removeUpdates(this);
		if (serviceForegroundIntent != null) {
			stopService(serviceForegroundIntent);
			serviceForegroundIntent = null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			startActivity(new Intent(CurrentLocus.this, MenuActivity.class));
//			return true;
//		}
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

	Thread t = new Thread() {
		@Override
		public void run() {
			String dateu = "";
			String timeu = "";
			Date date = new Date();
			try {
				dateu = URLEncoder.encode(SDF_date.format(date), "utf-8");
				timeu = URLEncoder.encode(SDF_time.format(date), "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String upload_server = sharedPreferences.getString("uploadServer", "http://sonichy.gearhostpreview.com/locusmap/add.php");
			String SU = upload_server + "?date=" + dateu + "&time=" + timeu + "&longitude=" + lgt + "&latitude=" + ltt + "&altitude=" + alt + "&speed=" + DF1.format(speed)	+ "&distance=" + DF2.format(dist);
			String SR = Utils.sendURLResponse(SU);
			//RWXML.append(Environment.getExternalStorageDirectory().getPath() + "/LocusMap/UCMap.log", "CurrentLocus.upload:" + SU);
			Message message = handler.obtainMessage();
			message.what = 0;
			message.obj = SR;
			handler.sendMessage(message);
		}
	};

	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0:
					textView_upload.setText("上传：" + msg.obj);
					break;
			}
		}
	};

//	class GPSReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			Log.e(Thread.currentThread().getStackTrace()[2] + "", intent.toString());
//			Bundle bundle = intent.getExtras();
//			long time = bundle.getLong("Time");
//			float accuracy = bundle.getFloat("Accuracy");
//			lgt = bundle.getDouble("Longitude");
//			ltt = bundle.getDouble("Latitude");
//			float speed = bundle.getFloat("Speed");
//			double altitude = bundle.getDouble("Altitude");
//			float bearing = bundle.getFloat("Bearing");
//			String duration = bundle.getString("Duration");
//			float dist = bundle.getFloat("Distance");
//			float lc = bundle.getFloat("LuChen");
//
//			if (checkBoxFollow.isChecked())
//				mapView.moveTo(lgt, ltt, mapView.getScale());
//			mapView.setLocationPosition(lgt, ltt, accuracy);
//			// mapView.refresh();
//
//			if (isFirst) {
//				isFirst = false;
//				lgt0 = lgt;
//				ltt0 = ltt;
//			}
//
//			Coordinate[] coords = new Coordinate[2];
//			coords[0] = new Coordinate(lgt, ltt);
//			coords[1] = new Coordinate(lgt0, ltt0);
//			Geometry geo = GF.createLineString(coords);
//			// 测试：速度>1显示绿色，否则显示红色。
//			if (speed > 1)
//				vlayer.addLine(geo, 2, 0xFF00FF00);
//			else
//				vlayer.addLine(geo, 2, 0xFFFF0000);
//			// mapView.refresh();
//			// dist =
//			// cn.creable.ucmap.openGIS.Arithmetic.Distance(GF.createPoint(new
//			// Coordinate(lgt0, ltt0)), GF.createPoint(new Coordinate(lgt,
//			// ltt)));
//
//			// 移到服务中
//			// Date date = new Date();
//			// long duration = time - time_start.getTime();
//			// if (accuracy < 10) {// 测试：精度小于10米才记录
//			// float[] results = new float[1];
//			// Location.distanceBetween(ltt, lgt, ltt0, lgt0, results);
//			// dist = results[0];
//			// lc += dist;
//			// RWXML.add(filename_gpx, SDF.format(date), String.valueOf(ltt),
//			// String.valueOf(lgt), String.valueOf(lc),
//			// SDF_time.format(duration));
//			// }
//
//			textView_current.setText(SDF.format(time_start) + "\n经度：" + lgt + "\n纬度：" + ltt + "\n高度：" + altitude + " 米\n速度：" + DF1.format(speed) + " 米/秒\n精度："
//					+ DF2.format(accuracy) + " 米\n位移：" + DF2.format(dist) + " 米\n时长：" + duration + "\n路程：" + DF2.format(lc) + " 米");
//			// new Thread(t).start();
//			textView_upload.setText("上传：" + SR);
//			lgt0 = lgt;
//			ltt0 = ltt;
//		}
//	}class GPSReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			Log.e(Thread.currentThread().getStackTrace()[2] + "", intent.toString());
//			Bundle bundle = intent.getExtras();
//			long time = bundle.getLong("Time");
//			float accuracy = bundle.getFloat("Accuracy");
//			lgt = bundle.getDouble("Longitude");
//			ltt = bundle.getDouble("Latitude");
//			float speed = bundle.getFloat("Speed");
//			double altitude = bundle.getDouble("Altitude");
//			float bearing = bundle.getFloat("Bearing");
//			String duration = bundle.getString("Duration");
//			float dist = bundle.getFloat("Distance");
//			float lc = bundle.getFloat("LuChen");
//
//			if (checkBoxFollow.isChecked())
//				mapView.moveTo(lgt, ltt, mapView.getScale());
//			mapView.setLocationPosition(lgt, ltt, accuracy);
//			// mapView.refresh();
//
//			if (isFirst) {
//				isFirst = false;
//				lgt0 = lgt;
//				ltt0 = ltt;
//			}
//
//			Coordinate[] coords = new Coordinate[2];
//			coords[0] = new Coordinate(lgt, ltt);
//			coords[1] = new Coordinate(lgt0, ltt0);
//			Geometry geo = GF.createLineString(coords);
//			// 测试：速度>1显示绿色，否则显示红色。
//			if (speed > 1)
//				vlayer.addLine(geo, 2, 0xFF00FF00);
//			else
//				vlayer.addLine(geo, 2, 0xFFFF0000);
//			// mapView.refresh();
//			// dist =
//			// cn.creable.ucmap.openGIS.Arithmetic.Distance(GF.createPoint(new
//			// Coordinate(lgt0, ltt0)), GF.createPoint(new Coordinate(lgt,
//			// ltt)));
//
//			// 移到服务中
//			// Date date = new Date();
//			// long duration = time - time_start.getTime();
//			// if (accuracy < 10) {// 测试：精度小于10米才记录
//			// float[] results = new float[1];
//			// Location.distanceBetween(ltt, lgt, ltt0, lgt0, results);
//			// dist = results[0];
//			// lc += dist;
//			// RWXML.add(filename_gpx, SDF.format(date), String.valueOf(ltt),
//			// String.valueOf(lgt), String.valueOf(lc),
//			// SDF_time.format(duration));
//			// }
//
//			textView_current.setText(SDF.format(time_start) + "\n经度：" + lgt + "\n纬度：" + ltt + "\n高度：" + altitude + " 米\n速度：" + DF1.format(speed) + " 米/秒\n精度："
//					+ DF2.format(accuracy) + " 米\n位移：" + DF2.format(dist) + " 米\n时长：" + duration + "\n路程：" + DF2.format(lc) + " 米");
//			// new Thread(t).start();
//			textView_upload.setText("上传：" + SR);
//			lgt0 = lgt;
//			ltt0 = ltt;
//		}
//	}

}