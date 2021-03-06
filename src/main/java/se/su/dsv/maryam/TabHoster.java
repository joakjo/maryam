package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class TabHoster extends FragmentActivity {
	public static final String		PROXIMITY_ALERT_ACTION	= "se.su.dsv.action.PROXY_ALERT";
	private FragmentTabHost			mTabHost;
	private boolean					doubleBackToExitPressedOnce;
	private final String			TAG						= "TabHoster";
	private Toast					mToast;
	private final SceneBroadcaster	mSceneBroadcaster		= new SceneBroadcaster();
	private LocalDatabase			db;
	private int						mId	= 1337;
	
	public static LocationManager mLocMan;
	private NotificationManager	mNotificationManager;

	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tab_hoster);
		
		
		db = Global.getInstance(this).getDatabase();
		
		mLocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		IntentFilter mIntentFilter = new IntentFilter(PROXIMITY_ALERT_ACTION);
		registerReceiver(mSceneBroadcaster, mIntentFilter);
		
		mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
		
		mTabHost.addTab(mTabHost.newTabSpec("BackToStartFragment")
				.setIndicator(getResources().getString(R.string.tab_scenes), getResources().getDrawable(R.drawable.icon_scene_tab)), 
				MainMapFragment.class, null);
		
		mTabHost.addTab(mTabHost.newTabSpec("Instructions")
				.setIndicator(getResources().getString(R.string.tab_instructoins), getResources().getDrawable(R.drawable.icon_instructions_tab)),
				InstructionFragment.class, null);
		mTabHost.addTab(mTabHost.newTabSpec("About")
				.setIndicator(getResources().getString(R.string.tab_about), getResources().getDrawable(R.drawable.icon_about_tab)),
				AboutFragment.class, null);
		mTabHost.addTab(mTabHost.newTabSpec("Stars")
				.setIndicator(getResources().getString(R.string.tab_sky), getResources().getDrawable(R.drawable.icon_stars_tab)),
				StarrySkyControllerFragment.class, null);
		
		mToast = Toast.makeText(this, getResources().getString(R.string.back_pressed), Toast.LENGTH_SHORT);
		
	}
	

	@Override
	public void onStop() {
		setUpNotification();
		
		if ((db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.OUTRO)
				|| (db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.QUEST_COMPLETE)) {
				try {
					MainMapFragment.getAudioService().stop();
				} catch (Exception e) {
				}
			}
		super.onStop();
	}

	@Override
	protected void onResume() {
		closeNotification();
		super.onResume();
	}
	
	// Exit with double back press 
	@Override
	public void onBackPressed() {
		if (doubleBackToExitPressedOnce) {
			if (mToast != null)
				mToast.cancel();
			cleanUpAndExit();
		} else {
			this.doubleBackToExitPressedOnce = true;
			mToast.show();

			// Go back to non clicked state after 2 seconds
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					doubleBackToExitPressedOnce = false;
				}
			}, 2000);
		}
	}
	
	
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.exit_activity_settings:
			cleanUpAndExit();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d("TEST", "klick");

	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.tab_hoster, menu);
	    return true;
	}
	
	
	

	private void setUpNotification() {
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.noti)
		        .setOngoing(true)
		        .setContentTitle(getResources().getString(R.string.notification_title))
		        .setContentText(getResources().getString(R.string.notification_message));
		// Creates an explicit intent for an Activity in your app
		
		Intent intent =
		        new Intent(this, TabHoster.class);
		// Sets the Activity to start in a new, empty task
		// Creates the PendingIntent
		PendingIntent notifyIntent =
		        PendingIntent.getActivity(
		        this,
		        0,
		        intent,
		        PendingIntent.FLAG_UPDATE_CURRENT
		);

		// Puts the PendingIntent into the notification builder
		mBuilder.setContentIntent(notifyIntent);
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		Notification noti = mBuilder.build();
		mNotificationManager.notify(mId , noti);
	}
	
	private void cleanUpAndExit() {
		ApplicationState.D = false;
		ApplicationState.SCENE_IS_PLAYING = false;
		try { unregisterReceiver(mSceneBroadcaster); } 
		catch (Exception e) {} 
		closeNotification(); 
		finish();
	}


	private void closeNotification() {
		try { mNotificationManager.cancel(mId); } 
		catch (Exception e) {}
	}

	@Override
	protected void onDestroy() {
		cleanUpAndExit();
		super.onDestroy();
	}
	
}
