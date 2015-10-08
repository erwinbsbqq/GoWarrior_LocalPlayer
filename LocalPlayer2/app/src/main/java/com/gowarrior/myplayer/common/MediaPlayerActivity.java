package com.gowarrior.myplayer.common;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnBufferingUpdateListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnCompletionListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnErrorListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnInfoListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnPreparedListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnSeekCompleteListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnTimedTextListener;
import com.gowarrior.myplayer.common.AbsMediaPlayer.OnVideoSizeChangedListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaPlayerActivity extends Activity implements
        OnCompletionListener, OnErrorListener, OnInfoListener,
        OnBufferingUpdateListener, OnPreparedListener, OnSeekCompleteListener,
        OnVideoSizeChangedListener, SurfaceHolder.Callback,
        MediaPlaybar.MediaPlayerControl,
        OnTimedTextListener {

    public final static String LOGTAG = "MediaPlayerActivity";

    public enum SKIPMODE {
        PREVIOUS, CURRENT, NEXT
    }

    private static final int SURFACE_AUTO = 0;
    private static final int SURFACE_STANDARD = 1;
    private static final int SURFACE_FULL = 2;
    private static final int SURFACE_MAX = 3;

    private static final String KEY_ASPECT_RATIO = "aspect_ratio";

    protected Display mDisplay;
    private ImageView mPause;
    private TextView mSubtitle;

    protected SurfaceView mSurfaceView;
    protected SurfaceHolder mSurfaceHolder;

    private int mPlayerType = 0;

    protected AbsMediaPlayer mPlayer;

    protected MediaPlaybar mController;
    private SharedPreferences mSharedPreferences;


    protected int mPosition = 0;
    protected String mSourceUri;
    private boolean mIsStarted = false;



    private Integer[] mTimedTexts;
    private Integer[] mAudioTracks;
    private MediaPlayer.TrackInfo[] mAudioTrackInfos;

    private Boolean mTrackInfoPrepared = false;
    private Handler mTackInfoHandler = new Handler();
    Runnable mTackInfoRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTrackInfoPrepared) {
                return;
            }
            if (mPlayer != null && mPlayer.isPlaying()) {
                prepareTracks();
                mTrackInfoPrepared = true;
            } else {
                mTackInfoHandler.postDelayed(mTackInfoRunnable, 3000);
            }
        }
    };

    protected String onGetSource(SKIPMODE mode) {
        return null;
    }

    protected String[] onGetTimedText(String filename) {
        return null;
    }

    protected void OnTimedTextReady(int count) {
        return;
    }

    protected void OnAudioTrackInfoReady(MediaPlayer.TrackInfo[] trackInfo) {
        return;
    }

    protected void onPlaybackError(int err) {
        return;
    }



    protected void prepareSource(String uri) {
        if (uri == null || uri.isEmpty()) {
            return;
        }
        mSourceUri = uri;
        try {

            mPlayer.setDataSource(uri);
        } catch (IllegalArgumentException e) {
            Log.v(LOGTAG, e.getMessage());
            finish();
            return;
        } catch (IllegalStateException e) {
            Log.v(LOGTAG, e.getMessage());
            finish();
            return;
        } catch (IOException e) {
            Log.v(LOGTAG, e.getMessage());
            onPlaybackError(1);
            return;
        } catch (Exception e) {
            finish();
            return;
        }

        mController.update();
        mPause.setVisibility(View.INVISIBLE);
        try {
            mPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            // Log.v(LOGTAG, e.getMessage());
            finish();
        } catch (Exception e) {
            finish();
            return;
        }
    }

    protected void setPlayerType(int type) {
        mPlayerType = type;
    }

    protected void createPlayer(int type, SurfaceHolder holder) {
        // mPlayer = new MediaPlayer();
        mPlayer = AbsMediaPlayer.getMediaPlayer(1);

        mPlayer.setOnErrorListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnPreparedListener(this);

        mPlayer.setOnSeekCompleteListener(this);
        mPlayer.setOnTimedTextListener(this);

        mPlayer.reset();
        mPlayer.setDisplay(holder);
    }

    protected void destroyPlayer() {
        if (mPlayer != null) {
            mPlayer.setDisplay(null);
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void initSurface() {
        mSurfaceView = (SurfaceView) this.findViewById(R.id.player_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);

        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mController.isShowing()) {
                    mController.hide();
                } else {
                    mController.show();
                }
            }
        });
    }

    public void skip(SKIPMODE mode) {
        String uri = onGetSource(mode);
        Log.v(LOGTAG, "skip: uri=" + uri);
        if (uri != null) {
            if (mPlayer == null) {
                return;
            }
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            }
            mPlayer.stop();
            mPlayer.reset();
            prepareSource(uri);
        } else {
            finish();
        }
    }

    private void initController() {
        mController = new MediaPlaybar(this);
        mController.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.v(LOGTAG, "onKey: keyCode=" + keyCode);
                if (KeyEvent.ACTION_UP == event.getAction()) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (mController.isShowing()) {
                                mController.hide();
                            }
                            break;

                        case KeyEvent.KEYCODE_HOME:
                            return false;

                        default:
                            break;
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mPlayerType = intent.getIntExtra("player_type", 0);

        // Stop screen from locking & off
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        // WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // setContentView(R.layout.main);

        initSurface();
        initController();
        mController.setMediaPlayer(this);
        mController.setAnchorView((RelativeLayout) findViewById(R.id.playbar_container));

        mPause = (ImageView) findViewById(R.id.player_pause);

        mSubtitle = (TextView) this.findViewById(R.id.subtitle);

        mDisplay = getWindowManager().getDefaultDisplay();
        //mVolumeBar = new VolumeBar(this);

        mSharedPreferences = getSharedPreferences("Settings",
                Activity.MODE_PRIVATE);
    }

    @Override
    protected void onStart() {
        Log.v(LOGTAG, "onStart Called");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.v(LOGTAG, "onStop Called");
        super.onStop();
        if (mPlayer.isPlaying()) {
            mPosition = mPlayer.getCurrentPosition();
        }

        mIsStarted = false;
        mPlayer.stop();
        destroyPlayer();
    }

    public void onResume() {
        Log.v(LOGTAG, "onResume");
        super.onResume();

        if (!mController.isShowing())
            mController.show();
    }

    @Override
    protected void onDestroy() {
        Log.v(LOGTAG, "onDestroy Called");
        mController.hide();
        mController.stopGetCurPos();
        destroyPlayer();
        //TODO
        //NmpServiceApi.setLastScreenMode(0);
        super.onDestroy();
    }

    private void doPauseResume() {

        if (mPlayer.isPlaying()) {
            pause();
            mPause.setVisibility(View.VISIBLE);
            mController.show();
        } else {
            start();
            mPause.setVisibility(View.INVISIBLE);
            mController.hide();
        }
        // mController.show();
        mPause.getParent().requestTransparentRegion(mSurfaceView);
    }

    public void hidePauseView() {
        mPause.setVisibility(View.INVISIBLE);
        mPause.getParent().requestTransparentRegion(mSurfaceView);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean keyHandled = true;
        Log.v(LOGTAG, "onKeyDown: keyCode=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                skip(SKIPMODE.NEXT);
                break;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_PAGE_UP:
                skip(SKIPMODE.PREVIOUS);
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!mController.isShowing())
                    mController.show();
                else
                    keyHandled = false;
                break;

            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (!mController.isSeeking()) {
                    doPauseResume();
                }
                break;

            case KeyEvent.KEYCODE_META_RIGHT:
            case KeyEvent.KEYCODE_INFO:
                if (mController.isShowing())
                    mController.hide();
                else
                    mController.show();
                break;

            case KeyEvent.KEYCODE_BACK:
                if (mController.isShowing())
                    mController.hide();
                else {
                    // if (mPlayer.isPlaying()) {
                    mPosition = mPlayer.getCurrentPosition();
                    // }
                    mPlayer.pause();
                    keyHandled = false;
                }
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                keyHandled = true;
                break;

            default:
                keyHandled = false;
                break;
        }

        if (!keyHandled)
            keyHandled = super.onKeyDown(keyCode, event);
        return keyHandled;
    }

    // -------------- Implement SurfaceHolder.Callback Interface
    // -------------------//

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(LOGTAG, "surfaceCreated Called");

        mController.setMediaPlayer(this);
        mController
                .setAnchorView((RelativeLayout) findViewById(R.id.playbar_container));
        createPlayer(mPlayerType, holder);
        // mController.show();
        prepareSource(onGetSource(SKIPMODE.CURRENT));
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.v(LOGTAG, "surfaceChanged Called");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(LOGTAG, "surfaceDestroyed Called");
    }

    // -------------- Implement OnXxxListener Interface -------------------//

    public boolean onError(AbsMediaPlayer mp, int whatError, int extra) {
        Log.v(LOGTAG, "onError Called");

        if (whatError == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            Log.v(LOGTAG, "onError: Server Died " + extra);
        } else if (whatError == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            Log.v(LOGTAG, "onError: Error Unknown " + extra);
        }

        onPlaybackError(whatError);

        return false;
    }

    public boolean onInfo(AbsMediaPlayer mp, int whatInfo, int extra) {
        if (whatInfo == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) {
            Log.v(LOGTAG, "onInfo: Bad Interleaving " + extra);
        } else if (whatInfo == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
            Log.v(LOGTAG, "onInfo: Not Seekable " + extra);
        } else if (whatInfo == MediaPlayer.MEDIA_INFO_UNKNOWN) {
            Log.v(LOGTAG, "onInfo: Unknown " + extra);
        } else if (whatInfo == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
            Log.v(LOGTAG, "onInfo: Video Track Lagging " + extra);
			/*
			 * Android Version 2.0 and Higher } else if (whatInfo ==
			 * MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
			 * Log.v(LOGTAG,"MediaInfo, Media Info Metadata Update " + extra);
			 */
        }
        return false;
    }

    public void onCompletion(AbsMediaPlayer mp) {
        Log.v(LOGTAG, "onCompletion Called");
        mPosition = 0;
        skip(SKIPMODE.NEXT);
    }

    public void onVideoSizeChanged(AbsMediaPlayer mp, int width, int height) {
        Log.v(LOGTAG, "onVideoSizeChanged, width: " + width + ", height: "
                + height);
        changeAspectRatio();
    }

    public void changeAspectRatio() {
        int ratio = mSharedPreferences.getInt(KEY_ASPECT_RATIO, 2);
        changeAspectRatio(ratio);
    }

    public void changeAspectRatio(int ratio) {
        // Get video size
        if (mPlayer == null) {
            return;
        }
        int videoWidth = mPlayer.getVideoWidth();
        int videoHeight = mPlayer.getVideoHeight();
        if (videoWidth <= 0 || videoHeight <= 0) {
            Log.v(LOGTAG, "Fail to get video size.");
            return;
        } else {
            Log.v(LOGTAG, "video size = " + videoWidth + "x" + videoHeight);
        }

        // Get current video display area size
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int displayWidth = size.x;
        int displayHeight = size.y;
        Log.v(LOGTAG, "window size = " + displayWidth + "x" + displayHeight);

        double ard = 0;
        double art = 0;
        switch (ratio) {

            case SURFACE_AUTO:
                // not maintaining aspect ratio.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // mPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
                break;
            case SURFACE_STANDARD:
                // maintaining aspect ratio.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // mPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
                ard = (double) displayWidth / (double) displayHeight;
                art = (double) videoWidth / (double) videoHeight;
                if (art < ard) {
                    displayWidth = displayHeight * videoWidth / videoHeight;
                } else {
                    displayHeight = displayWidth * videoHeight / videoWidth;
                }
                break;
            case SURFACE_FULL:
                // The whole surface area is always used.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // mPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }
                break;
            default:
                return;
        }

        Log.v(LOGTAG, "SurfaceView layout = " + displayWidth + "x"
                + displayHeight);
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = displayWidth;
        lp.height = displayHeight;
        mSurfaceView.setLayoutParams(lp);
        mSurfaceView.invalidate();
    }



    public void onPrepared(AbsMediaPlayer mp) {
        Log.v(LOGTAG, "onPrepared Called");

        changeAspectRatio();

        mTrackInfoPrepared = false;
        // prepareTracks();

        mp.start();
        mIsStarted = true;
        int currentPos = mp.getCurrentPosition();
        int time = Math.abs(mPosition - currentPos);
        if (time > 500) {
            mp.seekTo(mPosition);
        }
        mPosition = 0;

        // mController.setMediaPlayer(this);
        // mController.setAnchorView((RelativeLayout)findViewById(R.id.playbar_container));
        // mController.setEnabled(true);
        mController.show();
        mTackInfoHandler.postDelayed(mTackInfoRunnable, 3000);
    }

    private void prepareTracks() {
        Log.e(LOGTAG, "prepareTracks call");
        mTimedTexts = null;
        mAudioTracks = null;
        mTimedTexts = null;
        mAudioTrackInfos = null;

        String uri = mSourceUri;
        if (uri == null || uri.isEmpty()) {
            return;
        }
        try {
            File file = new File(uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && file.isFile()) {
                String[] timeTexts = onGetTimedText(uri);
                if (timeTexts != null) {
                    for (int i = 0; i < timeTexts.length; i++) {
                        Log.e(LOGTAG, "add TimeTestSource " + timeTexts[i]);
                        mPlayer.addTimedTextSource(timeTexts[i]);
                    }
                } else {
                    Log.e(LOGTAG, " get time test == null");
                }
                mTimedTexts = findTrackIndexFor(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
                Log.e(LOGTAG, "mTimeTests length == " + mTimedTexts.length);
                if (mTimedTexts != null && mTimedTexts.length > 0) {
                    if (mSubtitle != null) {
                        mSubtitle.setVisibility(View.VISIBLE);
                    }
                    OnTimedTextReady(mTimedTexts.length);
                    selectSubtitle(0);
                } else {
                    if (mSubtitle != null) {
                        mSubtitle.setVisibility(View.INVISIBLE);
                    }
                    OnTimedTextReady(0);
                }
            }
        } catch (Exception e) {
            Log.v(LOGTAG, "findTrackIndexFor TIMEDTEXT fail.");
        }

        try {
            File file = new File(uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && file.isFile()) {

                mAudioTracks = findTrackIndexFor(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
                MediaPlayer.TrackInfo[] tracks = mPlayer.getTrackInfo();
                ArrayList<MediaPlayer.TrackInfo> trackList = new ArrayList<MediaPlayer.TrackInfo>();
                if (tracks != null) {
                    for (int i = 0; i < tracks.length; i++) {
                        if (tracks[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                            trackList.add(tracks[i]);
                            // Log.v(LOGTAG, "MediaPlayer Audio Track " + i
                            // + " " + tracks[i].getTrackType()
                            // + " " + tracks[i].getLanguage());
                        }
                    }
                    mAudioTrackInfos = trackList
                            .toArray(new MediaPlayer.TrackInfo[trackList.size()]);
                }
                OnAudioTrackInfoReady(mAudioTrackInfos);
            }
        } catch (Exception e) {
            Log.v(LOGTAG, "findTrackIndexFor AUDIO fail.");
        }
    }

    @Override
    public void onSeekComplete(AbsMediaPlayer paramAbsMediaPlayer) {
        // TODO Auto-generated method stub
        mController.onSeekComplete();
    }

    @Override
    public void onBufferingUpdate(AbsMediaPlayer paramAbsMediaPlayer,
                                  int paramInt) {
        // TODO Auto-generated method stub
        mController.onBufferingComplete();
    }

    // -------------- Implement MediaController.MediaPlayerControl Interface
    // -------------------//

    public boolean canPause() {
        return true;
    }

    public boolean canSeekBackward() {
        return true;
    }

    public boolean canSeekForward() {
        return true;
    }

    public int getBufferPercentage() {
        return 0;
    }

    public int getCurrentPosition() {
        try {
            if (mIsStarted) {
                return mPlayer.getCurrentPosition();
            } else {
                Log.i(LOGTAG, "mIsStarted:"+mIsStarted);
                return -1;
            }
        } catch (NullPointerException e) {
            Log.v(LOGTAG, "null mPlayer");
            return -1;
        }
    }

    public int getDuration() {
        try {
            if (mIsStarted) {
                return mPlayer.getDuration();
            } else {
                Log.i(LOGTAG, "mIsStarted:"+mIsStarted);
                return -1;
            }
        } catch (NullPointerException e) {
            Log.v(LOGTAG, "null mPlayer");
            return -1;
        }
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public void pause() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    public void seekTo(int pos) {
        mPlayer.seekTo(pos);
    }

    public void start() {
        mPlayer.start();
    }

    public String getSourceName() {
        return null;
    }

    public String replace(CharSequence target, CharSequence replacement) {
        return Pattern.compile(target.toString(), Pattern.LITERAL)
                .matcher(target)
                .replaceAll(Matcher.quoteReplacement(replacement.toString()));
    }

    public String stripHtml(String html) {
        return Html.fromHtml(html).toString();
    }

    @Override
    public void onTimedText(AbsMediaPlayer paramAbsMediaPlayer, String text) {
        Log.v(LOGTAG, "onTimedText: " + text);
        if (text != null && mSubtitle != null) {
            text = text.replace("</span>\n<span>", "\\n");
            String text1 = stripHtml(text);
            text1 = text1.replace("\\n", "\n");
            SpannableString sp = new SpannableString(text1);
            sp.setSpan(new BackgroundColorSpan(Color.RED), 0, text1.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // mSubtitle.setText(text);
            mSubtitle.setText(sp);
        }
    }

    public void addTimedTextSource(String path) {
        mPlayer.addTimedTextSource(path);
    }

    public void closeSubtitle() {
        Log.e(LOGTAG, "closeSubtitle call");
        mSubtitle.setVisibility(View.INVISIBLE);
    }

    public void closeSubtitle(int index) {
        Log.e(LOGTAG, "closeSubtitle call index == " + index);
        mPlayer.deselectTrack(index);
        mSubtitle.setVisibility(View.INVISIBLE);
    }

    public void selectSubtitle(int index) {
        Log.e(LOGTAG, "selectSubtitle " + index);
        if (mTimedTexts != null && index < mTimedTexts.length && index >= 0) {
            mPlayer.selectTrack(mTimedTexts[index]);
            mSubtitle.setVisibility(View.VISIBLE);
        } else {
            mSubtitle.setVisibility(View.INVISIBLE);
        }
    }

    public void selectAudioTrack(int index) {
        if (mAudioTracks != null && index < mAudioTracks.length && index >= 0) {
            mPlayer.selectTrack(mAudioTracks[index]);
        }
    }

    private Integer[] findTrackIndexFor(int mediaTrackType) {
        Integer[] array = mPlayer.findTrackIndexFor(mediaTrackType);
        return array;
    }

    public int getTimedTextCount() {
        if (mTimedTexts != null) {
            return mTimedTexts.length;
        } else {
            return 0;
        }
    }

    public MediaPlayer.TrackInfo[] getAudioTrackInfo() {
        return mAudioTrackInfos;
    }

}
