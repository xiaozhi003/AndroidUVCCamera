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
import android.view.Surface;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import com.xiaozhi003.uvc.common.BaseActivity;
import com.eyecool.uvc.demo.R;
import com.xiaozhi003.uvc.demo.dialog.CameraDialog;
import com.xiaozhi003.uvc.usb.IFrameCallback;
import com.xiaozhi003.uvc.usb.USBMonitor;
import com.xiaozhi003.uvc.usb.USBMonitor.OnDeviceConnectListener;
import com.xiaozhi003.uvc.usb.USBMonitor.UsbControlBlock;
import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvc.usb.UVCCameraManager;
import com.xiaozhi003.uvc.util.CameraControl;
import com.xiaozhi003.uvc.widget.SimpleUVCCameraTextureView;

public final class TestActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final String TAG = TestActivity.class.getSimpleName();

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCameraManager mUVCCameraManager;
    private SimpleUVCCameraTextureView mUVCCameraView;
    private Surface mPreviewSurface;

    private int mPid = 0x2209;

    int prevreWidth = 640;
    int previewHeight = 480;
    private TextView mHintTv;

    private CameraControl mCameraControl;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        findViewById(R.id.openBtn).setOnClickListener(v -> openCamera());
        findViewById(R.id.startPreviewBtn).setOnClickListener(view -> startPreview());
        findViewById(R.id.stopPreviewBtn).setOnClickListener(view -> stopPreview());
        findViewById(R.id.closeBtn).setOnClickListener(v -> closeCamera());
        findViewById(R.id.upBtn).setOnClickListener(v -> forward(30));
        findViewById(R.id.downBtn).setOnClickListener(v -> reverse(30));
        findViewById(R.id.pressBtn).setOnClickListener(v -> pressTest());
        mHintTv = findViewById(R.id.hintTv);

        mUVCCameraView = (SimpleUVCCameraTextureView) findViewById(R.id.uvcCameraView);
        mUVCCameraView.setAspectRatio(prevreWidth / (float) previewHeight);

        mUVCCameraManager = new UVCCameraManager(this);
        mUVCCameraManager.setDisplayView(mUVCCameraView);
        mUVCCameraManager.setUVCCameraCallback(new UVCCameraManager.UVCCameraCallbackX() {

            @Override
            public void afterOpen(UVCCamera uvcCamera) {
                uvcCamera.openImage();
            }

            @Override
            public void beforePreview(UVCCamera uvcCamera) {
                uvcCamera.openImage();
            }

            @Override
            public void onOpened() {
            }

            @Override
            public void onPreview() {

            }

            @Override
            public void onOpenError(int error) {

            }
        });

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    boolean isRolling = false;

    public void forward(int num) {
        Log.i(TAG, "forward...");
        if (mCameraControl != null && !isRolling) {
            isRolling = true;
            new Thread(() -> mCameraControl.MotorForwardFwdRun(num, () -> {
                sleep(num * 3000 / 172);
                isRolling = false;
            })).start();
        }
    }

    public void reverse(int num) {
        Log.i(TAG, "reverse...");
        if (mCameraControl != null && !isRolling) {
            isRolling = true;
            new Thread(() -> mCameraControl.motorReverseFwsRun(num, () -> {
                sleep(num * 3000 / 172);
                isRolling = false;
            })).start();
        }
    }

    private void pressTest() {
        Log.i(TAG, "pressTest...");
        if (mUVCCameraManager.getUVCCamera() != null) {
            int press = mUVCCameraManager.getUVCCamera().getPress();
            Log.i(TAG, "press: " + press);
            mHintTv.setText("press: " + press);
        }
    }

    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        mUVCCameraManager.startPreview();
    }

    private void stopPreview() {
        mUVCCameraView.onPause();
    }

    private void openCamera() {
        List<UsbDevice> devices = mUSBMonitor.getDeviceList();
        if (devices == null)
            return;
        for (UsbDevice device : devices) {
            if (device.getProductId() == mPid || device.getProductId() == 0x107) {
                Log.i(TAG, "请求权限打开...");
                mUSBMonitor.requestPermission(device);
            }
        }
    }

    private void closeCamera() {
        mUVCCameraManager.closeCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        mUVCCameraManager.startPreview();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop...");
        mUVCCameraManager.stopPreview();

        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
        mUVCCameraManager.closeCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy...");
        mUVCCameraManager.closeCamera();
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        super.onDestroy();
    }

    private Toast mToast;

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(TestActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            openCameraDevice(device, ctrlBlock);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            Log.w(TAG, "onDisconnect, pid = " + device.getProductId());
            // XXX you should check whether the coming device equal to camera device that currently using
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Log.w(TAG, "onDettach...");
            Toast.makeText(TestActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            if (device.getProductId() == mPid || device.getProductId() == 0x107) {
                mUVCCameraManager.closeCamera();
            }
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    private synchronized void openCameraDevice(UsbDevice device, final UsbControlBlock ctrlBlock) {
        mUVCCameraManager.closeCamera();
        if (!(device.getProductId() == mPid || device.getProductId() == 0x107)) {
            return;
        }
        queueEvent(() -> {
            mUVCCameraManager.openCamera(ctrlBlock);
        }, 0);
    }

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

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    // final Bitmap bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private final IFrameCallback mIFrameCallback = frame -> {
        byte[] nv21 = null;
        if (frame.limit() > 0) {
            nv21 = new byte[frame.limit()];
            frame.get(nv21);
        }

        if (nv21 != null) {
        }
    };

}
