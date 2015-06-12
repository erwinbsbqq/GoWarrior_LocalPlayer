package com.gowarrior.myplayer.local;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.gowarrior.myplayer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;


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


    private  ArrayList<FileInfo> mDirEntries = new ArrayList<FileInfo>();
    private  ArrayList<FileInfo> mAudioFile  = new ArrayList<FileInfo>();
    private  ArrayList<FileInfo> mImageFile  = new ArrayList<FileInfo>();
    private  ArrayList<FileInfo> mVideoFile  = new ArrayList<FileInfo>();
    private  ArrayList<FileInfo> mApkFile    = new ArrayList<FileInfo>();


    //TODO
    public ArrayList<String> playlist;

    public enum FILETYPE {
        IMAGE, AUDIO, VIDEO, APK, DIR, UNKNOW
    }


//    public enum SORTTYPE {
//        NAME,TIME
//    }


    public String getCurrentPath() {
        return mCurrentPath;
    }

    public void setCurrentPath(String path) {
        mCurrentPath = path;
    }

    public void setmNeedRefresh(boolean refresh) {
        mNeedRefresh = refresh;
    }


    public  interface OnDirLoadedListener {
        void onDirLoaded(String path);
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
            listenerReference = new WeakReference<OnDirLoadedListener>(listener);
        }

        //1.back to here
        @Override
        protected Object doInBackground(Object... params) {


            //final OnDirLoadedListener listener = listenerReference.get();

            String path = (String) params[0];

            if (path == null || path.isEmpty()) {
                Log.d(LOGTAG,"doInBackgroud, path is empty");
                return  null;
            }


            mLock.lock();
            m_task = this;
            try {


                if (!path.equals(mCurrentPath)  || mNeedRefresh ) {

                    boolean isRootDir = isRootDir(path);
                    if (isRootDir) {
                        if (loadRootDirFiles()){
                            Log.d(LOGTAG,"loadRootDirFiles");
                            mCurrentPath = path;
                        }
                    } else {
                        if (loadFiles(path)) {
                            Log.d(LOGTAG, "loadFiles");
                            mCurrentPath = path;
                        }
                    }
                }


            }
            finally {

                mLock.unlock();
            }

            return params[0];
        }



        @Override
        protected void onPostExecute(Object o) {
            if (o == null) {
                return;
            }
            String path = (String) o;
            if (mLoadingPath == null || path.isEmpty()) {
                return;
            }

            if (!isCancelled()) {
                final OnDirLoadedListener listener = listenerReference.get();
                listener.onDirLoaded(path);
                mNeedRefresh = false;
            }
        }

        @Override
        protected void onCancelled(Object o) {
            super.onCancelled(o);
        }
    }

    private boolean loadRootDirFiles() {

        mDirEntries = new ArrayList<FileInfo>();
        mAudioFile = new ArrayList<FileInfo>();
        mImageFile = new ArrayList<FileInfo>();
        mVideoFile = new ArrayList<FileInfo>();
        mApkFile = new ArrayList<FileInfo>();


        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains("secure") || line.contains("asec")) {
                    continue;
                }
                if (line.contains("vfat") || line.contains("ntfs") || line.contains("fuseblk")) {
                    String [] columns = line.split(" ");
                    if (columns.length > 1) {
                        if (columns[1].contains("sd") ||columns[1].contains("usb")) {

                            File file = new File(columns[1]);
                            if (file.isDirectory()) {
                                FileInfo fileInfo = new FileInfo();
                                fileInfo.name = file.getName();
                                if (fileInfo.name.startsWith(".")) {
                                    continue;
                                }
                                fileInfo.path  = file.getAbsolutePath();
                                fileInfo.isDir = file.isDirectory();
                                fileInfo.friendlyName = getStorageFriendlyName(file);
                                mDirEntries.add(fileInfo);
                            }
                        }
                    }
                }

            }

            br.close();

        } catch (Exception e) {
            Log.v(LOGTAG, "Read /proc/mounts fail!");
            e.printStackTrace();
        }

        sortDir();
        return true;

    }


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
            Log.d(LOGTAG,"path is null");
            return;
        }

        File file = new File(path);
        if (! file.isDirectory()) {
            Log.d(LOGTAG,"file is null");
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



    public ArrayList<FileInfo>getDirInfo(String path, FILETYPE filetype) {
        if (path.equals(mCurrentPath)) {
            ArrayList<FileInfo> infoList = new ArrayList<FileInfo>();
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




    public void sortDir() {
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
            Log.d(LOGTAG,"loadFiles path is empty");
            return  false;
        }

        File dir = new File(path);
        if(!dir.isDirectory()) {
            return false;
        }



        mDirEntries = new ArrayList<FileInfo>();
        mAudioFile = new ArrayList<FileInfo>();
        mImageFile = new ArrayList<FileInfo>();
        mVideoFile = new ArrayList<FileInfo>();
        mApkFile = new ArrayList<FileInfo>();

        File [] fileList = dir.listFiles();
        Log.d(LOGTAG,"loadfiles, filelist length is " + fileList.length);

        fillDirInfoList(fileList);
        sortDir();


        return true;
    }



    private void fillDirInfoList(File[] fileList) {
        if (fileList == null) {
            return;
        }
        boolean isRoot = false;
        if(fileList.length > 0) {
            isRoot = isRootDir(fileList[0].getAbsolutePath());
        }

        //换成foreach方式
        for (File file : fileList) {
            if (!file.exists()) {
                break;
            }

            FileInfo fileInfo = new FileInfo();
            fileInfo.name = file.getName();
            if (fileInfo.name.startsWith(".")) {
                continue;
            }
            if (isRoot) {
                fileInfo.friendlyName = getStorageFriendlyName(file);
            } else {
                fileInfo.friendlyName = fileInfo.name;
            }

            fileInfo.path = file.getAbsolutePath();
            fileInfo.isDir = file.isDirectory();

            fileInfo.filetype = getFileType(file);
            switch (fileInfo.filetype) {
                case DIR:
                    mDirEntries.add(fileInfo);
                    continue;
                case APK:
                    mApkFile.add(fileInfo);
                    continue;
                case AUDIO:
                    mAudioFile.add(fileInfo);
                    continue;
                case IMAGE:
                    mImageFile.add(fileInfo);
                    continue;
                case VIDEO:
                    mVideoFile.add(fileInfo);
                    continue;
                default:
                    break;
            }


        }
    }

    //显示时调用
    private String getStorageFriendlyName(File file) {

        if (file != null) {
            String name = file.getName().toLowerCase();
            if (name.contains("usb")) {
                name = mFriendlyNameUsb + name.replace("usb", "");
            }else if (name.contains("sdcard")) {
                name = mFriendlyNameSd + name;
            }else if (name.contains("sd")) {
                name = mFriendlyNameHd + name;
            }
            return  name;
        }else{
            return null;
        }



    }

    // 显示时调用,改成public
    public String getFriendlyPath(String currentPath) {
        String path = null;
        try {
            File file = getCurrentStorageFile();
            if (file != null) {
                String storagePath = file.getAbsolutePath();
                if (currentPath.startsWith(currentPath)) {
                    path = currentPath.substring(storagePath.length());
                    path = getStorageFriendlyName(file) + path;
                } else {
                    path = mCurrentPath;
                }
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
            return "";
        }

        if (path != null) {
            return path;
        } else {
            return "";
        }
    }

    //搬过来
    private File getCurrentStorageFile() {
        try {
            int pos = 0;
            for (int i = 0; i < 2; i++) {
                pos = mCurrentPath.indexOf('/', pos + 1);
            }
            File file;
            if (pos > 0) {
                file = new File(mCurrentPath.substring(0, pos));
            } else {
                file = new File(mCurrentPath);
            }
            return file;
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
            return null;
        }
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

        } else if (matchFileType(name, mAudioSuffixes)) {
            fileType = FILETYPE.AUDIO;
        } else if (matchFileType(name, mImageSuffixes)) {
            fileType = FILETYPE.IMAGE;
        } else if (matchFileType(name, mVideoSuffixes)) {
            fileType = FILETYPE.VIDEO;
        }

        return  fileType;
    }


    private boolean matchFileType(String name, String[] suffixes) {
        boolean match = false;

        if (name == null || name.isEmpty()) {
            return  false;
        }

        String lowerName = name.toLowerCase();

        //换成foreach方式
        for (String element : suffixes) {
            if (lowerName.endsWith(element)) {
                match = true;
                break;
            }
        }

        return match;
    }

















}
