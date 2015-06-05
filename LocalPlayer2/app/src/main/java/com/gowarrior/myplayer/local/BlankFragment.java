package com.gowarrior.myplayer.local;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import com.gowarrior.myplayer.R;



/**
 * A simple {@link Fragment} subclass.
 */
public class BlankFragment extends Fragment {

    public final static String LOGTAG = "VideoFragment";

    private GridView gridView;
    private TextView emptyView;
    private TextView mCurrentPathView;
    private TextView mTipView;

    private FileHelper mFileHelper;

    public BlankFragment() {

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = getActivity();

        mFileHelper = FileHelper.getInstance(context);

        View rootView;

        rootView=inflater.inflate(R.layout.fragment_blank, container, false);

        gridView  = (GridView)rootView.findViewById(R.id.local_movie_gridView);
        emptyView = (TextView)rootView.findViewById(R.id.empty_view);
        mCurrentPathView = (TextView)rootView.findViewById(R.id.current_dir);
        mTipView  = (TextView)rootView.findViewById(R.id.tip_menu_key);

        gridView.setEmptyView(emptyView);



        return rootView;
    }


















}
