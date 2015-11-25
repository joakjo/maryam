package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

public class SceneActivity extends Fragment {
	private static final String	TAG	= "SCENE_ACTIVITY";
	private ImageButton		mBtn_restart, mBtn_pause, mBtn_resume;
	private TextView		mText_restart, mText_pause, mText_resume, mTextTime;
	private AudioService	mMediaService;
	private boolean			mBound	= false; // check if connection to the service is established
	private int				mSceneNumber;
	private Handler			mHandler;
	private TextView		mSceneNameTextView;
	private TextView		mSceneNumberTextView;
	private TextView		mTextTimeText;
	private LocalDatabase	db;
	private boolean	mLocalIsPlaying = false;

	/*
	 * Activity callbacks
	 */

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_scene, container, false);

		mHandler = new Handler();
		mSceneNumber = getArguments().getInt("sceneNumber", -1);

		setUpAudioService();

		db = Global.getInstance(getActivity()).getDatabase();

		return view;
	}

	@Override
	public void onResume() {
		if (mMediaService != null && !mMediaService.isPlaying() && !mMediaService.isPaused())
			returnToMap();
		if (mSceneNumber == -1) {
			onDestroy();
		} else {
			// Check if the media is playing already
			initViews();
			if (mMediaService.isPaused())
				toPausedState();
			else
				toPlayingState();
			mHandler.post(secondUpdater);
		}
		super.onResume();
	}

	@Override
	public void onDestroy() {
		if (mHandler != null)
			mHandler.removeCallbacks(secondUpdater);
		
		mLocalIsPlaying = false;
		super.onDestroy();
	}

	/*
	 * Click events for each media button
	 */
	private void restartClick() {
		// Restart the audio playback
		if (mBound) {
			mMediaService.seekTo(0);
			mMediaService.start();
			toPlayingState();
		}
	}

	private void pauseClick() {
		// Pause the audio playback
		if (mBound) {
			mMediaService.pause();
			toPausedState();
		}
	}

	private void resumeClick() {
		// Resume the audio playback
		if (mBound) {
			mMediaService.start();
			toPlayingState();
		}
	}

	public void dummyGotoMap() {
		returnToMap();
	}
	
	private void returnToMap() {
		db.addVisited(mSceneNumber);
		if (mBound) {
			mMediaService.stop();
			String fileName;
			int routeSize = db.getCurrentRoute().getPoints().size();

			// All nodes visited
			if (db.getVisited().size() == routeSize + 1) {
				fileName = "scen_ending";
				db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.OUTRO);
				db.setState(ApplicationState.STAR_SKY_STATE, ApplicationState.SKY_OPEN_NOT_POSTED);
			} else
				// Keep going in the IN_QUEST state
				fileName = "scen" + mSceneNumber + "_looped";

			// All but last node are visited
			if (db.getVisited().size() == db.getCurrentRoute().getPoints().size())
				db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.RETURN_TO_START);

			int scene = getResources().getIdentifier(fileName, "raw", getActivity().getPackageName());
			mMediaService.startNewMedia(scene, true, -1);
		}
		MainMapFragment parent = (MainMapFragment) getParentFragment();
		ApplicationState.SCENE_IS_PLAYING = false;
		parent.switchFragment(-1);
	}
	
	/*
	 * The two states of the media playback 
	 */
	
	private void toPlayingState() {
		// Disable and hide restart and resume buttons
		mBtn_restart.setVisibility(View.INVISIBLE);
		mBtn_restart.setEnabled(false);
		mText_restart.setVisibility(View.INVISIBLE);

		mBtn_resume.setVisibility(View.INVISIBLE);
		mBtn_resume.setEnabled(false);
		mText_resume.setVisibility(View.INVISIBLE);

		// Enable and show pause button
		mBtn_pause.setVisibility(View.VISIBLE);
		mBtn_pause.setEnabled(true);
		mText_pause.setVisibility(View.VISIBLE);
		
		// Start updater
		mHandler.post(secondUpdater);

		// Make scene number text gray
		mSceneNumberTextView.setText(getResources().getString(R.string.media_playing_scene)+" " + (db.getVisited().size()+1));
		mSceneNumberTextView.setTextColor(Color.parseColor("#FFFFFF"));
		
		mLocalIsPlaying = true;
	}

	private void toPausedState() {
		// Disable and hide pause button
		mBtn_pause.setVisibility(View.INVISIBLE);
		mBtn_pause.setEnabled(false);
		mText_pause.setVisibility(View.INVISIBLE);

		// Enable and show restart and resume buttons
		mBtn_restart.setVisibility(View.VISIBLE);
		mBtn_restart.setEnabled(true);
		mText_restart.setVisibility(View.VISIBLE);

		mBtn_resume.setVisibility(View.VISIBLE);
		mBtn_resume.setEnabled(true);
		mText_resume.setVisibility(View.VISIBLE);
		
		// Pause updater
		mHandler.removeCallbacks(secondUpdater);
		
		// Set the time if returned to this fragment in a paused state
		int timeLeft = mMediaService.getTotalDuration() - mMediaService.getCurrentPosition();
		String timeString = milliSecondsToTimer(timeLeft);
		mTextTime.setText(timeString);
		
		// Make scene number text gray
		mSceneNumberTextView.setText(getResources().getString(R.string.media_pause)+" " + (db.getVisited().size()+1));
		mSceneNumberTextView.setTextColor(Color.parseColor("#7A7A7A"));
		
		mLocalIsPlaying = false;
	}
	
	private void initViews() {
		Typeface demi = Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf");
		Typeface bold = Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_bold_alt.otf");
		
		mBtn_restart  = 	(ImageButton) getActivity().findViewById(R.id.button_restart);
		mText_restart =	(TextView)	  getActivity().findViewById(R.id.text_restart);
		mText_restart.setTypeface(bold);
		mBtn_restart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { restartClick(); } 
		});

		mBtn_pause  	 = (ImageButton)  getActivity().findViewById(R.id.button_pause);
		mText_pause   = (TextView) 	  getActivity().findViewById(R.id.text_pause);
		mText_pause.setTypeface(bold);
		mBtn_pause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { pauseClick(); } 
		});

		mBtn_resume   = (ImageButton)  getActivity().findViewById(R.id.button_resume);
		mText_resume  = (TextView) 	  getActivity().findViewById(R.id.text_resume);
		mText_resume.setTypeface(bold);
		mBtn_resume.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { resumeClick(); } 
		});
		
		mTextTimeText	  = (TextView)	getActivity().findViewById(R.id.text_time_text);
		mTextTimeText.setTypeface(demi);
		mTextTime	  = (TextView)	getActivity().findViewById(R.id.text_time_num);
		mTextTime.setTypeface(demi);
		
		mSceneNameTextView = ((TextView) getActivity().findViewById(R.id.text_scene_name));
		mSceneNameTextView.setText(Point.getStaticSceneName(mSceneNumber));
		mSceneNameTextView.setTypeface(bold);
		
		if(ApplicationState.D) {
			mSceneNameTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) { returnToMap(); } 
			});
		}
		
		mSceneNumberTextView = ((TextView) getActivity().findViewById(R.id.text_scene_number));
		mSceneNumberTextView.setTypeface(bold);
	}
	
	private void setUpAudioService() {
		mMediaService = MainMapFragment.getAudioService();
		mBound = true;
		if(mMediaService.isPaused())
			toPausedState();
		else if(!mMediaService.isPlaying() && mSceneNumber != -1) {
    		mMediaService.startNewMedia(getFile(mSceneNumber), false, mSceneNumber);
    		mPlayer = mMediaService.getSceneMediaPlayer();
    		
    		mPlayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					mLocalIsPlaying = false;
					returnToMap();
				}
			});
    		
    		mLocalIsPlaying = true;
        }
	}

	private int getFile(int sceneNumber) {
		String fileName;
		if(mSceneNumber == 8)
			fileName = "scen_ending";
		else  
			fileName = "scen_" + mSceneNumber;
		int scene = getResources().getIdentifier(fileName, "raw", getActivity().getPackageName());
		return scene;
	}
	
	private String milliSecondsToTimer(long milliseconds) {
		String finalTimerString = "";
		String minutesString = "";
		String secondsString = "";

		// Convert total duration into time
		int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
		int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

		// Prepending 0 to seconds if it is one digit
		minutesString = minutes < 10 ? "0" + minutes: "" + minutes;
		secondsString = seconds < 10 ? "0" + seconds : "" + seconds;

		finalTimerString = minutesString + ":" + secondsString;

		return finalTimerString;
	}
	
	private int			lastTimeStamp;
	// Updates the countdown each second
	private Runnable	secondUpdater	= new Runnable() {

		@Override
		public void run() {
			if(mMediaService != null && mLocalIsPlaying) {
				try {
				int timeLeft = mMediaService.getTotalDuration() - mMediaService.getCurrentPosition();
				String timeString = milliSecondsToTimer(timeLeft);
				mTextTime.setText(timeString);
				lastTimeStamp = timeLeft;
				} catch (IllegalStateException ise) {
//					mLocalIsPlaying = false;
//					if(lastTimeStamp < 3000)
//						returnToMap();
					ise.printStackTrace();
				}
			}
			
			// Run this again in 0,5 sec
			mHandler.postDelayed(this, 20);
		}
	};
	private MediaPlayer	mPlayer;
}
