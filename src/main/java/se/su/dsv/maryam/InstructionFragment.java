package se.su.dsv.maryam;

import se.su.dsv.maryam.R;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class InstructionFragment extends Fragment {
	
	private ViewGroup	root;
	private Typeface	mFont;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_instructions, container, false);
	}
	
	@Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

        MenuItem langMenuItem = menu.findItem(R.id.language_menu);
        if (langMenuItem != null)
        	langMenuItem.setVisible(false);
    }
	
	@Override
	public void onResume() {
		root	= (ViewGroup) getActivity().findViewById(R.id.instructions_viewgroup);
		mFont	= Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf");
		setFont(root, mFont);
		
		super.onResume();
	}
	
	/*
	 * Sets the font on all TextViews in the ViewGroup. Searches recursively for
	 * all inner ViewGroups as well. Just add a check for any other views you
	 * want to set as well (EditText, etc.)
	 */
	public void setFont(ViewGroup group, Typeface font) {
		int count = group.getChildCount();
		View v;
		for (int i = 0; i < count; i++) {
			v = group.getChildAt(i);
			if (v instanceof TextView)
				((TextView) v).setTypeface(font);
			else if (v instanceof ViewGroup)
				setFont((ViewGroup) v, font);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
}
