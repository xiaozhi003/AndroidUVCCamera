package com.xiaozhi003.uvc.demo.activity;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.List;

import com.xiaozhi003.uvc.common.BaseActivity;
import com.eyecool.uvc.demo.R;
import com.xiaozhi003.uvc.demo.config.UVCCameraConfig;
import com.xiaozhi003.uvc.demo.dialog.CameraDialog;
import com.xiaozhi003.uvc.demo.util.FileUtils;
import com.xiaozhi003.uvc.usb.Size;
import com.xiaozhi003.uvc.usb.USBMonitor;
import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvc.usb.UVCCameraManager;
import com.xiaozhi003.uvc.widget.UVCCameraTextureView;

/**
 * usb camera简单调用
 *
 * @author xiaozhi
 * @date 2021/4/1
 */
public class CommonActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final String TAG = CommonActivity.class.getSimpleName();

    public static final int REQUEST_SETTING = 12;

    private ImageButton mCameraButton;
    private Switch mMirrorSwitch;
    private Button mCaptureBtn;
    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCameraTextureView mUVCCameraTextureView;
    private UVCCameraManager mUVCCameraManager;
    private int mPreviewWidth = 640;
    private int mPreviewHeight = 480;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common);

        mUVCCameraManager = new UVCCameraManager(this);
        mCameraButton = (ImageButton) findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);
        findViewById(R.id.settingBtn).setOnClickListener(view -> {
            Intent intent = new Intent(this, SettingActivity.class);
            UVCCamera uvcCamera = mUVCCameraManager.getUVCCamera();
            if (uvcCamera != null) {
                List<Size> sizeList = uvcCamera.getSupportedSizeList();
                intent.putExtra(SettingActivity.EXTRA_PREVIEW_SIZE, (Serializable) sizeList);
                intent.putExtra(SettingActivity.EXTRA_USB_DEVICE, uvcCamera.getDevice());
            }

            startActivityForResult(intent, REQUEST_SETTING);
        });

        mCaptureBtn = findViewById(R.id.captureBtn);
        mCaptureBtn.setOnClickListener(view -> {
            if (mUVCCameraManager.isPreview()) {
                byte[] data = mUVCCameraManager.getCameraBytes();
                int width = mUVCCameraManager.getPreviewWidth();
                int height = mUVCCameraManager.getPreviewHeight();

                String name = System.currentTimeMillis() + "_" + width + "x" + height;
                FileUtils.writeFile(getExternalFilesDir("Camera") + "/" + name + ".bin", data);
                FileUtils.writeFile(getExternalFilesDir("Camera") + "/" + name + ".jpg", YuvTransformJpeg(data, width, height, 100));
            }
        });

        mMirrorSwitch = findViewById(R.id.mirrorSwitch);
        mMirrorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mUVCCameraTextureView.setMirror(isChecked));
        mMirrorSwitch.setChecked(false);

        mUVCCameraTextureView = findViewById(R.id.uvcCameraView);
        mUVCCameraTextureView.setAspectRatio(mPreviewWidth / (float) mPreviewHeight);
        mUVCCameraTextureView.setDisplayOrientation(0);
        mUVCCameraManager.setDisplayView(mUVCCameraTextureView);
        mUVCCameraManager.setUVCCameraCallback(mUVCCameraCallbackX);
        mUVCCameraManager.setPreviewSize(mPreviewWidth, mPreviewHeight);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart...");
        mUSBMonitor.register();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume...");
        mUVCCameraManager.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause...");
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
        super.onStop();
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
        mUVCCameraTextureView = null;
        mCameraButton = null;
        super.onDestroy();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if (mUVCCameraManager.getUVCCamera() == null) {
                    CameraDialog.showDialog(CommonActivity.this);
                } else {
                    mUVCCameraManager.closeCamera();
                }
            }
        }
    };

    private Toast mToast;

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {

        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            mUVCCameraManager.closeCamera();
            queueEvent(() -> {
                synchronized (mSync) {
                    mUVCCameraManager.openCamera(ctrlBlock);
                }
            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(CommonActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            if (mUVCCameraManager.getUVCCamera() != null && mUVCCameraManager.getUVCCamera().getUsbControlBlock() != null && device.equals(mUVCCameraManager.getUVCCamera().getUsbControlBlock().getDevice())) {
                mUVCCameraManager.closeCamera();
            }
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

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

    private UVCCameraManager.UVCCameraCallbackX mUVCCameraCallbackX = new UVCCameraManager.UVCCameraCallbackX() {
        @Override
        public void afterOpen(UVCCamera uvcCamera) {
            Log.i(TAG, "afterOpen...");
            try {
                JSONObject dataJson = new JSONObject(uvcCamera.getDescriptions());
                JSONObject descriptionJson = dataJson.getJSONObject("description");

                int venderId = descriptionJson.getInt("venderId");
                int productId = descriptionJson.getInt("productId");

                runOnUiThread(() -> Toast.makeText(CommonActivity.this, "open vid:pid [" + intToHexStr(venderId) + ":" + intToHexStr(productId) + "]", Toast.LENGTH_SHORT).show());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i(TAG, "supportedSize:" + uvcCamera.getSupportedSize());

            Log.i(TAG, "previewSizeMap.size:" + UVCCameraConfig.sUVCCameraConfigMap.size());
            UVCCameraConfig config = UVCCameraConfig.sUVCCameraConfigMap.get(uvcCamera.getDevice());
            if (config != null) {
                Size size = config.getSize();
                Log.i(TAG, "previewSize:" + size);
                mUVCCameraManager.setPreviewSize(size.width, size.height);
            }
        }

        @Override
        public void beforePreview(UVCCamera uvcCamera) {
            uvcCamera.openImage();
        }

        @Override
        public void afterPreview(UVCCamera camera) {
            super.afterPreview(camera);
            camera.updateCameraParams();

            StringBuffer sb = new StringBuffer();
            // 亮度
            int brightness = camera.getBrightnessDef();
            sb.append("亮度:" + brightness + ", min:" + camera.getBrightnessMin() + ", max:" + camera.getBrightnessMax() + "\n");
            // 对比度
            int contrast = camera.getContrastDef();
            sb.append("对比度:" + contrast + ", min:" + camera.getContrastMin() + ", max:" + camera.getContrastMax() + "\n");
            // 色调
            int hue = camera.getHueDef();
            sb.append("色调:" + hue + ", min:" + camera.getHueMin() + ", max:" + camera.getHueMax() + "\n");
            // 饱和度
            int saturation = camera.getSaturationDef();
            sb.append("饱和度：" + saturation + ", min:" + camera.getSaturationMin() + ", max:" + camera.getSaturationMax() + "\n");
            // 清晰度
            int sharpness = camera.getSharpnessDef();
            sb.append("清晰度：" + sharpness + ", min:" + camera.getSharpnessMin() + ", max:" + camera.getSharpnessMax() + "\n");
            // 伽玛
            int gamma = camera.getGammaDef();
            sb.append("伽玛：" + gamma + ", min:" + camera.getGammaMin() + ", max:" + camera.getGammaMax() + "\n");
            // 白平衡
            int whiteBalance = camera.getWhiteBalanceDef();
            sb.append("白平衡：" + whiteBalance + ", min:" + camera.getWhiteBalanceMin() + ", max:" + camera.getWhiteBalanceMax() + "\n");
            // 逆光补偿
            int backlightComp = camera.getBacklightCompDef();
            sb.append("逆光补偿：" + backlightComp + ", min:" + camera.getBacklightCompMin() + ", max:" + camera.getBacklightCompMax() + "\n");
            // 增益
            int gain = camera.getGainDef();
            sb.append("增益：" + gain + ", min:" + camera.getGainMin() + ", max:" + camera.getGainMax() + "\n");

            Log.i(TAG, sb.toString());

            FileUtils.writeFile(getExternalFilesDir("UVCCamera").getAbsolutePath() + "/" + camera.getUsbControlBlock().getDevice().getManufacturerName() + ".txt", sb.toString());

            runOnUiThread(() -> mUVCCameraTextureView.setAspectRatio(camera.getPreviewSize().width / (float) camera.getPreviewSize().height));
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
            Toast.makeText(CommonActivity.this, "open error: " + error, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "requestCode:" + requestCode + ", resultCode:" + resultCode);
        if (requestCode == REQUEST_SETTING && resultCode == RESULT_OK) {
            UsbDevice device = data.getParcelableExtra(SettingActivity.EXTRA_USB_DEVICE);
            if (device != null) {
                Log.i(TAG, device.toString());
                runOnUiThread(() -> mUSBMonitor.requestPermission(device), 1000);
            }
        }
    }

    private static byte[] YuvTransformJpeg(byte[] data, int width, int hight, int quality) {
        try {
            YuvImage image_jpeg =
                    new YuvImage(data, ImageFormat.NV21, width, hight, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image_jpeg.compressToJpeg(new Rect(0, 0, width, hight), quality, stream);
            return stream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "failed :" + e);
        }
        return null;
    }
}