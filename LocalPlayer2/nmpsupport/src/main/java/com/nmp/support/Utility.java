package com.nmp.support;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;

import com.nmp.service.IService;

public class Utility {
	private static final String TAG = "Utility";
	
    private static Utility mInstance = new Utility();
	private Utility() {
		Log.d(TAG, "Create Singleton Instance");
	}

	protected void finalize( )
	{
		disconnectNmpService();
	}

	private Context mAppContext = null;
	private IService mIService = null;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mIService = null;
			connectNmpService();
			Log.d(TAG, "Service disconnected success");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mIService = IService.Stub.asInterface(service);
			Log.d(TAG, "Service connected success");
		}
	};

	private void connectNmpService() {
		if (mAppContext != null) {
			Intent service = new Intent(IService.class.getName());
			mAppContext.bindService(service, mServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	private void disconnectNmpService() {
		if (mAppContext != null) {
			mAppContext.unbindService(mServiceConnection);
		}
	}

    public static Utility getInstance() {
        return mInstance;
    }
    
    public static String getMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.toString();
        }
        return msg;
    }
    
	public void init(Context context) {
		if (mAppContext == null) {
			mAppContext = context.getApplicationContext();
			connectNmpService();
			initLanguage(ConfigReader.getLanguage());
		}
	}
	
	public Context getAppContext() {
		return mAppContext;
	}
	
	public IService getNmpService() {
		if (mIService == null) {
			Log.e(TAG, "Fix me: Unexpected case, NMPService Handle is null !!!");
		}
		return mIService;
	}
	
    public long getTotalMemorySize() {
        if (mAppContext == null) {
            return 0;
        }
        ActivityManager am = (ActivityManager)mAppContext.getSystemService(Context.ACTIVITY_SERVICE);

        MemoryInfo memoryInfo = new MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.totalMem;
        Log.d(TAG, "memSize: " + memSize);
        if (memSize > 0x20000000) {
            memSize = 1024*1024*1024;   //1GB
        } else {
            memSize = 512*1024*1024;    //512MB
        }
          
        return memSize ;  
    }
    
    public long getTotalFlashSize() {  
        File path = Environment.getDataDirectory();  
        StatFs stat = new StatFs(path.getPath());  
        long blockSize = stat.getBlockSizeLong();
        long blockCount = stat.getBlockCountLong();
        long totalSize = blockCount * blockSize;
        
        try {
            Log.d(TAG, String.format("blockSize=%d, blockCount=%d, totalSize=%d", blockSize, blockCount, totalSize));
        } catch (Exception e) {
            Log.d(TAG, Utility.getMessage(e));
        }
        
        long gb = 1024 * 1024 * 1024;
        if (totalSize > 0xC0000000L) {
            totalSize = 8 * gb; //8GB
        } else if (totalSize > 0x40000000L) {
            totalSize = 4 * gb; //4GB
        } else {
            totalSize = 2 * gb; //2GB
        }
        return totalSize;  
    } 

    @SuppressLint("DefaultLocale")
    public String formatByteSize(long size)
    {
        long kb = 1024;
        long mb = (kb * 1024);
        long gb = (mb * 1024);
        float fsize = (float)size;
        if (size < kb) {
            return String.format("%.2f ", fsize);
        } else if (size < mb) {
            return String.format("%.2f KB", fsize / kb);
        } else if (size < gb) {
            return String.format("%.2f MB", fsize / mb);
        } else {
            return String.format("%.2f GB", fsize / gb);
        }
    }
    
    private void initLanguage(int lang) {
        if (Global.SUPPORT_MULTI_LANGUAGE) {
            if ((lang >= 0) && (lang < ConfigReader.LangLocales.length)) {
                Log.d(TAG, "lang=" + lang);
                Resources resources = mAppContext.getResources();
                Configuration config = resources.getConfiguration();
                DisplayMetrics dm = resources.getDisplayMetrics();
                //config.locale = ConfigReader.LangLocales[lang];
				config.locale = Locale.getDefault();
                resources.updateConfiguration(config, dm);        
            }
        }
    }    
}
