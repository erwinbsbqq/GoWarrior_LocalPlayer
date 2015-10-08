package com.gowarrior.myplayer.local;

import android.alisdk.AliSettings;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.gowarrior.myplayer.R;
import com.gowarrior.myplayer.common.MediaPlayerActivity;
import com.gowarrior.myplayer.common.OptionSettingsAdapter;
import com.gowarrior.myplayer.common.PlayerWidget;

import java.util.ArrayList;
import java.util.HashMap;

public class LocalAudioSettings extends PlayerWidget {
    private String LOGTAG = "LocalAudioSettings";
    private ListView mOptionList;
    private ImageView mArrowTipsUp;
    private ImageView mArrowTipsDown;
    private OptionSettingsAdapter mOptionSettingsAdapter;
    private ArrayList<HashMap<String, Object>> mItemList = new ArrayList<HashMap<String, Object>>();

    private SharedPreferences mOptionSharedPreferences;
    private SharedPreferences.Editor mOptionPreferencesEditor;
    private final String ITEM_NAME = "Title";
    private final String ITEM_VALUE = "Value";

    private AliSettings mAliSettings;

    private int mCurrentSelectItem = 0;
    private final int VISIBLE_ITEM_NUM = 1;

    private onItemKeyDownListener mListener = null;
    private onOptionSelectedListener mOnOptionSelectedListener = null;
    private MediaPlayerActivity mActivity;
    private View mView;

    class NmpMenuItem {
        private String mKey;
        private String mName;
        private int mDefaultIndex;
        private int mIndex;
        private SparseArray<String> mOptions;

        NmpMenuItem(String key, String name, int defaultIndex, int index,
                    SparseArray<String> options) {
            mKey = key;
            mName = name;
            mDefaultIndex = defaultIndex;
            mIndex = index;
            mOptions = options;
        }

        public String getKey() {
            return mKey;
        }

        public void setKey(String key) {
            this.mKey = key;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            this.mName = name;
        }

        public int getDefaultIndex() {
            return mDefaultIndex;
        }

        public void setDefaultIndex(int value) {
            this.mDefaultIndex = value;
        }

        public int getIndex() {
            return mIndex;
        }

        public void setIndex(int value) {
            this.mIndex = value;
        }

        public int incIndex() {
            if (mOptions.size() < 1) {
                return -1;
            }
            if (this.mIndex < mOptions.size() - 1) {
                mIndex++;
            } else {
                mIndex = 0;
            }
            return mIndex;
        }

        public int decIndex() {
            if (mOptions.size() < 1) {
                return -1;
            }
            if (this.mIndex > 0) {
                mIndex--;
            } else {
                mIndex = mOptions.size() - 1;
            }
            return mIndex;
        }

        public SparseArray<String> getOptions() {
            return mOptions;
        }

        public void setOptions(SparseArray<String> options) {
            this.mOptions = options;
        }
    };

    private SparseArray<String> mSubtitleOpts = null;
    private SparseArray<String> mTrackOpts = null;
    private SparseArray<String> mPlayOrderOpts = null;
    private SparseArray<String> mChannelOpts = null;
    private SparseArray<String> mAspectRatioOpts = null;
    private SparseArray<String> mBrightnessOpts = null;
    private SparseArray<String> mContrastOpts = null;
    private SparseArray<String> mSaturationOpts = null;

    private NmpMenuItem mSubtitleMI = null;
    private NmpMenuItem mTrackMI = null;
    private NmpMenuItem mPlayOrderMI = null;
    private NmpMenuItem mChannelMI = null;
    private NmpMenuItem mAspectRatioMI = null;
    private NmpMenuItem mBrightnessMI = null;
    private NmpMenuItem mContrastMI = null;
    private NmpMenuItem mSaturationMI = null;

    ArrayList<NmpMenuItem> mMenu = null;

    public interface onItemKeyDownListener {
        public boolean onItemKeyDown(int keyCode, int id);
    }

    public interface onOptionSelectedListener {
        public void onOptionSelected(String key, int index, int value,
                                     String name);
    }

