package com.gowarrior.myplayer.local;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.common.TabPageIndicator;

import java.util.Locale;



public class MainActivity extends FragmentActivity  implements FragmentListener   {


    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    TabPageIndicator mIndicator;
    

    //private static FileHelper.SORTTYPE mSorttype = FileHelper.SORTTYPE.NAME;

    private FileHelper mFileHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        //
        mViewPager.requestFocus();

        System.gc();


    }


    private void init(){

        mFileHelper =  FileHelper.getInstance(this);
        mFileHelper.init();

        setContentView(R.layout.local_file_browser); //1.重构，从onCreate中挪过来


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);




        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mViewPager);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void focusTabs() {
        mIndicator.focusOnTabs();

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean keyHandled = true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                focusTabs();
                break;

            default:
                keyHandled = false;
                break;
        }
        if (!keyHandled)
            keyHandled = super.onKeyDown(keyCode, event);

        return keyHandled;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm){
            super(fm);
        }

        public Object instantiateItem(ViewGroup container, int position) {
            //Log.v(LOGTAG, String.valueOf(container.getId()));
            Fragment fragment = (Fragment) super.instantiateItem(container,
                    position);
            return fragment;
        }

        @Override
        public Fragment getItem(int position) {

            if (position == 0 ||position == 1||position ==2||position ==3) {
                Fragment fragment = new GridFragment();
                Bundle args = new Bundle();
                args.putInt(GridFragment.ARG_SECTION_NUMBER, position);

                //
                fragment.setArguments(args);


                return fragment;
            }
            return  null;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section4).toUpperCase(l);
            }
            return super.getPageTitle(position);
        }
    }



























}
