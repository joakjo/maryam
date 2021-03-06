package se.su.dsv.maryam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.su.dsv.maryam.R;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class BackToStartFragment <E extends FragmentActivity> extends Fragment {
	private static GoogleMap		mMapObj;
	private SupportMapFragment		mMapFrag		= new SupportMapFragment();

	private Point					mEndingPoint;
	private List<Point>				mPoints;
	private HashMap<Marker, Point>	mMarkerPoint;

	private final String			TAG				= "BackToStartFragment";
	private static SceneBroadcaster	mSceneBroadcaster;
	private ViewGroup				root;
	private Typeface				mFont;
	private final int				PIN_NOT_VISITED	= R.drawable.pointer_light;
	private final int				PIN_VISITED		= R.drawable.pointer_dark;
	private LocalDatabase			db;
	private List<Point>				mVisitedPoints;
	private PolylineOptions	mRouteTaken;
	private int	mDefaultZoom;
	private LocationListener	mLocLis;
	private LatLng	mPosition;
	private AsyncTask<String, Integer, String>	mProximityAlert;
	private E context;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Replacing the placeholder for the map with the real map
		View view = inflater.inflate(R.layout.fragment_find_ending_point, container, false);
		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		ft.replace(R.id.map_placeholder, mMapFrag);
		ft.commit();
		
		context = (E) getActivity();

		init();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		mPosition = mEndingPoint.getLatLng();
		String sceneName = mEndingPoint.getSceneName();
		String title = mEndingPoint.getTitle();
		String routeName = mEndingPoint.getTitle();

		if (mMapFrag != null) {
			mMapObj = mMapFrag.getMap();
			if (mMapObj != null) {
				mMapObj.clear();
				zoomToDest(mPosition, mDefaultZoom);
				setMarker(mPosition, sceneName, title);
				mMapObj.setMyLocationEnabled(true);// show user on map
				addPointsToMap();
				
				// Add the lines
				mRouteTaken = new PolylineOptions();
				for(Point visitedPoint : mVisitedPoints)
						addLine(new LatLng(visitedPoint.getLat(), visitedPoint.getLng()));
				
				if(ApplicationState.D) setFakeProxyAlert();
			}
		}
		mProximityAlert = new SetProximityTask().execute("");
		
		setFont();
	}
	
	private void addPointsToMap() {
		for(Point point : mPoints) {
			MarkerOptions options = new MarkerOptions();
			options.position(new LatLng(point.getLat(), point.getLng()))
			.title(point.getSceneName())
			.snippet(point.getTitle());
			if(mVisitedPoints.contains(point) && point.getPlace() != 0)
				options.icon(getCustomMarker(0.8f, 0.8f, PIN_VISITED));
			else
				options.icon(getCustomMarker(0.8f, 0.8f, PIN_NOT_VISITED));
				
			mMapObj.addMarker(options);
		}
	}
	
	private List<Point> initPoints(){
		ArrayList<Point> tmp = new ArrayList<Point>();
		HashMap<Integer, Point> points = db.getCurrentRoute().getPoints();
		for(int i = 0; i < points.size(); i++){
			tmp.add(points.get(i));
		}
		return tmp;
	}
	
	private void addLine(LatLng point) {
		mRouteTaken.add(point);
		Polyline line = mMapObj.addPolyline(mRouteTaken);
		line.setWidth(line.getWidth()-2);
		line.setColor(Color.rgb(89, 98, 173)); // 
	}
	
	private List<Point> initVisited() {
		ArrayList<Point> tmp = new ArrayList<Point>();
		try {
			for (Integer i : db.getVisited())
				tmp.add(mPoints.get(i));
		}catch (Exception e) {};		

		return tmp;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
		intent.putExtra("sceneNumber", mEndingPoint.getPlace());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, intent,  PendingIntent.FLAG_CANCEL_CURRENT);
		pendingIntent.cancel();
		mMapObj.setMyLocationEnabled(false);
		if(mProximityAlert != null)
			mProximityAlert.cancel(true);
	}
	
	private void zoomToDest(LatLng position, int zoomLevel) {
		mMapObj.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel));
	}

	private void setMarker(LatLng position, String sceneName, String title) {
		mMapObj.addMarker(new MarkerOptions()
											.position(position)
											.title(sceneName)
											.snippet(title)
											.icon(getCustomMarker(0.8f, 0.8f, PIN_NOT_VISITED)));
	}
	
	private BitmapDescriptor getCustomMarker(float scaleWidth, float scaleHeight, int marker){
		Bitmap originalMarker = BitmapFactory.decodeResource(getResources(), marker);
	    int width = originalMarker.getWidth();
	    int height = originalMarker.getHeight();

	    Matrix matrix = new Matrix();
	    matrix.postScale(scaleWidth, scaleHeight);
	    Bitmap bitmap = Bitmap.createBitmap(originalMarker, 0, 0, width, height, matrix, true);
	    BitmapDescriptor bm = BitmapDescriptorFactory.fromBitmap(bitmap);
	    
	    return bm;
	} 

	private void setProximityAlert(LatLng position) {
		float radius = mEndingPoint.getRadius();
		long expiration = 1000*60*30; // Expire in 30 min

		Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
		intent.putExtra("sceneNumber", mEndingPoint.getPlace());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		TabHoster.mLocMan.addProximityAlert(position.latitude, position.longitude, radius, expiration, pendingIntent);
	}

	private void setFakeProxyAlert() {
		mMapObj.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			public void onInfoWindowClick(Marker marker) {
				DummyReceiver.onReceive(getActivity(), mEndingPoint.getPlace());
			}
		});
	}

	private void init() {
		// Initialized the map
		try {
			MapsInitializer.initialize(getActivity());
		} catch (GooglePlayServicesNotAvailableException e) {
			e.printStackTrace();
		}
		
		db = Global.getInstance(getActivity()).getDatabase();
		
		mDefaultZoom = 15;
		mPoints = initPoints();
		mVisitedPoints 	= initVisited();
		Point startPoint = db.getCurrentRoute().getPoints().get(0);
		mEndingPoint = Point.getEndPointFromStartPoint(startPoint);
		 
		// setup the receiver and Location manager
//		mLocMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//		IntentFilter mIntentFilter = new IntentFilter(PROXIMITY_ALERT_ACTION);
//		mSceneBroadcaster = new SceneBroadcaster();
//		getActivity().registerReceiver(mSceneBroadcaster, mIntentFilter);

	}
	
	public void setFont() {
		TextView titleView = (TextView) getActivity().findViewById(R.id.end_title_text);
		titleView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_bold_alt.otf"));
		
		TextView description_text = (TextView) getActivity().findViewById(R.id.text_upper_end_description);
		description_text.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));
	}
	
	private class SetProximityTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... arg0) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				setProximityAlert(mPosition);
			} catch(Exception e) {
				
			}
			
			return null;
		}
	}
	
		private void removetProximityAlert(LatLng position) {
			Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
			intent.putExtra("sceneNumber", mEndingPoint.getPlace());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
			
			TabHoster.mLocMan.removeProximityAlert(pendingIntent); // to make sure that two intents aren't fired
	}
	
}
