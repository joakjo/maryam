package se.su.dsv.maryam;

import android.provider.BaseColumns;

public final class DatabaseContract {
	private static final String TEXT_TYPE = " TEXT";
	private static final String INTEGER_TYPE = " INTEGER";
	private static final String REAL_TYPE = " REAL";
	private static final String COMMA_SEP = ",";
	
	public static final String SQL_CREATE_USER_DATA =
	    "CREATE TABLE " + UserData.TABLE_NAME + " (" +
	    UserData._ID + " INTEGER PRIMARY KEY," +
	    UserData.COLUMN_NAME_USER_ID + INTEGER_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_STAR_POS_X + REAL_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_STAR_POS_Y + REAL_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_MESSAGE + TEXT_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_SENDER + TEXT_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_ROUTE_ID + INTEGER_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_DATE + TEXT_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_SEEN_ANIMATION + INTEGER_TYPE + COMMA_SEP +
	    UserData.COLUMN_NAME_USER_LANG + TEXT_TYPE + COMMA_SEP +
	    " )";

	public static final String SQL_DELETE_USER_DATA =
	    "DROP TABLE IF EXISTS " + UserData.TABLE_NAME;

	public DatabaseContract() {
	}
	
	public static abstract class UserData implements BaseColumns {
		public static final String TABLE_NAME = "userdata";
		public static final String COLUMN_NAME_USER_ID = "userId";
		public static final String COLUMN_NAME_STAR_POS_X = "starPosX";
		public static final String COLUMN_NAME_STAR_POS_Y = "starPosY";
		public static final String COLUMN_NAME_MESSAGE = "message";
		public static final String COLUMN_NAME_SENDER = "sender";
		public static final String COLUMN_NAME_ROUTE_ID = "routeId";
		public static final String COLUMN_NAME_DATE = "date";
		public static final String COLUMN_NAME_SEEN_ANIMATION = "seenAnimation";
		public static final String COLUMN_NAME_USER_LANG = "userlang";
	}
	

}
