package se.su.dsv.maryam;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import se.su.dsv.maryam.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteStatement;

@SuppressLint("UseSparseArrays")
public class LocalDatabase extends SQLiteOpenHelper {
	private static final String	DATABASE_NAME		= "MaryamDatabase";
	private static final int	DATABASE_VERSION	= 1;
	private final Context		mContext;
	final int					DEFAULT_CURRENT		= 0, DEFAULT_VISITED = 0;
	private String				TAG					= "LOCALDATABASE";
	private SQLiteDatabase db = null;

    /** Constructor */
    public LocalDatabase(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	this.mContext = context;
    }
    
    public void closeDatabase(){
		if (db != null)
			db.close();    		
    }
    
    public void cleanDatabaseFromComments(){
    	db = getWritableDatabase();
    	String[] sql = mContext.getString(R.string.Database_dropTables_always).split("\n");
		db.beginTransaction();
		try {
			execMultipleSQL(db, sql);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		onCreate(db);
    }
    /**
     * Execute all of the SQL statements in the String[] array
     * @param db The database on which to execute the statements
     * @param sql An array of SQL statements to execute
     */
    private void execMultipleSQL(SQLiteDatabase db, String[] sql){
    	for( String s : sql )
    		if (s.trim().length()>0)
    			db.execSQL(s);
    }
    
    /** Called when it is time to create the database */
	@Override
	public void onCreate(SQLiteDatabase db) {
		String[] sql = mContext.getString(R.string.Database_onCreate).split("\n");
		db.beginTransaction();
		try {
			// Create tables & test data
			execMultipleSQL(db, sql);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	db.endTransaction();
        }
	}
	
	/** Called when the database must be upgraded */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String[] always = mContext.getString(R.string.Database_dropTables_always).split("\n");
		String[] newRoute = mContext.getString(R.string.Database_dropTables_newRoute).split("\n");

		db.beginTransaction();
		try {
			switch (oldVersion) {
			default:
				execMultipleSQL(db, always);
				execMultipleSQL(db, newRoute);
				db.setTransactionSuccessful();
			}
		} 
		catch (SQLException e) { e.printStackTrace(); } 
		finally { db.endTransaction(); }

		onCreate(db);
	}
	
	public void eraseDatabaseForNewRoute() {
		db = getWritableDatabase();
		String[] sql = mContext.getString(R.string.Database_dropTables_newRoute).split("\n");
		db.beginTransaction();
		try {
			execMultipleSQL(db, sql);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}

	}
	
