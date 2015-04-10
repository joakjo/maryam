package se.su.dsv.maryam;

import java.io.IOException;
import java.util.Date;

import se.su.dsv.maryam.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PostCommentFragment extends Fragment {
	private TextView		mTitleText, mNameText, mAnswerText, mInfoText;
	private EditText		mNameEdit, mAnswerEdit;
	private TextView		mPostButton, mCancelButton;
	private LocalDatabase	mLocalDB;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_post_comment_starry_sky, container, false);
		mLocalDB = Global.getInstance(getActivity()).getDatabase();
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
		mTitleText = (TextView) getActivity().findViewById(R.id.post_comment_text_title);
		mTitleText.setTypeface(bold);

		mNameText = (TextView) getActivity().findViewById(R.id.post_comment_text_name);
		mNameText.setTypeface(demi);

		mAnswerText = (TextView) getActivity().findViewById(R.id.post_comment_text_answer);
		mAnswerText.setTypeface(demi);

		mInfoText = (TextView) getActivity().findViewById(R.id.post_comment_text_info);
		mInfoText.setTypeface(demi);

		// EditTexts
		mNameEdit = (EditText) getActivity().findViewById(R.id.post_comment_edit_name);
		mAnswerEdit = (EditText) getActivity().findViewById(R.id.post_comment_edit_answer);

		// Buttons
		mPostButton = (TextView) getActivity().findViewById(R.id.post_comment_button_post);
		mPostButton.setTypeface(bold);
		mPostButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mNameEdit.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(mAnswerEdit.getWindowToken(), 0);

				new DownloadFilesTask().execute("");
			}
		});

		mCancelButton = (TextView) getActivity().findViewById(R.id.post_comment_button_cancel);
		mCancelButton.setTypeface(bold);
		mCancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mLocalDB.setState(ApplicationState.STAR_SKY_STATE, ApplicationState.SKY_OPEN_HAS_POSTED_TEMP);
				switchToTheSky();
			}
		});
	}

	private void switchToTheSky() {
		StarrySkyControllerFragment frag = (StarrySkyControllerFragment) getParentFragment();
		frag.switchFragment();
	}

	private String			mAnswer, mSender;
	private ProgressDialog	mPostInProgressDialog;
	private String			mRes;

	private boolean postComment() {
		mAnswer = null;
		mSender = null;
		boolean success = false;
		
		if (mAnswerEdit.getText() != null && !mAnswerEdit.getText().toString().equals("")) {
			mAnswer = mAnswerEdit.getText().toString();

			if (mNameEdit.getText() != null && !mNameEdit.getText().toString().equals(""))
				mSender = mNameEdit.getText().toString();
			else
				mSender = "Anonym";
			try {
				double starPosX = Math.random();
				double starPosY = Math.random();
				DBGetter.postComment(mLocalDB.getCurrentRoute().getId(), starPosX, starPosY, mLocalDB.getUserIdFromLocalDatabase(), mAnswer, mSender, getResources().getString(R.string.lang_main));
				mLocalDB.setUserComment(mLocalDB.getCurrentRoute().getId(), starPosX, starPosY, mAnswer, mSender, new Date(), getResources().getString(R.string.lang_main));
				success = true;
			} catch (IOException e) {
				e.printStackTrace();
			} 
		} else {
			mRes = getResources().getString(R.string.fill_in_comment_first);
		}
		return success;
	}

	private class DownloadFilesTask extends AsyncTask<String, Integer, Boolean> {
		@Override
		protected void onPreExecute() {
			mRes = null;
			mPostInProgressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.please_wait),
					getResources().getString(R.string.uploading_star), true);
			mPostInProgressDialog.setCancelable(false);
			super.onPreExecute();
		}

		protected Boolean doInBackground(String... urls) {
			return postComment();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mPostInProgressDialog.dismiss();
			if (result) {
				mLocalDB.setState(ApplicationState.STAR_SKY_STATE, ApplicationState.SKY_OPEN_HAS_POSTED);
				StarrySkyControllerFragment parent = (StarrySkyControllerFragment) getParentFragment();
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
				parent.switchFragment();
			} else {
				mRes = getResources().getString(R.string.server_problem);
			}
			if (mRes != null)
				Toast.makeText(getActivity(), mRes, Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}
	}
}