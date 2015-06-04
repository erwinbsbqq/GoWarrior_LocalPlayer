package com.gowarrior.myplayer.local;

import android.content.Context;
import android.os.AsyncTask;

import com.gowarrior.myplayer.R;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jerry.xiong on 2015/6/3.
 */
public class FileHelper {

    public final static String LOGTAG = "FileHelper";

    //2.3
    private  static File mRootFile;
    //2.4
    private ExecutorService mExecutorService = null;
    //1.构造
    private static FileHelper mFileHelper = null;
    private Context mContext = null;

    //2.文件与路径加载
    private String[] mImageSuffixes;
    private String[] mAudioSuffixes;
    private String[] mVideoSuffixes;
    private String mFriendlyNameUsb;
    private String mFriendlyNameSd;
    private String mFriendlyNameHd;

    //文件类型
    public enum FILETYPE {
        IMAGE, AUDIO, VIDEO, APK, DIR, UNKNOW;
    }

    //类型排序
    public enum SORTTYPE {
        NAME,TIME;
    }

    public  interface OnDirLoadedListener {
        void onDirLoaded(String Path);
    }

    private OnDirLoadedListener mOnDirLoadedListener = null;

    public void setOnDirLoadedListener(OnDirLoadedListener listener) {
        mOnDirLoadedListener = listener;
    }

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
        mRootFile = new File("/storage");

        //2.4
        mExecutorService =  Executors.newFixedThreadPool(5);

    }


    //3 建立目录和文件加载的异步任务

    class DirLoadingTask extends AsyncTask<Object,Void,Object> {
        public DirLoadingTask() {
        }

        @Override
        protected Object doInBackground(Object... params) {
            return null;
        }
    }



























}
