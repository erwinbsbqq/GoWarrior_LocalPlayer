package com.gowarrior.myplayer.local;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.local.FileHelper.OnDirLoadedListener;

import java.io.File;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */

public class GridFragment extends Fragment implements OnDirLoadedListener {

    public final static String LOGTAG = "GridFragment";
    public static final String ARG_SECTION_NUMBER = "section_number";

    private int mSectionNumber;

    private GridView gridView;
    private TextView emptyView;
    private TextView mCurrentPathView;
    private TextView mTipView;

    private FileHelper mFileHelper;
    //test
    private FileHelper.FILETYPE mFileType = FileHelper.FILETYPE.IMAGE;
    private ImageWorker mImageWorker;

    private boolean mIsLoading = false;
    private String mChildName;


    private FragmentListener mFragmentListener;

    private BroadcastReceiver mPlugReceiver;

    public GridFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFragmentListener = (FragmentListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mSectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
        Context context = getActivity();
        View rootView;

        mFileHelper = FileHelper.getInstance(context);

        mImageWorker = new ImageWorker(context);

        switch (mSectionNumber) {
            case 0:
                mFileType = FileHelper.FILETYPE.IMAGE;
                rootView=inflater.inflate(R.layout.fragment_blank, container, false);
                break;
            case 1:
                mFileType = FileHelper.FILETYPE.VIDEO;
                rootView=inflater.inflate(R.layout.fragment_blank, container, false);
                break;
            case 2:
                mFileType = FileHelper.FILETYPE.AUDIO;
                rootView=inflater.inflate(R.layout.fragment_blank, container, false);
                break;
            case 3:
                mFileType = FileHelper.FILETYPE.APK;
                rootView=inflater.inflate(R.layout.fragment_blank, container, false);
                break;
            default:
                return null;
        }




        gridView  = (GridView)rootView.findViewById(R.id.local_filebrowser_gridView);
        emptyView = (TextView)rootView.findViewById(R.id.empty_view);
        mCurrentPathView = (TextView)rootView.findViewById(R.id.current_dir);
        mTipView  = (TextView)rootView.findViewById(R.id.tip_menu_key);

        gridView.setEmptyView(emptyView);

        //debug
        gridView.getCount();


        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Object obj = gridView.getItemAtPosition(position);
                if (obj == null) {
                    Log.d(LOGTAG,"obj is null");
                    return;
                }
                FileInfo fileInfo = (FileInfo) obj;
                if (fileInfo.isDir) {
                    mIsLoading = true;
                    mTipView.setText(R.string.loading_dir);
                    mChildName = "";

                    mFileHelper.loadDir(fileInfo.path, GridFragment.this);
                }



            }
        });

        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                updatePageNumber();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        gridView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                updatePageNumber();

            }
        });

        gridView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                String currentPath = mFileHelper.getCurrentPath();
                if (currentPath == null || currentPath.isEmpty()) {
                    return false;
                }

                    File currentFile = new File(mFileHelper.getCurrentPath());

                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_BACK:
                        if (FileHelper.isRootDir(mFileHelper.getCurrentPath())) {
                            mFileHelper.cancelLoading();
                            mIsLoading = false;
                            return false;
                        }
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            mIsLoading = true;
                            mTipView.setText(R.string.loading_dir);

                            //when back to /storage/ext, back to /storage direct for pair with enter action
                            //b暂时挪过来的一段代码
                            if (currentFile.getParentFile().getAbsolutePath().equals("/storage/ext")) {
                                mFileHelper.setCurrentPath("/storage/ext");
                                currentFile = new File(mFileHelper.getCurrentPath());
                            }
                            //e暂时挪过来的一段代码

                            mChildName = currentFile.getName();

                            mFileHelper.loadDir(currentFile.getParentFile().getAbsolutePath(), GridFragment.this);
                        }
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        if (gridView.getSelectedItemPosition() < gridView.getNumColumns()) {

                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    if (!mIsLoading) {
                                        mFragmentListener.focusTabs();
                                    }
                                }

                            return true;
                        } else {
                            return false;
                        }
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        return false;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            return true;
                        }
                        gridView.onKeyDown(keyCode, event);

                        if (gridView.getSelectedItemPosition() > 0 && gridView.getCount() > 1) {
                            Log.d(LOGTAG,"gridView.setselection before left possion is" + gridView.getSelectedItemPosition()) ;
                            gridView.setSelection(gridView.getSelectedItemPosition() - 1);
                            Log.d(LOGTAG,"gridView.setselection left possion is" + (gridView.getSelectedItemPosition() -1)) ;
                        }
                        return true;

                    case KeyEvent.KEYCODE_DPAD_RIGHT:

                        //换成KeyEvent.ACTION_UP会跳格
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            return true;
                        }
                        gridView.onKeyDown(keyCode,event);
                        if ((gridView.getSelectedItemPosition() < (gridView.getCount() - 1)) && (gridView.getCount() > 1)) {
                            gridView.setSelection(gridView.getSelectedItemPosition() + 1);
                            Log.d(LOGTAG, "gridView.setselection possion right is" + (gridView.getSelectedItemPosition()+1) ) ;
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        emptyView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                String currentPath = mFileHelper.getCurrentPath();
                if (currentPath == null || currentPath.isEmpty()) {
                    return false;
                }
                File currentFile = new File(mFileHelper.getCurrentPath());
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (FileHelper.isRootDir(currentFile.getAbsolutePath())) {

                            mFragmentListener.focusTabs();
                            return true;
                        }
                    }
                    mIsLoading = true;
                    mTipView.setText(R.string.loading_dir);

                    //when back to /storage/ext, back to /storage direct for pair with enter action
                    if (currentFile.getParentFile().getAbsolutePath().equals("/storage/ext")) {
                        mFileHelper.setCurrentPath("/storage/ext");
                        currentFile = new File(mFileHelper.getCurrentPath());
                    }

                    mChildName = currentFile.getName();
                    mFileHelper.loadDir(currentFile.getParentFile().getAbsolutePath(), GridFragment.this);
                    gridView.requestFocus();
                return  true;
                }else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        mFragmentListener.focusTabs();
                    }
                    return  true;
                }else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return true;
            }

                return false;
            }

        });

        if(getUserVisibleHint()) {
            String path = mFileHelper.getCurrentPath();
            mIsLoading = true;
            mTipView.setText(R.string.loading_dir);
            mFileHelper.setmNeedRefresh(true);
            if (path == null) {
                mFileHelper.loadRootDir(FileHelper.getRootPath(),GridFragment.this);
            } else {
                mFileHelper.loadDir(path, GridFragment.this);

            }
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        if (mPlugReceiver != null) {
            getActivity().unregisterReceiver(mPlugReceiver);
            mPlugReceiver = null;
        }
        Log.d(LOGTAG, "destroy gridfragment");
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser){
            if (mFileHelper != null) {
                mIsLoading = true;
                mTipView.setText(R.string.loading_dir);
                mFileHelper.loadDir(mFileHelper.getCurrentPath(),this);
            }
            initReceiver();
        } else {
            if (mPlugReceiver != null) {
                getActivity().unregisterReceiver(mPlugReceiver);
                mPlugReceiver = null;
            }


        }
    }

    //U盘识别
    private void initReceiver() {
        if (mPlugReceiver != null) {
            return;
        }

        mPlugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                String path = intent.getData().getPath();
                File currentFile = new File(mFileHelper.getCurrentPath());

                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {

                    if (FileHelper.isRootDir(currentFile.getAbsolutePath())) {
                        mIsLoading = true;
                        mTipView.setText(R.string.loading_dir);
                        mFileHelper.setmNeedRefresh(true);
                        mFileHelper.loadRootDir(FileHelper.getRootPath(),GridFragment.this);
                    }
                } else if (action.equals(Intent.ACTION_MEDIA_REMOVED) ||action.equals(intent.ACTION_MEDIA_BAD_REMOVAL)) {
                    String currentPath = currentFile.getAbsolutePath();
                    if (currentPath.startsWith(path) || FileHelper.isRootDir(currentPath)) {
                        mIsLoading = true;
                        mTipView.setText(R.string.loading_dir);
                        mFileHelper.loadRootDir(FileHelper.getRootPath(), GridFragment.this);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.setPriority(1);
        filter.addDataScheme("file");

        if (mPlugReceiver != null) {
            getActivity().registerReceiver(mPlugReceiver, filter);
        }

    }

    private void updatePageNumber() {
        //TODO

    }

    @Override
    public void onDirLoaded(String path) {
        mIsLoading = false;
        mTipView.setText(R.string.menu_key_to_tab);

        if (FileHelper.isRootDir(mFileHelper.getCurrentPath())) {
            mCurrentPathView.setText("");
            emptyView.setText(R.string.connect_tip);
        }else {
            mCurrentPathView.setText(mFileHelper.getFriendlyPath(path));
            emptyView.setText(R.string.no_content);
        }

        ArrayList<FileInfo> infoList = mFileHelper.getDirInfo(path,mFileType);

        if (infoList == null){
            return;
        }

        FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), mFileHelper.getCurrentPath());
        gridView.setAdapter(fileListAdapter);

        if (mChildName != null && !mChildName.isEmpty()) {

            for (int i = 0; i < infoList.size(); i++) {
                Log.d(LOGTAG,"infoList size is "+ infoList.size());
                FileInfo fileInfo = infoList.get(i);
                if (fileInfo.isDir) {
                    if (mChildName.equals(fileInfo.name)) {
                        gridView.setSelection(i);
                        break;
                    }
                } else {
                    break;
                }
            }

        }




    }


    class FileListAdapter extends BaseAdapter {

        private Context mContext;
        ArrayList<FileInfo> fileInfos;
        private LayoutInflater mInflater = null;

        public FileListAdapter(Context context, String path) {
            super();
            this.mContext = context;
            fileInfos = mFileHelper.getDirInfo(path, mFileType);
        }

        @Override
        public int getCount() {
            if (fileInfos == null) {
                return 0;
            }
            Log.d(LOGTAG,"the count is " + fileInfos.size());
            return fileInfos.size();
        }

        @Override
        public Object getItem(int position) {
            if (fileInfos == null) {
                return null;
            }
            return  fileInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            mInflater = LayoutInflater.from(mContext);
            if (convertView == null) {

                convertView = mInflater.inflate(R.layout.local_grid_item,null);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView1);
            TextView  textView  = (TextView)  convertView.findViewById(R.id.item_label);
            FileInfo fileInfo = fileInfos.get(position);
            textView.setText(fileInfo.friendlyName);
            if (fileInfo.isDir) {

                    imageView.setImageResource(R.drawable.local_folder);

            } else {
                switch (mFileType) {
                    case IMAGE:
                        mImageWorker.setLoadingImage(R.drawable.local_image);
                        mImageWorker.loadImage(fileInfo.path,
                                ImageWorker.FILETYPE.IMAGE, imageView);
                        break;
                    case VIDEO:
                        mImageWorker.setLoadingImage(R.drawable.local_video);
                        mImageWorker.loadImage(fileInfo.path,
                                ImageWorker.FILETYPE.VIDEO, imageView);
                        break;
                    //加上音视频文件的图标展示
                    case AUDIO:
                        imageView.setImageResource(R.drawable.local_audio);
                        break;
                    case APK:
                        imageView.setImageResource(R.drawable.local_apk);
                        mImageWorker.loadImage(fileInfo.path,
                                ImageWorker.FILETYPE.APK, imageView);
                        break;
                    default:
                        break;
                }
            }

            return convertView;
        }



    }























}
