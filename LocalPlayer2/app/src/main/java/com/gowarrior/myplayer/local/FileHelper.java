package com.gowarrior.myplayer.local;

import android.content.Context;
import android.os.AsyncTask;

import com.gowarrior.myplayer.R;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jerry.xiong on 2015/6/3.
 */
public class FileHelper {

    public final static String LOGTAG = "FileHelper";


    private ExecutorService mExecutorService = null;

    private static FileHelper mFileHelper = null;
    private Context mContext = null;


    private static ReentrantLock mLock = new ReentrantLock();


    private String[] mImageSuffixes;
    private String[] mAudioSuffixes;
    private String[] mVideoSuffixes;
    private String mFriendlyNameUsb;
    private String mFriendlyNameSd;
    private String mFriendlyNameHd;



    private String mCurrentPath;
    private boolean mNeedRefresh = false;

    //文件类型
    public enum FILETYPE {
        IMAGE, AUDIO, VIDEO, APK, DIR, UNKNOW;
    }

    //类型排序
    public enum SORTTYPE {
        NAME,TIME;
    }


    public String getCurrentPath() {
        return mCurrentPath;
    }

    public void setCurrentPath(String path) {
        mCurrentPath = path;
    }

    public void setmNeedRefresh(boolean refresh) {
        mNeedRefresh = refresh;
    }

    //路径加载监听接口
    public  interface OnDirLoadedListener {
        void onDirLoaded(String Path);
    }




    public static FileHelper getInstance(Context context) {
        if (mFileHelper == null && context != null) {
            mFileHelper = new FileHelper(context);
        }
        return mFileHelper;
    }

    private FileHelper(Context context) {
            if (mContext == null && context != null) {
                mContext = context;
            }

        initStoragePath();
    }




    private  void initStoragePath() {

        mImageSuffixes = mContext.getResources().getStringArray(
                R.array.fileEndingImage);
        mAudioSuffixes = mContext.getResources().getStringArray(
                R.array.fileEndingAudio);
        mVideoSuffixes = mContext.getResources().getStringArray(
                R.array.fileEndingVideo);


        mFriendlyNameUsb = mContext.getResources().getString(R.string.friendly_name_usb);
        mFriendlyNameSd  = mContext.getResources().getString(R.string.friendly_name_sd);
        mFriendlyNameHd  = mContext.getResources().getString(R.string.friendly_name_hd);


        mExecutorService =  Executors.newFixedThreadPool(5);

    }




    class DirLoadingTask extends AsyncTask<Object,Void,Object> {
        public DirLoadingTask() {
        }

        @Override
        protected Object doInBackground(Object... params) {

            String path = (String) params[0];

            if (path == null || path.isEmpty()) {
                return  null;
            }


            mLock.lock();

            try {
                //3.2 todo something


            }
            finally {

                mLock.unlock();
            }

            return null;
        }






    }


//6. 硬编码，实现有点不太好

    public static String getRootPath() {
        return "/storage";
    }

    public void init() {
        mCurrentPath = getRootPath();
    }

    public static boolean isRootDir(String path) {
        File file = new File(path);
        return (file.exists() && file.getAbsolutePath().equals("/storage"));
    }
























}
