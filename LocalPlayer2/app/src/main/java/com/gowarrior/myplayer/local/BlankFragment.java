package com.gowarrior.myplayer.local;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.gowarrior.myplayer.R;


//2
import com.gowarrior.myplayer.local.FileHelper.OnDirLoadedListener;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 */
                                            //3
public class BlankFragment extends Fragment implements OnDirLoadedListener {

    public final static String LOGTAG = "VideoFragment";

    private GridView gridView;
    private TextView emptyView;
    private TextView mCurrentPathView;
    private TextView mTipView;

    private FileHelper mFileHelper;
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
        //import ImageWroker类，显示文件和文件夹
        mImageWorker = new ImageWorker(context);


        rootView=inflater.inflate(R.layout.fragment_blank, container, false);

        gridView  = (GridView)rootView.findViewById(R.id.local_movie_gridView);
        emptyView = (TextView)rootView.findViewById(R.id.empty_view);
        mCurrentPathView = (TextView)rootView.findViewById(R.id.current_dir);
        mTipView  = (TextView)rootView.findViewById(R.id.tip_menu_key);

        gridView.setEmptyView(emptyView);

        //点击时
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //TODO

            }
        });
        //选中时  ,OnItemSelectedListener api 19和22有区别
        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //当前目录下文件数
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
                if (currentPath == null|| currentPath.isEmpty()) {
                    return  false;
                }

                File currentFile = new File(mFileHelper.getCurrentPath());
                switch (event.getKeyCode()){
                    case KeyEvent.KEYCODE_BACK:
                        if (FileHelper.isRootDir(mFileHelper.getCurrentPath())) {
                            mFileHelper.cancelLoading();
                            mIsLoading = false;
                            return false;
                        }
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            mIsLoading =true;
                            mTipView.setText("Loading");

                            //when back to /storage/ext, back to /storage direct for pair with enter action
                            //b暂时挪过来的一段代码
                            if (currentFile.getParentFile().getAbsolutePath().equals("/storage/ext")) {
                                mFileHelper.setCurrentPath("/storage/ext");
                                currentFile = new File(mFileHelper.getCurrentPath());
                            }
                            //e暂时挪过来的一段代码

                            mChildName = currentFile.getName();
                            //1.直接输入BlankFragment.this报错，要引入接口OnDirLoadedListener
                            mFileHelper.loadDir(currentFile.getParentFile().getAbsolutePath(),BlankFragment.this);

                        }
                }

                return false;
            }
        });






        return rootView;
    }

    private void updatePageNumber() {
        //TODO

    }

    @Override
    public void onDirLoaded(String Path) {
        //TODO
    }
}
