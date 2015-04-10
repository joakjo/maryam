package se.su.dsv.maryam;

import com.google.android.gms.maps.model.LatLng;

public class Point {
	private double		lat, lng;
	private String		title;
	private int			place, radius, id;
	
	private static String[]	sceneNames	= { "På trappan", "Längs muren", "Hos bröder och systrar", "I valvet", "Mot öppet hav", "Vägkorsning", "Under dadelträdet", "På trappan" };
//	private static String[]	sceneNames	= { "On the stairs", "Along the wall", "With brothers and sisters", "In the arcade", "To the open sea", "At the crossroads", "Under the date tree", "On the stairs" };

	public Point(double lat, double lng, int radius, int place, int id, String title) {
		this.lat = lat;
		this.lng = lng;
		this.radius = radius;
		this.place = place;
		this.id = id;
		this.title = title;
	}

	public int getId() {
		return id;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public int getPlace() {
		return place;
	}

	public String getTitle() {
		return title;
	}

	public int getRadius() {
		return radius;
	}

	public LatLng getLatLng() {
		return new LatLng(lat, lng);
	}

	public String getSceneName() {
		return sceneNames[place];
	}
	
	public static String getStaticSceneName(int place) {
		return sceneNames[place];
	}
	
	public static Point getEndPointFromStartPoint(Point start) {
		return new Point(start.lat, start.lng, start.radius,
				sceneNames.length-1, -1, start.title);
	}

	@Override
	public String toString() {
		return "Point [lat=" + lat + ", lng=" + lng + ", title=" + title + ", place=" + place + ", radius=" + radius + ", id=" + id + "]";
	}

}




















