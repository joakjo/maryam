package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

public class SkyNotOpenFragment extends Fragment {
	private static final String	TAG	= "SkyNotOpenFragment";
	private TextView	mTitleText, mPara1Text, mPara2Text;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_sky_not_open, container, false);
		
		return v;
	}
	
	@Override
	public void onResume() {
		initViews();
		super.onResume();
	}

	private void initViews() {
		Typeface demi = Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf");
		Typeface bold = Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_bold_alt.otf");
		
		// TextViews
		mTitleText = (TextView) getActivity().findViewById(R.id.sky_not_open_title);
		mTitleText.setTypeface(bold);
		
		mPara1Text = (TextView) getActivity().findViewById(R.id.sky_not_open_para1);
		mPara1Text.setTypeface(demi);
		
		mPara2Text = (TextView) getActivity().findViewById(R.id.sky_not_open_para2);
		mPara2Text.setTypeface(demi);
	}
}
