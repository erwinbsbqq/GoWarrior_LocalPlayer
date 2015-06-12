package com.gowarrior.myplayer.common;

import android.view.View;

/**
 * Created by jerry.xiong on 2015/6/12.
 */
public abstract class PlayerWidget {


    public void show() {
        View v = this.getView();
        if (v != null && v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        View v = this.getView();
        if (v != null && View.VISIBLE == v.getVisibility()) {
            v.setVisibility(View.GONE);
        }
    }

    public int getVisible() {
        View v = this.getView();
        return v.getVisibility();
    }

    public void requestFocus() {
        View v = this.getView();
        v.requestFocus();
    }

    public void clearFocus() {
        View v = this.getView();
        v.clearFocus();
    }

    public void appNotify(String type, Object value) { }

    protected abstract View getView();

    public abstract boolean onKeyDown(int keyCode);

    public abstract void focus(boolean f);
}
