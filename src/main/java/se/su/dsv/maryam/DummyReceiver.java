package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.content.Context;
import android.support.v4.app.FragmentManager;

public class DummyReceiver {
	public static void onReceive(Context callerContext, int sceneNumber) {
		try {
			LocalDatabase db = Global.getInstance(callerContext).getDatabase();
			
			// Getting tabhoster
			TabHoster th = (TabHoster) callerContext;

			// Getting the fragmentmanager
			FragmentManager fm = th.getSupportFragmentManager();
			
			// Getting the MainMapFragment to be able to switch fragment
			MainMapFragment mf = null;
			try {
			mf = (MainMapFragment) fm.findFragmentById(R.id.realtabcontent);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(db.getVisited().size() == 0 && db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.MAKE_READY)
				db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.IN_QUEST);
			else if(db.getVisited().size() == 0)
				db.setState(ApplicationState.MAIN_MAP_STATE, ApplicationState.MAKE_READY);
			

			mf.switchFragment(sceneNumber);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
