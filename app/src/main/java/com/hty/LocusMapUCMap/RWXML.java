package com.hty.LocusMapUCMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

public class RWXML {
	static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	static SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	static Date duration;
	static double distance;
	static int seconds, h, m, s;
	static DecimalFormat DF = new DecimalFormat("0.0");
	static DecimalFormat DF1 = new DecimalFormat("0");

	static void create(String time) {
		String dir = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/";
		String fn = time + "UC.gpx";
		String filepath = dir + fn;
		// Log.e("gpx", filepath);
		File file = new File(dir);
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File(filepath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// e.printStackTrace();
				Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
			}
		}
		StringBuilder sb = new StringBuilder(time);
		sb.insert(4, "-");
		sb.insert(7, "-");
		sb.insert(10, " ");
		sb.insert(13, ":");
		sb.insert(16, ":");
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(filepath, true);
			bw = new BufferedWriter(fw);
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><metadata><starttime>"
					+ sb
					+ "</starttime><endtime>"
					+ time
					+ "</endtime><distance>0</distance><duration>00:00:00</duration><maxspeed>0</maxspeed></metadata><trk><trkseg></trkseg></trk></gpx>";
			bw.write(content);
			bw.flush();
			bw.close();
			fw.close();
		} catch (IOException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
	}

	static void add(String filename, String time, String latitude, String longitude, String distance, String duration) {
		String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		Document doc = null;
		try {
			doc = db.parse(new FileInputStream(filepath));
		} catch (SAXException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		} catch (IOException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		doc.getDocumentElement().getElementsByTagName("endtime").item(0).setTextContent(time);
		doc.getDocumentElement().getElementsByTagName("distance").item(0).setTextContent(distance);
		doc.getDocumentElement().getElementsByTagName("duration").item(0).setTextContent(duration);
		Element Etrkpt = doc.createElement("trkpt");
		Attr attr = doc.createAttribute("lat");
		attr.setValue(latitude);
		Attr attr2 = doc.createAttribute("lon");
		attr2.setValue(longitude);
		Etrkpt.setAttributeNode(attr);
		Etrkpt.setAttributeNode(attr2);
		Element Etime = doc.createElement("time");
		Etime.setTextContent(time);
		Etrkpt.appendChild(Etime);
		doc.getDocumentElement().getElementsByTagName("trkseg").item(0).appendChild(Etrkpt);
		TransformerFactory TFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = TFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 换行
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // 缩进
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(filepath));
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
	}

	static Coordinate[] read(String filename) {
		String starttime = "", endtime = "", sdistance = "", sduration = "", info;
		String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
		DocumentBuilder DB = null;
		try {
			DB = DBF.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		Document doc = null;
		try {
			doc = DB.parse(new FileInputStream(filepath));
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
			return null;
		} catch (SAXException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		} catch (IOException e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		try {
			starttime = doc.getDocumentElement().getElementsByTagName("starttime").item(0).getFirstChild().getTextContent();
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		try {
			endtime = doc.getDocumentElement().getElementsByTagName("endtime").item(0).getFirstChild().getTextContent();
		} catch (Exception e) {
		}
		try {
			sdistance = doc.getDocumentElement().getElementsByTagName("distance").item(0).getFirstChild().getTextContent();
			distance = Double.parseDouble(sdistance);
			sdistance = sdistance.substring(0, sdistance.indexOf("."));
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		try {
			sduration = doc.getDocumentElement().getElementsByTagName("duration").item(0).getFirstChild().getTextContent();
			h = Integer.parseInt(sduration.substring(0, 2));
			m = Integer.parseInt(sduration.substring(3, 5));
			s = Integer.parseInt(sduration.substring(6));
			seconds = h * 3600 + m * 60 + s;
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		String v = "?";
		if (seconds != 0)
			v = DF.format(distance / seconds) + " 米/秒";
		info = filename + "\n开始时间：" + starttime + "\n结束时间：" + endtime + "\n路程：" + sdistance + " 米\n时长：" + sduration + " (" + seconds + " 秒)\n平均速度：" + v;
		MainApplication.setmsg(info);
		Element root = doc.getDocumentElement();
		NodeList trkpt = root.getElementsByTagName("trkpt");
		int trkpt_length = trkpt.getLength();
		// Log.e("trkpt_length", trkpt_length + "");
		if (trkpt_length > 1) {
			Coordinate[] coords = new Coordinate[trkpt_length];
			for (int i = 0; i < trkpt_length; i++) {
				Element elemt = (Element) trkpt.item(i);
				coords[i] = new Coordinate(Double.parseDouble(elemt.getAttribute("lon")), Double.parseDouble(elemt.getAttribute("lat")));
			}
			return coords;
		} else {
			return null;
		}
	}

	static void del(String filename) {
		String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		File file = new File(filepath);
		file.delete();
		//MainApplication.setfn("");
		Log.e(Thread.currentThread().getStackTrace()[2] + "", "del(" + filepath + ")");
	}

	static void append(String file, String conent) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			Date date = new Date();
			out.write(SDF.format(date) + ": " + conent + "\n");
		} catch (Exception e) {
			// e.printStackTrace();
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				// e.printStackTrace();
				Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
			}
		}
	}

}