	public int getUserIdFromLocalDatabase() {
		int localUserId = 0;
		Cursor c = null;
		try{
			db = getReadableDatabase(); 
			c = db.rawQuery("SELECT * FROM userdata", null);
			c.moveToFirst();
			localUserId = Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("userId")));
		}
		finally{
			if (c != null)
				c.close();
		}
		return localUserId;
	}
	
	public Comment getUserCommentFromLocalDatabase() {
		Comment cc = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery("SELECT * FROM userdata", null);
			c.moveToFirst();
			int routeId = Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("routeId")));
			int placeId = -1; // Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("placeId"))); // // Ej hï¿½mtad frï¿½n databasen
			int z = Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("userId")));
			double xpos = Double.valueOf(c.getDouble(c.getColumnIndexOrThrow("starPosX")));
			double ypos = Double.valueOf(c.getDouble(c.getColumnIndexOrThrow("starPosY")));
			String message = c.getString(c.getColumnIndexOrThrow("message"));
			String sender = c.getString(c.getColumnIndexOrThrow("sender"));
			Date date = new Date(Long.valueOf(c.getString(c.getColumnIndexOrThrow("date"))));
			cc = new Comment(-1, routeId, placeId, z, xpos, ypos, message, sender, date);
		} catch (IllegalArgumentException e) {
		} finally {
			if (c != null)
				c.close();
		}
		return cc;
	}
	
	public boolean setUserComment(int routeId, double xpos, double ypos, String message, String sender, Date date, String lang){
		String sql = String.format(Locale.US, "UPDATE userdata SET  routeId = '%d', starPosX = '%f', starPosY = '%f', message = '%s', sender = '%s', date = '%s', userlang = '%s'", routeId, xpos, ypos, message, sender, String.valueOf(date.getTime()), lang);
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public void setSeenAnimation(boolean seenAnimation){
		int seenAnimationInt = (seenAnimation) ? 1 : 0;
		String sql = String.format(Locale.US, "UPDATE userdata SET seenAnimation = '%d'", seenAnimationInt);
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public boolean getSeenAnimation(){
		Cursor c = null;
		try {
			db = getReadableDatabase(); 
			c = db.rawQuery("SELECT seenAnimation FROM userdata", null);
			if (c.moveToFirst()){
				if (Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("seenAnimation"))) == 1)
					return true;
			}
		} finally {
			if (c != null)
				c.close();
		}
		return false;
	}

	public void addRoutesFromAPI(HashMap<Integer, Route> routesFromAPI) {
		ArrayList<String> sqlList = new ArrayList<String>();
		int apiUserId = 0;

        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
		for (Route rt : routesFromAPI.values()) {
            SQLiteStatement stmt = db.compileStatement("INSERT INTO route (routeId, color, title, extra, current) VALUES (?, ?, ?, ?, ?)");
            stmt.bindLong(1, rt.getId());
            stmt.bindLong(2, rt.getColor());
            stmt.bindString(3, rt.getTitle());
            stmt.bindString(4, rt.getExtra());
            stmt.bindLong(5, DEFAULT_CURRENT);
            for (Point p : rt.getPoints().values()) {
                SQLiteStatement pStmt = db.compileStatement("INSERT INTO point (pointId, title, place, radius, latitude, longitude, routeId) VALUES (?, ?, ?, ?, ?, ?, ?)");
                pStmt.bindLong(1, p.getId());
                pStmt.bindString(2, p.getTitle());
                pStmt.bindLong(3, p.getPlace());
                pStmt.bindLong(4, p.getRadius());
                pStmt.bindDouble(5, p.getLat());
                pStmt.bindDouble(6, p.getLng());
                pStmt.bindLong(7, rt.getId());
                pStmt.executeInsert();
            }
            stmt.executeInsert();
            apiUserId = rt.getUserId();
		}
		
		// Add userId to database if first time
		
		if (getUserIdFromLocalDatabase() == 0){
			SQLiteStatement stmt = db.compileStatement("UPDATE userdata SET userId = ?");
			stmt.bindLong(1, apiUserId);
			stmt.execute();
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}

	public void addCommentsFromAPI(ArrayList<Comment> commentsFromAPI) {
		String[] sql = new String[commentsFromAPI.size()];
		ArrayList<String> sqlList = new ArrayList<String>();
		for (Comment c : commentsFromAPI)
			sqlList.add(String
					.format(Locale.US,
							"INSERT INTO comment(apiId, routeId, z, starPosX, starPosY, message, sender, date, lang) VALUES ('%d', '%d', '%d', '%f', '%f', '%s', '%s', '%d', '%s')",
							c.getId(), c.getRouteId(), c.getZ(), c.getXpos(), c.getYpos(), c.getMessage(), c.getSender(), c.getDate().getTime(), c.getLang()));
		db = getWritableDatabase();
		db.beginTransaction();
		try {
			execMultipleSQL(db, sqlList.toArray(sql));
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
			db.close();
		}
	}

	public ArrayList<Comment> getComments(int limit, String lang) {
		ArrayList<Comment> comments = new ArrayList<Comment>();
		Cursor c = null;
		String query = "SELECT * FROM comment WHERE lang = '" + lang + "' ORDER BY date ASC LIMIT " + limit;
		try {			
			db = getReadableDatabase(); 
			if (lang.equalsIgnoreCase("ALL"))
				query = "SELECT * FROM comment ORDER BY date ASC LIMIT " + limit;
			c = db.rawQuery(query, null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				int id = Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("apiId")));
				int routeId = Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("routeId")));
				int placeId = -1; // Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("placeId")));
				// // Ej hï¿½mtad frï¿½n databasen
				int z = Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("z")));
				double xpos = Double.valueOf(c.getDouble(c.getColumnIndexOrThrow("starPosX")));
				double ypos = Double.valueOf(c.getDouble(c.getColumnIndexOrThrow("starPosY")));
				String message = c.getString(c.getColumnIndexOrThrow("message"));
				String sender = c.getString(c.getColumnIndexOrThrow("sender"));
				Date date = new Date(Long.valueOf(c.getString(c.getColumnIndexOrThrow("date"))));
				Comment cc = new Comment(id, routeId, placeId, z, xpos, ypos, message, sender, date);
				cc.setLang(c.getString(c.getColumnIndexOrThrow("lang")));

				comments.add(cc);
				c.moveToNext();

			}
		} catch (IllegalArgumentException e) {
		} finally {
			if (c != null)
				c.close();
		}
		return comments;
	}

	public HashMap<Integer, Route> getRoutes() {
		RoutesCursor rc = null;
		SQLiteDatabase rcdb = null;
		PointsCursor pc = null;
		SQLiteDatabase pcdb = null;
		HashMap<Integer, Route> routes = new HashMap<Integer, Route>();
		HashMap<Integer, Point> points = null;
		try {
			rc = getRoutesCursor();
			rcdb = rc.getDatabase();
			rc.moveToFirst();
			while (!rc.isAfterLast()) {
				try {
					pc = getPointCursor(rc.getColRouteId());
					pcdb = pc.getDatabase();
					points = new HashMap<Integer, Point>();
					while (!pc.isAfterLast()) {
						points.put(pc.getColPlace(), new Point(pc.getColLatitude(), pc.getColLongitude(), pc.getColRadius(), pc.getColPlace(), pc.getColPointId(), pc.getColTitle()));
						pc.moveToNext();
					}
				} catch (Exception e) {
				} finally {
					if (null != pc) {
						try {
							pc.close();
						} catch (SQLException e) {
						}
					}
					if (pcdb != null)
						pcdb.close();
				}
				routes.put(rc.getColRouteId(), new Route(rc.getColRouteId(), rc.getColColor(), rc.getColTitle(), rc.getColExtra(), points));
				rc.moveToNext();
			}
		} finally {
			if (null != rc) {
				try {
					rc.close();
				} catch (SQLException e) {
				}
			}
			if (rcdb != null)
				rcdb.close();
			
		}
		System.out.println("pooop: " +routes);
		return routes;
	}
	

	public void addVisited(int pointId){
		String sql = String.format(Locale.US, "INSERT INTO visited(visited) VALUES('%d')", pointId);
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public List<Integer> getVisited(){
		List<Integer> visitedList = new ArrayList<Integer>();
		Cursor c = null;
		try {
			db = getReadableDatabase(); 
			c = db.rawQuery("SELECT * FROM visited", null);
			c.moveToFirst();
			while (!c.isAfterLast()){
				visitedList.add(Integer.valueOf(c.getInt(c.getColumnIndexOrThrow("visited"))));
				c.moveToNext();
			}
		} finally {
			if (c != null)
				c.close();
		}
		return visitedList;
	}
	
	public void setSkyCoords(int[] skypos){
		String sql = String.format(Locale.US, "INSERT INTO skyPosition(xPos, yPos) VALUES('%d', '%d')", skypos[0], skypos[1]);
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int[] getSkyCoords() {
		int[] skypos = new int[]{0,0};
		Cursor c = null;
		try {
			db = getReadableDatabase(); 
			c = db.rawQuery("SELECT * FROM skyPosition", null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				skypos[0] = c.getInt(c.getColumnIndexOrThrow("xPos")); 
				skypos[1] = c.getInt(c.getColumnIndexOrThrow("yPos"));
				c.moveToNext();
			}
		} finally {
			if (c != null)
				c.close();
		}
		return skypos;
	}
	
	public void addStarPosition(XY xy){
		String sql = String.format(Locale.US, "INSERT INTO starPosition(xPos, yPos) VALUES('%f', '%f')", xy.getX(), xy.getY());
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<XY> getStarPositions() {
		ArrayList<XY> listXY = new ArrayList<XY>();
		Cursor c = null;
		try {
			db = getReadableDatabase(); 
			c = db.rawQuery("SELECT * FROM starPosition", null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				XY xy = new XY(Float.valueOf(c.getFloat(c.getColumnIndexOrThrow("xPos"))), Float.valueOf(c.getFloat(c.getColumnIndexOrThrow("yPos"))));
				listXY.add(xy);
				c.moveToNext();
			}
		} finally {
			if (c != null)
				c.close();
		}
		return listXY;
	}
	
	public void setState(int stateId, int stateValue) {
		String sql = String.format(Locale.US, "UPDATE states SET stateValue = '%d' WHERE stateId= '%d'", stateValue, stateId);
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getState(int stateId) {
		int stateValue = 0;
		Cursor c = null;
		try {
			db = getReadableDatabase(); 
			c = db.rawQuery("SELECT stateValue FROM states WHERE stateId =" + stateId, null);
			if (c.moveToFirst())
				stateValue = c.getInt(c.getColumnIndex("stateValue"));
		} finally {
			if (c != null)
				c.close();
		}
		return stateValue;
	}

	public String getLang() {
		String lang = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			String sql = String.format(Locale.US, "SELECT userlang FROM userdata");
			c = db.rawQuery(sql, null);
			if (c.moveToFirst())
				lang = c.getString(c.getColumnIndex("userlang"));
		} finally {
			if (c != null)
				c.close();
		}
		return lang;
	}

	public void setLang(String lang) {
		String sql = String.format(Locale.US, "UPDATE userdata SET userlang = '%s'", lang);
			db = getWritableDatabase();
			db.execSQL(sql);
	}
	
	public void setCurrentRoute(int routeId) {
		String sql = String.format(Locale.US, "UPDATE route SET current = 1 WHERE routeId = '%d'", routeId);
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Route getCurrentRoute() {
		HashMap<Integer, Point> points = new HashMap<Integer, Point>();
		int id, color;
		String title, extra;
		PointsCursor pc = null;

		// Get current route
		String sql = "SELECT * FROM route where current = 1";
		Cursor c = null;
		try {
			db = getReadableDatabase(); 
			c = db.rawQuery(sql, null);
			c.moveToFirst();
			id = c.getInt(c.getColumnIndexOrThrow("routeId"));
			color = c.getInt(c.getColumnIndexOrThrow("color"));
			title = c.getString(c.getColumnIndexOrThrow("title"));
			extra = c.getString(c.getColumnIndexOrThrow("extra"));
		} finally {
			if (c != null)
				c.close();
		}

		// Get points for current route

		try {
			pc = getPointCursor(id);
			while (!pc.isAfterLast()) {
				points.put(pc.getColPlace(),
						new Point(pc.getColLatitude(), pc.getColLongitude(), pc.getColRadius(), pc.getColPlace(), pc.getColPointId(), pc.getColTitle()));
				pc.moveToNext();
			}
		} catch (Exception e) {
		} finally {
			if (null != pc) {
				try {
					SQLiteDatabase pcdb = pc.getDatabase();
					pc.close();
					pcdb.close();
					
				} catch (SQLException e) {
				}
			}
		}
		return new Route(id, color, title, extra, points);
	}

	/**
	 * Returns a RoutesCursor for all routes
	 */
	public RoutesCursor getRoutesCursor() {
		db = getReadableDatabase();
		RoutesCursor c = (RoutesCursor) db.rawQueryWithFactory(new RoutesCursor.Factory(), RoutesCursor.QUERY, null, null);
		c.moveToFirst();
		return c;
	}

	/**
	 * Returns a PointsCursor for the specified routeId
	 * 
	 * @param routeId
	 *            The _id of the route
	 */
	public PointsCursor getPointCursor(int routeId) {
		String sql = PointsCursor.QUERY + routeId;
		db = getReadableDatabase();
		PointsCursor c = (PointsCursor) db.rawQueryWithFactory(new PointsCursor.Factory(), sql, null, null);
		c.moveToFirst();
		return c;
	}

    
	public static class RoutesCursor extends SQLiteCursor {
		/** The query for this cursor */
		private static final String	QUERY	= "SELECT routeId, color, title, extra, current FROM route";

		/** Cursor constructor */
		@SuppressWarnings("deprecation")
		private RoutesCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
			super(db, driver, editTable, query);
		}

		/** Private factory class necessary for rawQueryWithFactory() call */
		private static class Factory implements SQLiteDatabase.CursorFactory {
			@Override
			public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
				return new RoutesCursor(db, driver, editTable, query);
			}
		}

		/* Accessor functions -- one per database column */
		public int getColRouteId() {return getInt(getColumnIndexOrThrow("routeId"));}
		public int getColColor() {return getInt(getColumnIndexOrThrow("color"));}
		public String getColTitle() {return getString(getColumnIndexOrThrow("title"));}
		public String getColExtra() {return getString(getColumnIndexOrThrow("extra"));}
	}

	/**
	 * Provides self-contained query-specific cursor for Job Detail. The query
	 * and all accessor methods are in the class.
	 */
	public static class PointsCursor extends SQLiteCursor {
		/** The query for this cursor */
		private static final String	QUERY	= "SELECT pointId, title, place, radius, latitude, longitude FROM point WHERE routeId = ";

		/** Cursor constructor */
		@SuppressWarnings("deprecation")
		private PointsCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
			super(db, driver, editTable, query);
		}

		/** Private factory class necessary for rawQueryWithFactory() call */
		private static class Factory implements SQLiteDatabase.CursorFactory {
			@Override
			public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
				return new PointsCursor(db, driver, editTable, query);
			}
		}

		/* Accessor functions -- one per database column */
		public int getColPointId() { return getInt(getColumnIndexOrThrow("pointId"));}
		public String getColTitle() {return getString(getColumnIndexOrThrow("title"));}
		public int getColPlace() {return getInt(getColumnIndexOrThrow("place"));}
		public int getColRadius() {return getInt(getColumnIndexOrThrow("radius"));}
		public double getColLatitude() {return getDouble(getColumnIndexOrThrow("latitude"));}
		public double getColLongitude() {return getDouble(getColumnIndexOrThrow("longitude"));}
		public int getColVisited() {return getInt(getColumnIndexOrThrow("visited"));}
	}

}
