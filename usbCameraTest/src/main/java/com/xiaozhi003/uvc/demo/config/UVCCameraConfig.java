package com.xiaozhi003.uvc.demo.config;

import android.hardware.usb.UsbDevice;

import java.util.HashMap;
import java.util.Map;

import com.xiaozhi003.uvc.usb.Size;

public class UVCCameraConfig {

    public static Map<UsbDevice, UVCCameraConfig> sUVCCameraConfigMap = new HashMap<>();

    private Size mSize;

    public Size getSize() {
        return mSize;
    }

    public void setSize(Size size) {
        mSize = size;
    }
}
