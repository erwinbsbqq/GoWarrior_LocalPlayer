package com.gowarrior.myplayer.local;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.common.AbsMediaPlayer;
import com.gowarrior.myplayer.common.MediaPlayerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalAudioPlayer extends MediaPlayerActivity {

    public final static String LOGTAG = "LocalAudioPlayer";

    private String mPath;
    private ArrayList<String> mList;
    private int mPlayIndex;

    private TypedArray mPictures;
    private int mBgIndex = 0;
    private int mAnimId;



    private LocalAudioSettings mOptionSetting;
    private long mTimeout;
    private static final int MSG_HIDE_SELF = 1;
    private static final int OPTION_TIMEOUT = 3000;

    private Time mTime = new Time();
    private Random mRandom = new Random();

    //private LrcRead mLrcRead;
    //private LyricView mLyricView;
    private ImageView mAlbumCover;
    private ImageView mBackgroundImageView;
    private TextView mTitle;
    private TextView mAlbum;
    private TextView mArtist;
    //private List<LyricContent> mLyricList = new ArrayList<LyricContent>();
    //private boolean hasLyrics = false;
    //private String mCurrentUrl;
    //private Handler mLrcHandler = new Handler();
    private Runnable mRunnable;
    private int mDuration;

    //private SearchLRC mSearchLRC;
    //LrcTask mLrtask;
    private ExecutorService mExecutorService = null;

    Handler mCurrentTimehandler;
    Runnable mCurrentTimeRunnable;
    TextView mCurrentTimeView;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;

    TextView mNextSongView;
    TextView mNextSongViewTip;
    ImageView mPauseView;

    private BroadcastReceiver mPlugReceiver;
    private FileHelper mFileHelper;

    private View mTipView = null;
    private TextView mTipViewA = null;
    private TextView mTipViewB = null;
    private Handler mTipHandler = new Handler();
    private Runnable mTipRunnable = new Runnable() {

        @Override
        public void run() {
            mTipView.setVisibility(View.INVISIBLE);
            if (mOptionSetting.getItemValue("play_order") == 2
                    || mList.size() < 2) {
                finish();
            } else {
                skip(SKIPMODE.NEXT);
            }
        }

    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_SELF:
                    Log.i(LOGTAG, "timeout, hide option settings");
                    mOptionSetting.hide();
                    // mController.show();
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && true == isOptionSettingShowing()) {
            Log.i(LOGTAG, "receive onkey down, reset time out");
            resetTimeOut();
        }

        return super.dispatchKeyEvent(event);
    }

    private void showOptionSettings(long timeout) {
        Log.i(LOGTAG, "show option settings");
        mOptionSetting.show();
        mOptionSetting.requestFocus();
        mTimeout = timeout;
        resetTimeOut();
    }

    private void hideOptionSettings() {
        int msg = MSG_HIDE_SELF;
        mHandler.removeMessages(msg);

        Log.i(LOGTAG, "hide option settings");
        mOptionSetting.hide();
        // mController.show();
    }

    private void resetTimeOut() {
        int msg = MSG_HIDE_SELF;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessageDelayed(msg, mTimeout);
    }

    private boolean isOptionSettingShowing() {
        return (mOptionSetting.getVisible() == View.VISIBLE);
    }

