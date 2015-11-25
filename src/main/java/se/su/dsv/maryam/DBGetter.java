package se.su.dsv.maryam;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.annotation.SuppressLint;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

@SuppressLint("UseSparseArrays")
public class DBGetter {
	final static String mTextEncoding = "UTF-8";
	final static String		HASHKEY		= "banan";
	@SuppressLint("SimpleDateFormat")
	final static DateFormat	formatter	= new SimpleDateFormat("yy-MM-dd HH:mm:ss");
	final static String		URL			= "http://ratsteater.se.preview.binero.se/maryam/backend/gateway_ios.php";

	private static List<NameValuePair> queryBuilder(HashMap<String, String> map) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(map.size() + 1);
		String query = "";
		int i = 0;
		try {
			for (Entry<String, String> e : map.entrySet()) {

				nameValuePairs.add(new BasicNameValuePair(e.getKey(), e.getValue()));
				query += e.getKey() + "=" + URLEncoder.encode(e.getValue(), mTextEncoding);
				query += (++i == map.size()) ? "" : "&";
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String secret = md5(HASHKEY + query.toString());
		nameValuePairs.add(new BasicNameValuePair("md5", secret));

		query += "&md5" + "=" + secret;
		return nameValuePairs;
	}

	private static String md5(String string) {
		byte[] hash;

		try {
			hash = MessageDigest.getInstance("MD5").digest(string.getBytes(mTextEncoding));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 is not supported", e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 is not supported", e);
		}

		StringBuilder hex = new StringBuilder(hash.length * 2);

		for (byte b : hash) {
			int i = (b & 0xFF);
			if (i < 0x10)
				hex.append('0');
			hex.append(Integer.toHexString(i));
		}

		return hex.toString();
	}

	public static void addHookUp(int userId, int pointId) throws IOException, SocketTimeoutException {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("command", "addHookUp");
		map.put("routeId", String.valueOf(userId));
		map.put("pointId", String.valueOf(pointId));

		addHookUp(queryBuilder(map));
	}

