package com.gowarrior.myplayer.common;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.gowarrior.myplayer.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jerry.xiong on 2015/6/12.
 */
public class OptionSettingsAdapter extends BaseAdapter {
    private String LOGTAG = "OptionSettingsAdapter";
    private final String ITEM_NAME = "Title";
    private final String ITEM_VALUE = "Value";
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<HashMap<String, Object>> mList = null;

    static class SettingsViewHolder {
        TextView mTitleView;
        TextSwitcher mValueView;
    }

    public OptionSettingsAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    public void setDataList(ArrayList<HashMap<String, Object>> list) {
        mList = list;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mList.size();
    }

    @Override
    public HashMap<String, Object> getItem(int arg0) {
        // TODO Auto-generated method stub
        return mList.get(arg0);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        SettingsViewHolder holder;
        if (convertView == null) {
            holder = new SettingsViewHolder();
            convertView = mInflater
                    .inflate(R.layout.option_settings_item, null);
            holder.mTitleView = (TextView) convertView
                    .findViewById(R.id.option_id_others_title);
            holder.mValueView = (TextSwitcher) convertView
                    .findViewById(R.id.option_id_others_value);
            holder.mValueView.setFactory(new TextSwitcher.ViewFactory() {

                @Override
                public View makeView() {
                    // TODO Auto-generated method stub
                    TextView tv = new TextView(mContext);
                    FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    tv.setLayoutParams(layout);
                    tv.setTextSize(32);
                    tv.setTextColor(Color.WHITE);
                    tv.setGravity(Gravity.CENTER);
                    tv.setSingleLine(true);
                    return tv;
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (SettingsViewHolder) convertView.getTag();
        }

        if (mList != null && mList.size() > 0 && holder != null) {
            int pos = position;
            if (pos >= mList.size()) {
                pos = 0;
            }
            HashMap<String, Object> map = mList.get(pos);
            if (map != null) {
                String name = map.get(ITEM_NAME).toString();
                String value = map.get(ITEM_VALUE).toString();
                if (holder.mTitleView.getText() != name) {
                    holder.mTitleView
                            .setText((CharSequence) map.get(ITEM_NAME));
                }
                TextView textView = (TextView) holder.mValueView
                        .getCurrentView();
                String str = textView.getText().toString();
                if (str != value) {
                    holder.mValueView.setText((CharSequence) map
                            .get(ITEM_VALUE).toString());
                    Log.i(LOGTAG,
                            "Item[" + position + "] ==> " + map.get(ITEM_NAME)
                                    + ": " + map.get(ITEM_VALUE));
                }
                // Log.i(LOGTAG, "Item[" + position +"] ==> " +
                // map.get(ITEM_NAME) + ": " + map.get(ITEM_VALUE));
                // holder.mTitleView.setText((CharSequence) map.get(ITEM_NAME));
                // holder.mValueView.setText((CharSequence)
                // map.get(ITEM_VALUE).toString());
            }
        }
        return convertView;
    }
}
