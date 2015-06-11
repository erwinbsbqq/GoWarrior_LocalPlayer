package com.gowarrior.myplayer.common;

import android.media.MediaPlayer;
import android.media.MediaPlayer.TrackInfo;
import android.media.TimedText;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DefMediaPlayer extends AbsMediaPlayer implements
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnTimedTextListener {

    public final static String LOGTAG = "DefMediaPlayer";

    protected static DefMediaPlayer sInstance = null;

    private MediaPlayer mPlayer;

    protected AbsMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = null;
    protected AbsMediaPlayer.OnCompletionListener mOnCompletionListener = null;
    protected AbsMediaPlayer.OnErrorListener mOnErrorListener = null;
    protected AbsMediaPlayer.OnInfoListener mOnInfoListener = null;
    protected AbsMediaPlayer.OnPreparedListener mOnPreparedListener = null;
    protected AbsMediaPlayer.OnProgressUpdateListener mOnProgressUpdateListener = null;
    protected AbsMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = null;
    protected AbsMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = null;
    protected AbsMediaPlayer.OnTimedTextListener mOnTimedTextListener = null;

    private boolean mIsReady = false;

    public static DefMediaPlayer getInstance() {
        if (sInstance == null)
            sInstance = new DefMediaPlayer();
        sInstance.createPlayer();
        return sInstance;
    }

    private void createPlayer() {
        if (mPlayer == null) {
            mIsReady = false;
            Log.v(LOGTAG, "new MediaPlayer");
            mPlayer = new MediaPlayer();
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnInfoListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
            mPlayer.setOnPreparedListener(this);
            // mPlayer.setOnProgressUpdateListener(this);
            mPlayer.setOnSeekCompleteListener(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mPlayer.setOnTimedTextListener(this);
            }
        }
    }

    protected DefMediaPlayer() {
    }

    @Override
    public int getCurrentPosition() {
        int pos = mPlayer.getCurrentPosition();
        // Log.v(LOGTAG, "Current Position = " + pos);
        return pos;
    }

    @Override
    public int getDuration() {
        if (mIsReady) {
            return mPlayer.getDuration();
        } else {
            return -1;
        }
    }

    @Override
    public int getVideoHeight() {
        return mPlayer.getVideoHeight();
    }

    @Override
    public int getVideoWidth() {
        return mPlayer.getVideoWidth();
    }

    @Override
    public int invoke(Parcel paramParcel1, Parcel paramParcel2) {
        return 0;
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        Log.v(LOGTAG, "prepare()");
        mPlayer.prepare();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        Log.v(LOGTAG, "prepareAsync()");
        mPlayer.prepareAsync();
    }

    @Override
    public void release() {
        Log.v(LOGTAG, "release()");
        mIsReady = false;
        mPlayer.release();
        mPlayer = null;
    }

    @Override
    public void reset() {
        Log.v(LOGTAG, "reset()");
        mIsReady = false;
        mPlayer.reset();
    }

    @Override
    public void seekTo(int paramInt) {
        if (mIsReady) {
            Log.v(LOGTAG, "seekTo " + paramInt);
            mPlayer.seekTo(paramInt);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException {
        Log.v(LOGTAG, "setDataSource: " + path);
        mIsReady = false;
        mPlayer.setDataSource(path);
    }

    @Override
    public void setDataSource(String path, int paramInt) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException {
        Log.v(LOGTAG, "setDataSource: " + path);
        mIsReady = false;
        mPlayer.setDataSource(path);
    }

    @Override
    public void setDataSource(String path, String paramString2, int paramInt)
            throws IOException, IllegalArgumentException, SecurityException,
            IllegalStateException {
        Log.v(LOGTAG, "setDataSource: " + path);
        mIsReady = false;
        mPlayer.setDataSource(path);
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        mPlayer.setDisplay(holder);
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    @Override
    public void setOnProgressUpdateListener(OnProgressUpdateListener listener) {
        // TODO Auto-generated method stub

    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    public void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    // @Override
    // public void setScreenOnWhilePlaying(boolean paramBoolean) {
    // // TODO Auto-generated method stub
    //
    // }

    @Override
    public void start() {
        Log.v(LOGTAG, "start()");
        mPlayer.start();
    }

    @Override
    public void stop() {
        Log.v(LOGTAG, "stop()");
        mIsReady = false;
        mPlayer.stop();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        mOnSeekCompleteListener.onSeekComplete(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        mIsReady = true;
        mOnPreparedListener.onPrepared(this);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        return mOnInfoListener.onInfo(this, what, extra);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        return mOnErrorListener.onError(this, what, extra);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        mOnCompletionListener.onCompletion(this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.v(LOGTAG, LogTag.getMethodLine());
        mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }

    @Override
    public void setOnTimedTextListener(
            OnTimedTextListener paramOnTimedTextListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        mOnTimedTextListener = paramOnTimedTextListener;
    }

    @Override
    public void addTimedTextSource(String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Log.e(LOGTAG, "SDK Version is wrong");
            return;
        }
        Log.e(LOGTAG, "add subtitle :" + path);
        try {
            if (path == null || path.isEmpty()) {
                return;
            }
            if (path.endsWith(".sub") && path.contains("*")) {
                int index = path.indexOf("*");
                String idxpath = path.substring(0, index);
                String subpath = path.substring(index + 1);
                Log.e(LOGTAG, "idxpath == " + idxpath);
                Log.e(LOGTAG, "subpath == " + subpath);
                File subfile = new File(subpath);
                File idxfile = new File(idxpath);
                if (!subfile.exists() || !idxfile.exists()) {
                    Log.e(LOGTAG, "subtitle file not exist");
                    return;
                }
            } else {
                File file = new File(path);
                if (!file.exists()) {
                    return;
                }
            }

            mPlayer.addTimedTextSource(path,
                    MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
        } catch (IllegalArgumentException e) {
            Log.e(LOGTAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(LOGTAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOGTAG, e.getMessage());
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
        }
        Log.e(LOGTAG, "add subtitle :" + path + "succeed");
    }

    @Override
    public void onTimedText(MediaPlayer mp, TimedText text) {
        Log.d(LOGTAG, "onTimedText DefMediaPlayer   " + text);
        if (mOnTimedTextListener != null && text != null) {
            Log.v(LOGTAG, "onTimedText DefMediaPlayer   " + text.getText());
            mOnTimedTextListener.onTimedText(this, text.getText());
        }
    }

    @Override
    public void selectTrack(int index) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        try {
            Log.v(LOGTAG, "MediaPlayer selectTrack " + 1);
            mPlayer.selectTrack(index);
        } catch (Exception exception) {
            Log.v(LOGTAG, "selectTrack fail");
        }
    }

    @Override
    public void deselectTrack(int index) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        try {
            Log.v(LOGTAG, "MediaPlayer deselectTrack " + index);
            mPlayer.deselectTrack(index);
        } catch (Exception exception) {
            Log.v(LOGTAG, "deselectTrack fail");
        }
    }

    public Integer[] findTrackIndexFor(int mediaTrackType) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return list.toArray(new Integer[0]);
        }

        Log.v(LOGTAG, "Begin findTrackIndexFor type = " + mediaTrackType);
        try {
            TrackInfo[] trackInfo = mPlayer.getTrackInfo();
            for (int i = 0; i < trackInfo.length; i++) {
                // Log.v(LOGTAG, "TrackInfo " + i + " type = " +
                // trackInfo[i].getTrackType()
                // + " language = " + trackInfo[i].getLanguage());
                if (trackInfo[i].getTrackType() == mediaTrackType) {
                    list.add(i);
                }
            }
        } catch (Exception exception) {
            String msg = exception.getMessage();
            if (msg != null) {
                Log.v(LOGTAG, "findTrackIndexFor fail msg = " + msg);
            }
            Log.v(LOGTAG, "findTrackIndexFor  " + mediaTrackType + "  fail");
        }
        return list.toArray(new Integer[0]);
    }

    @Override
    public TrackInfo[] getTrackInfo() {
        TrackInfo[] trackInfo = null;
        try {
            trackInfo = mPlayer.getTrackInfo();
        } catch (Exception exception) {
            Log.v(LOGTAG, "getTrackInfo fail");
        }
        return trackInfo;
    }

    @Override
    public void setVideoScalingMode(int mode) {
        try {
            mPlayer.setVideoScalingMode(mode);
        } catch (Exception e) {
            Log.v(LOGTAG, "setVideoScalingMode fail");
            return;
        }
    }

}
