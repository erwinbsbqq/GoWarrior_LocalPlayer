package com.gowarrior.myplayer.common;

/**
 * Created by jerry.xiong on 2015/6/11.
 */

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gowarrior.myplayer.R;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 *
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 *   has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 *   setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 *   otherwise by using the MediaController(Context, boolean) constructor
 *   with the boolean set to false
 * </ul>
 */
public class MediaPlaybar extends RelativeLayout {
    private static final String LOGTAG = "MediaPlaybar";

    private static final int MSG_FADE_OUT = 1;
    private static final int MSG_SHOW_PROGRESS = 2;

    private static final int SEEK_BAR_MAX = 1000;
    private static final int SEEK_BAR_STEP = 10;
    private static final int SEEK_STEP_IN_MS = 30000;   // 30 seconds

    private MediaPlayerControl  mPlayer;
    private Context mContext;
    private ViewGroup mAnchor;
    private View mRoot;
    private SeekBar mProgress;
    private TextView mTitle;
    private TextView            mTime;
    private Runnable 			mRunnable = null;
    private boolean             mShowing = false;
    private boolean             mSeeking = false;
    private boolean             mPositionChanged = false;
    private int                 mTimeout = 5000;
    private long				mTargetPosition = 0;
    private boolean             mUseFastForward;
    private boolean             mFromXml;
    StringBuilder               mFormatBuilder;
    Formatter mFormatter;
    private Handler mHandler = new MessageHandler(this);

    public MediaPlaybar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = null;
        mContext = context;
        mUseFastForward = true;
        mFromXml = true;

        Log.i(LOGTAG, LOGTAG);
    }

    public MediaPlaybar(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;

        Log.i(LOGTAG, LOGTAG);
    }

    public MediaPlaybar(Context context) {
        this(context, true);

        Log.i(LOGTAG, LOGTAG);
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        mAnchor = view;

        RelativeLayout.LayoutParams frameParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        frameParams.addRule(CENTER_IN_PARENT, TRUE);
//        frameParams.addRule(ALIGN_PARENT_BOTTOM);

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    public void setTimeout(int timeout) {
        mTimeout = timeout;
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_playbar, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {

        mProgress = (SeekBar) v.findViewById(R.id.playbar_slider);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
                seeker.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        if(mPositionChanged) {
                            mHandler.removeCallbacks(mRunnable);
                            Log.i(LOGTAG, "OnClicked, target is position: " + mTargetPosition);
                            //mPlayer.seekTo((int) mTargetPosition);
                            doSeek((int) mTargetPosition);
                        }
                    }
                });
            }
            mProgress.setMax(SEEK_BAR_MAX);
            mProgress.setKeyProgressIncrement(SEEK_BAR_STEP);
        }

        mTitle = (TextView) v.findViewById(R.id.playbar_title);

        mTime = (TextView) v.findViewById(R.id.playbar_time);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(mTimeout);
    }

    public void update() {
        Log.v(LOGTAG, "showTitle: mTitle=" + mTitle + " mPlayer=" + mPlayer);

        if (mTitle != null && mPlayer != null) {
            Log.v(LOGTAG, "showTitle: sourceName=" + mPlayer.getSourceName());
            mTitle.setText(mPlayer.getSourceName());
        }

        if (mProgress != null) {
            mProgress.setProgress(0);
            mProgress.setSecondaryProgress(0);
        }

        if (mTime != null) {
            mTime.setText("");
        }
    }

    private void resetAutoHideTimer() {
        if (mTimeout != 0) {
            Message msg = mHandler.obtainMessage(MSG_FADE_OUT);
            mHandler.removeMessages(MSG_FADE_OUT);
            mHandler.sendMessageDelayed(msg, mTimeout);
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {
        Log.v(LOGTAG, "show: mShowing="+mShowing);
        if (!mShowing && mAnchor != null) {
            update();
            setProgress();

            RelativeLayout.LayoutParams tlp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            tlp.addRule(CENTER_IN_PARENT, TRUE);

            mAnchor.addView(this, tlp);
            mAnchor.setVisibility(VISIBLE);
            mShowing = true;
        }

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        startGetCurPos(0);

        resetAutoHideTimer();
    }

    public boolean isShowing() {
        return mShowing;
    }

    public boolean isSeeking() {
        return mSeeking;
    }

    public void onSeekComplete() {
    	/* for local player */
        Log.i(LOGTAG, "on Seek completed");
        mSeeking = false;
        startGetCurPos(500);
    }

    public void onBufferingComplete() {
    	/* for online player */
        Log.i(LOGTAG, "on Buffering completed");
        mSeeking = false;
        startGetCurPos(500);
    }
    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        Log.v(LOGTAG, "hide");

        if (mAnchor == null) {
            return;
        }

        try {
            mAnchor.setVisibility(INVISIBLE);
            mAnchor.removeView(this);
            //mHandler.removeMessages(MSG_SHOW_PROGRESS);
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        mShowing = false;
        //mSeeking = false;
    }

    public void startGetCurPos(int timeout) {
        mHandler.removeMessages(MSG_SHOW_PROGRESS);
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_PROGRESS, timeout);
    }

    public void stopGetCurPos() {
        mHandler.removeMessages(MSG_SHOW_PROGRESS);
    }

    private void doSeek(int time) {
        int target = 0;
        int total = mPlayer.getDuration();

        mPositionChanged = false;

        if(time >= total)
            target = time - 5;
        else
            target = time;

        Log.i(LOGTAG, "seek to real position: " + target);
        mPlayer.seekTo(target);
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        Log.v(LOGTAG, "setProgress");

        if (mPlayer == null || isSeeking()) {
            return 0;
        }

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0 && position >= 0) {
                // use long to avoid overflow
                long pos =  position / (duration / SEEK_BAR_MAX);
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mTime != null) {
            mTime.setText(stringForTime(position)+"/"+stringForTime(duration));
        }

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(LOGTAG, "onTouchEvent");
        show(mTimeout);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        Log.v(LOGTAG, "onTrackballEvent");
        show(mTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean skipKeyHandled = false;
        boolean keyHandled = false;
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (!mSeeking)
                        skipKeyHandled = true;
                    break;
                default:
                    break;
            }
        }
        if(skipKeyHandled == false) {
            keyHandled = super.dispatchKeyEvent(event);
        }

        return keyHandled;

    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            Log.v(LOGTAG, "onStartTrackingTouch");
            resetAutoHideTimer();

            //mSeeking = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(MSG_SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            Log.v(LOGTAG, "onProgressChanged: progress=" + progress);

            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            resetAutoHideTimer();

