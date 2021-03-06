package se.su.dsv.maryam;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {
	private Typeface	mFont;
	private ViewGroup	root;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_about, container, false);

		return v;
	}

	@Override
	public void onResume() {
		TextView tw0 = (TextView) getActivity().findViewById(R.id.text_about_title);
		tw0.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_bold_alt.otf"));
		
		TextView tw1 = (TextView) getActivity().findViewById(R.id.text_about);
		tw1.setText(Html.fromHtml(getString(R.string.about_maryam_part1)));
		tw1.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));
		
		TextView tw2 = (TextView) getActivity().findViewById(R.id.text_about2);
		tw2.setText(Html.fromHtml(getString(R.string.about_maryam_part2)));
		tw2.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));

		super.onResume();
	}
	
//	/*
//	 * Sets the font on all TextViews in the ViewGroup. Searches recursively for
//	 * all inner ViewGroups as well. Just add a check for any other views you
//	 * want to set as well (EditText, etc.)
//	 */
//	public void setFont(ViewGroup group, Typeface font) {
//		int count = group.getChildCount();
//		View v;
//		for (int i = 0; i < count; i++) {
//			v = group.getChildAt(i);
//			if (v instanceof TextView)
//				((TextView) v).setTypeface(font);
//			else if (v instanceof ViewGroup)
//				setFont((ViewGroup) v, font);
//		}
//	}

}
