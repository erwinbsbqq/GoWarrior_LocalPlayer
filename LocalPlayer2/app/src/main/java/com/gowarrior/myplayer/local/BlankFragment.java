package com.gowarrior.myplayer.local;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gowarrior.myplayer.R;



/**
 * A simple {@link Fragment} subclass.
 */
public class BlankFragment extends Fragment {


    private FileHelper mFileHelper;

    public BlankFragment() {

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFileHelper = FileHelper.getInstance(getActivity());

        View rootView;

        rootView=inflater.inflate(R.layout.fragment_blank, container, false);
        //Todo

        return rootView;
    }


















}
