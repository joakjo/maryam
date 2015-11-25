package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.support.v4.app.FragmentManager;

public class SceneBroadcaster extends BroadcastReceiver {

	@Override
	public void onReceive(Context callerContext, Intent callerIntent) {
		boolean entering = callerIntent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);

		if (entering &&  !ApplicationState.SCENE_IS_PLAYING) {
			try {
				ApplicationState.SCENE_IS_PLAYING = true;
				LocalDatabase db = Global.getInstance(callerContext).getDatabase();

				TabHoster th = (TabHoster) callerContext;
				FragmentManager fm = th.getSupportFragmentManager();
				MainMapFragment mf = null;

				mf = (MainMapFragment) fm.findFragmentById(R.id.realtabcontent);

				if (db.getVisited().size() == 0 && db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.MAKE_READY)
					db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.IN_QUEST);
				else if (db.getVisited().size() == 0)
					db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.MAKE_READY);

				mf.switchFragment(callerIntent.getIntExtra("sceneNumber", -1));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
