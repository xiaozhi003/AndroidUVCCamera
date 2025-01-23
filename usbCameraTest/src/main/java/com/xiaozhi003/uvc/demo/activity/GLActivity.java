/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.xiaozhi003.uvc.demo.activity;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.xiaozhi003.uvc.common.BaseActivity;
import com.eyecool.uvc.demo.R;
import com.xiaozhi003.uvc.demo.dialog.CameraDialog;
import com.xiaozhi003.uvc.demo.util.MeasureUtils;
import com.xiaozhi003.uvc.glview.UVCGLSurfaceView;
import com.xiaozhi003.uvc.usb.USBMonitor;
import com.xiaozhi003.uvc.usb.USBMonitor.OnDeviceConnectListener;
import com.xiaozhi003.uvc.usb.USBMonitor.UsbControlBlock;
import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvc.usb.UVCCameraManager;

public final class GLActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final String TAG = GLActivity.class.getSimpleName();

    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCameraManager mUVCCameraManager;
    //    private SimpleUVCCameraTextureView mUVCCameraView;
    // for open&start / stop&close camera preview
    private ImageButton mCameraButton;
    private int mPreviewWidth = 640;
    private int mPreviewHeight = 480;
    private UVCGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);
        mCameraButton = findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);

        mUVCCameraManager = new UVCCameraManager(this);
        mUVCCameraManager.setUVCCameraCallback(mUVCCameraCallbackX);
        mUVCCameraManager.setPreviewSize(mPreviewWidth, mPreviewHeight);
        mUVCCameraManager.setDisplayView(mGLSurfaceView);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        layoutView();
    }

    private void layoutView() {
        int[] screenSize = MeasureUtils.getScreenSize(this);

        mGLSurfaceView.post(() -> {
            ViewGroup.LayoutParams lp = mGLSurfaceView.getLayoutParams();
            if (screenSize[0] > screenSize[1]) {
                lp.width = screenSize[1];
                lp.height = screenSize[1];
            } else {
                lp.width = screenSize[0];
                lp.height = screenSize[0];
            }
            mGLSurfaceView.setLayoutParams(lp);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume...");
        mGLSurfaceView.onResume();
        mUVCCameraManager.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause...");
        mGLSurfaceView.onPause();
        mUVCCameraManager.stopPreview();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop...");
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
        mUVCCameraManager.closeCamera();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy...");
        synchronized (mSync) {
            mUVCCameraManager.closeCamera();
            if (mToast != null) {
                mToast.cancel();
                mToast = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        mCameraButton = null;
        super.onDestroy();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if (mUVCCameraManager.getUVCCamera() == null) {
                    CameraDialog.showDialog(GLActivity.this);
                } else {
                    mUVCCameraManager.closeCamera();
                }
            }
        }
    };

    private Toast mToast;

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            mUVCCameraManager.closeCamera();
            queueEvent(() -> {
                synchronized (mSync) {
                    mUVCCameraManager.openCamera(ctrlBlock);
                }
            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(GLActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            if (mUVCCameraManager.getUVCCamera() != null && mUVCCameraManager.getUVCCamera().getUsbControlBlock() != null && device.equals(mUVCCameraManager.getUVCCamera().getUsbControlBlock().getDevice())) {
                mUVCCameraManager.closeCamera();
            }
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // FIXME
                }
            }, 0);
        }
    }

    private UVCCameraManager.UVCCameraCallbackX mUVCCameraCallbackX = new UVCCameraManager.UVCCameraCallbackX() {
        @Override
        public void afterOpen(UVCCamera uvcCamera) {
            Log.i(TAG, "afterOpen...");
            try {
                JSONObject dataJson = new JSONObject(uvcCamera.getDescriptions());
                JSONObject descriptionJson = dataJson.getJSONObject("description");

                int venderId = descriptionJson.getInt("venderId");
                int productId = descriptionJson.getInt("productId");

                runOnUiThread(() -> Toast.makeText(GLActivity.this, "open vid:pid [" + intToHexStr(venderId) + ":" + intToHexStr(productId) + "]", Toast.LENGTH_SHORT).show());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void beforePreview(UVCCamera uvcCamera) {
            uvcCamera.openImage();
        }

        @Override
        public void onOpened() {
            Log.i(TAG, "onOpened...");
        }

        @Override
        public void onPreview() {
            Log.i(TAG, "onPreview...");
        }

        @Override
        public void onOpenError(int error) {
            Log.e(TAG, "onOpenError...");
            Toast.makeText(GLActivity.this, "open error: " + error, Toast.LENGTH_SHORT).show();
        }
    };

    private String intToHexStr(int number) {
        String hex = Integer.toHexString(number);
        if (hex.length() < 4) {
            hex = "0" + hex;
        }

        return hex;
    }
}
