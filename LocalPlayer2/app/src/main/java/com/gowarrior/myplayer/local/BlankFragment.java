package com.gowarrior.myplayer.local;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class BlankFragment extends Fragment implements OnDirLoadedListener {

    public final static String LOGTAG = "VideoFragment";

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

    public BlankFragment() {

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = getActivity();
        View rootView;

        mFileHelper = FileHelper.getInstance(context);

        mImageWorker = new ImageWorker(context);


        rootView=inflater.inflate(R.layout.fragment_blank, container, false);

        gridView  = (GridView)rootView.findViewById(R.id.local_movie_gridView);
        emptyView = (TextView)rootView.findViewById(R.id.empty_view);
        mCurrentPathView = (TextView)rootView.findViewById(R.id.current_dir);
        mTipView  = (TextView)rootView.findViewById(R.id.tip_menu_key);

        gridView.setEmptyView(emptyView);


        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //TODO

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
                            mTipView.setText("Loading");

                            //when back to /storage/ext, back to /storage direct for pair with enter action
                            //b暂时挪过来的一段代码
                            if (currentFile.getParentFile().getAbsolutePath().equals("/storage/ext")) {
                                mFileHelper.setCurrentPath("/storage/ext");
                                currentFile = new File(mFileHelper.getCurrentPath());
                            }
                            //e暂时挪过来的一段代码

                            mChildName = currentFile.getName();

                            mFileHelper.loadDir(currentFile.getParentFile().getAbsolutePath(), BlankFragment.this);
                        }
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        if (gridView.getSelectedItemPosition() < gridView.getNumColumns()) {
                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                if (!mIsLoading) {
                                    //TODO
                                }
                            }
                            return true;
                        } else {
                            return false;
                        }
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        return false;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            return true;
                        }
                        gridView.onKeyDown(keyCode, event);

                        if (gridView.getSelectedItemPosition() > 0 && gridView.getCount() > 1) {
                            gridView.setSelection(gridView.getSelectedItemPosition() - 1);
                        }
                        return true;

                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            return true;
                        }
                        if ((gridView.getSelectedItemPosition() < (gridView.getCount() - 1)) && (gridView.getCount() > 1)) {
                            gridView.setSelection(gridView.getSelectedItemPosition() + 1);
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
                    //TODO
                    return true;
                }
                return false;
            }

        });

        if(getUserVisibleHint()) {
            String path = mFileHelper.getCurrentPath();
            mIsLoading = true;
            mTipView.setText("Loading");
            mFileHelper.setmNeedRefresh(true);
            if (path == null) {
                mFileHelper.loadRootDir(FileHelper.getRootPath(),BlankFragment.this);
            }
        }

        return rootView;
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

        FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), mFileHelper.getCurrentPath());
        gridView.setAdapter(fileListAdapter);

        if (mChildName != null && !mChildName.isEmpty()) {

            for (int i = 0; i < infoList.size(); i++) {
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
                //加载单个文件的样式布局
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
                    default:
                        break;
                }
            }

            return convertView;
        }
    }























}
