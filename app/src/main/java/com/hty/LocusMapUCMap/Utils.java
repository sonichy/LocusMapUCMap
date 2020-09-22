package com.hty.LocusMapUCMap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class Utils {
	static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	static String sendURLResponse(String urlString) {
		Log.e("url", urlString);
		HttpURLConnection conn = null;
		URL url = null;
		String RC = "";
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		try {
			RC = conn.getResponseCode() + "";
		} catch (IOException e) {
			RC = "?";
			e.printStackTrace();
		}
		if (conn != null) {
			conn.disconnect();
		}
		return RC;
	}

	static double getDistance(double JA, double WA, double JB, double WB) {
		double R = 6371393;
		double AB2 = 2 * R * R * (1 - Math.cos(Math.toRadians(WA)) * Math.cos(Math.toRadians(WB)) * Math.cos(Math.toRadians(JA) - Math.toRadians(JB)) - Math.sin(Math.toRadians(WA)) * Math.sin(Math.toRadians(WB)));
		double AB = Math.sqrt(AB2);
		double c = 2 * Math.asin(AB / 2 / R);
		double hu = c * R;
		Date date = new Date();
		RWXML.append(Environment.getExternalStorageDirectory().getPath() + "/LocusMap/UCMap.log", SDF.format(date) + "Utils.getDistance(" + JA + ", " + WA + ", " + JB + ", " + WB + "): AB2=" + AB2 + "c=" + c);
		return hu;
	}

}