package se.su.dsv.maryam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import se.su.dsv.maryam.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public  class  FindStartPointFragment <E extends FragmentActivity>  extends Fragment {
	private static GoogleMap		mMapObj;
	private LocalDatabase			db;
	private HashMap<Integer, Route>	routeMap		= null;
	private final int				PIN_NOT_VISITED	= R.drawable.pointer_light;
	private SupportMapFragment		mMapFrag		= new SupportMapFragment();
	private LatLng	mPosition;
	private int	mRadius, mClickForDebug;
	private E context;
	private AsyncTask<String, Integer, String>	mProximityTask; 

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		db = Global.getInstance(getActivity()).getDatabase();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Replacing the placeholder for the map with the real map
		context = (E) getActivity();
		View view = inflater.inflate(R.layout.fragment_find_start_point, container, false);
		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		ft.replace(R.id.map_placeholder, mMapFrag);
		ft.commit();
		

		init();

		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		routeMap = db.getRoutes();
		mClickForDebug = 0;
		
		Route closestRoute = routeMap.get(getClosestRoute());
		System.out.println("closestRoute : " + closestRoute);
		System.out.println("getPoints: "+closestRoute.getPoints());
		System.out.println("getPoints(0)" + closestRoute.getPoints().get(2));
		
		
		mPosition = closestRoute.getPoints().get(0).getLatLng();
		String sceneName = closestRoute.getPoints().get(0).getSceneName();
		String title = closestRoute.getPoints().get(0).getTitle();
		String routeName = closestRoute.getTitle();
		mRadius = closestRoute.getPoints().get(0).getRadius();
		
		removeProximityAlert(mPosition, mRadius);

		if (mMapFrag != null) {
			mMapObj = mMapFrag.getMap();
			if (mMapObj != null) {
				mMapObj.clear();
				zoomToDest(mPosition, 12);
				setMarker(mPosition, sceneName, title);
				mMapObj.setMyLocationEnabled(true);// show user on map
				setFakeProxyAlert();
			}
		}
		setDescriptionText(title, routeName);
		mProximityTask = new SetProximityTask().execute("");
		
	}

	@Override
	public void onDestroy() {
		cleanUp();
//		getActivity().unregisterReceiver(mSceneBroadcaster);
		mMapObj.setMyLocationEnabled(false);
		mMapObj.clear();
		if(mProximityTask != null)
			mProximityTask.cancel(true);
//		mLocMan = null;
		
		super.onDestroy();
	}

	private void cleanUp() {
//		if(mLocMan == null)
//			mLocMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
		intent.putExtra("sceneNumber", -1);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, intent,  PendingIntent.FLAG_CANCEL_CURRENT);
		TabHoster.mLocMan.removeProximityAlert(pendingIntent);
		pendingIntent.cancel();
	}
	
	private void setDescriptionText(String title, String routeName) {
		// Intro text with the city variable
		TextView upperDescriptionView = (TextView) getView().findViewById(R.id.text_upper_description);
		String upperDescription = getActivity().getString(R.string.start_point_text);
		upperDescriptionView.setText(upperDescription);
		upperDescriptionView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));
		
		TextView titleView = (TextView) getView().findViewById(R.id.start_title_text);
		titleView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_bold_alt.otf"));
	}

	private void zoomToDest(LatLng position, int zoomLevel) {
		mMapObj.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomLevel));
	}

	private void setMarker(LatLng position, String sceneName, String title) {
		Marker point = mMapObj.addMarker(new MarkerOptions()
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

	private int getClosestRoute() {
		// Get the users latest known location
		Criteria criteria = new Criteria();
		String bestProvider = TabHoster.mLocMan.getBestProvider(criteria, false);
		Location locationOfUser = TabHoster.mLocMan.getLastKnownLocation(bestProvider);
//		mLocMan.requestSingleUpdate(criteria, listener, Looper.getMainLooper());

		int closestRouteId = -1;
		float tmpClosest = Float.MAX_VALUE;

		// Find the closest start point based on the users location
		for (Map.Entry<Integer, Route> e : routeMap.entrySet()){
			Point startingPoint = e.getValue().getPoints().get(0);
			float[] res = new float[3];
//			Location.distanceBetween(59.405782, 17.946253, startingPoint.getLat(), startingPoint.getLng(), res);
			try {
				Location.distanceBetween(locationOfUser.getLatitude(), locationOfUser.getLongitude(), startingPoint.getLat(), startingPoint.getLng(), res);
			}
			catch (NullPointerException nep) {
				this.showErrorDialog();
			}
			if (res[0] < tmpClosest){
				tmpClosest = res[0];
				closestRouteId = e.getKey();
			}
		}
		if (closestRouteId == -1)
			db.setCurrentRoute(1);
		else 
			db.setCurrentRoute(closestRouteId);
		return closestRouteId;
	}

	private void setProximityAlert(LatLng position, int radius2) {
		float radius = radius2;
		long expiration = 1000*60*30; // Expire in 30 min

		Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
		intent.putExtra("sceneNumber", -1);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		TabHoster.mLocMan.addProximityAlert(position.latitude, position.longitude, radius, expiration, pendingIntent);
	}
	private void removeProximityAlert(LatLng position, int radius2) {
		float radius = radius2;
		long expiration = 1000*60*30; // Expire in 30 min
		
		Intent intent = new Intent(TabHoster.PROXIMITY_ALERT_ACTION);
		intent.putExtra("sceneNumber", -1);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		TabHoster.mLocMan.removeProximityAlert(pendingIntent); // to make sure that two intents aren't fired
	}
	
	

	private void setFakeProxyAlert() {
		mMapObj.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			public void onInfoWindowClick(Marker marker) {
				if(ApplicationState.D)
					DummyReceiver.onReceive(getActivity(), -1);
				
				if (++mClickForDebug == 5 && ApplicationState.D == false){
					ApplicationState.D = true;
					Toast.makeText(getActivity(), "Rundan nu klickbar!", Toast.LENGTH_SHORT).show();
				}
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

		// setup the receiver and Location manager
//		mLocMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//		IntentFilter mIntentFilter = new IntentFilter(PROXIMITY_ALERT_ACTION);
//		mSceneBroadcaster = new SceneBroadcaster();
//		getActivity().registerReceiver(mSceneBroadcaster, mIntentFilter);

	}
	
	private void showErrorDialog() {
		final Context context = getActivity();
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		String title, message;
		title = context.getResources().getString(R.string.no_position_null_title);
		alertDialogBuilder.setTitle(title);
		message = context.getResources().getString(R.string.no_position_null_message);

		// set dialog message
		alertDialogBuilder
			.setMessage(message)
			.setCancelable(false)
			.setNegativeButton(context.getResources().getString(R.string.no_position_null_close),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
								getActivity().finish();
						}
					});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
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
				setProximityAlert(mPosition, mRadius);
			} catch(Exception e) {
				
			}
			
			return null;
		}
	}
	
	

}
