package com.gowarrior.myplayer.local;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gowarrior.myplayer.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LocalImagePlayer extends Activity {

    public final static String LOGTAG = "LocalImagePlayer";

    private final int THRESHOLD_SHOW_TIPS = 5000;
    private final int THRESHOLD_SHOW_LOADING = 1000;


    private final int SKIP_MODE_PREVIOUS = -1;
    private final int SKIP_MODE_CURRENT = 0;
    private final int SKIP_MODE_NEXT = 1;

    private String mPath;
    private ArrayList<String> mList;
    private int mPlayIndex;



    private int mAnimId = R.id.local_image_view;

    private ImageView mView;
    private int[] mDegrees;
    private ProgressBar mLoading;
    private boolean mIsFadingOut = false;
    private boolean mShowLoading = false;
    private BitmapWorkerTask mDecodeTask;
    private Handler mHandler;

    // private Pair<Bitmap, Point> mDecodeResult;
    // private Bitmap mDecodeResult;

    private LocalImageSettings mOptionSetting;
    private long mTimeout;
    private static final int MSG_HIDE_SELF = 1;
    private static final int OPTION_TIMEOUT = 3000;

    private Time mTime = new Time();
    private Random mRandom = new Random();
    private String mCurrentUrl = null;

    private BroadcastReceiver mPlugReceiver;
    private ExecutorService mExecutorService = null;

    private ImageCache mImageCache;
    private Lock mAsyncTaskLock;
    private boolean inDecoding = false;

    private FileHelper mFileHelper;

    private View mTipView = null;
    private TextView mTipViewA = null;
    private TextView mTipViewB = null;
    private Handler mTipHandler = new Handler();
    private Runnable mTipRunnable = new Runnable() {

        @Override
        public void run() {
            mTipView.setVisibility(View.INVISIBLE);
            if (mOptionSetting.getItemValue("play_order") == 2
                    || mList.size() < 2) {
                finish();
            } else {
                switchImage(SKIP_MODE_NEXT);
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileHelper = FileHelper.getInstance(this);
        Intent intent = getIntent();

        String action = intent.getAction();
        Set<String> cates = intent.getCategories();
        Uri uri = intent.getData();
        String scheme = intent.getScheme();

        Log.v(LOGTAG, "Action=" + action);
        Log.v(LOGTAG, "Categories=" + cates);
        Log.v(LOGTAG, "Uri=" + uri);
        Log.v(LOGTAG, "Scheme=" + scheme);



        if (uri != null) {
            try {
                String path = uri.getPath();
                File file = new File(path);
                if (mList == null) {
                    mList = new ArrayList<String>();
                } else {
                    mList.clear();
                }
                mList.add(file.getName());
                mPath = path
                        .substring(0, path.length() - mList.get(0).length());
                mPlayIndex = 0;
            } catch (Exception e) {
                finish();
            }
        } else {
            mPath = intent.getStringExtra("path");
            // mList = intent.getStringArrayListExtra("play_list");
            mList = mFileHelper.playlist;
            mPlayIndex = intent.getIntExtra("start_index", 0);

        }
        Log.v(LOGTAG, "onCreate: mPlayIndex=" + mPlayIndex);

        mExecutorService =  Executors.newFixedThreadPool(1);

        mDegrees = new int[mList.size()];

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.local_image_player);
        mView = (ImageView) findViewById(R.id.local_image_view);

        mTipView = findViewById(R.id.state_tip);
        mTipViewA = (TextView) findViewById(R.id.state_tip_text1);
        mTipViewB = (TextView) findViewById(R.id.state_tip_text2);

        mLoading = (ProgressBar) findViewById(R.id.local_image_loading);
        mAsyncTaskLock = new ReentrantLock();
        mImageCache = new ImageCache(1);

        mHandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ImageView tips = (ImageView) findViewById(R.id.local_image_tips);
                tips.setVisibility(View.INVISIBLE);
            }
        };
        mHandler.postDelayed(runnable, THRESHOLD_SHOW_TIPS);
        startDecodeTask(getImage(SKIP_MODE_CURRENT));

        ViewGroup root = (ViewGroup) findViewById(R.id.player_container);
        mOptionSetting = new LocalImageSettings(this, root);
        mOptionSetting.hide();
        mTime.setToNow();
        mRandom.setSeed(mTime.toMillis(true));
        initReceiver();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOGTAG, "onDestroy Called");
        if (mPlugReceiver != null) {
            unregisterReceiver(mPlugReceiver);
        }
    }

    public static class ImageCache extends LruCache<String, Bitmap> {
        public ImageCache(int maxSize) {
            super(maxSize);
        }
    }

    private void initReceiver() {
        if (mPlugReceiver != null) {
            return;
        }
        mPlugReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.v(LOGTAG, "BroadcastReceiver: " + intent.getAction());
                Log.v(LOGTAG, "Path: " + intent.getData().getPath());

                String action = intent.getAction();
                String path = intent.getData().getPath();
                if (action.equals(Intent.ACTION_MEDIA_REMOVED)
                        || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                    String currentPath = mPath;
                    if (currentPath.startsWith(path)) {
                        finish();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED); // system bug
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.setPriority(1);
        filter.addDataScheme("file");
        if (mPlugReceiver != null) {
            registerReceiver(mPlugReceiver, filter);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mOptHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_SELF:
                    Log.i(LOGTAG, "timeout, hide option settings");
                    mOptionSetting.hide();
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN
                && true == isOptionSettingShowing()) {
            Log.i(LOGTAG, "receive onkey down, reset time out");
            resetTimeOut();
        }

        return super.dispatchKeyEvent(event);
    }

    private void showOptionSettings(long timeout) {
        Log.i(LOGTAG, "show option settings");
        mOptionSetting.show();
        mOptionSetting.requestFocus();
        mTimeout = timeout;
        resetTimeOut();
    }

    private void hideOptionSettings() {
        int msg = MSG_HIDE_SELF;
        mHandler.removeMessages(msg);

        Log.i(LOGTAG, "hide option settings");
        mOptionSetting.hide();
    }

    private void resetTimeOut() {
        int msg = MSG_HIDE_SELF;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessageDelayed(msg, mTimeout);
    }

    private boolean isOptionSettingShowing() {
        return (mOptionSetting.getVisible() == View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        Log.v(LOGTAG, "onKeyDown: keyCode=" + keyCode);

        mTipView.setVisibility(View.INVISIBLE);

        boolean keyHandled = true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (!isOptionSettingShowing())
                    showOptionSettings(OPTION_TIMEOUT);
                break;

            case KeyEvent.KEYCODE_INFO:
                if (isOptionSettingShowing())
                    hideOptionSettings();
                keyHandled = false; // to let super class show playbar
                break;

            case KeyEvent.KEYCODE_BACK:
                if (isOptionSettingShowing()) {
                    hideOptionSettings();
                } else {
                    finish();
                    keyHandled = false;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                rotateAntiClockwise();
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                rotateClockwise();
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!inDecoding ) {
                    Log.d(LOGTAG, "KEYCODE_DPAD_LEFT --->>!!");
                    switchImage(SKIP_MODE_PREVIOUS);
                } else {
                    Log.d(LOGTAG, "KEYCODE_DPAD_LEFT consume --->>!!");
                }
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!inDecoding ) {
                    Log.d(LOGTAG, "KEYCODE_DPAD_RIGHT --->>!!");
                    switchImage(SKIP_MODE_NEXT);
                } else {
                    Log.d(LOGTAG, "KEYCODE_DPAD_RIGHT consume --->>!!");
                }
                break;

            default:
                keyHandled = false;
                break;
        }

        if (!keyHandled)
            keyHandled = super.onKeyDown(keyCode, event);
        return keyHandled;
    }

    private String getImage(int mode) {
        String uri = null;



        if (mode == SKIP_MODE_CURRENT) {
            if ((mPlayIndex >= 0) && (mPlayIndex < mList.size())) {
                uri = mPath + "/" + mList.get(mPlayIndex);
            }
            mCurrentUrl = uri;
            return uri;
        }

        switch (mOptionSetting.getItemValue("play_order")) { // play order
            case 0:
                mPlayIndex = (mode == SKIP_MODE_NEXT ? mPlayIndex + 1
                        : mPlayIndex - 1);
                break;
            case 1:
                mPlayIndex = (mode == SKIP_MODE_NEXT ? mPlayIndex + 1
                        : mPlayIndex - 1);
                if (mPlayIndex >= mList.size()) {
                    mPlayIndex = 0;
                } else if (mPlayIndex < 0) {
                    mPlayIndex = mList.size() - 1;
                }
                break;
            case 2:
                // not change
                break;
            case 3:
                mPlayIndex = mRandom.nextInt(mList.size());
                break;
            default:
                break;
        }
        Log.v(LOGTAG, "mPlayIndex =" + mPlayIndex);

        if ((mPlayIndex >= 0) && (mPlayIndex < mList.size())) {
            uri = mPath + "/" + mList.get(mPlayIndex);
        }
        mCurrentUrl = uri;
        return uri;
    }

    private void startDecodeTask(String uri) {
        Log.d(LOGTAG, "startDecodeTask <<---!!");
        if ((mDecodeTask != null)) { // && (mDecodeTask.getStatus() ==
            // AsyncTask.Status.RUNNING)
            mDecodeTask.cancel(true);
        }
        Log.d(LOGTAG, "new BitmapWorkerTask !!");
        mDecodeTask = new BitmapWorkerTask();
        mDecodeTask.executeOnExecutor(mExecutorService, uri);
        Log.d(LOGTAG, "startDecodeTask --->>!!");
        showLoading(true);
    }

    private void showLoading(boolean flag) {
        Log.d(LOGTAG, "showLoading flag = " + flag);
        if (flag) {
            mShowLoading = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mShowLoading) {
                        mLoading.setVisibility(View.VISIBLE);
                        Log.d(LOGTAG, "showLoading running ");
                    }
                }
            };
            mHandler.postDelayed(runnable, THRESHOLD_SHOW_LOADING);
        } else {
            mShowLoading = false;
            mLoading.setVisibility(View.INVISIBLE);
        }
    }

    private void switchImage(int mode) {
        if (mTipView.getVisibility() == View.VISIBLE) {
            return;
        }
        String uri = getImage(mode);
        if (uri == null) {
            finish();
        } else {
            triggerSwitchAnimation();
            startDecodeTask(uri);
        }
    }

    private void freeImage() {
        mView.setImageBitmap(null);
    }

    private void rotateAntiClockwise() {
        mDegrees[mPlayIndex] -= 90;
        // mView.setRotation(mRotateDegree);
        triggerRotateAnimation();
    }

    private void rotateClockwise() {
        mDegrees[mPlayIndex] += 90;
        // mView.setRotation(mRotateDegree);
        triggerRotateAnimation();
    }

    private void triggerRotateAnimation() {
        // ObjectAnimator oa = ObjectAnimator.ofFloat(mView, "rotation",
        // mRotateDegree);
        ObjectAnimator oa = ObjectAnimator.ofFloat(mView, "rotation",
                mDegrees[mPlayIndex]);
        oa.setDuration(500);
        oa.start();
    }

    private void triggerSwitchAnimation() {
        mAnimId = R.id.local_image_view;
        fadeOut();
    }

    private void fadeIn() {
        Object target = (Object) findViewById(mAnimId);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(target, "alpha", 1f);
        fadeIn.setDuration(1000);
        fadeIn.start();
    }

    private boolean showImage(Bitmap bitmap) {
        if (bitmap == null) {
            mView.setRotation(0);
            mView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mView.setAlpha(1f);
            mView.setImageResource(R.drawable.local_image_broken);
            return false;
        }

        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        if ((bitmap.getWidth() <= screenSize.x)
                && (bitmap.getHeight() <= screenSize.y)) {
            mView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            mView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        try {
            mView.setRotation(mDegrees[mPlayIndex]);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.v(LOGTAG, "ArrayIndexOutOfBoundsException mPlayIndex ="
                    + mPlayIndex);
            mView.setRotation(0);
        }
        mView.setImageBitmap(bitmap);

        return true;
    }

    private void showImageWithAnim(Bitmap bitmap) {
        showLoading(false);
        if (showImage(bitmap)) {
            fadeIn();
        }
    }

    private void fadeOut() {
        Object target = (Object) findViewById(mAnimId);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(target, "alpha", 0f);
        fadeOut.setDuration(500);
        fadeOut.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub
                mIsFadingOut = false;
                // if (mDecodeResult != null) {
                // showImageWithAnim(mDecodeResult);
                // mDecodeResult = null;
                // } else {
                // freeImage();
                // }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // TODO Auto-generated method stub

            }
        });

        mIsFadingOut = true;
        fadeOut.start();
    }

    @SuppressWarnings("null")
    private Pair<byte[], Point> decodeBitmap(String uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri, options);

        // if (options.outWidth > MAX_SUPPORTED_WIDTH || options.outHeight >
        // MAX_SUPPORTED_HEIGHT) {
        // return new Pair<Bitmap, Point>(null, null);
        // }

        Point size = new Point();
        size.x = options.outWidth;
        size.y = options.outHeight;

        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        options.inSampleSize = getInSampleSize(options, screenSize.x,
                screenSize.y);
        // options.inSampleSize = getInSampleSize(options, 720, 405);
        // options.inSampleSize = getInSampleSize(options, 800, 600);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inDither = false;
        options.inPurgeable = true;
        // options.inTempStorage = new byte[16 * 1024];
        FileInputStream is = null;
        Bitmap bmp = null;
        ByteArrayOutputStream baos = null;

        try {
            is = new FileInputStream(uri);
            bmp = BitmapFactory.decodeFileDescriptor(is.getFD(), null, options);
            double scale = getScaling(options.outWidth * options.outHeight,
                    1280 * 720);
            Bitmap bmp2 = Bitmap.createScaledBitmap(bmp,
                    (int) (options.outWidth * scale),
                    (int) (options.outHeight * scale), true);
            bmp.recycle();
            baos = new ByteArrayOutputStream();
            bmp2.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bmp2.recycle();
            return new Pair<byte[], Point>(baos.toByteArray(), size);
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOGTAG, e.getMessage());
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
        } finally {
            try {
                if (is != null)
                    is.close();
                if (baos != null)
                    baos.close();
            } catch (Exception e) {
                Log.e(LOGTAG, e.getMessage());
            }
            System.gc();
        }
        if (baos == null) {
            return new Pair<byte[], Point>(null, size);
        }
        return new Pair<byte[], Point>(baos.toByteArray(), size);
    }

    private static double getScaling(int src, int des) {
        double scale = Math.sqrt((double) des / (double) src);
        return scale;
    }

    private int getInSampleSize(BitmapFactory.Options options, int reqWidth,
                                int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        Log.v(LOGTAG, "getInSampleSize: outWidth=" + width + " outHeight="
                + height);
        // Log.v(LOGTAG, "getInSampleSize: reqWidth=" + reqWidth + " reqHeight="
        // + reqHeight);

        // if ((width <= MAX_UNSCALE_WIDTH) && (height <= MAX_UNSCALE_HEIGHT)) {
        // return 1;
        // }

        int wInSampleSize = 1;
        if (width > reqWidth) {
            wInSampleSize = Math.round((float) width / (float) reqWidth);
        }

        int hInSampleSize = 1;
        if (height > reqHeight) {
            hInSampleSize = Math.round((float) height / (float) reqHeight);
        }

        // Log.v(LOGTAG, "getInSampleSize: wInSampleSize=" + wInSampleSize +
        // " hInSampleSize=" + hInSampleSize);

        // scale to a bigger bitmap
        // return roundToPower2((wInSampleSize < hInSampleSize) ? wInSampleSize
        // : hInSampleSize);

        // scale to a smaller bitmap
        return roundToPower2((wInSampleSize > hInSampleSize) ? wInSampleSize
                : hInSampleSize); //
    }

    private int roundToPower2(int value) {
        int power = 1;
        while (power * 2 <= value) {
            power *= 2;
        }
        return power;
    }

    private Point getImageSize(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        Point size = new Point();
        size.x = options.outWidth;
        size.y = options.outHeight;
        return size;
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private String getNetImage(String path){

            String mImageFile;

            {
                String G_cacheDir = getCacheDir().getAbsolutePath() + "/";
                String fileSavePath = "";
                String fileName = "";

                try {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        fileSavePath = Environment.getExternalStorageDirectory() + "/";
                    }
                    else
                    {
                        fileSavePath =G_cacheDir ;
                    }
                    fileSavePath = G_cacheDir ;

                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(5 * 1000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Charser", "GBK,utf-8;q=0.7,*;q=0.3");
                    int length = conn.getContentLength();
                    InputStream is = conn.getInputStream();
                    Log.d(LOGTAG, "file Length = " + length);
                    File file = new File(fileSavePath);
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    fileName = "1.jpg";

                    File apkFile = new File(fileSavePath, fileName);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    boolean first = true;
                    boolean is_jpeg = false;
                    byte buf[] = new byte[4096];
                    byte eoi[] = new byte[2];
                    do {
                        int numread = is.read(buf);
                        count += numread;
                        if(first)
                        {
                            if((byte)0xFF == buf[0] && (byte)0xD8 == buf[1])
                            {
                                is_jpeg = true;
                            }
                            first = false;
                        }
                        if (numread <= 0) {
                            Log.d(LOGTAG, "####file count = " + count);
                            Log.d(LOGTAG, "####is_jpeg = " + is_jpeg + ",eoi[0] = " + eoi[0] + ",eoi[1] = " + eoi[1]);
                            if(is_jpeg)
                            {
                                if(0xFF != eoi[0] || 0xD9 != eoi[1])//no EOI flag
                                {
                                    eoi[0] = (byte)0xFF;
                                    eoi[1] = (byte)0xD9;
                                    fos.write(eoi, 0, 2);
                                }
                            }
                            break;
                        }
                        fos.write(buf, 0, numread);
                        if(is_jpeg)
                        {
                            if(numread >= 2)
                            {
                                eoi[0] = buf[numread-2];
                                eoi[1] = buf[numread-1];
                            }
                            else
                            {
                                if(numread == 1)
                                {
                                    eoi[0] = eoi[1];
                                    eoi[1] = buf[0];
                                }
                            }
                        }
                    } while (true);
                    fos.close();
                    is.close();
                    buf = null;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mImageFile = fileSavePath + fileName;

            }

            return mImageFile;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            inDecoding = true;
            Bitmap bitmap = null;
            bitmap = mImageCache.get(params[0]);
            if (bitmap == null) {
                Log.v(LOGTAG, "mImageCache not hit!!");
                String tmpUrl = params[0];
                if(tmpUrl.indexOf("http://") == 0)//net src
                {
                    params[0] = getNetImage(params[0]);

                }
                Point size = getImageSize(params[0]);
                if (size.x == 1280 && size.y == 720) {
                    // sdk bug. work around.
                    // Bitmap.compress 1280x720 image will exception
                    bitmap = BitmapFactory.decodeFile(params[0]);
                } else if (size.x <= 0 || size.y <= 0) {
                    return null;
                } else {
                    Pair<byte[], Point> pair = decodeBitmap(params[0]);
                    bitmap = BitmapFactory.decodeByteArray(pair.first, 0,
                            pair.first.length);
                }
                if (bitmap != null) {
                    Log.v(LOGTAG, "mImageCache add new bitmap!!");
                    mImageCache.put(params[0], bitmap);
                }
            } else {
                Log.v(LOGTAG, "mImageCache hit!!");
            }

            if (bitmap == null) {
                inDecoding = false;
                return null;
            }
            inDecoding = false;
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            Log.v(LOGTAG, "onPostExecute <<---!!");
            if (bitmap != null) {
                showImageWithAnim(bitmap);
            } else {
                // show unsupported tip
                Log.v(LOGTAG, "onPostExecute null bitmap!!");
                mTipViewA.setText(mList.get(mPlayIndex));
                // mTipViewB.setText(R.string.msg_media_unsupported);
                mTipView.setVisibility(View.VISIBLE);
                mTipHandler.postDelayed(mTipRunnable, 2000);
            }
        }
    }
}
