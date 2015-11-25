package se.su.dsv.maryam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class ProximityBroadcaster extends BroadcastReceiver {
	//private final String	TAG	= "ProximityBroadcaster";

	@Override
	public void onReceive(Context callerContext, Intent callerIntent) {
		callerContext.unregisterReceiver(this);
		boolean entering = callerIntent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);

		if (entering){
				callerContext.startActivity(new Intent(callerContext, TestForStart.class));
		}
	}

}
