package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

public class CheckHelper {
	public final static int	CONNECTION_TYPE	= 1;
	public final static int	POSITION_TYPE 	= 2;
	
	public static <E extends Activity> boolean checkForGPS(E context) {
	     LocationManager lm = null;
	     boolean gps_enabled = false,network_enabled = false;
		if (lm == null)
			lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			network_enabled = lm
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}
		if(!gps_enabled || !network_enabled) {
			showErrorDialog(context, POSITION_TYPE);
			return false;
		}
		return true;
	}
	
	
	public static <E extends Activity> boolean checkFor3G(E context) {
		 ConnectivityManager cm =
			        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			    NetworkInfo netInfo = cm.getActiveNetworkInfo();
			    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			    	return true;
			    }
			    showErrorDialog(context, CONNECTION_TYPE);
			    return false;
	}
	
	public static <E extends Activity> void showErrorDialog(final E context, final int type) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		String title, message;
		if(type == CONNECTION_TYPE) {
			title = context.getResources().getString(R.string.no_connection_title);
			message = context.getResources().getString(R.string.no_connection_message);
		} else { // POSITION_TYPE
			title = context.getResources().getString(R.string.no_position_title);
			message = context.getResources().getString(R.string.no_position_message);
		}
		
		// set title
		alertDialogBuilder.setTitle(title);

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setNegativeButton(
						context.getResources().getString(
								R.string.check_helper_close),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								try {
									((TabHoster) context).finish();
								} catch (ClassCastException cce) {
									try {
										((Splash) context).finish();
									} catch (ClassCastException e) {
									}
								}
							}
						})
				.setNeutralButton(
						context.getResources().getString(
								R.string.check_helper_settings),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(type == CONNECTION_TYPE) {
									Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
									intent.addCategory(Intent.ACTION_MAIN);
									final ComponentName cn = new ComponentName("com.android.phone","com.android.phone.MobileNetworkSettings");
									intent.setComponent(cn);
									intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									context.startActivity(intent);
								}
								else if(type == POSITION_TYPE) { 
									Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									context.startActivity(intent);
								}
							}
							
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}
	
	public static boolean resumeState(final PreSplash context) {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            context.finishMeOff(PreSplash.RESUME);
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		        	context.finishMeOff(PreSplash.NO_RESUME);
		            break;
		            
		        }
		    }
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder
		.setTitle(context.getResources().getString(R.string.resume_state_title))
		.setMessage(context.getResources().getString(R.string.resume_state_message))
		.setPositiveButton(context.getResources().getString(R.string.resume_state_yes), dialogClickListener)
		.setNegativeButton(context.getResources().getString(R.string.resume_state_no), dialogClickListener).setCancelable(false).show();

		return false;
	}
}