//            if (!mSeeking)
//                mSeeking = true;

            long duration = mPlayer.getDuration();
            mTargetPosition = (duration * progress) / SEEK_BAR_MAX;
            //mPlayer.seekTo((int) newposition);
            if (mTime != null) {
                mTime.setText(stringForTime((int)mTargetPosition)+"/"+stringForTime((int)duration));
            }
            mPositionChanged = true;

            mHandler.removeCallbacks(mRunnable);
            mRunnable = new Runnable() {
                //            	long pos = mTargetPosition;
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(mPositionChanged) {
                        Log.i(LOGTAG, "target position is " + mTargetPosition);
                        doSeek((int) mTargetPosition);
                    }
                }
            };
            mHandler.postDelayed(mRunnable, 1500);

            mSeeking = true;
        }

        public void onStopTrackingTouch(SeekBar bar) {
            Log.v(LOGTAG, "onStopTrackingTouch");
//          mSeeking = false;
            setProgress();
            resetAutoHideTimer();

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS);
        }
    };

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();
        //        boolean isFullScreen();
//        void    toggleFullScreen();
        String   getSourceName();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<MediaPlaybar> mView;

        MessageHandler(MediaPlaybar view) {
            mView = new WeakReference<MediaPlaybar>(view);
        }
        @Override
        public void handleMessage(Message msg) {
            MediaPlaybar view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case MSG_FADE_OUT:
                    view.hide();
                    break;
                case MSG_SHOW_PROGRESS:
//                	Log.i(LOGTAG, "is seeking: " + view.isSeeking());
//                	Log.i(LOGTAG, "is showing: " + view.isShowing());
//                	Log.i(LOGTAG, "is playing: " + view.mPlayer.isPlaying());
                    if (!view.isSeeking() /*&& view.isShowing() && view.mPlayer.isPlaying()*/) {
                        Log.v(LOGTAG, "handleMessage: update progress");
                        pos = view.setProgress();
                        msg = obtainMessage(MSG_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }
}