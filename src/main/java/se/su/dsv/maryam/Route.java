package se.su.dsv.maryam;
import java.util.HashMap;

public class Route {
	private int id, color, userId;
	private String title, extra;
	private HashMap<Integer, Point> points;

	public Route(int id, int color, String title, String extra, HashMap<Integer, Point> points) {
		this.id = id;
		this.color = color;
		this.title = title;
		this.extra = extra;
		this.points = points;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public HashMap<Integer, Point> getPoints() {
		return points;
	}

	public void setPoints(HashMap<Integer, Point> points) {
		this.points = points;
	}

	@Override
	public String toString() {
		return "Route [id=" + id + ", color=" + color + ", title=" + title + ", extra=" + extra + ", points=" + points + "]";
	}
}
