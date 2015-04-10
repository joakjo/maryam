package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StarrySkyControllerFragment extends Fragment {
	private final String	TAG				= "StarrySkyControllerFragment";
	public static final String	STARRY_FRAG_TAG	= "StarrySkyControllerFragmentTagVemArJockeJagArIafFredrikHEHE";
	private LocalDatabase	db;
	private AudioService	mMediaService;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main_map, container, false);

		db = Global.getInstance(getActivity()).getDatabase();
		switchFragment();

		return view;
	}
	
	@Override
	public void onResume() {
		setUpAudioService();
		super.onResume();
	}
	
	private void setUpAudioService() {
//		mMediaService = ((MainMapFragment) getParentFragment()).getAudioService();
		mMediaService = MainMapFragment.getAudioService();
		int scene1 = getResources().getIdentifier("scen_ending", "raw", getActivity().getPackageName());
		int scene2 = getResources().getIdentifier("wall_looped", "raw", getActivity().getPackageName());
		
		if (db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.OUTRO) {
			if (mMediaService != null && !mMediaService.isPlaying()) {
				mMediaService.startNewMedia(scene1, scene2, false, 999);
			}
		} else if(db.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.QUEST_COMPLETE){
			if (mMediaService != null && !mMediaService.isPlayingLooped())
				mMediaService.startNewMedia(scene2, true, -1);
		}
	}

	public void switchFragment() {
		CheckHelper.checkFor3G(getActivity());

		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		Fragment toRemove = getChildFragmentManager().findFragmentByTag(STARRY_FRAG_TAG);
		if (toRemove != null) ft.remove(toRemove);

		switch (db.getState(ApplicationState.STAR_SKY_STATE)) {
		case ApplicationState.SKY_NOT_OPEN:
			ft.add(R.id.fragment_main_container, new SkyNotOpenFragment(), STARRY_FRAG_TAG);
			break;
			
		case ApplicationState.SKY_OPEN_NOT_POSTED:
			ft.add(R.id.fragment_main_container, new PostCommentFragment(), STARRY_FRAG_TAG);
			break;
			
		case ApplicationState.SKY_OPEN_HAS_POSTED:
			ft.add(R.id.fragment_main_container, new StarrySkyFragment(), STARRY_FRAG_TAG);
			break;
			
		case ApplicationState.SKY_OPEN_HAS_POSTED_TEMP:
			db.setState(ApplicationState.STAR_SKY_STATE, ApplicationState.SKY_OPEN_NOT_POSTED);
			ft.add(R.id.fragment_main_container, new StarrySkyFragment(), STARRY_FRAG_TAG);
			break;
		}

		ft.commit();
	}
}
