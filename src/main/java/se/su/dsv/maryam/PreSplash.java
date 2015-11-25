package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PreSplash extends Activity {
	private LocalDatabase	db;
	public static final int	RESUME		= 0;
	public static final int	NO_RESUME	= 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
        db = Global.getInstance(this).getDatabase();
        if (db.getVisited().size() != 0){
            CheckHelper.resumeState(this);
        }
        else
            finishMeOff(NO_RESUME);
	}

	 public void finishMeOff(int state) {
		 Intent i = new Intent(this, Splash.class);
		 i.putExtra("resume", state);
		 startActivity(i);
		 finish();
	 }

}
