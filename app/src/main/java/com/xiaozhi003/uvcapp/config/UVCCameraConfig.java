package com.xiaozhi003.uvcapp.config;

import android.hardware.usb.UsbDevice;

import java.util.HashMap;
import java.util.Map;

import com.xiaozhi003.uvc.usb.Size;

public class UVCCameraConfig {

    public static Map<UsbDevice, UVCCameraConfig> sUVCCameraConfigMap = new HashMap<>();

    private Size mSize;

    private int mVid;

    private int mPid;

    public Size getSize() {
        return mSize;
    }

    public void setSize(Size size) {
        mSize = size;
    }

    public int getVid() {
        return mVid;
    }

    public void setVid(int vid) {
        mVid = vid;
    }

    public int getPid() {
        return mPid;
    }

    public void setPid(int pid) {
        mPid = pid;
    }
}
