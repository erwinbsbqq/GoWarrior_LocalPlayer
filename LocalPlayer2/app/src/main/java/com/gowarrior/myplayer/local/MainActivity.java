package com.gowarrior.myplayer.local;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.common.TabPageIndicator;

import java.util.Locale;



public class MainActivity extends FragmentActivity  implements FragmentListener   {


    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    TabPageIndicator mIndicator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        //mViewPager.requestFocus();


    }


    private void init(){
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
    public void focusTabs() {
        mIndicator.focusOnTabs();

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

            Fragment fragment = new GridFragment();



            return fragment;
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
