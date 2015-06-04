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

    //2.3  5没有使用去掉
    //private  static File mRootFile;
    //2.4
    private ExecutorService mExecutorService = null;
    //1.构造
    private static FileHelper mFileHelper = null;
    private Context mContext = null;

    //3 使用ReentrantLock的锁定机制
    private static ReentrantLock mLock = new ReentrantLock();

    //2.文件与路径加载
    private String[] mImageSuffixes;
    private String[] mAudioSuffixes;
    private String[] mVideoSuffixes;
    private String mFriendlyNameUsb;
    private String mFriendlyNameSd;
    private String mFriendlyNameHd;


    // 4.当前路径
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

// Getter,Setter
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

    //7. 去掉mOnDirLoadedListener 没有使用到
//    private OnDirLoadedListener mOnDirLoadedListener = null;
//
//    public void setOnDirLoadedListener(OnDirLoadedListener listener) {
//        mOnDirLoadedListener = listener;
//    }

    //1.构造函数 单例
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
        // 2.路径初始化
        initStoragePath();
    }


    //2.路径相关

    private  void initStoragePath() {
        //2.1 添加values/arrays.xml 指定文件类型
        mImageSuffixes = mContext.getResources().getStringArray(
                R.array.fileEndingImage);
        mAudioSuffixes = mContext.getResources().getStringArray(
                R.array.fileEndingAudio);
        mVideoSuffixes = mContext.getResources().getStringArray(
                R.array.fileEndingVideo);

        //2.2 values/strings.xml 添加存储介质名称
        mFriendlyNameUsb = mContext.getResources().getString(R.string.friendly_name_usb);
        mFriendlyNameSd  = mContext.getResources().getString(R.string.friendly_name_sd);
        mFriendlyNameHd  = mContext.getResources().getString(R.string.friendly_name_hd);

        //2.3 JELLY_BEAN 一下系统可能用/mnt，这里只支持4.2以上系统
        //5
        //mRootFile = new File("/storage");

        //2.4
        mExecutorService =  Executors.newFixedThreadPool(5);

    }


    //3 建立目录和文件加载的异步任务

    class DirLoadingTask extends AsyncTask<Object,Void,Object> {
        public DirLoadingTask() {
        }

        @Override
        protected Object doInBackground(Object... params) {

            String path = (String) params[0];

            if (path == null || path.isEmpty()) {
                return  null;
            }

            //3.1
            mLock.lock();

            try {
                //3.2 todo something


            }
            finally {
                //3.1 此finally块为必须，标准用法
                mLock.unlock();
            }

            return null;
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



























}