	private static void addHookUp(List<NameValuePair> nameValuePairs) throws IOException, SocketTimeoutException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL);
		HttpResponse response = null;
		
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 3000;
		int timeoutSocket = 5000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, mTextEncoding));

		// Execute
		response = httpclient.execute(httppost);
	}

	public static int postComment(int routeId, double xPos, double yPos, int userId, String message, String sender, String lang) throws IOException, SocketTimeoutException {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("command", "postComment");
		map.put("routeId", String.valueOf(routeId));
		map.put("placeId", "-1");
		map.put("x", String.valueOf(xPos));
		map.put("y", String.valueOf(yPos));
		map.put("z", String.valueOf(userId));
		map.put("txt", message);
		map.put("sender", sender);
		map.put("lang", lang);

		return postComment(queryBuilder(map));
	}

	private static int postComment(List<NameValuePair> nameValuePairs) throws IOException, SocketTimeoutException {
		int commentId = -1;
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL);
		HttpResponse response = null;
		
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 3000;
		int timeoutSocket = 5000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, mTextEncoding));
			// Execute
			response = httpclient.execute(httppost);
			// Parse p-list
			NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(response.getEntity().getContent());
			commentId = ((NSNumber) rootDict.objectForKey("id")).intValue(); // Får nullpointer, ändrat i APIt?
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return commentId;
	}

	public static ArrayList<Comment> getComments(int max, String lang) throws IOException, SocketTimeoutException {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("command", "getComments");
		map.put("routeId", "-1");
		map.put("placeId", "-1");
		map.put("offset", "0");
		map.put("count", String.valueOf(max));
		map.put("lang", lang);
		return getComments(queryBuilder(map), lang);
	}

	public static ArrayList<Comment> getCommentsDelta(Date time, int max, String lang) throws IOException, SocketTimeoutException{
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("command", "getCommentsDelta");
		map.put("routeId", "-1");
		map.put("placeId", "-1");
		map.put("time", formatter.format(time));
		map.put("max", String.valueOf(max));
		map.put("lang", lang);

		return getComments(queryBuilder(map), lang);
	}

	public static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	private static ArrayList<Comment> getComments(List<NameValuePair> nameValuePairs, String lang) throws IOException, SocketTimeoutException {
		ArrayList<Comment> commentList = new ArrayList<Comment>();
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL);
		HttpResponse response = null;
		
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 3000;
		int timeoutSocket = 5000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, mTextEncoding));

			// Execute
			response = httpclient.execute(httppost);

			// Parse p-list
			NSDictionary rootDict = null;
			NSObject[] commentArray = null;
			try {
				rootDict = (NSDictionary) PropertyListParser.parse(response.getEntity().getContent());
				commentArray = ((NSArray) rootDict.objectForKey("comments")).getArray();
			} catch (ClassCastException e){ 
				// Betyder att det inte finns några nya delta-comments 	
			} catch (Exception e) {
				// Biblioteksskaparen låter den kasta Exception.
				e.printStackTrace();
			}
			if (commentArray != null)
				for (int x = 0; x < commentArray.length; x++) {
					NSDictionary commentDict = (NSDictionary) commentArray[x];
	
					int id = ((NSNumber) commentDict.objectForKey("id")).intValue();
					int routeId = ((NSNumber) commentDict.objectForKey("routeId")).intValue();
					int placeId = ((NSNumber) commentDict.objectForKey("placeId")).intValue();
					double xPos = ((NSNumber) commentDict.objectForKey("x")).doubleValue();
					double yPos = ((NSNumber) commentDict.objectForKey("y")).doubleValue();
					int z = ((NSNumber) commentDict.objectForKey("z")).intValue();
					String message = URLDecoder.decode(commentDict.objectForKey("txt").toString(), mTextEncoding);
					String sender = URLDecoder.decode(commentDict.objectForKey("sender").toString(), mTextEncoding);
					String dateString = URLDecoder.decode(commentDict.objectForKey("datum").toString(), mTextEncoding);
					Date date = new Date(formatter.parse(dateString).getTime());
	
					Comment comment = new Comment(id, routeId, placeId, z, xPos, yPos, message, sender, date);
					comment.setLang(lang);
					commentList.add(comment);
				}

		} catch (ClientProtocolException e) {
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return commentList;
	}

	public static HashMap<Integer, Route> getRoutes() throws SocketTimeoutException {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("command", "getRoutes");
		return getRoutes(queryBuilder(map));
	}

	private static HashMap<Integer, Route> getRoutes(List<NameValuePair> nameValuePairs) throws SocketTimeoutException{
		HashMap<Integer, Route> routeMap = new HashMap<Integer, Route>();
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL);
		HttpResponse response = null;
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 3000;
		int timeoutSocket = 5000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, mTextEncoding));

			// Execute
			response = httpclient.execute(httppost);

			// Parse p-list
			NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(response.getEntity().getContent());
			NSObject[] routesArray = ((NSArray) rootDict.objectForKey("routes")).getArray();

			for (int x = 0; x < routesArray.length; x++) {
				HashMap<Integer, Point> pointMap = new HashMap<Integer, Point>();
				NSDictionary routeDict = (NSDictionary) routesArray[x];

				NSObject[] pointsArray = ((NSArray) routeDict.objectForKey("points")).getArray();
				for (int i = 0; i < pointsArray.length; i++) {

					NSDictionary pointDict = (NSDictionary) pointsArray[i];

					double lat = ((NSNumber) pointDict.objectForKey("lat")).doubleValue();
					double lng = ((NSNumber) pointDict.objectForKey("lon")).doubleValue();
					int id = ((NSNumber) pointDict.objectForKey("id")).intValue();
					int innerRadius = ((NSNumber) pointDict.objectForKey("innerRadius")).intValue();
					int outerRadius = ((NSNumber) pointDict.objectForKey("outerRadius")).intValue();
					int place = ((NSNumber) pointDict.objectForKey("place")).intValue();
					String title = URLDecoder.decode(pointDict.objectForKey("title").toString(), mTextEncoding);

					Point point = new Point(lat, lng, innerRadius, place, id, title);
					pointMap.put(place, point);
				}

				int routeId = ((NSNumber) routeDict.objectForKey("id")).intValue();
				int color = ((NSNumber) routeDict.objectForKey("color")).intValue();
				String routeTitle = URLDecoder.decode(routeDict.objectForKey("title").toString(), mTextEncoding);
				String extra = URLDecoder.decode(routeDict.objectForKey("extra").toString(), mTextEncoding);

				Route route = new Route(routeId, color, routeTitle, extra, pointMap);
				route.setUserId(((NSNumber)rootDict.objectForKey("uid")).intValue());
				routeMap.put(routeId, route);
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(routeMap);
		return routeMap;
	}
}