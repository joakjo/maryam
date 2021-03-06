package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import se.su.dsv.maryam.AudioService.LocalBinder;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainMapFragment extends Fragment {
	private LocalDatabase	db;
	private Thread	mLoopingThread;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_main_map, container, false);

		
		Intent intent = new Intent(getActivity(), AudioService.class);
		getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		db = Global.getInstance(getActivity()).getDatabase();

//		 baseCaseSwitchFragment();

		return view;
	}

	private void baseCaseSwitchFragment() {
		if (mMediaService != null
				&& !((db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.OUTRO) || (db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.QUEST_COMPLETE))) {
			switchFragment(mMediaService.getCurrentSceneNumber());
		} else
			switchFragment(-1);
	}

	@Override
	public void onDestroy() {
		if (mBound) {
			try {
				mMediaService.stop();
			} catch (Exception e) {}
		
			getActivity().unbindService(mConnection);
			getActivity().stopService(new Intent(getActivity(), AudioService.class));
		}
		mBound = false;
		mMediaService = null;
		
		joinLoopingThread();
		super.onDestroy();
	}
	
	private void joinLoopingThread() {
		try {
			while(mLoopingThread.isAlive())
				mLoopingThread.join();
		} catch (Exception e) {
		}
		mLoopingThread = null;
	}
	
	public void switchFragment(int sceneNumber) {
		CheckHelper.checkFor3G(getActivity());
		CheckHelper.checkForGPS(getActivity());
		
		final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		Fragment toRemove = getChildFragmentManager().findFragmentByTag("mapTag");
		if (toRemove != null) ft.remove(toRemove);
		
		
		
				
				
		
//		// If a scene is to be played and displayed
		if (sceneNumber != -1) {
			Bundle b = new Bundle();
			b.putInt("sceneNumber", sceneNumber);
			SceneActivity sa = new SceneActivity();
			sa.setArguments(b);
			ft.add(R.id.fragment_main_container, sa, "mapTag");
		}
		// Else perform a regular fragment switching
		else {
			
			//Start looped music if resumed
			

			switch (db.getState(ApplicationState.MAIN_MAP_STATE)) { // Current state

			case ApplicationState.NO_STARTING_POINT:
				ft.add(R.id.fragment_main_container, new FindStartPointFragment(), "mapTag");
				break;
				
			case ApplicationState.MAKE_READY:
				ft.add(R.id.fragment_main_container, new StartPointFound(), "mapTag");
				break;
				
			case ApplicationState.IN_QUEST:
				resumeLooped();
				ft.add(R.id.fragment_main_container, new MaryamMapFragment(), "mapTag");
				break;
				
			case ApplicationState.RETURN_TO_START:
				resumeLooped();
				ft.add(R.id.fragment_main_container, new BackToStartFragment(), "mapTag");
				break;
				
			case ApplicationState.OUTRO:
			case ApplicationState.QUEST_COMPLETE:
				ft.add(R.id.fragment_main_container, new ShowStarPathFragment(), "mapTag");
				break;
				
			default:
				break;
			}
		}
		
		ft.commitAllowingStateLoss();
		
	}

	private void resumeLooped() {
		if(mBound && !mMediaService.isPlayingLooped()) {
			int lastPlayedScene = db.getVisited().get(db.getVisited().size()-1);
			String fileName = "scen" + lastPlayedScene+"_looped";
			int scene = getResources().getIdentifier(fileName, "raw", getActivity().getPackageName());
			mMediaService.startNewMedia(scene, true, -1);
		}
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private static AudioService		mMediaService;
	private static boolean				mBound;
	private ServiceConnection	mConnection	= new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
				// We've bound to LocalService, cast the IBinder and get LocalService instance
				LocalBinder binder = (LocalBinder) service;
				mMediaService = binder.getService();
				mBound = true;
				switchFragment(mMediaService.getCurrentSceneNumber());
			}
			
			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				mBound = false;
			}
		};

	public static AudioService getAudioService() {
		if (mBound)
			return mMediaService;
		else
			return null;
	}
	

	
	private void checkForGPS() {
	     LocationManager lm = null;
	     boolean gps_enabled = false,network_enabled = false;
		if (lm == null)
			lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			network_enabled = lm
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}
		if(!gps_enabled /*&& !network_enabled*/)
			showErrorDialog(2);
	}
	
	private void showErrorDialog(int type) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
		String title, message;
		if(type == 1) {
			title = getResources().getString(R.string.no_connection_title);
			message = getResources().getString(R.string.no_connection_message); 
		} else {
			title = getResources().getString(R.string.no_position_title);
			message = getResources().getString(R.string.no_position_message); 
		}
		
		// set title
		alertDialogBuilder.setTitle(title);

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setNegativeButton(getResources().getString(R.string.check_helper_close),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// if this button is clicked, close
								// current activity
								getActivity().finish();
							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}


}
