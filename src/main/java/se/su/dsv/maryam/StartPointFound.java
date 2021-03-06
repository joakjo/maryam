package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class StartPointFound extends Fragment {
	
	private TextView	mText;
	private ImageButton	mStart;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Replacing the placeholder for the map with the real map
		View view = inflater.inflate(R.layout.fragment_start_point_found, container, false);
		return view;
	}
	 @Override
	public void onResume() {
		setUpViews();
		super.onResume();
	}
	 
	private void setUpViews() {
		mText = (TextView) getActivity().findViewById(R.id.text_start_point_found);
		mText.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));
		
		mStart = (ImageButton) getActivity().findViewById(R.id.start_point_found_button);
		mStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DummyReceiver.onReceive(getActivity(), 0);
			}
		});
	}
	
	public void click(View v) {
		
	}

}
