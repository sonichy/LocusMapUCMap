package com.hty.LocusMapUCMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

public class RWXML {
	static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	static SimpleDateFormat SDF_Hms = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	static Date duration;
	//static double distance;
	static DecimalFormat DF = new DecimalFormat("0.0");
	static DecimalFormat DF1 = new DecimalFormat("0");
	static String dir = Environment.getExternalStorageDirectory().getPath() + File.separator + "LocusMap" + File.separator;

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
			} catch (Exception e) {
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
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
	}

	static void add(String filename, String time, String latitude, String longitude, String distance, String duration) {
		String filepath = dir + filename;
		DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
		DocumentBuilder DB = null;
		try {
			DB = DBF.newDocumentBuilder();
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		Document doc = null;
		try {
			doc = DB.parse(new FileInputStream(filepath));
		} catch (Exception e) {
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
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 换行
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // 缩进
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(filepath));
		try {
			transformer.transform(source, result);
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
	}

	static Coordinate[] read(String filename) {
		String starttime = "", endtime = "", sdistance = "", sduration = "", info;
		int seconds=0, h, m, s;
		Double distance = 0.0;
		String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
		DocumentBuilder DB = null;
		try {
			DB = DBF.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		Document doc = null;
		try {
			doc = DB.parse(new FileInputStream(filepath));
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
			return null;
		}
		try {
			starttime = doc.getDocumentElement().getElementsByTagName("starttime").item(0).getTextContent();
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		try {
			endtime = doc.getDocumentElement().getElementsByTagName("endtime").item(0).getTextContent();
		} catch (Exception e) {
		}
		try {
			sdistance = doc.getDocumentElement().getElementsByTagName("distance").item(0).getTextContent();
			distance = Double.parseDouble(sdistance);
			sdistance = sdistance.substring(0, sdistance.indexOf("."));
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		try {
			sduration = doc.getDocumentElement().getElementsByTagName("duration").item(0).getTextContent();
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
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
			}
		}
	}

	static boolean merge(List<String> list) {
		DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
		DocumentBuilder DB = null;
		try {
			DB = DBF.newDocumentBuilder();
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		Document doc = null, doc1 = null;
		for (int i=0; i<list.size(); i++) {
			String filepath = dir + list.get(i);
            Log.e(Thread.currentThread().getStackTrace()[2] + "", filepath);
			if (i == 0) {
				try {
                    //Log.e(Thread.currentThread().getStackTrace()[2] + "", filepath);
					doc = DB.parse(new FileInputStream(filepath));
					continue;
				} catch (Exception e) {
					Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
				}
			} else {
				try {
					doc1 = DB.parse(new FileInputStream(filepath));
				} catch (Exception e) {
					Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
				}
            }
			String endtime = doc1.getDocumentElement().getElementsByTagName("endtime").item(0).getTextContent();
			doc.getDocumentElement().getElementsByTagName("endtime").item(0).setTextContent(endtime);

			String sdistance1 = doc.getDocumentElement().getElementsByTagName("distance").item(0).getTextContent();
			double distance1 = Double.parseDouble(sdistance1);
			String sdistance2 = doc1.getDocumentElement().getElementsByTagName("distance").item(0).getTextContent();
			double distance2 = Double.parseDouble(sdistance2);
			double distance = distance1 + distance2;
			doc.getDocumentElement().getElementsByTagName("distance").item(0).setTextContent(distance + "");

            String starttime = doc.getDocumentElement().getElementsByTagName("starttime").item(0).getTextContent();

            //调试：转ASCII编码
//            StringBuilder SB = new StringBuilder();
//            char[] ch = endtime.toCharArray();
//            for (int k=0; k<ch.length; k++) {
//                SB.append(ch[k]).append("(").append(Integer.valueOf(ch[k]).intValue()).append(")");
//            }
//            Log.e(Thread.currentThread().getStackTrace()[2] + "", SB.toString());

            try {
                Date date1 = SDF.parse(starttime);
                Date date2 = SDF.parse(endtime.replace("\u00A0", " ")); // Unparseable date：去掉ASCII(160)不间断空格nbsp
                Date date = new Date(date2.getTime() - date1.getTime());
                SDF_Hms.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                doc.getDocumentElement().getElementsByTagName("duration").item(0).setTextContent(SDF_Hms.format(date));
            } catch (Exception e) {
                Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
            }

			NodeList nodeList = doc1.getDocumentElement().getElementsByTagName("trkseg").item(0).getChildNodes();
			for (int j=0; j<nodeList.getLength(); j++) {
                Node node = doc.importNode(nodeList.item(j), true);
                doc.getDocumentElement().getElementsByTagName("trkseg").item(0).appendChild(node);
            }
        }
		Transformer transformer = null;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 换行
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // 缩进
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(dir + list.size() + "m" + list.get(0)));
		try {
			transformer.transform(source, result);
		} catch (Exception e) {
			Log.e(Thread.currentThread().getStackTrace()[2] + "", e.toString());
		}
		return true;
	}

}