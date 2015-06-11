package com.gowarrior.myplayer.common;

import android.media.MediaPlayer;
import android.os.Parcel;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by jerry.xiong on 2015/6/11.
 */
public abstract class AbsMediaPlayer {
    public final static String LOGTAG = "AbsMediaPlayer";

    protected  static  AbsMediaPlayer getDefMediaPlayer() {
        Log.v(LOGTAG, "Using DefMediaPlayer");
        return DefMediaPlayer.getInstance();
    }

    public  static  AbsMediaPlayer getMediaPlayer() {
        return  getDefMediaPlayer();
    }

    public abstract int getCurrentPosition();

    public abstract int getDuration();

    public abstract int getVideoHeight();

    public abstract int getVideoWidth();

    public abstract int invoke(Parcel paramParcel1, Parcel paramParcel2);

    public abstract boolean isPlaying();

    public abstract void pause();

    public abstract void prepare() throws IOException, IllegalStateException;

    public abstract void prepareAsync() throws IllegalStateException ;

    public abstract void release();

    public abstract void reset();

    public abstract void seekTo(int paramInt);

    public abstract void setDataSource(String paramString) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    public abstract void setDataSource(String paramString, int paramInt) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    public abstract void setDataSource(String paramString1, String paramString2, int paramInt) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    public abstract void setDisplay(SurfaceHolder paramSurfaceHolder);

    public abstract void setOnBufferingUpdateListener(OnBufferingUpdateListener paramOnBufferingUpdateListener);

    public abstract void setOnCompletionListener(OnCompletionListener paramOnCompletionListener);

    public abstract void setOnErrorListener(OnErrorListener paramOnErrorListener);

    public abstract void setOnInfoListener(OnInfoListener paramOnInfoListener);

    public abstract void setOnPreparedListener(OnPreparedListener paramOnPreparedListener);

    public abstract void setOnProgressUpdateListener(OnProgressUpdateListener paramOnProgressUpdateListener);

    public abstract void setOnSeekCompleteListener(OnSeekCompleteListener paramOnSeekCompleteListener);

    public abstract void setOnVideoSizeChangedListener(OnVideoSizeChangedListener paramOnVideoSizeChangedListener);

    public abstract void setOnTimedTextListener(OnTimedTextListener paramOnTimedTextListener);



    public abstract void start();

    public abstract void stop();

    public abstract void addTimedTextSource(String path);
    public abstract void selectTrack(int index);
    public abstract void deselectTrack(int index);
    public abstract Integer[] findTrackIndexFor(int mediaTrackType);
    public abstract void setVideoScalingMode(int mode);

    public static interface OnBufferingUpdateListener {
        public abstract void onBufferingUpdate(AbsMediaPlayer paramAbsMediaPlayer, int paramInt);
    }

    public static interface OnCompletionListener {
        public abstract void onCompletion(AbsMediaPlayer paramAbsMediaPlayer);
    }

    public static interface OnErrorListener {
        public abstract boolean onError(AbsMediaPlayer paramAbsMediaPlayer, int paramInt1, int paramInt2);
    }

    public static interface OnInfoListener{
        public abstract boolean onInfo(AbsMediaPlayer paramAbsMediaPlayer, int paramInt1, int paramInt2);
    }

    public static interface OnPreparedListener {
        public abstract void onPrepared(AbsMediaPlayer paramAbsMediaPlayer);
    }

    public static interface OnProgressUpdateListener {
        public abstract void onProgressUpdate(AbsMediaPlayer paramAbsMediaPlayer, int paramInt1, int paramInt2);
    }

    public static interface OnSeekCompleteListener {
        public abstract void onSeekComplete(AbsMediaPlayer paramAbsMediaPlayer);
    }

    public static interface OnVideoSizeChangedListener {
        public abstract void onVideoSizeChanged(AbsMediaPlayer paramAbsMediaPlayer, int paramInt1, int paramInt2);
    }
    public static interface OnTimedTextListener {
        public abstract void onTimedText(AbsMediaPlayer paramAbsMediaPlayer, String text);
    }
    public MediaPlayer.TrackInfo[] getTrackInfo() {
        return null;
    }

}