//    public int lyricsIndex() {
//        int index = 0;
//        int duration = 0;
//        int currentTime = 0;
//        if (mPlayer.isPlaying()) {
//            currentTime = getCurrentPosition();
//            duration = mPlayer.getDuration();
//        }
//        if (currentTime < duration) {
//            for (int i = 0; i < mLyricList.size(); i++) {
//                if (i < mLyricList.size() - 1) {
//                    if (currentTime < mLyricList.get(i).getLyricTime()
//                            && i == 0) {
//                        index = i;
//                    }
//                    if (currentTime > mLyricList.get(i).getLyricTime()
//                            && currentTime < mLyricList.get(i + 1)
//                            .getLyricTime()) {
//                        index = i;
//                    }
//                }
//                if (i == mLyricList.size() - 1
//                        && currentTime > mLyricList.get(i).getLyricTime()) {
//                    index = i;
//                }
//            }
//        }
//        // System.out.println(index);
//        return index;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        mFileHelper = FileHelper.getInstance(this);
        Intent intent = getIntent();

        String action = intent.getAction();
        Set<String> cates = intent.getCategories();
        Uri uri = intent.getData();
        String scheme = intent.getScheme();

        Log.v(LOGTAG, "Action=" + action);
        Log.v(LOGTAG, "Categories=" + cates);
        Log.v(LOGTAG, "Uri=" + uri);
        Log.v(LOGTAG, "Scheme=" + scheme);



        if (uri != null) {
            try {
                String path = uri.getPath();
                File file = new File(path);
                if (mList == null) {
                    mList = new ArrayList<String>();
                } else {
                    mList.clear();
                }
                mList.add(file.getName());
                mPath = path
                        .substring(0, path.length() - mList.get(0).length());
                mPlayIndex = 0;
            } catch (Exception e) {
                finish();
            }
        } else {
            // mSourceUri = intent.getStringExtra("path");
            mPath = intent.getStringExtra("path");
            // mList = intent.getStringArrayListExtra("play_list");
            mList = mFileHelper.playlist;
            mPlayIndex = intent.getIntExtra("start_index", 0);

        }

        Log.v(LOGTAG, "onCreate: mPlayIndex=" + mPlayIndex);

        // mPlayerType = intent.getIntExtra("player_type", 0);

        // mPictures = getResources().obtainTypedArray(R.array.audioBackground);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.local_audio_player);
        setPlayerType(1);
        super.onCreate(savedInstanceState);

        // Do this after calling super.onCreate(...)
        // mController.setTimeout(0);

        ViewGroup root = (ViewGroup) findViewById(R.id.player_container);
        mOptionSetting = new LocalAudioSettings(this, root);
        mOptionSetting.hide();
        mTime.setToNow();
        mRandom.setSeed(mTime.toMillis(true));

        mBackgroundImageView = (ImageView) findViewById(R.id.local_audio_background);
        mAlbumCover = (ImageView) findViewById(R.id.local_audio_album_cover); //
        mTitle = (TextView) findViewById(R.id.local_audio_title);
        mAlbum = (TextView) findViewById(R.id.local_audio_album);
        mArtist = (TextView) findViewById(R.id.local_audio_artist);
        mCurrentTimeView = (TextView) findViewById(R.id.local_audio_time);
        mNextSongViewTip = (TextView) findViewById(R.id.local_audio_next_tip);
        mNextSongView = (TextView) findViewById(R.id.local_audio_next);
        mPauseView = (ImageView) findViewById(R.id.player_pause);
        mTipView = findViewById(R.id.state_tip);
        mTipViewA = (TextView) findViewById(R.id.state_tip_text1);
        mTipViewB = (TextView) findViewById(R.id.state_tip_text2);

        mExecutorService = (ExecutorService) Executors.newFixedThreadPool(5);
//        mLyricView = (LyricView) findViewById(R.id.lyricshow);
//        mLrcRead = new LrcRead();
//        mRunnable = new Runnable() {
//            public void run() {
//                if (mPlayer != null && mPlayer.getCurrentPosition() < mDuration
//                        && mPlayer.isPlaying() && hasLyrics) {
//                    mLyricView.SetIndex(lyricsIndex());
//                    mLyricView.invalidate();
//                    mLrcHandler.postDelayed(mRunnable, 100);
//                }
//            }
//        };

