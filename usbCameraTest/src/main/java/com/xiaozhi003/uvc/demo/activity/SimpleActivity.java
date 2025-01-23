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

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

import com.xiaozhi003.uvc.common.BaseActivity;
import com.eyecool.uvc.demo.R;
import com.xiaozhi003.uvc.demo.dialog.CameraDialog;
import com.xiaozhi003.uvc.usb.IFrameCallback;
import com.xiaozhi003.uvc.usb.USBMonitor;
import com.xiaozhi003.uvc.usb.USBMonitor.OnDeviceConnectListener;
import com.xiaozhi003.uvc.usb.USBMonitor.UsbControlBlock;
import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvc.widget.SimpleUVCCameraTextureView;

public final class SimpleActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final String TAG = SimpleActivity.class.getSimpleName();

    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SimpleUVCCameraTextureView mUVCCameraView;
    // for open&start / stop&close camera preview
    private ImageButton mCameraButton;
    private Surface mPreviewSurface;

    int prevreWidth = 640;
    int previewHeight = 480;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        mCameraButton = (ImageButton) findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);

        mUVCCameraView = (SimpleUVCCameraTextureView) findViewById(R.id.UVCCameraTextureView1);
        mUVCCameraView.setAspectRatio(prevreWidth / (float) previewHeight);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart...");
        mUSBMonitor.register();
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.setPreviewDisplay(mPreviewSurface);
                mUVCCamera.startPreview();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop...");

        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
            }
            releaseCamera();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy...");

        synchronized (mSync) {
            releaseCamera();
            if (mToast != null) {
                mToast.cancel();
                mToast = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        mUVCCameraView = null;
        mCameraButton = null;
        super.onDestroy();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if (mUVCCamera == null) {
                    CameraDialog.showDialog(SimpleActivity.this);
                } else {
                    releaseCamera();
                }
            }
        }
    };

    private Toast mToast;

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
//            Toast.makeText(SimpleActivity.this, "USB_DEVICE_ATTACHED vid = " + device.getVendorId() + ", pid = " + device.getProductId(), Toast.LENGTH_SHORT).show();
//            mUSBMonitor.requestPermission(device);
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            releaseCamera();
            queueEvent(() -> {
                synchronized (mSync) {
                    final UVCCamera camera = new UVCCamera();
                    int status = camera.openWithError(ctrlBlock);
                    if (status < 0) {
                        runOnUiThread(() -> Toast.makeText(SimpleActivity.this, "open error: " + status, Toast.LENGTH_SHORT).show());
                        return;
                    }

                    try {
                        JSONObject dataJson = new JSONObject(camera.getDescriptions());
                        JSONObject descriptionJson = dataJson.getJSONObject("description");

                        int venderId = descriptionJson.getInt("venderId");
                        int productId = descriptionJson.getInt("productId");

                        runOnUiThread(() -> Toast.makeText(SimpleActivity.this, "USB_DEVICE_OPEN vid = " + intToHexStr(venderId) + ", pid = " + intToHexStr(productId), Toast.LENGTH_SHORT).show());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    try {
                        camera.setPreviewSize(prevreWidth, previewHeight, UVCCamera.FRAME_FORMAT_MJPEG);
                    } catch (final IllegalArgumentException e) {
                        // fallback to YUV mode
                        try {
                            camera.setPreviewSize(prevreWidth, previewHeight, UVCCamera.DEFAULT_PREVIEW_MODE);
                        } catch (final IllegalArgumentException e1) {
                            camera.destroy();
                            return;
                        }
                    }
                    final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                    if (st != null) {
                        mPreviewSurface = new Surface(st);
                        camera.setPreviewDisplay(mPreviewSurface);
                        camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
                        camera.openImage();
                        camera.startPreview();
                    }

                    mUVCCamera = camera;
                }

            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(SimpleActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            if (mUVCCamera != null && mUVCCamera.getUsbControlBlock() != null && device.equals(mUVCCamera.getUsbControlBlock().getDevice())) {
                releaseCamera();
            }
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    private synchronized void releaseCamera() {
        Log.i(TAG, "releaseCamera...");
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
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

    private String intToHexStr(int number) {
        String hex = Integer.toHexString(number);
        if (hex.length() < 4) {
            hex = "0" + hex;
        }

        return hex;
    }

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    // final Bitmap bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            frame.clear();
//            synchronized (bitmap) {
//                bitmap.copyPixelsFromBuffer(frame);
//            }
//            mImageView.post(mUpdateImageTask);
        }
    };
//
//    private final Runnable mUpdateImageTask = new Runnable() {
//        @Override
//        public void run() {
//            synchronized (bitmap) {
//                mImageView.setImageBitmap(bitmap);
//            }
//        }
//    };
}
