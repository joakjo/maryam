package se.su.dsv.maryam;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import se.su.dsv.maryam.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class ShowStarPathFragment extends Fragment {
	private SurfaceHolder		mOurHolder;
	private int					mScreenWidth, mScreenHeight;
	private PathSurface			mView;
	private List<Point>			mPoints;
	private List<XY>			mPixels;
	private LocalDatabase		mDB;
	private AudioService		mMediaService;
	private boolean				mBound;
	private static final String	TAG				= "ShowStarPathFragment";
	private Thread				ourThread;
	private boolean				mMusicUpdating	= false;
	private boolean				mFirstVisit;
	private int					mX_OFFSET;
	private int					mY_OFFSET;
	private int	smallestX;
	private int	biggestX;
	private int	smallestY;
	private int	biggestY;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}
	
	@Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

        MenuItem langMenuItem = menu.findItem(R.id.language_menu);
        if (langMenuItem != null)
        	langMenuItem.setVisible(false);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mDB = Global.getInstance(getActivity()).getDatabase();
		mFirstVisit = mDB.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.OUTRO;
		mView = new PathSurface(getActivity());
		getScreenSize();

		mPixels = initPixelList();

		return mView;
	}

	@Override
	public void onPause() {
		mView.pause();
		super.onPause();
	}
	
	@Override
	public void onResume() {
		setUpAudioService();
		mView.resume();
		super.onResume();
	}
	
	private void setUpAudioService() {
//		mMediaService = ((MainMapFragment) getParentFragment()).getAudioService();
		mMediaService = MainMapFragment.getAudioService();
		mBound = true;
		int scene1 = getResources().getIdentifier("scen_ending", "raw", getActivity().getPackageName());
		int scene2 = getResources().getIdentifier("wall_looped", "raw", getActivity().getPackageName());
		
		if (mDB.getState(ApplicationState.MAIN_MAP_STATE) == ApplicationState.OUTRO) {
			if (mMediaService != null && !mMediaService.isPlaying()) {
				mMediaService.startNewMedia(scene1, scene2, false, 999);
			}
		} else {
			if (mMediaService != null && !mMediaService.isPlayingLooped())
				mMediaService.startNewMedia(scene2, true, -1);
		}
	}

	private List<XY> initPixelList() {
//		ArrayList<XY> tmp = new ArrayList<XY>();
//		tmp.add(new XY(360, 551));
//		tmp.add(new XY(377, 676));
//		tmp.add(new XY(217, 675));
//		tmp.add(new XY(277, 409));
//		tmp.add(new XY(483, 344));
//		tmp.add(new XY(560, 519));

		// Get the points
		ArrayList<XY> tmp = mDB.getStarPositions();
		
		smallestX = mScreenWidth;
		biggestX = 0;
		smallestY = mScreenHeight;
		biggestY = 0;
		for(XY current : tmp) {
			smallestX = (Math.round(current.getX()) <= smallestX ? Math.round(current.getX()) : smallestX);
			smallestY = (Math.round(current.getY()) <= smallestY ? Math.round(current.getY()) : smallestY);
			biggestX = (Math.round(current.getX()) >= biggestX ? Math.round(current.getX()) : biggestX);
			biggestY = (Math.round(current.getY()) >= biggestY ? Math.round(current.getY()) : biggestY);
		}
		return tmp;
	}

	private int getYOffset(int height) {
		int diff = (biggestY - smallestY) / 2;
		int center = height / 2;
		int yLeft = center - diff;
		
		return yLeft - smallestY;
	}

	private int getXOffset(int width) {
		int diff = (biggestX - smallestX) / 2;
		int center = width / 2;
		int xLeft = center - diff;
		
		return xLeft - smallestX;
	}

	@SuppressWarnings("deprecation")
	private void getScreenSize() {
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		mScreenHeight = display.getHeight();
		mScreenWidth = display.getWidth();
	}


	@Override
	public void onDestroy() {
		if (mMediaService != null && (mMediaService.isPlayingLooped() || mMediaService.isPlaying()))
			mMediaService.stop();
//		closeThread();
		super.onDestroy();
	}

	private class PathSurface extends SurfaceView implements Runnable {

		private Thread	mOurThread;
		private boolean	mIsRunning	= false;
		private boolean	mDoDraw		= false;
		private Bitmap	mScaledBackground;

		public PathSurface(Context context) {
			super(context);
			mOurHolder = getHolder();
		}

		@Override
		public void run() {
			while (mIsRunning) {
				
				if(mDoDraw) {
					if(mFirstVisit) drawFirstTimeWithAnimation();
					else drawEverythingAtOnce();
				}
			}
		}

		private void drawEverythingAtOnce() {
			int alpha = 255;
			Canvas canvas;
			while (!mOurHolder.getSurface().isValid() && mDoDraw) {
			}
			canvas = mOurHolder.lockCanvas();
			
			mX_OFFSET = getXOffset(canvas.getWidth());
			mY_OFFSET = getYOffset(canvas.getHeight());
			
			mScaledBackground = decodeFile(R.drawable.bakgrund2x);
			canvas.drawBitmap(mScaledBackground, 0, 0, null);
			mScaledBackground.recycle();

			Paint starPaint = new Paint();
			starPaint.setAlpha(alpha);

			final int Y_OFFSET = -100;

			for (int i = 0; i < (mPixels.size()) && mDoDraw; i++) {
				XY xy = mPixels.get(i);
				Bitmap star = BitmapFactory.decodeResource(getResources(), R.drawable.star1);
				canvas.drawBitmap(star, xy.getX()+mX_OFFSET, xy.getY() + mY_OFFSET, starPaint);
			}

			for (int i = 0; i < mPixels.size() && mDoDraw; i++) {
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setStrokeWidth(5);
				paint.setColor(Color.WHITE);
				paint.setAlpha(alpha);

				Paint paint2 = new Paint(paint);
				paint2.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER));
				paint2.setAntiAlias(true);
				paint2.setAlpha(alpha);

				XY xy = mPixels.get(i);
				drawTheLine(i, canvas, paint, paint2, Y_OFFSET, xy);

			}
			mOurHolder.unlockCanvasAndPost(canvas);
			mDoDraw = false;
		}

		private void drawFirstTimeWithAnimation() {
			if (mDoDraw) {
				int fadeSpeed = 100;
				
				// Fade in all the stars
				
				int alpha = mFirstVisit ? 0 : 255;

				do {
					while (!mOurHolder.getSurface().isValid() && mDoDraw) {
					}
					Canvas canvas = mOurHolder.lockCanvas();
					
					mX_OFFSET = getXOffset(canvas.getWidth());
					mY_OFFSET = getYOffset(canvas.getHeight());
					
					mScaledBackground = decodeFile(R.drawable.bakgrund2x);
					canvas.drawBitmap(mScaledBackground, 0, 0, null);
					mScaledBackground.recycle();

					Paint starPaint = new Paint();
					starPaint.setAlpha(alpha);

					for (int i = 0; i < (mPixels.size())  && mDoDraw; i++) {
						XY xy = mPixels.get(i);
						Bitmap star = BitmapFactory.decodeResource(getResources(), R.drawable.star1);
						canvas.drawBitmap(star, xy.getX()+mX_OFFSET, xy.getY() + mY_OFFSET, starPaint);
					}
					alpha = (alpha + fadeSpeed) <= 255 ? (alpha + fadeSpeed) : 255;
					
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mOurHolder.unlockCanvasAndPost(canvas);
				} while(alpha < 255 && mDoDraw);
				
				Canvas canvas;
				// Draw one line at the time
				for (int i = 0; i < mPixels.size() && mDoDraw; i++) {
					alpha = mFirstVisit ? 0 : 255;
					do {
						while (!mOurHolder.getSurface().isValid() && mDoDraw) {
						}
						canvas = mOurHolder.lockCanvas();
						
						mScaledBackground = decodeFile(R.drawable.bakgrund2x);
						canvas.drawBitmap(mScaledBackground, 0, 0, null);
						mScaledBackground.recycle();

						Paint paint = new Paint();
						paint.setAntiAlias(true);
						paint.setStrokeWidth(5);
						paint.setColor(Color.WHITE);
						paint.setAlpha(alpha);

						Paint paint2 = new Paint(paint);
						paint2.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER));
						paint2.setAntiAlias(true);
						paint2.setAlpha(alpha);

						final int Y_OFFSET = -100;
						XY xy = mPixels.get(i);

						Bitmap star = BitmapFactory.decodeResource(getResources(), R.drawable.star1);
						for (int ii = 0; ii < mPixels.size() && mDoDraw; ii++) {
							XY tempXy = mPixels.get(ii);
							canvas.drawBitmap(star, tempXy.getX()+mX_OFFSET, tempXy.getY() + mY_OFFSET, null);
						}
						
						for(int iii = 0; iii < i && mDoDraw; iii++) {
							XY tempXy = mPixels.get(iii);
							Paint p1 = new Paint(paint);
							p1.setAlpha(255);
							Paint p2 = new Paint(paint2);
							p2.setAlpha(255);
							drawTheLine(iii, canvas, p1, p2, Y_OFFSET, tempXy);
						}

						drawTheLine(i, canvas, paint, paint2, Y_OFFSET, xy);
						alpha = (alpha + fadeSpeed) <= 255 ? (alpha + fadeSpeed) : 255;

						try {
								Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if(mFirstVisit)
							mOurHolder.unlockCanvasAndPost(canvas);
					} while (alpha < 255 && mDoDraw);
					if(!mFirstVisit)
						mOurHolder.unlockCanvasAndPost(canvas);
				}
			}

			mDoDraw = false;
		}

		private Bitmap decodeFile(int fileName){
		    Bitmap b1 = null;
		    Bitmap b2 = null;
		    try {
		        //Decode image size
		        BitmapFactory.Options o = new BitmapFactory.Options();
		        o.inJustDecodeBounds = true; 

		        InputStream fis = getActivity().getResources().openRawResource(fileName);
		        BitmapFactory.decodeStream(fis, null, o);
		        
		        fis.close();

		        final int height = o.outHeight;
		        final int width = o.outWidth;
		        int scale= 1;

		        if (height > mScreenHeight || width > mScreenWidth) {
		            // Calculate ratios of height and width to requested height and width
		            final int heightRatio = Math.round((float) height / (float) mScreenHeight);
		            final int widthRatio = Math.round((float) width / (float) mScreenWidth);

		            // Choose the smallest ratio as inSampleSize value, this will guarantee
		            // a final image with both dimensions larger than or equal to the
		            // requested height and width.
		            scale = heightRatio < widthRatio ? heightRatio : widthRatio;
		        } else {
		        	
		        }

		        //Decode with inSampleSize
		        BitmapFactory.Options o2 = new BitmapFactory.Options();
		        o2.inSampleSize = scale;
		        fis = getActivity().getResources().openRawResource(fileName);
		        
		        b1 = BitmapFactory.decodeStream(fis, null, o2);
		        b2 = Bitmap.createScaledBitmap(b1, mScreenWidth, mScreenHeight, true);
		        
		        b1.recycle();
		        fis.close();
		    } catch (IOException e) {
		    }
		    return b2;
		}
		
		private void drawTheLine(int i, Canvas canvas, Paint paint, Paint paint2, final int Y_OFFSET, XY xy) {
			XY xyTo;
			if (i < mPixels.size() - 1)
				xyTo = mPixels.get(i + 1);
			else
				xyTo = mPixels.get(0);

			Bitmap star = BitmapFactory.decodeResource(getResources(), R.drawable.star1);
			canvas.drawLine(xy.getX() + (star.getWidth() / 2) + mX_OFFSET, xy.getY() + (star.getHeight() / 2) + mY_OFFSET, xyTo.getX()
					+ (star.getWidth() / 2) + mX_OFFSET, xyTo.getY() + (star.getHeight() / 2) + mY_OFFSET, paint2);

			canvas.drawLine(xy.getX() + (star.getWidth() / 2) + mX_OFFSET, xy.getY() + (star.getHeight() / 2) + mY_OFFSET, xyTo.getX()
					+ (star.getWidth() / 2) + mX_OFFSET, xyTo.getY() + (star.getHeight() / 2) + mY_OFFSET, paint);
		}

		public void pause() {
			mScaledBackground.recycle();
			mDoDraw = false;
			mIsRunning = false;
			while (true && mOurHolder != null) {
				try {
					mOurThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
			mOurThread = null;
		}

		public void resume() {
			mOurThread = new Thread(this);
			mOurThread.start();
			mIsRunning = true;
			mDoDraw = true;
		}
	}
}