//        mSearchLRC = new SearchLRC();

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        mCurrentTimehandler = new Handler();
        mCurrentTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (mDuration < 0 && mPlayer != null) {
                    mDuration = mPlayer.getDuration();
                }
                if (mPlayer != null && mPlayer.getCurrentPosition() < mDuration
                        && mPlayer.isPlaying()) {
                    if (mCurrentTimeView != null) {
                        int position = mPlayer.getCurrentPosition();
                        int duration = mPlayer.getDuration();
                        mCurrentTimeView.setText(stringForTime(position)
                                + " / " + stringForTime(duration));
                    }
                }
                mCurrentTimehandler.postDelayed(mCurrentTimeRunnable, 1000);
            }
        };
        initReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOGTAG, "onDestroy Called");
        if (mPlugReceiver != null) {
            unregisterReceiver(mPlugReceiver);
        }
    }

    private void initReceiver() {
        if (mPlugReceiver != null) {
            return;
        }
        mPlugReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.v(LOGTAG, "BroadcastReceiver: " + intent.getAction());
                Log.v(LOGTAG, "Path: " + intent.getData().getPath());

                String action = intent.getAction();
                String path = intent.getData().getPath();
                if (action.equals(Intent.ACTION_MEDIA_REMOVED)
                        || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                    String currentPath = mPath;
                    if (currentPath.startsWith(path)) {
                        mPlayer.stop();
                        finish();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED); // system bug
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.setPriority(1);
        filter.addDataScheme("file");
        if (mPlugReceiver != null) {
            registerReceiver(mPlugReceiver, filter);
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
                    .toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private void resetMediaData() {
        mTitle.setText(getSourceName());
        mAlbum.setText("");
        mArtist.setText("");
        mAlbumCover.setImageResource(R.drawable.local_audio_album);
    }

    private void showMetaData() {
        try {
            // MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            // mmr.setDataSource(mCurrentUrl);
            String title = null;
            // title =
            // mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null) {
                Log.d(LOGTAG, "title:" + title);
                mTitle.setText(title);
//            } else {
//                File file = new File(mCurrentUrl);
//                String shortName = file.getName();
//                int pos = shortName.lastIndexOf('.');
//                shortName = shortName.substring(0, pos);
//                mTitle.setText(file.getName());
            }
            String album = null;
            // album =
            // mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (album != null) {
                Log.d(LOGTAG, "mime:" + album);
                mAlbum.setText(album);
            } else {
                mAlbum.setText("");
            }
            String artist = null;
            // artist =
            // mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist != null) {
                Log.d(LOGTAG, "mime:" + artist);
                mArtist.setText(artist);
            } else {
                mArtist.setText("");
            }
            byte[] pic = null;
            // pic = mmr.getEmbeddedPicture();
            if (pic != null) {
                Bitmap bm = BitmapFactory.decodeByteArray(pic, 0, pic.length);
                if (bm != null) {
                    Bitmap ReflectedImage = ReflectedImage(bm);
                    mAlbumCover.setImageBitmap(ReflectedImage);
                    // Bitmap blurImage = BlurImage(bm, 6);
                    // Todo blurImage.setDensity(density);
                    // mBackgroundImageView.setImageBitmap(blurImage);
                }
            } else {
                mAlbumCover.setImageResource(R.drawable.local_audio_album);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
            return;
        }
    }

    private String getName(int index) {
        if (mList != null && index < mList.size()) {
            File file = new File(mList.get(index));
            String shortName = file.getName();
            int pos = shortName.lastIndexOf('.');
            shortName = shortName.substring(0, pos);
            return shortName;
        } else {
            return null;
        }
    }

    private void showNext() {
        int order = mOptionSetting.getItemValue("play_order");
        if (order == 0) { // 顺序播放
            if (mPlayIndex == (mList.size() - 1)) {
                mNextSongViewTip.setVisibility(View.INVISIBLE);
                mNextSongView.setVisibility(View.INVISIBLE);
                return;
            }
        } else if (order == 3) { // 随机播放
            mNextSongViewTip.setVisibility(View.INVISIBLE);
            mNextSongView.setVisibility(View.INVISIBLE);
            return;
        } else if (order == 2) { // 单曲
            mNextSongViewTip.setVisibility(View.VISIBLE);
            mNextSongView.setVisibility(View.VISIBLE);
            mNextSongView.setText(getName(mPlayIndex));
            return;
        }

        mNextSongViewTip.setVisibility(View.VISIBLE);
        mNextSongView.setVisibility(View.VISIBLE);
        if (mPlayIndex < (mList.size() - 1)) {
            mNextSongView.setText(getName(mPlayIndex + 1));
        } else {
            mNextSongView.setText(getName(0));
        }
    }

//    private boolean prepareLylics(String path) {
//        if (path == null) {
//            return false;
//        }
//        int pos = path.lastIndexOf('.');
//        String lrcPath = path.substring(0, pos);
//        lrcPath += ".lrc";
//        File lrc = new File(lrcPath);
//        if (!lrc.exists()) {
//            return false;
//        }
//
//        try {
//            mLrcRead.Read(lrc.getAbsolutePath());
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            return false;
//        }
//
//        mLyricList = mLrcRead.GetLyricContent();
//        // 设置歌词资源
//        mLyricView.setSentenceEntities(mLyricList);
//        for (int i = 0; i < mLrcRead.GetLyricContent().size(); i++) {
//            System.out.println(mLrcRead.GetLyricContent().get(i).getLyricTime()
//                    + "-");
//            System.out.println(mLrcRead.GetLyricContent().get(i).getLyric()
//                    + "----");
//        }
//        return true;
//    }

//    private boolean prepareLylics(ArrayList<String> lines) {
//        if (lines == null || lines.size() == 0) {
//            return false;
//        }
//        mLrcRead.Read(lines);
//        mLyricList = mLrcRead.GetLyricContent();
//        mLyricView.setSentenceEntities(mLyricList);
//        for (int i = 0; i < mLrcRead.GetLyricContent().size(); i++) {
//            System.out.println(mLrcRead.GetLyricContent().get(i).getLyricTime()
//                    + "-");
//            System.out.println(mLrcRead.GetLyricContent().get(i).getLyric()
//                    + "----");
//        }
//        return true;
//    }

//    private void playLyrics() {
//        mLrcHandler.postDelayed(mRunnable, 100);
//    }

    // @Override
    // public boolean onKeyDown(int keyCode, KeyEvent event) {
    // Log.v(LOGTAG, "onKeyDown: keyCode="+keyCode);
    //
    // boolean keyHandled = true;
    // switch (keyCode) {
    // case KeyEvent.KEYCODE_BACK:
    // finish();
    // break;
    //
    // default:
    // keyHandled = false;
    // break;
    // }
    //
    // if (!keyHandled)
    // keyHandled = super.onKeyDown(keyCode, event);
    // return keyHandled;
    // }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean keyHandled = true;
        Log.v(LOGTAG, "onKeyDown: keyCode=" + keyCode);
        mTipView.setVisibility(View.INVISIBLE);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (mController.isShowing())
                    mController.hide();

                if (!isOptionSettingShowing())
                    showOptionSettings(OPTION_TIMEOUT);
                break;

            case KeyEvent.KEYCODE_INFO:
                if (isOptionSettingShowing())
                    hideOptionSettings();
                keyHandled = false; // to let super class show playbar
                break;

            case KeyEvent.KEYCODE_BACK:
                if (isOptionSettingShowing()) {
                    hideOptionSettings();
                } else {
                    finish();
                    keyHandled = false;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                skip(SKIPMODE.PREVIOUS);
                keyHandled = true;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                skip(SKIPMODE.NEXT);
                keyHandled = true;
                break;

            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mPlayer.isPlaying()) {
                    pause();
                    mPauseView.setVisibility(View.VISIBLE);
                } else {
                    start();
                   // mLrcHandler.postDelayed(mRunnable, 100);
                    mPauseView.setVisibility(View.INVISIBLE);
                }
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

    @Override
    public void onPrepared(AbsMediaPlayer mp) {
        super.onPrepared(mp);
        mController.hide();
        showMetaData();
        mDuration = mPlayer.getDuration();

        // show local lrc file
//        hasLyrics = prepareLylics(mCurrentUrl);
//        if (hasLyrics) {
//            mLyricView.setVisibility(View.VISIBLE);
//            playLyrics();
//        } else {
//            mLyricView.setVisibility(View.GONE);
//        }
//
//        // Get lrc online
//        if (!hasLyrics) {
//            String title = "";
//            String artist = "";
//            if (mTitle != null) {
//                title = mTitle.getText().toString();
//                if (mCurrentUrl.endsWith(mTitle.getText().toString())) {
//                    title = title.substring(0, title.lastIndexOf("."));
//                }
//            }
//            if (mArtist != null) {
//                artist = mArtist.getText().toString();
//            }
//            if (mLrtask != null) {
//                // return;
//                mLrtask.cancel(true);
//                mLrtask = null;
//            }
//            mLrtask = new LrcTask();
//            mLrtask.executeOnExecutor(mExecutorService, title, artist);
//        }

        // if (!hasLyrics && mArtist != null && mTitle != null) {
        // mLrtask.cancel(true);
        // mLrtask.executeOnExecutor(mExecutorService,
        // mTitle.getText().toString(),
        // mArtist.getText().toString());
        // }

        mCurrentTimehandler.postDelayed(mCurrentTimeRunnable, 1000);
    }

//    private void showLrcOnline(ArrayList<String> lines) {
//        hasLyrics = prepareLylics(lines);
//        if (hasLyrics) {
//            mLyricView.setVisibility(View.VISIBLE);
//            playLyrics();
//        } else {
//            mLyricView.setVisibility(View.GONE);
//        }
//    }

    private void changeBackground() {
        mAnimId = R.id.local_audio_background;
        fadeOut();
    }

    private int getImageIndex() {
        Random random = new Random();
        int i = random.nextInt(mPictures.length());
        if (i == mBgIndex) {
            i = (mBgIndex + 1) % mPictures.length();
        }
        mBgIndex = i;
        return mBgIndex;
    }

    private void setBgImage() {
        ImageView bg = (ImageView) findViewById(R.id.local_audio_background);
        bg.setImageDrawable(mPictures.getDrawable(getImageIndex()));
    }

    private int setBgImageRandom() {
        Random random = new Random();
        int index = 0;
        for (int i = 0; i < 3; i++) {
            index = random.nextInt(6);
            if (index != mBgIndex) {
                break;
            }
        }
        int[] ids = { R.drawable.local_audio_bg1, R.drawable.local_audio_bg2,
                R.drawable.local_audio_bg3, R.drawable.local_audio_bg4,
                R.drawable.local_audio_bg5, R.drawable.local_audio_bg6 };
        mBgIndex = index;
        ImageView bg = (ImageView) findViewById(R.id.local_audio_background);
        bg.setBackgroundResource(ids[mBgIndex]);
        return mBgIndex;
    }

    private void fadeIn() {
        Object target = (Object) findViewById(mAnimId);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(target, "alpha", 1f);
        fadeIn.setDuration(3000);
        fadeIn.start();
    }

    private void fadeOut() {
        Object target = (Object) findViewById(mAnimId);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(target, "alpha", 0f);
        fadeOut.setDuration(1000);
        fadeOut.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub
                // setBgImage();
                setBgImageRandom();
                // setBgSurface();
                fadeIn();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // TODO Auto-generated method stub

            }
        });
        fadeOut.start();
    }

    @Override
    protected String onGetSource(SKIPMODE mode) {
        if (mTipView.getVisibility() == View.VISIBLE) {
            return "";
        }

       //mLyricView.setVisibility(View.GONE);

        mTipHandler.removeCallbacks(mTipRunnable);
        changeBackground();
        String uri = null;



        if (mode == SKIPMODE.CURRENT) {
            if ((mPlayIndex >= 0) && (mPlayIndex < mList.size())) {
                uri = mPath + "/" + mList.get(mPlayIndex);
            } else {
                return null;
            }
           // mCurrentUrl = uri;
            resetMediaData();
            showNext();
            return uri;
        }

        switch (mOptionSetting.getItemValue("play_order")) { // play order
            case 0:
                mPlayIndex = (mode == SKIPMODE.NEXT ? mPlayIndex + 1
                        : mPlayIndex - 1);
                break;
            case 1:
                mPlayIndex = (mode == SKIPMODE.NEXT ? mPlayIndex + 1
                        : mPlayIndex - 1);
                if (mPlayIndex >= mList.size()) {
                    mPlayIndex = 0;
                } else if (mPlayIndex < 0) {
                    mPlayIndex = mList.size() - 1;
                }
                break;
            case 2:
                // not change
                break;
            case 3:
                mPlayIndex = mRandom.nextInt(mList.size());
                break;
            default:
                break;
        }

        if ((mPlayIndex >= 0) && (mPlayIndex < mList.size())) {
            uri = mPath + "/" + mList.get(mPlayIndex);
        } else {
            return null;
        }
        //mCurrentUrl = uri;
        resetMediaData();
        showNext();
        return uri;
    }

    @Override
    public String getSourceName() {
        String name = null;
        if (mPlayIndex >= 0 && mPlayIndex < mList.size()) {
            name = mList.get(mPlayIndex);
        } else {
            name = String.valueOf("");
        }
        return name;
    }

    @Override
    public void onPlaybackError(int err) {
        String name = getSourceName();
        mTipViewA.setText(name);
        // mTipViewB.setText(R.string.msg_media_unsupported);
        mTipView.setVisibility(View.VISIBLE);
        mTipHandler.postDelayed(mTipRunnable, 2000);
    }

    private class LrcTask extends AsyncTask<String, Integer, ArrayList<String>> {
        // onPreExecute方法用于在执行后台任务前做一些UI操作

        ArrayList<String> mLrc = new ArrayList<String>();

        @Override
        protected void onPreExecute() {
            Log.i(LOGTAG, "onPreExecute() called");
            mLrc.clear();
        }

        // doInBackground方法内部执行后台任务,不可在此方法内修改UI
        @Override
        protected ArrayList<String> doInBackground(String... params) {
            Log.i(LOGTAG, "doInBackground(Params... params) called");
            ArrayList<String> result = new ArrayList<String>();
//            try {
//                result = mSearchLRC.getLRC(params[0], params[1]);
//            } catch (Exception e) {
//                Log.e(LOGTAG, e.getMessage());
//                return result;
//            }
            return result;
        }

        // onProgressUpdate方法用于更新进度信息
        @Override
        protected void onProgressUpdate(Integer... progresses) {
            Log.i(LOGTAG, "onProgressUpdate(Progress... progresses) called");
        }

        // onPostExecute方法用于在执行完后台任务后更新UI,显示结果
        @Override
        protected void onPostExecute(ArrayList<String> result) {
            Log.i(LOGTAG, "onPostExecute(Result result) called");
//            if (result != null) {
//                showLrcOnline(result);
//            }
        }

        // onCancelled方法用于在取消执行中的任务时更改UI
        @Override
        protected void onCancelled() {
            Log.i(LOGTAG, "onCancelled() called");
            mLrc.clear();
        }
    }

    Bitmap BlurImage(Bitmap input, int radius) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return input;
        }
        RenderScript rsScript = RenderScript.create(this);
        Allocation alloc = Allocation.createFromBitmap(rsScript, input);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript,
                alloc.getElement());
        blur.setRadius(radius);
        blur.setInput(alloc);

        Bitmap result = Bitmap.createBitmap(input.getWidth(),
                input.getHeight(), input.getConfig());
        Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);
        blur.forEach(outAlloc);
        outAlloc.copyTo(result);

        rsScript.destroy();
        return result;
    }

    public static Bitmap ReflectedImage(Bitmap originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        Matrix matrix = new Matrix();
        // 实现图片翻转90度
        matrix.preScale(1, -1);
        // 创建倒影图片（是原始图片的一半大小）
        Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0,
                height / 2, width, height / 2, matrix, false);
        // 创建总图片（原图片 + 倒影图片）
        Bitmap finalReflection = Bitmap.createBitmap(width,
                (height + height / 2), Bitmap.Config.ARGB_8888);
        // 创建画布
        Canvas canvas = new Canvas(finalReflection);
        canvas.drawBitmap(originalImage, 0, 0, null);
        // 把倒影图片画到画布上
        canvas.drawBitmap(reflectionImage, 0, height + 1, null);
        Paint shaderPaint = new Paint();
        // 创建线性渐变LinearGradient对象
        LinearGradient shader = new LinearGradient(0,
                originalImage.getHeight(), 0, finalReflection.getHeight() + 1,
                0x70ffffff, 0x00ffffff, Shader.TileMode.MIRROR);
        shaderPaint.setShader(shader);
        shaderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        // 画布画出反转图片大小区域，然后把渐变效果加到其中，就出现了图片的倒影效果。
        canvas.drawRect(0, height + 1, width, finalReflection.getHeight(),
                shaderPaint);
        return finalReflection;
    }
}