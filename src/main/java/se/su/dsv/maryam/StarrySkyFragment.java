package se.su.dsv.maryam;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import se.su.dsv.maryam.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class StarrySkyFragment extends Fragment {
	private static final String					TAG					= "StarrySkyFragment";
	private int									mScreenHeight;
	private int									mScreenWidth;
	private SkySurfaceView						mView;
	private SurfaceHolder						ourHolder;
	private int									mStarSkyHeight;
	private int									mStarSkyWidth, mSkyX, mSkyY, mDrawStarPosX, mDrawStarPosY;
	private ArrayList<Comment>					mStarList			= new ArrayList<Comment>();
	private LocalDatabase						mDB;
	private HashMap<Integer, Route>				routeMap			= null;
	private Canvas								canvas;
	private boolean								mDoDrawing, mIsRunning, mIsScrolling, D = false, mStarAnimationRunning = true, mStarAnimation = false;
	float										centerAnimationX	= 100f, centerAnimationY = 100f;
	GestureDetector								gestureDetector		= new GestureDetector(getActivity(), new GestureListener());
	private AsyncTask<String, Integer, Boolean>	asyncTask;
	private String								userLang			= null;
	private boolean								polling				= true;
	private AsyncTask<String, Integer, Boolean>	pollingTask;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mDB = Global.getInstance(getActivity()).getDatabase();
		if (routeMap == null)
			routeMap = mDB.getRoutes();
		getScreenSize();
		if (userLang == null)
			userLang = getResources().getString(R.string.lang_main);
	}
	
	@Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
        MenuItem langMenuItem = menu.findItem(R.id.language_menu);
        if (langMenuItem != null)
        	langMenuItem.setVisible(true);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mDoDrawing = false;
		mIsRunning = true;
		mView = new SkySurfaceView(getActivity());

		if (mDB.getState(ApplicationState.STAR_SKY_STATE) == ApplicationState.SKY_OPEN_HAS_POSTED) {
			clearSpaceForUserStar();
		}
		
		gestureDetector.setIsLongpressEnabled(false);
		
		mView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});
		
		return mView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.stars_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.language_menu:
