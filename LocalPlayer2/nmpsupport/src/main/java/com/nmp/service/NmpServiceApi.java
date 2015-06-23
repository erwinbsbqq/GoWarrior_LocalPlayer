package com.nmp.service;

import com.nmp.support.Utility;

import android.os.RemoteException;
import android.util.Log;

public class NmpServiceApi {
    private static final String TAG = "NmpServiceApi";

    // mode = 1, show the last frame while channel changed
    //      = 0, show black screen while channel changed
    public static void setLastScreenMode(int mode) {
        Log.d(TAG, "setLastScreenMode: mode=" + mode);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setLastScreenMode(mode);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }

    public static void setScaleRatio(int ratio, int count_ratio) {
        Log.d(TAG, "setScaleRatio: ratio=" + ratio + "count=" + count_ratio);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setScaleRatio(ratio, count_ratio);
            } catch (Exception e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }

    public static void setStereoMode(int mode) {
        Log.d(TAG, "setStereoMode: mode=" + mode);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setStereoMode(mode);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }

    public static void setBrightness(int type, int brightness) {
        Log.d(TAG, "setBrightness: type=" + type + "brightness=" + brightness);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setBrightness(type, brightness);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }

    public static void setContrast(int type, int constast) {
        Log.d(TAG, "setContrast: type=" + type + "constast=" + constast);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setContrast(type, constast);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }

    public static void setSaturation(int type, int saturation) {
        Log.d(TAG, "setSaturation: type=" + type + "saturation=" + saturation);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setSaturation(type, saturation);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }

    public static void switchResolution() {
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                int resolution  = service.getResolution();
                Log.d(TAG, "current resolution = " + resolution);
                switch(resolution){
                    case 4:   //RESOLUTION_720P50
                        service.setResolution(5);
                        break;
                    case 5:   //RESOLUTION_720P60
                        service.setResolution(11);
                        break;
                    case 11:   //RESOLUTION_1080P50
                        service.setResolution(12);
                        break;
                    case 12:  //RESOLUTION_1080P60
                        service.setResolution(4);
                        break;
                    default:
                        service.setResolution(4);
                        break;
                }
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }
    
    public static boolean checkInternetConnected() {
        boolean connected = true;
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                connected = service.getInternetConnectStatus();
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
        return connected;
    }
    
    public static void setAdbdEnable(boolean flag) {
        Log.d(TAG, "set adbd enable " + flag);
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.setAdbdEnable(flag);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }
    
    public static void runCmd(String cmd) {
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                service.executeCmd(cmd);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
    }
}
