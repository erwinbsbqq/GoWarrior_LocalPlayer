package com.gowarrior.myplayer.local;

import android.content.Context;
import android.os.AsyncTask;

import com.gowarrior.myplayer.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private static String mLoadingPath;

    private boolean mNeedRefresh = false;


    private  DirLoadingTask m_task = null;

    //7 文件信息列表
    private  ArrayList<FileInfo> mDirEntries = new ArrayList<>();
    private  ArrayList<FileInfo> mAudioFile  = new ArrayList<>();
    private  ArrayList<FileInfo> mImageFile  = new ArrayList<>();
    private  ArrayList<FileInfo> mVideoFile  = new ArrayList<>();
    private  ArrayList<FileInfo> mApkFile    = new ArrayList<>();

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
        private final WeakReference<OnDirLoadedListener> listenerReference;
        public DirLoadingTask(OnDirLoadedListener listener) {
            listenerReference = new WeakReference<>(listener);
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


    public void loadDir(String path, OnDirLoadedListener listener) {

        if (path == null){
            return;
        }

        File file = new File(path);
        if (file == null ||! file.isDirectory()) {
            return;
        }




        //load folder asynchronous
        cancelLoading();
        m_task = null;

        loadRootDir(path, listener);

    }

    public void loadRootDir(String path, OnDirLoadedListener listener) {
        mLoadingPath = path;
        DirLoadingTask task = new DirLoadingTask(listener);
        task.executeOnExecutor(mExecutorService, path);

    }

    public void cancelLoading() {
        if (m_task != null) {
            m_task.cancel(true);
        }
    }

    //7 先建立FileInfo类文件

    public ArrayList<FileInfo>getDirInfo(String path, FILETYPE filetype) {
        if (path.equals(mCurrentPath)) {
            ArrayList<FileInfo> infoList = new ArrayList<>();
            infoList.addAll(mDirEntries);

            switch (filetype) {
                case APK:
                    infoList.addAll(mApkFile);
                    break;
                case AUDIO:
                    infoList.addAll(mAudioFile);
                    break;
                case IMAGE:
                    infoList.addAll(mImageFile);
                    break;
                case VIDEO:
                    infoList.addAll(mVideoFile);
                    break;
                default:
                    break;
            }

            return  infoList;
        }
        return  null;
    }




    public void sortDir(SORTTYPE sortType) {
        Collections.sort(mDirEntries, new FileInfoNameComparator());
        Collections.sort(mApkFile, new FileInfoNameComparator());
        Collections.sort(mAudioFile, new FileInfoNameComparator());
        Collections.sort(mImageFile, new FileInfoNameComparator());
        Collections.sort(mVideoFile, new FileInfoNameComparator());
    }

    public class FileInfoNameComparator implements Comparator<FileInfo> {
        @Override
        public int compare(FileInfo file1, FileInfo file2) {
            return file1.name.compareToIgnoreCase((file2.name));
        }
    }





    private  boolean loadFiles(String path) {
        if(path == null ||path.isEmpty()) {
            return  false;
        }

        File dir = new File(path);
        if(!dir.isDirectory()) {
            return false;
        }

        File [] fileList = dir.listFiles();

        fillDirInfoList(fileList);
        sortDir(SORTTYPE.NAME);


        return true;
    }


    //fileInfo 数据的填充
    private void fillDirInfoList(File[] fileList) {
        if (fileList == null) {
            return;
        }
        boolean isRoot = false;
        if(fileList.length > 0) {
            isRoot = isRootDir(fileList[0].getAbsolutePath());
        }

        for (int i = 0; i < fileList.length; i++) {
            File file = fileList[i];
            if (!file.exists()){
                break;
            }

            FileInfo fileInfo = new FileInfo();
            fileInfo.name = file.getName();
            if (fileInfo.name.startsWith(".")){
                continue;;
            }
            if (isRoot){
                fileInfo.friendlyName = getStorageFriendlyName(file);
            } else {
                fileInfo.friendlyName = fileInfo.name;
            }

            fileInfo.path  = file.getAbsolutePath();
            fileInfo.isDir = file.isDirectory();

            fileInfo.filetype = getFileType(fileList[i]);


        }
    }

    //内部方法，用private,显示根目录为“我的U盘-1”之类
    private String getStorageFriendlyName(File file) {
        String name = new String("");
        if (file != null) {
            name = file.getName().toLowerCase();
        }
        if (name.contains("usb")) {
            name = mFriendlyNameUsb + name.replace("usb", "");
        }else if (name.contains("sdcard")) {
            name = mFriendlyNameSd + name;
        }else if (name.contains("sd")) {
            name = mFriendlyNameHd + name;
        }

        return  name;
    }

    private FILETYPE getFileType(File file) {
        FILETYPE fileType = FILETYPE.UNKNOW;

        if (file == null) {
            return  fileType;
        }

        String name = file.getName();

        if (file.isDirectory()){
            fileType = FILETYPE.DIR;
        } else if (name.toLowerCase().endsWith(".apk")) {
            fileType = FILETYPE.APK;
            //mAudioSuffixesinitStoragePath等在获取相关值
        } else if (matchFileType(name, mAudioSuffixes)) {
            fileType = FILETYPE.AUDIO;
        } else if (matchFileType(name, mImageSuffixes)) {
            fileType = FILETYPE.IMAGE;
        } else if (matchFileType(name, mVideoSuffixes)) {
            fileType = FILETYPE.VIDEO;
        }

        return  fileType;
    }

    //在指定的String数组里找到对应的文件后缀名
    private boolean matchFileType(String name, String[] suffixes) {
        boolean match = false;
        if (name == null || name.isEmpty()) {
            return  match;
        }
        //为了好比较，先全部转成小写
        String lowerName = name.toLowerCase();
        for (int i = 0; i < suffixes.length; i++) {
            if (lowerName.endsWith(suffixes[i])) {
                match = true;
                break;
            }
        }

        return match;
    }














}
