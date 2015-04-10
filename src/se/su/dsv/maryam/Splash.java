package se.su.dsv.maryam;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.NoHttpResponseException;

import se.su.dsv.maryam.R;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class Splash extends Activity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {
	
	private static final String	TAG			= "SPLASH";
	private LocalDatabase		db;
	private boolean				mWorkIsDone	= false;
	private boolean				mClicked	= false;
	private boolean				mFirstVisit;
	private Thread				mThread;
	private long				msAcc		= 0;
	private long				splashTime	= 4000;
	private int					resumeState;
	private LocationListener	mCoarseLis;
	private LocationListener	mFineLis;
	private LocationManager		mLocMan;
	
	private LocationClient 	mLocationClient;
	private LocationRequest mLocationRequestHigh;
	private LocationRequest mLocationRequestLow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		resumeState = getIntent().getIntExtra("resume", PreSplash.NO_RESUME);
		
		
		setUpThread();
		setTouchListener();
		mFirstVisit = true;
		
//		setUpLocationListener();
		
		// new
		checkForPlayService();
		createLocationRequest();
		mLocationClient = new LocationClient(this, this, this);
	}

	private void createLocationRequest() {
		mLocationRequestHigh = LocationRequest.create();
		mLocationRequestHigh.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequestHigh.setInterval(200);
		mLocationRequestHigh.setFastestInterval(200);

		mLocationRequestLow = LocationRequest.create();
		mLocationRequestLow.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		mLocationRequestLow.setInterval(200);
		mLocationRequestLow.setFastestInterval(200);
	}

	private void checkForPlayService() {
		int errorCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (errorCode != ConnectionResult.SUCCESS) {
			Log.d(TAG, "FAIL");
			GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
		} else
			Log.d(TAG, "SUCCESS");
	}
	
	private void setUpLocationListener() {
		try {
			mLocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			mCoarseLis = new LocationListener() {
				public void onLocationChanged(Location location) {
					mLocMan.removeUpdates(this);
				}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
				public void onProviderEnabled(String provider) {}
				public void onProviderDisabled(String provider) {}
			};
			Criteria coarse = new Criteria();
			coarse.setAccuracy(Criteria.ACCURACY_COARSE);
			mLocMan.requestLocationUpdates(mLocMan.getBestProvider(coarse, true), 0, 0, mCoarseLis);
			
			mFineLis = new LocationListener() {
				public void onLocationChanged(Location location) {
					mLocMan.removeUpdates(this);
				}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
				public void onProviderEnabled(String provider) {}
				public void onProviderDisabled(String provider) {}
			};
			Criteria fine = new Criteria();
			fine.setAccuracy(Criteria.ACCURACY_FINE);
			mLocMan.requestLocationUpdates(mLocMan.getBestProvider(fine, true), 0, 0, mFineLis);
		} catch(Exception e) {
//			e.printStackTrace()
			//this should work
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mLocationClient.connect();
	}
	
	@Override
	protected void onResume() {
		CheckHelper.checkFor3G(this); 
		CheckHelper.checkForGPS(this);
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		try { mLocMan.removeUpdates(mCoarseLis); mLocMan.removeUpdates(mFineLis); }
		catch (Exception e) {;}
		finally { mLocationClient.disconnect(); }
		super.onStop();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus) {
			if(mFirstVisit) {
				mThread.start();
				mFirstVisit = false;
			}
			initDB();
		}
	}

	private void initDB() {
		try {
			db = Global.getInstance(this).getDatabase();
			if (resumeState == PreSplash.NO_RESUME)
				db.eraseDatabaseForNewRoute();
			db.cleanDatabaseFromComments();
			
			new DownloadDataTask().execute("");
			
			
		} catch (Exception e) {
			CheckHelper.checkFor3G(this);
		}

	}

	private void setTouchListener() {
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.splash_parent);
		rl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mClicked = true;
			}
		});
	}
	
	private void setUpThread() {
		mThread = new Thread() {
			@Override
			public void run() {
				try {
					while (msAcc < splashTime && !mClicked) {
						msAcc = msAcc + 100;
						sleep(100);
					}
					while (!mWorkIsDone)
						sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					finishSplash();
				}
			}
		};
	}

	private void finishSplash() {
		// Just a basic forwarding
		Intent i = new Intent(this, TabHoster.class);
		i.putExtra("init", false);
		startActivity(i);
		Splash.this.finish();
	}
	
	private class DownloadDataTask extends AsyncTask<String, Integer, Integer> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(String... urls) {
			try {
				db.addRoutesFromAPI(DBGetter.getRoutes());
				db.addCommentsFromAPI(DBGetter.getComments(100, "EN"));//db.getLang()));
				db.addCommentsFromAPI(DBGetter.getComments(100, "SE"));//db.getLang()));
				db.closeDatabase();
			} catch (NoHttpResponseException e) {
				e.printStackTrace();
				return 1;
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				return 2;
			} catch (IOException e){
				e.printStackTrace();
				return 3;
			}
			
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			switch (result){
			case 1:
			case 2:
				makeDialogBox();
				break;
			case 3:
				CheckHelper.checkFor3G(Splash.this);
				break;
			case 0:
				mWorkIsDone = true;
				break;
			}
		}
		

		
	}
	private void makeDialogBox() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_NEUTRAL:
					Splash.this.finish();
					break;
				}
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(Splash.this);
		builder
		.setTitle(Splash.this.getResources().getString(R.string.downloading_error_title))
		.setMessage(Splash.this.getResources().getString(R.string.downloading_error_message)).setNeutralButton("Ok", dialogClickListener).show();
		
	}

	@Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.d(TAG, msg);
        
        msg = "Removing updates";
        mLocationClient.removeLocationUpdates(this);
        Log.d(TAG, msg);
    }

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				connectionResult.startResolutionForResult(this, 9000);
			} catch (IntentSender.SendIntentException e) {
				e.printStackTrace();
			}
		} else {
			Toast.makeText(this, "FAIL: " + connectionResult.getErrorCode(),
					Toast.LENGTH_SHORT).show();
			Log.d(TAG, "FAIL: " + connectionResult.getErrorCode());
		}
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		try {
		// Display the connection status
		Location loc = mLocationClient.getLastLocation();
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		
//		Toast.makeText(this, "Last known: "+lat+", "+lon, Toast.LENGTH_LONG).show();
		
		mLocationClient.requestLocationUpdates(mLocationRequestHigh, this);
		mLocationClient.requestLocationUpdates(mLocationRequestLow, this);
		}
		catch (NullPointerException npe) {
			CheckHelper.showErrorDialog(this, CheckHelper.POSITION_TYPE);
		}
	}
	
	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
		mLocationClient.removeLocationUpdates(this);
	}
	
}
