package com.gowarrior.myplayer.local;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.common.AbsMediaPlayer;
import com.gowarrior.myplayer.common.MediaPlayerActivity;

import com.gowarrior.myplayer.local.LocalVideoSettings.onItemKeyDownListener;
import com.gowarrior.myplayer.local.LocalVideoSettings.onOptionSelectedListener;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class LocalVideoPlayer extends MediaPlayerActivity implements
        onItemKeyDownListener, onOptionSelectedListener {

    public final static String LOGTAG = "LocalVideoPlayer";

    private String mPath;
    private ArrayList<String> mList;
    private int mPlayIndex;

    private LocalVideoSettings mOptionSetting;
    private long mTimeout;
    private static final int MSG_HIDE_SELF = 1;
    private static final int OPTION_TIMEOUT = 3000;

    private Time mTime = new Time();
    private Random mRandom = new Random();

    private BroadcastReceiver mPlugReceiver;
    private ArrayList<String> mSubPlayList;
    private int mSubPlayIndex = 0;
    private String mCDROM_MountPoint = "/storage/cdrom";
    private String mRecordPath;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPreferencesEditor;
    private ArrayList<History> mHistory;
    private int mHistorySize = 10;

    private FileHelper mFileHelper;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_SELF:
                    Log.i(LOGTAG, "timeout, hide option settings");
                    mOptionSetting.hide();
                    break;

                default:
                    break;
            }
        }
    };

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //TODO
        //Utility.getInstance().init(this);
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
            mPath = intent.getStringExtra("path");
            // mList = intent.getStringArrayListExtra("play_list");
            mList = mFileHelper.playlist;
            mPlayIndex = intent.getIntExtra("start_index", 0);

        }
        Log.v(LOGTAG, "onCreate: mPlayIndex=" + mPlayIndex);

        mHistorySize = getResources().getInteger(R.integer.local_history_size);
        mSharedPreferences = getSharedPreferences("local_history",
                Activity.MODE_PRIVATE);
        loadHistory();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.local_video_player);
        setPlayerType(1);
        super.onCreate(savedInstanceState);

        ViewGroup root = (ViewGroup) findViewById(R.id.player_container);
        mTipView = findViewById(R.id.state_tip);
        mTipViewA = (TextView) findViewById(R.id.state_tip_text1);
        mTipViewB = (TextView) findViewById(R.id.state_tip_text2);
        mOptionSetting = new LocalVideoSettings(this, root);
        // mOptionSetting.setOnItemKeyDownListener(this);
        mOptionSetting.setOnOptionSelectedListener(this);
        mOptionSetting.hide();

        mTime.setToNow();
        mRandom.setSeed(mTime.toMillis(true));
        initReceiver();
    }

    @Override
    public void onDestroy() {
        Log.v(LOGTAG, "onDestroy Called");
        if (mPlugReceiver != null) {
            unregisterReceiver(mPlugReceiver);
        }
        saveHistory();
        super.onDestroy();
    }

    @Override
    public void finish() {
        try {
            Log.v(LOGTAG,"Record:" + mPosition
                    + " " + mRecordPath);
            File file = new File(mRecordPath);
            if (mPosition >= 3000) {
                addHistory(file, mPosition, mSubPlayIndex);
            } else {
                removehistory(file);
            }
        } catch (Exception e) {
            ;
        }
        super.finish();
    }

    @Override
    public void onPrepared(AbsMediaPlayer mp) {
        try {
            int duration = mPlayer.getDuration();
            int toEnd = duration - mPosition;
            if (toEnd < 500) {
                // mPosition = 0;
            }
        } catch (Exception e) {
            ;
        }
        super.onPrepared(mp);
    }

    public void onCompletion(AbsMediaPlayer mp) {
        mPosition = 0;
        File file = new File(mRecordPath);
        removehistory(file);
        super.onCompletion(mp);
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && true == isOptionSettingShowing()) {
            Log.i(LOGTAG, "receive onkey down, reset time out");
            resetTimeOut();
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean keyHandled = true;
        Log.v(LOGTAG, "onKeyDown: keyCode=" + keyCode);
        mTipView.setVisibility(View.INVISIBLE);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                // case KeyEvent.KEYCODE_PAGE_UP:
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
                    keyHandled = false;
                }
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
    protected String onGetSource(SKIPMODE mode) {
        if (mTipView.getVisibility() == View.VISIBLE) {
            return "";
        }
        mTipHandler.removeCallbacks(mTipRunnable);

        String uri = null;

        // sub play list (ignore play_order)
        uri = getSubPlayListSource(mode);
        if (uri != null) {
            return uri;
        }



        if (mode == SKIPMODE.CURRENT) {
            if ((mPlayIndex >= 0) && (mPlayIndex < mList.size())) {
                uri = mPath + "/" + mList.get(mPlayIndex);
            } else {
                return null;
            }
            mRecordPath = uri;
            //uri = mountIso(uri);
            //uri = getBluRayUri(uri);
            History history = getHistory(new File(mRecordPath));
            if (history != null) {
                mPosition = (int) history.position;
                if (history.playListIndex != -1 && mSubPlayList != null) {
                    if (history.playListIndex < mSubPlayList.size()) {
                        uri = mSubPlayList.get(history.playListIndex);
                    }
                }
            }
            return uri;
        }
        int playPrder = mOptionSetting.getItemValue("play_order");
        switch (playPrder) { // play order
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

        mRecordPath = uri;
        //uri = mountIso(uri);
        //uri = getBluRayUri(uri);
        History history = getHistory(new File(mRecordPath));
        if (history != null) {
            mPosition = (int) history.position;
            if (history.playListIndex != -1 && mSubPlayList != null) {
                if (history.playListIndex < mSubPlayList.size()) {
                    uri = mSubPlayList.get(history.playListIndex);
                }
            }
        }
        return uri;
    }




    private String getSubPlayListSource(SKIPMODE mode) {
        if (mSubPlayList == null) {
            return null;
        }

        if (mode == SKIPMODE.NEXT) {
            mSubPlayIndex++;
        } else if (mode == SKIPMODE.PREVIOUS) {
            mSubPlayIndex--;
        } else {
            // not change
        }
        if (mSubPlayList != null && mSubPlayIndex >= 0
                && mSubPlayIndex < mSubPlayList.size()) {
            return mSubPlayList.get(mSubPlayIndex);
        }
        if (mSubPlayList != null) {
            mSubPlayList.clear();
        }
        mSubPlayIndex = 0;
        return null;
    }

    @Override
    public String getSourceName() {
        if (mPlayIndex >= 0 && mPlayIndex < mList.size()) {
            return mList.get(mPlayIndex);
        } else {
            return "";
        }
    }

    private void showOptionSettings(long timeout) {
        Log.i(LOGTAG, "show option settings");
        mOptionSetting.show();
        mOptionSetting.requestFocus();
        mTimeout = timeout;
        resetTimeOut();
    }

    @Override
    public String[] onGetTimedText(String filename) {
        File file = new File(filename);
        String name = file.getName();
        int pos = name.lastIndexOf('.');
        if (pos < 1) {
            return null;
        }
        final String shortName = name.substring(0, pos - 1);
        File dir = file.getParentFile();
        if (dir != null) {
            File[] files = null;
            files = dir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    if (filename.startsWith(shortName)
                            && filename.toLowerCase().endsWith(".srt")) {
                        return true;
                    } else if (filename.startsWith(shortName)
                            && filename.toLowerCase().endsWith(".sub")) {
                        return true;
                    } else if (filename.startsWith(shortName)
                            && filename.toLowerCase().endsWith(".smi")) {
                        return true;
                    } else if (filename.startsWith(shortName)
                            && filename.toLowerCase().endsWith(".ssa")) {
                        return true;
                    } else if (filename.startsWith(shortName)
                            && filename.toLowerCase().endsWith(".ass")) {
                        return true;
                    } else if (filename.startsWith(shortName)
                            && filename.toLowerCase().endsWith(".idx")) {
                        return true;
                    }
                    return false;
                }
            });
            if (files != null) {
                ArrayList<String> nameList = new ArrayList<String>();
                for (int i = 0; i < files.length; i++) {
                    String subidx = null;
                    String idxpath = null;
                    String subpath = null;
                    if (files[i].getAbsolutePath().endsWith(".sub")) {
                        subpath = files[i].getAbsolutePath();
                        Log.e(LOGTAG, "find sub " + subpath);
                        for (int j = 0; j < files.length; j++) {
                            Log.e(LOGTAG,
                                    "finding idx " + files[j].getAbsolutePath());
                            if (files[j].getAbsolutePath().endsWith(".idx")) {
                                idxpath = files[j].getAbsolutePath();
                                Log.e(LOGTAG, "find idx: " + idxpath);
                                break;
                            } else {
                                Log.e(LOGTAG, files[j].getAbsolutePath()
                                        + "not end with idx");
                            }
                        }
                        Log.e(LOGTAG, "subpath == " + subpath);
                        Log.e(LOGTAG, "idxpath == " + idxpath);
                    }

                    if (idxpath != null) {
                        if (subpath != null) {
                            subidx = idxpath + "*" + subpath;
                            nameList.add(subidx);
                            Log.e(LOGTAG, "add idx+sub " + subidx);
                        }
                    } else {
                        Log.e(LOGTAG,
                                "add subtitle " + files[i].getAbsolutePath());
                        if (!files[i].getAbsolutePath().endsWith(".idx"))
                            nameList.add(files[i].getAbsolutePath());
                    }

                }
                return nameList.toArray(new String[nameList.size()]);
            }
        }
        return null;
    }

    public void OnTimedTextReady(int count) {
        Log.e(LOGTAG, "OnTimedTextReady " + count);
        mOptionSetting.setSubtitleNumber(count);
        if (count > 0 && mOptionSetting.getItemIndex("subtitle") == 0) {
            closeSubtitle();
            // selectSubtitle(-1); // close subtitle
        } else {
            selectSubtitle(0);
        }
    }

    public void OnAudioTrackInfoReady(MediaPlayer.TrackInfo[] trackInfo) {
        if (trackInfo == null) {
            mOptionSetting.setAudioTrackNumber(0);
        } else {
            mOptionSetting.setAudioTrackNumber(trackInfo.length);
        }
        return;
    }

    private void hideOptionSettings() {
        int msg = MSG_HIDE_SELF;
        mHandler.removeMessages(msg);

        Log.i(LOGTAG, "hide option settings");
        mOptionSetting.hide();
    }

    private void resetTimeOut() {
        int msg = MSG_HIDE_SELF;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessageDelayed(msg, mTimeout);
    }

    private boolean isOptionSettingShowing() {
        return (mOptionSetting.getVisible() == View.VISIBLE);
    }

    @Override
    public boolean onItemKeyDown(int keyCode, int id) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            Log.i(LOGTAG, "onItemKeyDown, to next value" + ", item id: " + id);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            Log.i(LOGTAG, "onItemKeyDown, to prev value" + ", item id: " + id);
        }
        return true;
    }

    @Override
    public void onSeekComplete(AbsMediaPlayer mp) {
        mController.onSeekComplete();
        hidePauseView();
    }

    @Override
    public void onPlaybackError(int err) {
        mTipViewA.setText(getSourceName());
        // mTipViewB.setText(R.string.msg_media_unsupported);
        mTipView.setVisibility(View.VISIBLE);
        if (!mController.isShowing()) {
            mController.show();
        }
        mTipHandler.postDelayed(mTipRunnable, 2000);
    }

    @Override
    public void onOptionSelected(String key, int index, int value, String name) {
        if (key == "track") {
            selectAudioTrack(value);
            Log.v(LOGTAG, "onOptionChanged subtitle " + (value));
        } else if (key == "subtitle") {
            Log.d("LocalVideoPlayer",
                    "jerry.xiong DEBUG onOptionSelected: value=" + value);
            if (index == 0) {
                closeSubtitle(mOptionSetting.getItemIndex("subtitle"));
            } else
                selectSubtitle(value -1);
            Log.v(LOGTAG, "onOptionChanged track " + value);
        }
    }

    // play history

    class History {
        public History(String name, long position, int playlistIndex) {
            this.name = name;
            this.position = position;
            this.playListIndex = playlistIndex;
        }

        public String name;
        public int playListIndex;
        public long position;
    }

    private void addHistory(File file, long position, int playlistIndex) {
        try {
            int index = playlistIndex;
            if (!file.isDirectory()) {
                if (file.getName().equalsIgnoreCase(".iso")) {
                    // is iso file
                } else {
                    index = -1;
                }
            } else {
                // is playable directory
            }
            History history = new History(file.getName(), position, index);
            String name = file.getName();
            for (int i = 0; i < mHistory.size(); i++) {
                if (name.equals(mHistory.get(i).name)) {
                    mHistory.remove(i);
                    mHistory.add(history);
                    return;
                }
            }

            if (mHistory.size() >= mHistorySize) {
                mHistory.remove(0);
            }
            mHistory.add(history);
        } catch (Exception e) {
            return;
        }
    }

    private void removehistory(File file) {
        try {
            boolean found = false;
            String name = file.getName();
            for (int i = 0; i < mHistory.size(); i++) {
                if (name.equals(mHistory.get(i).name)) {
                    mHistory.remove(i);
                    found = true;
                }
            }
            if (found == true) {
                saveHistory();
            }
        } catch (Exception e) {

        }
        return;
    }

    private History getHistory(File file) {
        try {
            String name = file.getName();
            for (int i = 0; i < mHistory.size(); i++) {
                if (name.equals(mHistory.get(i).name)) {
                    if (file.isDirectory()
                            || (!file.isDirectory() && file.getName()
                            .equalsIgnoreCase(".iso"))) {
                        if (mHistory.get(i).playListIndex == -1) {
                            return null;
                        }
                    }
                    return mHistory.get(i);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private boolean saveHistory() {
        try {
            mSharedPreferencesEditor = mSharedPreferences.edit();
            mSharedPreferencesEditor.putInt("count", mHistory.size());
            for (int i = 0; i < mHistory.size(); i++) {
                History history = mHistory.get(i);
                String item = String.valueOf(history.position) + ":"
                        + history.playListIndex + ":" + history.name;
                mSharedPreferencesEditor.putString(String.valueOf(i), item);
            }
            return mSharedPreferencesEditor.commit();
        } catch (Exception e) {
            return false;
        }
    }

    private void loadHistory() {
        try {
            mHistory = new ArrayList<History>();
            int count = mSharedPreferences.getInt("count", 0);

            for (int i = 0; i < count; i++) {
                String item = mSharedPreferences.getString(String.valueOf(i),
                        "");
                if (item.isEmpty()) {
                    break;
                }
                int pos1 = item.indexOf(':');
                if (pos1 <= 0) {
                    break;
                }
                int pos2 = item.indexOf(':', pos1 + 1);
                if (pos2 <= 0) {
                    break;
                }
                long position = Long.valueOf(item.substring(0, pos1));
                int playlistIndex = Integer.valueOf(item.substring(pos1 + 1,
                        pos2));
                String name = item.substring(pos2 + 1);
                mHistory.add(new History(name, position, playlistIndex));
            }
        } catch (Exception e) {
            return;
        }
    }

}

