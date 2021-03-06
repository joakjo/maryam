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
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MaryamMapFragment <E extends FragmentActivity> extends Fragment {
	private static GoogleMap		mMapObj;
	private SupportMapFragment		mMapFrag		= new SupportMapFragment();
	private LocalDatabase			db;

	private List<Point>				mPoints;
	private List<Point>				mVisitedPoints;
	private HashMap<Marker, Point>	mMarkerPoint;
	private List<PendingIntent>		mUsedIntents;

	private PolylineOptions			mRouteTaken;
	private float					mDefaultZoom;

	private final int				PIN_NOT_VISITED	= R.drawable.pointer_light;
	private final int				PIN_VISITED		= R.drawable.pointer_dark;
	
	private E context;
	private AsyncTask<String, Integer, String>	mProximityTask; 
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_maryam_map_fragment, container, false);
		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		ft.replace(R.id.maryam_map_fragment_placeholder, mMapFrag);
		ft.commit();
		
		context = (E) getActivity();
		
		db = Global.getInstance(getActivity()).getDatabase();
		
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		init();
		
		// Make sure that no proximity alerts are still active
		cleanUpProximityAlerts();
		
		// Add markers to map and zoom to the action
		if (mMapFrag != null) {
			mMapObj = mMapFrag.getMap();
			if (mMapObj != null) {
				mMapObj.clear();
				addPointsToMap();
				zoomToDest(mDefaultZoom);

				// Add the lines
				for(Point visitedPoint : mVisitedPoints)
					addLine(new LatLng(visitedPoint.getLat(), visitedPoint.getLng()));
				
				// Show user on map
				mMapObj.setMyLocationEnabled(true);
				
				// Set on click listeners for the markers
				if(ApplicationState.D) setFakeProxyAlert();
				
				mProximityTask = new SetProximityTask().execute("");
			}
		}
	}
	
	@Override
	public void onPause() {
		zoomToDest(mDefaultZoom);
		ArrayList<Integer> 	points = new ArrayList<Integer>(db.getVisited());
		ArrayList<XY> 		pixels = new ArrayList<XY>(db.getStarPositions());
		
		if(pixels.size() < points.size()) {
			try {
				Point p = db.getCurrentRoute().getPoints().get(points.get(points.size() - 1));
				Projection proj = mMapObj.getProjection();
				android.graphics.Point po = proj.toScreenLocation(p.getLatLng());
				db.addStarPosition(new XY(po.x, po.y));
			} catch (Exception e) { e.printStackTrace(); }

		}

		// If it's the last point before returning to the start
		pixels = new ArrayList<XY>(db.getStarPositions());
		if (pixels.size() == mPoints.size() - 1) {
			for(Point tmp : mPoints) {
				Projection proj = mMapObj.getProjection();
				android.graphics.Point po = proj.toScreenLocation(tmp.getLatLng());
				XY XYtmp = new XY(po.x, po.y);
				if (!pixels.contains(XYtmp)) {
					db.addStarPosition(XYtmp);
					break;
				}
			}
		}
		
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		cleanUpProximityAlerts();
		mMapObj.setMyLocationEnabled(false);
		mMapObj.clear();
		if(mProximityTask != null)
			mProximityTask.cancel(true);
	}
	
	private void addPointsToMap() {
		for(Point point : mPoints) {
			MarkerOptions options = new MarkerOptions();
			options.position(new LatLng(point.getLat(), point.getLng()))
			.title(point.getSceneName())
			.snippet(point.getTitle());
			if(mVisitedPoints.contains(point))
				options.icon(getCustomMarker(0.8f, 0.8f, PIN_VISITED));
			else
				options.icon(getCustomMarker(0.8f, 0.8f, PIN_NOT_VISITED));
				
			Marker m = mMapObj.addMarker(options);
			mMarkerPoint.put(m, point);
		}
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
	
	private void zoomToDest(float zoomLevel) {
		LatLng startPoint = new LatLng(mPoints.get(0).getLat(),mPoints.get(0).getLng());
		mMapObj.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, zoomLevel));
	}
	
	private void addLine(LatLng point) {
		mRouteTaken.add(point);
		Polyline line = mMapObj.addPolyline(mRouteTaken);
		line.setWidth(line.getWidth()-2);
		line.setColor(Color.rgb(89, 98, 173)); // 
	}
	
	private void setProximityAlert(Point point) {
		float radius = point.getRadius();
		long expiration = 1000*60*30; // Expire in 30 min

		Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
		intent.putExtra("sceneNumber", point.getPlace());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, point.getPlace(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
		mUsedIntents.add(pendingIntent);
		
		TabHoster.mLocMan.addProximityAlert(point.getLat(), point.getLng(), radius, expiration, pendingIntent);
	}
	
	private void cleanUpProximityAlerts() {
		for (Point point : mPoints) {
			Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
			intent.putExtra("sceneNumber", point.getPlace());
			PendingIntent pendingIntent = PendingIntent
					.getBroadcast(getActivity(), point.getPlace(), intent,
							PendingIntent.FLAG_CANCEL_CURRENT);
			TabHoster.mLocMan.removeProximityAlert(pendingIntent);
			pendingIntent.cancel();
		}
	}
	
	private void setFakeProxyAlert() {
		mMapObj.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

	        public void onInfoWindowClick(Marker marker) {
				if (!mVisitedPoints.contains(mMarkerPoint.get(marker))) {
					DummyReceiver.onReceive(getActivity(), mMarkerPoint.get(marker).getPlace());
				}
	        }
	    });
	}
	
	
	 ////////////////////////////////////////
	/////		 INIT METHODS	       /////
   ////////////////////////////////////////
	
	private void init() {
		// Initialized the map
		try {
			MapsInitializer.initialize(getActivity());
		} catch (GooglePlayServicesNotAvailableException e) {
			e.printStackTrace();
		}
		
		mDefaultZoom 	= 15;
		mPoints 		= initPoints();
		mRouteTaken 	= new PolylineOptions();
		mVisitedPoints 	= initVisited();
		mMarkerPoint	= new HashMap<Marker, Point>();
		mUsedIntents	= new ArrayList<PendingIntent>();
		
//		mLocMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//		IntentFilter intentFilter = new IntentFilter(PROXIMITY_ALERT_ACTION);
//		mSceneBroadcaster = new SceneBroadcaster();
//		getActivity().registerReceiver(mSceneBroadcaster, intentFilter);
	}
	
	private List<Point> initPoints(){
		ArrayList<Point> tmp = new ArrayList<Point>();
		HashMap<Integer, Point> points = db.getCurrentRoute().getPoints();
		for(int i = 0; i < points.size(); i++){
			tmp.add(points.get(i));
		}
		return tmp;
	}
	
	private List<Point> initVisited() {
		ArrayList<Point> tmp = new ArrayList<Point>();
		try {
			for (Integer i : db.getVisited())
				tmp.add(mPoints.get(i));
		}catch (Exception e) {};		

		return tmp;
	}
	
	private class SetProximityTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... arg0) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Set proximity alerts
			try {
				for(Point point : mPoints)
					if(!mVisitedPoints.contains(point))
						setProximityAlert(point);
			} catch(Exception e) {
				
			}
			
			return null;
		}
	}
	
	

}
