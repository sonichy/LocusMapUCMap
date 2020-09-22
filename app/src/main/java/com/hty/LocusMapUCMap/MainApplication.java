package com.hty.LocusMapUCMap;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

public class MainApplication extends Application {
	public static Context context;
	private static String wfn = "", fn = "", msg = "", mode = "";
	private final List<Activity> activityList = new LinkedList<>();
	private static MainApplication instance;
	//static Date time_start;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e(Thread.currentThread().getStackTrace()[2] + "", "Android " + Build.VERSION.SDK_INT + " >= " + Build.VERSION_CODES.N);
		//Android7分享崩溃：https://blog.csdn.net/xiaoyu940601/article/details/54406725
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
			StrictMode.setVmPolicy(builder.build());
		}
	}

	public static void setmsg(String s) {
		msg = s;
	}

	public static String getmsg() {
		return msg;
	}

	public static void setwfn(String s) {
		wfn = s;
	}

	public static String getwfn() {
		return wfn;
	}

	//public static void setfn(String s) {
	//	fn = s;
	//}

	//public static String getfn() {
	//	return fn;
	//}

	public static void setMode(String s) {
		mode = s;
	}

	public static String getMode() {
		return mode;
	}

	//public static Context getContext() {
	//	return context;
	//}

	static MainApplication getInstance() {
		if (null == instance) {
			instance = new MainApplication();
		}
		return instance;
	}

	public void addActivity(Activity activity) {
		activityList.add(activity);
	}

	public void exit() {
		for (Activity activity : activityList) {
			activity.finish();
		}
		System.exit(0);
	}

}