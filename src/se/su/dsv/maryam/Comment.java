package se.su.dsv.maryam;
import java.util.Date;

import android.graphics.Rect;

public class Comment {
	private int id, routeId, placeId, z;
	private double xpos, ypos;
	private String message, sender, lang;
	private Date date;
	private Rect rect;

	public Comment(int id, int routeId, int placeId, int z, double xpos, double ypos, String message, String sender, Date date) {
		this.id = id;
		this.routeId = routeId;
		this.placeId = placeId;
		this.z = z;
		this.xpos = xpos;
		this.ypos = ypos;
		this.message = message;
		this.sender = sender;
		this.date = date;
		rect = new Rect(-5000, -5000, -5000, -5000);
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getRouteId() {
		return routeId;
	}

	public void setRouteId(int routeId) {
		this.routeId = routeId;
	}

	public int getPlaceId() {
		return placeId;
	}

	public void setPlaceId(int placeId) {
		this.placeId = placeId;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public double getXpos() {
		return xpos;
	}

	public void setXpos(double xpos) {
		this.xpos = xpos;
	}

	public double getYpos() {
		return ypos;
	}

	public void setYpos(double ypos) {
		this.ypos = ypos;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public Rect getRect() {
		return rect;
	}

	public void setRect(Rect rect) {
		this.rect = rect;
	}

	@Override
	public String toString() {
		return "Comment [id=" + id + ", routeId=" + routeId + ", placeId=" + placeId + ", z=" + z + ", xpos=" + xpos + ", ypos=" + ypos + ", message=" + message + ", sender=" + sender + ", lang=" + lang + ", date=" + date + ", rect=" + rect + "]";
	}
}
