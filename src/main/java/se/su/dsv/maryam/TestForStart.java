package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class TestForStart extends Activity {
	private MediaPlayer mMedia;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_for_start);

		// Activate the screen if screen is off
		mMedia = MediaPlayer.create(this, R.raw.scen_0);
		mMedia.setLooping(true);
		mMedia.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mMedia.stop();
	}
	
	public void backToTabHoster(View v) {
		Intent i = new Intent(this, TabHoster.class);
		i.putExtra("init", true); // true means start point is found
		startActivity(i);
	}
}