//							Log.d("HEJ", "nr1");
//			Log.d(TAG, "Switch");
			AlertDialog dialog = new AlertDialog.Builder(getActivity()).setMessage(getResources().getString(R.string.language_menu_message))
					.setPositiveButton(getResources().getString(R.string.language_menu_main_language), new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							mDB.setLang(getResources().getString(R.string.lang_main));
							asyncTask = new SwitchLanguageTask().execute(getResources().getString(R.string.lang_main));
						}
					}).setNegativeButton(getResources().getString(R.string.language_menu_all_languages), new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							mDB.setLang("ALL");
							asyncTask = new SwitchLanguageTask().execute("ALL");

							// Do stuff when user neglects.
						}
					}).setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							dialog.dismiss();
							// Do stuff when cancelled
						}
					}).create();
			dialog.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void clearSpaceForUserStar() {
		// Clearing space for user comment (pun not intended)
		Rect r = new Rect((mScreenWidth / 2) - 80, (mScreenHeight / 3) - 80, (mScreenWidth / 2) + 80, (mScreenHeight / 3) + 80);
		for (int i = 0; i < mStarList.size(); i++) {
			if (r.contains((int) (mStarList.get(i).getXpos() * mStarSkyWidth), (int) (mStarList.get(i).getYpos() * mStarSkyHeight))) {
				
				mStarList.get(i).setXpos(Math.random());
				mStarList.get(i).setYpos(Math.random());
				mStarList.get(i).getRect().set(mDrawStarPosX - 20, mDrawStarPosY - 20, mDrawStarPosX + 20, mDrawStarPosY + 20);
			}
			// Remove user comment from API (should come from local database)
			if (mStarList.get(i).getZ() == mDB.getUserIdFromLocalDatabase())
				mStarList.remove(i);
		}
		mDoDrawing = true;
	}
	  
	@Override
	public void onPause() {
		mDB.setSkyCoords(new int[] {mSkyX, mSkyY});
		asyncTask.cancel(true);
		pollingTask.cancel(true);
		mView.pause();
		super.onPause();
	}

	@Override
	public void onResume() {
		if (mDB.getState(ApplicationState.STAR_SKY_STATE) == ApplicationState.SKY_OPEN_HAS_POSTED_TEMP) {
			mDB.setState(ApplicationState.STAR_SKY_STATE, ApplicationState.SKY_OPEN_NOT_POSTED);
			switchToTheSky();
		}
		mView.resume();
		super.onResume();

		pollingTask = new DownloadDeltaCommentsTask().execute("");
		asyncTask = new SwitchLanguageTask().execute(mDB.getLang());
	}
	
	private void switchToTheSky() {
		StarrySkyControllerFragment frag = (StarrySkyControllerFragment) getParentFragment();
		frag.switchFragment();
	}
	
	public void openDialog(int x, int y, String answer, String sender, int routeId) {
		TabHoster context = (TabHoster) getActivity();

		// Inflate the popup_layout.xml
		LinearLayout viewGroup = (LinearLayout) context.findViewById(R.id.star_pressed);
		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = layoutInflater.inflate(R.layout.dialog_star_pressed_popup, viewGroup);

		// Creating the PopupWindow
		final PopupWindow popup = new PopupWindow(context);
		popup.setContentView(layout);
		popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		popup.setFocusable(true);

		// Some offset to align the popup a bit to the right, and a bit down,
		// relative to button's position.
		int OFFSET_Y = 0;
		int OFFSET_X = 0;

		// Clear the default translucent background
		popup.setBackgroundDrawable(new BitmapDrawable());

		// Displaying the popup at the specified location, + offsets.
		popup.showAtLocation(layout, Gravity.NO_GRAVITY, x + OFFSET_X, y + OFFSET_Y);
		
		TextView ans = (TextView) layout.findViewById(R.id.dialog_star_pressed_answer);
		ans.setText(answer);
		ans.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));
		ans.setWidth(mScreenWidth/3);
		
		TextView sen = (TextView) layout.findViewById(R.id.dialog_star_pressed_sender);
		sen.setText(sender);
		sen.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_bold_alt.otf"));

		TextView cit = (TextView) layout.findViewById(R.id.dialog_star_pressed_city);
		cit.setText(", " + routeMap.get(routeId).getTitle());
		cit.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "neutra_text_demi.otf"));
		// Getting a reference to Close button, and close the popup when
		// clicked.
	}

	private class GestureListener extends SimpleOnGestureListener {

		public boolean onDown(MotionEvent e) {
			for (int i = 0; i < mStarList.size(); i++) {
				if (mStarList.get(i).getRect().contains((int)e.getX(), (int)e.getY())) {
					Comment c = mStarList.get(i);
					openDialog(Math.round(e.getX()), Math.round(e.getY()), c.getMessage(), c.getSender(), c.getRouteId());
					return false;
				}
			}
			while (!ourHolder.getSurface().isValid());
			mIsScrolling = true;
			return true;
		};

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return super.onDoubleTap(e);
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (!mStarAnimation) {
				mSkyX -= Math.round(distanceX);
				mSkyY -= Math.round(distanceY);
			}
			mDoDrawing = true;
			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void getScreenSize() {
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		mScreenHeight = display.getHeight();
		mScreenWidth  = display.getWidth();
		mStarSkyHeight = mScreenHeight * 3;
		mStarSkyWidth = mScreenWidth * 3;
		int[] skypos = mDB.getSkyCoords();
		mSkyX = skypos[0];
		mSkyY = skypos[1];
	}
	
	private class SkySurfaceView extends SurfaceView implements Runnable {
		private Thread	ourThread;
		private Bitmap	mStarBitmapArray[]			= { BitmapFactory.decodeResource(getResources(), R.drawable.star1),
															BitmapFactory.decodeResource(getResources(), R.drawable.star2),
															BitmapFactory.decodeResource(getResources(), R.drawable.star3),
															BitmapFactory.decodeResource(getResources(), R.drawable.star4) };
		private Bitmap	mStarAnimationBitmapArray[]	= { BitmapFactory.decodeResource(getResources(), R.drawable.anim1),
															BitmapFactory.decodeResource(getResources(), R.drawable.anim2),
															BitmapFactory.decodeResource(getResources(), R.drawable.anim3),
															BitmapFactory.decodeResource(getResources(), R.drawable.anim4),
															BitmapFactory.decodeResource(getResources(), R.drawable.anim5),
															BitmapFactory.decodeResource(getResources(), R.drawable.anim6),
															BitmapFactory.decodeResource(getResources(), R.drawable.anim7)};

		public SkySurfaceView(Context context) {
			super(context);
			ourHolder = getHolder();
		}
		
		int i = 0;

		@Override
		public void run() {
			if (mDB.getState(ApplicationState.STAR_SKY_STATE) == ApplicationState.SKY_OPEN_HAS_POSTED) {
				if (!mDB.getSeenAnimation()) {
					mSkyX = 0;
					mSkyY = 0;
					for (i = 0; i < 7 && mStarAnimationRunning; i++)
						doDrawStarAnimation(i);
					mDB.setSeenAnimation(true);
				}

				// Post usercomment after staranimation
				Comment userCommentFromDatabase = mDB.getUserCommentFromLocalDatabase();
				userCommentFromDatabase.setXpos((double) (mScreenWidth / 2) / mStarSkyWidth);
				userCommentFromDatabase.setYpos((double) (mScreenHeight / 3) / mStarSkyHeight);
				mStarList.add(userCommentFromDatabase);
			}
			
			
			while (mIsRunning) {
				if (mDoDrawing && !mStarAnimation) {
					doDraw();
					mDoDrawing = false;
				}
			}
		}
		
		public void pause() {
			mIsRunning = false;
			mStarAnimationRunning = false;
			while (true) {
				try {
					ourThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
			ourThread = null;
		}
		
		public void resume() {
			mStarAnimationRunning = true;
			mIsScrolling = false;
			ourThread = new Thread(this);
			ourThread.start();
			mIsRunning = true;
			mDoDrawing = true;
		}

		public void doDraw() {
			if (!mIsScrolling)
				while (!ourHolder.getSurface().isValid());
			canvas = ourHolder.lockCanvas();
			canvas.drawRGB(0, 0, 0);
			drawStars();
			ourHolder.unlockCanvasAndPost(canvas);
		}

		public void doDrawStarAnimation(int i) {
			try {
				while (!ourHolder.getSurface().isValid());
				canvas = ourHolder.lockCanvas();
				canvas.drawRGB(0, 0, 0);
				drawStars();
				Bitmap starBitmap = mStarAnimationBitmapArray[i];
				canvas.drawBitmap(starBitmap, mScreenWidth / 2 - (starBitmap.getWidth() / 2), mScreenHeight / 3 - (starBitmap.getHeight() / 2), null);
				Thread.sleep(200);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ourHolder.unlockCanvasAndPost(canvas);
		}

		private void drawStars() {
			for (int i = 0; i < mStarList.size(); i++) {
				mDrawStarPosX = (int) ((mStarList.get(i).getXpos() * mStarSkyWidth) + mSkyX) % mStarSkyWidth;
				if (mDrawStarPosX < -100) // -100 so the star will be drawn until the whole star is out of the picture
					mDrawStarPosX += mStarSkyWidth;
				
				mDrawStarPosY = (int) ((mStarList.get(i).getYpos() * mStarSkyHeight) + mSkyY) % mStarSkyHeight;
				if (mDrawStarPosY < -100) // -100 so the star will be drawn until the whole star is out of the picture
					mDrawStarPosY += mStarSkyHeight;
				
				int starSize = 3;
				if (i >= 0.25*mStarList.size() && i < 0.50*mStarList.size())
					starSize = 2;
				else if (i >= 0.50*mStarList.size() && i < 0.75*mStarList.size())
					starSize = 1;
				else if (i >= 0.75*mStarList.size())
					starSize = 0;
				if ((mDrawStarPosX) < mScreenWidth && (mDrawStarPosY) < mScreenHeight) {
						
					int centerX = mStarBitmapArray[starSize].getWidth()/2; 
					int centerY = mStarBitmapArray[starSize].getHeight()/2; 
					Paint p = new Paint();
					p.setColor(Color.CYAN);
					mStarList.get(i).getRect().set(mDrawStarPosX - 20, mDrawStarPosY - 20, mDrawStarPosX + 20, mDrawStarPosY + 20);
					canvas.drawBitmap(mStarBitmapArray[starSize], mDrawStarPosX-centerX, mDrawStarPosY-centerY, null);
				}
			}
		}
		
	}
		private void moveStarsAwayFromOthers() {
			// If stars are too close to each other they get moved
			for (int c = 0; c < mStarList.size(); c++) {
				Comment firstComment = mStarList.get(c);
				for (int g = c; g < mStarList.size(); g++) {
					Comment secondComment = mStarList.get(g);
					if (g != c) {
						firstComment.getRect().set((int)(firstComment.getXpos()*mStarSkyWidth)-25, (int)(firstComment.getYpos()*mStarSkyHeight)-25, (int)(firstComment.getXpos()*mStarSkyWidth)+80, (int)(firstComment.getYpos()*mStarSkyHeight)+80);
						if (firstComment.getRect().contains((int)((secondComment.getXpos()*mStarSkyWidth))+20, (int)(secondComment.getYpos()*mStarSkyHeight)+20)) {
							firstComment.setXpos(Math.random());
							firstComment.setYpos(Math.random());
							c = -1;
							break;
						}
					}
				}
			}
			mDoDrawing = true;
		}
		
	private class DownloadDeltaCommentsTask extends AsyncTask<String, Integer, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(String... urls) {
			try {
				while (polling) {
//					Log.d("Sleep", "sleeping 5 sec");
					Thread.sleep(5000);
					Date dateLastStar = mStarList.get(mStarList.size()-1).getDate();
					String lang = mDB.getLang();
					if (lang == null) {
						lang = getResources().getString(R.string.lang_main);
					}
					ArrayList<Comment> list = DBGetter.getCommentsDelta(dateLastStar, 50, lang);
//					Log.d("poll", list.toString());
					mDB.addCommentsFromAPI(list);
					mStarList.addAll(list);
					moveStarsAwayFromOthers();
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e){
				e.printStackTrace();
			} catch (InterruptedException e) {
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				mDoDrawing = true;
		}
	}
	private class LoadDBCommentsTask extends AsyncTask<String, Integer, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Boolean doInBackground(String... urls) {
//			try {
				mStarList = mDB.getComments(100, "ALL");
				if (mDB.getState(ApplicationState.STAR_SKY_STATE) == ApplicationState.SKY_OPEN_HAS_POSTED) {
					mStarList.add(mDB.getUserCommentFromLocalDatabase());
					clearSpaceForUserStar();
				}
				moveStarsAwayFromOthers();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (IndexOutOfBoundsException e){
//				e.printStackTrace();
//			}
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				mDoDrawing = true;
		}
	}
	private ProgressDialog	mDownloadingStarsProgressDialog;
	private String mRes;
	ArrayList<Comment> list;
	
	private class SwitchLanguageTask extends AsyncTask<String, Integer, Boolean> {
		@Override
		protected void onPreExecute() {
			if (pollingTask != null)
				pollingTask.cancel(true);

			if (!mStarAnimationRunning){
				mDownloadingStarsProgressDialog = ProgressDialog.show(getActivity(), getResources().getString(R.string.please_wait),
				/* getResources().getString(R.string.uploading_star) */"Downloading Stars!", true);
				mDownloadingStarsProgressDialog.setCancelable(false);
			}
			super.onPreExecute();
		}

		protected Boolean doInBackground(String... urls) {
			boolean success = false;
			String lang = urls[0];
//			Log.d("sdflkjsdflksjdfklj", lang);
			if (lang.equals("onCreateView")) {
				lang = mDB.getLang();
				if (lang == null)
					lang = getResources().getString(R.string.lang_main);
			}
			// Date dateLastStar = mStarList.get(0).getDate();

			mStarList = mDB.getComments(100, lang);
//			Log.d("SECRET", mDB.getComments(100, lang).toString());

			if (mDB.getState(ApplicationState.STAR_SKY_STATE) == ApplicationState.SKY_OPEN_HAS_POSTED) {
				mStarList.add(mDB.getUserCommentFromLocalDatabase());
//				Log.d("FDSFDSFDSFDSFSDF!", mDB.getUserCommentFromLocalDatabase().toString());
				clearSpaceForUserStar();
			}
			moveStarsAwayFromOthers();
			mDB.setLang(lang);
			success = true;
			mDB.closeDatabase();
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				mDoDrawing = true;
			} else {
				mRes = getResources().getString(R.string.server_problem);
			}
			if (mRes != null)
				Toast.makeText(getActivity(), mRes, Toast.LENGTH_SHORT).show();
			if (mDownloadingStarsProgressDialog != null)
				mDownloadingStarsProgressDialog.dismiss();
			mDB.closeDatabase();
			pollingTask = new DownloadDeltaCommentsTask().execute("");

			super.onPostExecute(result);
		}
	}

}