    public LocalAudioSettings(Context context, ViewGroup root) {
        // TODO Auto-generated constructor stub
        mActivity = (MediaPlayerActivity) context;
        View.inflate(context, R.layout.option_settings, root);

        mAliSettings = new AliSettings();

        mView = mActivity.findViewById(R.id.option_settings);
        mArrowTipsUp = (ImageView) mView.findViewById(R.id.option_id_up_tips);
        mArrowTipsDown = (ImageView) mView
                .findViewById(R.id.option_id_down_tips);
        mOptionList = (ListView) mView.findViewById(R.id.option_id_others_list);
        mOptionList.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
                // Log.i(LOGTAG, "onScroll, head:" + firstVisibleItem +
                // ",visible:" + visibleItemCount + ", total: " +
                // totalItemCount);
                if (totalItemCount > VISIBLE_ITEM_NUM) {
                    int lastItem = firstVisibleItem + visibleItemCount - 1;
                    Log.i(LOGTAG, "last item: " + lastItem);
                    if (lastItem == VISIBLE_ITEM_NUM) {
                        mArrowTipsUp.setVisibility(View.INVISIBLE);
                    } else if (lastItem == VISIBLE_ITEM_NUM + 1) {
                        mArrowTipsUp.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        mOptionList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                mCurrentSelectItem = arg2;
                if ((mCurrentSelectItem == mMenu.size() - 1)) {
                    mArrowTipsDown.setVisibility(View.INVISIBLE);
                } else if (mCurrentSelectItem + VISIBLE_ITEM_NUM == mMenu
                        .size() - 1) {
                    mArrowTipsDown.setVisibility(View.VISIBLE);
                }
                // Log.i(LOGTAG, "current select item: " + mCurrentSelectItem +
                // ",total: " + mTotalItems);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }

        });

        mOptionList.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub
                boolean handled = false;
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) || (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT))) {
                    int pos = mOptionList.getSelectedItemPosition();

					/* only dispatch the event if the item was added in parent */
                    if (mListener != null && pos >= VISIBLE_ITEM_NUM) {
                        mListener
                                .onItemKeyDown(keyCode, pos - VISIBLE_ITEM_NUM);
                    } else {
                        changeItemValue(keyCode, pos);
                    }
                    handled = true;
                }

                return handled;
            }
        });

        mOptionSharedPreferences = context.getSharedPreferences("Settings",
                Activity.MODE_PRIVATE);
        mOptionPreferencesEditor = mOptionSharedPreferences.edit();

        initMenu(context);
        mOptionSettingsAdapter = new OptionSettingsAdapter(context);
        mOptionSettingsAdapter.setDataList(mItemList);
        mOptionList.setAdapter(mOptionSettingsAdapter);
    }

    private void initMenu(Context context) {
        Resources resource = ((Context)mActivity).getResources();

        mSubtitleOpts = new SparseArray<String>();
        mTrackOpts = new SparseArray<String>();

        mPlayOrderOpts = new SparseArray<String>();
        mPlayOrderOpts.append(0, resource.getString(R.string.order_sequence));
        mPlayOrderOpts.append(1, resource.getString(R.string.order_all));
        mPlayOrderOpts.append(2, resource.getString(R.string.order_single));
        mPlayOrderOpts.append(3, resource.getString(R.string.order_random));

        mChannelOpts = new SparseArray<String>();
        mChannelOpts.append(0, resource.getString(R.string.channel_stereo));
        mChannelOpts.append(1, resource.getString(R.string.channel_left));
        mChannelOpts.append(2, resource.getString(R.string.channel_right));

        mAspectRatioOpts = new SparseArray<String>();
        // mAspectRatioOpts.append(0, resource.getString(R.string.ratio_auto));
        mAspectRatioOpts.append(1, resource.getString(R.string.ratio_standard));
        mAspectRatioOpts.append(2, resource.getString(R.string.ratio_fullscreen));

        mBrightnessOpts = new SparseArray<String>();
        mBrightnessOpts.append(0, "0");
        mBrightnessOpts.append(10, "10");
        mBrightnessOpts.append(20, "20");
        mBrightnessOpts.append(30, "30");
        mBrightnessOpts.append(40, "40");
        mBrightnessOpts.append(50, "50");
        mBrightnessOpts.append(60, "60");
        mBrightnessOpts.append(70, "70");
        mBrightnessOpts.append(80, "80");
        mBrightnessOpts.append(90, "90");
        mBrightnessOpts.append(100, "100");

        mContrastOpts = new SparseArray<String>();
        mContrastOpts.append(0, "0");
        mContrastOpts.append(10, "10");
        mContrastOpts.append(20, "20");
        mContrastOpts.append(30, "30");
        mContrastOpts.append(40, "40");
        mContrastOpts.append(50, "50");
        mContrastOpts.append(60, "60");
        mContrastOpts.append(70, "70");
        mContrastOpts.append(80, "80");
        mContrastOpts.append(90, "90");
        mContrastOpts.append(100, "100");

        mSaturationOpts = new SparseArray<String>();
        mSaturationOpts.append(0, "0");
        mSaturationOpts.append(10, "10");
        mSaturationOpts.append(20, "20");
        mSaturationOpts.append(30, "30");
        mSaturationOpts.append(40, "40");
        mSaturationOpts.append(50, "50");
        mSaturationOpts.append(60, "60");
        mSaturationOpts.append(70, "70");
        mSaturationOpts.append(80, "80");
        mSaturationOpts.append(90, "90");
        mSaturationOpts.append(100, "100");

        int index;
        int value;
        value = mOptionSharedPreferences.getInt("subtitle", 1);
        index = mOptionSharedPreferences.getInt("subtitle", 1);
        mSubtitleMI = new NmpMenuItem("subtitle",
                resource.getString(R.string.subtitle), 1, index, mSubtitleOpts);

        value = mOptionSharedPreferences.getInt("track", 0);
        // index = mOptionSharedPreferences.getInt("track", 0);
        mTrackMI = new NmpMenuItem("track",
                resource.getString(R.string.track), 0, 0, mTrackOpts);

        value = mOptionSharedPreferences.getInt("play_order", 0);
        index = mPlayOrderOpts.indexOfKey(value);
        if (index < 0) {
            index = 0;
            mOptionPreferencesEditor.putInt("play_order",
                    mPlayOrderOpts.keyAt(index));
            mOptionPreferencesEditor.commit();
        }
        mPlayOrderMI = new NmpMenuItem("play_order",
                resource.getString(R.string.play_order), 0, index,
                mPlayOrderOpts);

        //value = mOptionSharedPreferences.getInt("snd_channel", 0);
        value = 0; //No need to record
        index = mChannelOpts.indexOfKey(value);
        if (index < 0) {
            index = 0;
            mOptionPreferencesEditor.putInt("snd_channel",
                    mChannelOpts.keyAt(index));
            mOptionPreferencesEditor.commit();
        }
        mChannelMI = new NmpMenuItem("snd_channel",
                resource.getString(R.string.audio_channel), 0, index,
                mChannelOpts);

        value = mOptionSharedPreferences.getInt("aspect_ratio", 2);
        index = mAspectRatioOpts.indexOfKey(value);
        if (index < 0) {
            index = 1;
            mOptionPreferencesEditor.putInt("aspect_ratio",
                    mAspectRatioOpts.keyAt(index));
            mOptionPreferencesEditor.commit();
        }
        mAspectRatioMI = new NmpMenuItem("aspect_ratio",
                resource.getString(R.string.aspect_ratio), 0, index,
                mAspectRatioOpts);

        value = mOptionSharedPreferences.getInt("brightness", 50);
        index = mBrightnessOpts.indexOfKey(value);
        if (index < 0) {
            index = 5;
            mOptionPreferencesEditor.putInt("brightness",
                    mBrightnessOpts.keyAt(index));
            mOptionPreferencesEditor.commit();
        }
        mBrightnessMI = new NmpMenuItem("brightness",
                resource.getString(R.string.brightness), 5, index,
                mBrightnessOpts);

        value = mOptionSharedPreferences.getInt("contrast", 50);
        index = mContrastOpts.indexOfKey(value);
        if (index < 0) {
            index = 5;
            mOptionPreferencesEditor.putInt("contrast",
                    mContrastOpts.keyAt(index));
            mOptionPreferencesEditor.commit();
        }
        mContrastMI = new NmpMenuItem("contrast",
                resource.getString(R.string.contrast), 5, index,
                mContrastOpts);

        value = mOptionSharedPreferences.getInt("saturation", 50);
        index = mSaturationOpts.indexOfKey(value);
        if (index < 0) {
            index = 5;
            mOptionPreferencesEditor.putInt("saturation",
                    mSaturationOpts.keyAt(index));
            mOptionPreferencesEditor.commit();
        }
        mSaturationMI = new NmpMenuItem("saturation",
                resource.getString(R.string.saturation), 5, index,
                mSaturationOpts);

        mMenu = new ArrayList<NmpMenuItem>();
        mMenu.add(mPlayOrderMI);
        // mMenu.add(mChannelMI);
        // mMenu.add(mAspectRatioMI);
        // mMenu.add(mBrightnessMI);
        // mMenu.add(mContrastMI);
        // mMenu.add(mSaturationMI);

        updateViewData();
    }

    private void updateViewData() {
        mItemList.clear();
        for (int i = 0; i < mMenu.size(); i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            NmpMenuItem menuItem = mMenu.get(i);
            SparseArray<String> opts = menuItem.getOptions();
            if (opts.size() < 0) {
                break;
            }
            map.put(ITEM_NAME, menuItem.getName());
            int optIndex = menuItem.getIndex();
            String valueName = null;
            if (optIndex < opts.size()) {
                valueName = opts.valueAt(optIndex);
            } else {
                menuItem.setIndex(menuItem.getDefaultIndex()); // amend index
                valueName = opts.valueAt(menuItem.getDefaultIndex());
            }
            map.put(ITEM_VALUE, valueName);
            mItemList.add(map);
        }
    }

    public int getItemIndex(String key) {
        if (key == null || key.isEmpty()) {
            return -1;
        }

        NmpMenuItem menuItem = null;
        String itemKey;
        int itemIndex = -1;
        for (int i = 0; i < mMenu.size(); i++) {
            menuItem = mMenu.get(i);
            itemKey = menuItem.getKey();
            if (key == itemKey) {
                itemIndex = i;
                break;
            }
        }
        if (itemIndex == -1) {
            return -1;
        }

        int optIndex = menuItem.getIndex();
        return optIndex;
    }

    public int getItemValue(String key) {
        if (key == null || key.isEmpty()) {
            return -1;
        }

        NmpMenuItem menuItem = null;
        String itemKey;
        int itemIndex = -1;
        for (int i = 0; i < mMenu.size(); i++) {
            menuItem = mMenu.get(i);
            itemKey = menuItem.getKey();
            if (key == itemKey) {
                itemIndex = i;
                break;
            }
        }
        if (itemIndex == -1) {
            return -1;
        }

        SparseArray<String> opt = menuItem.getOptions();
        int value = -1;
        int optIndex = menuItem.getIndex();
        if (optIndex >= 0 && optIndex < opt.size()) {
            value = opt.keyAt(optIndex);
        }
        return value;
    }

    public void setSubtitle(String[] subtitle) {
    }

    public void setSubtitleNumber(int totalNumber) {
        mSubtitleOpts.clear();
        NmpMenuItem menuItem;
        Resources resource = ((Context)mActivity).getResources();

        int index = -1;
        for (int i = 0; i < mMenu.size(); i++) {
            menuItem = mMenu.get(i);
            String key = menuItem.getKey();
            if (key == "subtitle") {
                index = i;
                break;
            }
        }
        if (totalNumber < 1) {
            if (index != -1) {
                mMenu.remove(index);
                mMenu.trimToSize();
                updateViewData();
                mOptionSettingsAdapter.notifyDataSetChanged();
            }
            return;
        }

        if (index == -1) {
            mMenu.add(0, mSubtitleMI);
        } else {
            if (mSubtitleMI.getIndex() != 0) {
                mSubtitleMI.setIndex(1);
            }
            mSubtitleOpts.clear();
        }
        mSubtitleOpts.append(-1, resource.getString(R.string.close));
        for (int i = 0; i < totalNumber; i++) {
            mSubtitleOpts.append(i,
                    String.valueOf(resource.getString(R.string.subtitle))
                            + String.valueOf(i + 1));
        }
        updateViewData();
        mOptionSettingsAdapter.notifyDataSetChanged();
    }

    public void setAudioTrack(String[] tracks) {
    }

    public void setAudioTrackNumber(int totalNumber) {
        mTrackOpts.clear();
        NmpMenuItem menuItem;
        Resources resource = ((Context)mActivity).getResources();
        int index = -1;
        for (int i = 0; i < mMenu.size(); i++) {
            menuItem = mMenu.get(i);
            String key = menuItem.getKey();
            if (key == "track") {
                index = i;
                break;
            }
        }
        if (totalNumber < 2) {
            if (index != -1) {
                mMenu.remove(index);
                mMenu.trimToSize();
                updateViewData();
                mOptionSettingsAdapter.notifyDataSetChanged();
            }
            return;
        }

        if (index == -1) {
            mMenu.add(0, mTrackMI);
        } else {
            mTrackMI.setIndex(0);
            mTrackOpts.clear();
        }
        for (int i = 0; i < totalNumber; i++) {
            mTrackOpts.append(i, String.valueOf(resource.getString(R.string.track))
                    + String.valueOf(i + 1));
        }
        updateViewData();
        mOptionSettingsAdapter.notifyDataSetChanged();
    }

    protected void onItemSelected(int item, int value) {
        return;
    }

    private void changeItemValue(int key, int index) {
        Log.e(LOGTAG, "onChange index: " + index + ", key: " + key);
        if (index >= mMenu.size()) {
            return;
        }
        NmpMenuItem menuItem = mMenu.get(index);
        SparseArray<String> opts = menuItem.getOptions();
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            menuItem.decIndex();
        } else {
            menuItem.incIndex();
        }
        int optIndex = menuItem.getIndex();
        if (optIndex < 0) {
            return;
        }

        String itemKey = menuItem.getKey();
        int itemValue = opts.keyAt(optIndex);

        // if (itemKey == "subtitle") {
        // } else if (itemKey == "track") {
        // } else if (itemKey == "play_order") {
        // }

        if (itemKey == "snd_channel") {
            mAliSettings.setStereoMode(itemValue);
        } else if (itemKey == "aspect_ratio") {
            mActivity.changeAspectRatio(itemValue);
        } else if (itemKey == "brightness") {
            mAliSettings.setBrightness(AliSettings.DISPLAY_VIDEO, itemValue);
        } else if (itemKey == "contrast") {
            mAliSettings.setContrast(AliSettings.DISPLAY_VIDEO, itemValue);
        } else if (itemKey == "saturation") {
            mAliSettings.setSaturation(AliSettings.DISPLAY_VIDEO, itemValue);
        }

        String itemName = opts.valueAt(optIndex);
        mItemList.get(index).put(ITEM_VALUE, itemName);

        mOptionPreferencesEditor
                .putInt(menuItem.getKey(), opts.keyAt(optIndex));
        mOptionPreferencesEditor.commit();
        // mOptionSettingsAdapter.notifyDataSetChanged();
        changeSetting(index, key);

        if (mOnOptionSelectedListener != null) {
            mOnOptionSelectedListener.onOptionSelected(menuItem.getKey(),
                    optIndex, itemValue, itemName);
        }
    }

    public void changeSetting(int position, int key) {
        if (position >= 0 && position < mItemList.size()) {
            LinearLayout view = (LinearLayout) mOptionList.getChildAt(position
                    - mOptionList.getFirstVisiblePosition());
            TextView titleView = (TextView) view
                    .findViewById(R.id.option_id_others_title);
            TextSwitcher valueView = (TextSwitcher) view
                    .findViewById(R.id.option_id_others_value);
            HashMap<String, Object> map = mItemList.get(position);
            if (map != null) {
                String name = map.get(ITEM_NAME).toString();
                String value = map.get(ITEM_VALUE).toString();
                if (titleView.getText() != name) {
                    titleView.setText((CharSequence) map.get(ITEM_NAME));
                }
                TextView textView = (TextView) valueView.getCurrentView();
                String str = textView.getText().toString();
                if (str != value) {
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                        valueView.setInAnimation(mActivity,
                                android.R.anim.slide_in_left);
                        valueView.setOutAnimation(mActivity,
                                android.R.anim.slide_out_right);
                    } else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        valueView.setInAnimation(mActivity,
                                R.anim.slide_in_right);
                        valueView.setOutAnimation(mActivity,
                                R.anim.slide_out_left);
                    }

                    valueView.setText((CharSequence) map.get(ITEM_VALUE)
                            .toString());
                    Log.i(LOGTAG,
                            "changeSetting[" + position + "] ==> "
                                    + map.get(ITEM_NAME) + ": "
                                    + map.get(ITEM_VALUE));
                }
                // Log.i(LOGTAG, "Item[" + position +"] ==> " +
                // map.get(ITEM_NAME) + ": " + map.get(ITEM_VALUE));
            }
        }
    }

    public void setOnItemKeyDownListener(onItemKeyDownListener listener) {
        mListener = listener;
    }

    public void setOnOptionSelectedListener(onOptionSelectedListener listener) {
        mOnOptionSelectedListener = listener;
    }

    @Override
    protected View getView() {
        // TODO Auto-generated method stub
        return mView;
    }

    @Override
    public boolean onKeyDown(int keyCode) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void focus(boolean f) {
        // TODO Auto-generated method stub

    }

    public void show() {
        mOptionList.setSelection(0);
        super.show();
    }
}
