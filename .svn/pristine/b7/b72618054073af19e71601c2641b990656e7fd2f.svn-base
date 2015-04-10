package se.su.dsv.maryam;

import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;

public class AudioService extends Service {
	private final IBinder	mBinder			= new LocalBinder();
	private MediaPlayer		mMedia;
	private MediaPlayer		mMediaLoop;
	private int				mSceneNumber	= -1;
	private AudioManager	mAudioManager;
	private boolean			mAudioListener = false;

	// Inner class the will return the instance of the service
	public class LocalBinder extends Binder {
        AudioService getService() { 
        	return AudioService.this; 
        }
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	/**
	 * Stops the current playing media (if any) and starts
	 * the one passed as argument
	 * @param id the resource id for the file to be played
	 * @param loop set true if the media should loop
	 */
	public void startNewMedia(int id, boolean loop, int sceneNumber) {
		if(!mAudioListener)
			setUpFocusListener();
		
		if (mMedia != null) {
			mMedia.stop();
			mMedia.release();
			mMedia = null;
		}
		if (mMediaLoop != null) {
			mMediaLoop.stop();
			mMediaLoop.release();
			mMediaLoop = null;
		}
		if(loop) {
			mMediaLoop = MediaPlayer.create(this, id);
			mMediaLoop.setLooping(loop);
			mMediaLoop.start();
		}
		else {
			mMedia = MediaPlayer.create(this, id);
			mMedia.setLooping(loop);
			mMedia.start();
		}
		mSceneNumber = sceneNumber;
	}
	
	int id1, id2;
	public void startNewMedia(int id11, int id22, boolean loop, int sceneNumber) {
		if(!mAudioListener)
			setUpFocusListener();
		
		if (mMedia != null) {
			mMedia.stop();
			mMedia.release();
			mMedia = null;
		}
		if (mMediaLoop != null) {
			mMediaLoop.stop();
			mMediaLoop.release();
			mMediaLoop = null;
		}
		
		id1 = id11; id2 = id22;
		mMedia = MediaPlayer.create(this, id1);
		
		if(sceneNumber == 999) {
		mMedia.setLooping(loop);
		mMedia.start();
		mMedia.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				startNewMedia(id2, true, 99);
				LocalDatabase db = Global.getInstance(getApplicationContext()).getDatabase();
				db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.QUEST_COMPLETE);
			}
		});
		}
		
	}
	/**
	 * @throws IllegalStateException if the media player is not initiated
	 */
	public void start() throws IllegalStateException {
		if(mMedia != null) 
			mMedia.start();
		else if(mMediaLoop != null) 
			mMediaLoop.start();
		else 
			throw new IllegalStateException("MediaPlayer is not initiated");
	}
	
	/**
	 * @return true if a file is being played
	 */
	public boolean isPlaying() {
		if(mMedia != null) return mMedia.isPlaying();
		return false;
	}
	
	/**
	 * @return true if a file is being played
	 */
	public boolean isPlayingLooped() {
		if(mMediaLoop != null) return mMediaLoop.isPlaying();
		return false;
	}
	
	/**
	 * @throws IllegalStateException if the media player is not initiated
	 */
	public void stop() throws IllegalStateException {
		if(mMedia != null) 
			mMedia.stop();
		else if (mMediaLoop != null)
			mMediaLoop.stop();
		else 
			throw new IllegalStateException("MediaPlayer is not initiated");
	}
	
	/**
	 * @throws IllegalStateException if the media player is not initiated
	 */
	public void pause() throws IllegalStateException {
		if(mMedia != null) 
			mMedia.pause();
		else if(mMediaLoop != null) 
			mMediaLoop.pause();
		else 
			throw new IllegalStateException("MediaPlayer is not initiated");
	}
	
	/**
	 * @return true if a file is paused
	 */
	public boolean isPaused() {
		// Media is not playing and has been started before
		if(mMedia != null) return !mMedia.isPlaying() && (mMedia.getCurrentPosition() != 0);
		return false;
	}
	
	/**
	 * @throws IllegalStateException if the media player is not initiated
	 */
	public void seekTo(int msec) throws IllegalStateException {
		if(mMedia != null)
			mMedia.seekTo(msec);
		else
			throw new IllegalStateException("MediaPlayer is not initiated");
	}
	
	public MediaPlayer getSceneMediaPlayer() {
		return mMedia;
	}
	
	public int getTotalDuration() {
		if(mMedia != null)
			return mMedia.getDuration();
		else
			throw new IllegalStateException("MediaPlayer is not initiated");
	}
	
	public int getCurrentPosition() {
		if(mMedia != null)
			return mMedia.getCurrentPosition();
		else
			throw new IllegalStateException("MediaPlayer is not initiated");
	}
	
	public int getCurrentSceneNumber() {
		return mSceneNumber;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		if (mAudioListener)
			mAudioManager.abandonAudioFocus(afChangeListener);
		mAudioListener = false;
		return super.onUnbind(intent);
	}
	@Override
	public boolean stopService(Intent name) {
		if (mAudioListener)
			mAudioManager.abandonAudioFocus(afChangeListener);
		mAudioListener = false;
		return super.stopService(name);
	}
	
	private void setUpFocusListener() {
		mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		int result = mAudioManager.requestAudioFocus(afChangeListener,
				// Use the music stream.
				AudioManager.STREAM_MUSIC,
				// Request permanent focus.
				AudioManager.AUDIOFOCUS_GAIN);

		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			// Start playback.
		}
	}
	
	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
	    public void onAudioFocusChange(int focusChange) {
	        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
	        	pause();
	        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
	        	start();
	        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
	        	pause();
//	            mAudioManager.abandonAudioFocus(afChangeListener);
//	            stop();
	        }
	    }
	};
	
}











































