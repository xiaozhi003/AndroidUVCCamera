package com.xiaozhi003.uvcapp;

import android.Manifest;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import com.xiaozhi003.uvc.usb.Size;
import com.xiaozhi003.uvc.usb.USBMonitor;
import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvc.usb.UVCCameraManager;
import com.xiaozhi003.uvc.util.FileUtils;
import com.xiaozhi003.uvcapp.config.UVCCameraConfig;
import com.xiaozhi003.uvcapp.databinding.ActivityMainBinding;
import com.xiaozhi003.uvcapp.dialog.CameraDialog;
import com.xiaozhi003.uvcapp.permission.IPermissionsResult;
import com.xiaozhi003.uvcapp.permission.PermissionUtils;
import com.xiaozhi003.uvcapp.view.UVCBottomDialogFragment;
import com.xiaozhi003.uvcapp.view.UVCDeviceInfoDialogFragment;
import com.xiaozhi003.uvcapp.view.UVCVideoFormatDialogFragment;

public class MainActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent {

    private static final String TAG = "USBCamera";

    // 需要请求的危险权限
    private String[] requestPermissions = new String[]{
            Manifest.permission.CAMERA,    // 相机权限
    };
    private final Object mSync = new Object();

    private ActivityMainBinding binding;
    private Context mContext;

    private USBMonitor mUSBMonitor;
    private UVCCameraManager mUVCCameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this;
        setSupportActionBar(binding.toolbar);

        binding.fab.setOnClickListener(view -> capture());

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mUVCCameraManager = new UVCCameraManager(this);
        mUVCCameraManager.setDisplayView(binding.content.previewView);
        mUVCCameraManager.setUVCCameraCallback(mUVCCameraCallbackX);

        PermissionUtils.getInstance().requestPermission(this, requestPermissions, new IPermissionsResult() {
            @Override
            public void passPermissions() {

            }

            @Override
            public void forbidPermissions() {
                Toast.makeText(mContext, "用户未授权", Toast.LENGTH_SHORT).show();
            }
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
        mUVCCameraManager.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause...");
        mUVCCameraManager.stopPreview();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUSBMonitor.unregister();
        mUVCCameraManager.closeCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

        } else if (id == R.id.action_control) {
            if (mUVCCameraManager.getUVCCamera() != null) {
                UVCBottomDialogFragment dialog = new UVCBottomDialogFragment(mUVCCameraManager.getUVCCamera());
                Bundle bundle = new Bundle();
                dialog.setArguments(bundle);
                dialog.show(getSupportFragmentManager(), "dialog_fragment");
            }
        } else if (id == R.id.action_device) {
            synchronized (mSync) {
                if (mUVCCameraManager.getUVCCamera() == null) {
                    CameraDialog.showDialog(this);
                } else {
                    mUVCCameraManager.closeCamera();
                    binding.content.tvConnectUSBCameraTip.setVisibility(View.VISIBLE);
                    invalidateOptionsMenu();
                }
            }
        } else if (id == R.id.action_device_info) {
            if (mUVCCameraManager.getUVCCamera() != null) {
                UVCDeviceInfoDialogFragment dialog = new UVCDeviceInfoDialogFragment(mUVCCameraManager.getUVCCamera().getDevice());
                dialog.show(getSupportFragmentManager(), "Device Info");
            }
        } else if (id == R.id.action_mirror) {
            binding.content.previewView.setMirror(binding.content.previewView.isMirror() ? false : true);
        } else if (id == R.id.action_video_format) {
            if (mUVCCameraManager.getUVCCamera() != null) {
                UVCVideoFormatDialogFragment dialog = new UVCVideoFormatDialogFragment(mUVCCameraManager.getUVCCamera());
                dialog.setOnVideoFormatSelectListener(size -> {
                    if (size.width != mUVCCameraManager.getPreviewWidth() || size.height != mUVCCameraManager.getPreviewHeight()) {
                        mUVCCameraManager.stopPreview();
                        mUVCCameraManager.setPreviewSize(size.width, size.height);
                        mUVCCameraManager.startPreview();
                    }
                });
                dialog.show(getSupportFragmentManager(), "video_format");
            }
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mUVCCameraManager.isPreview()) {
            menu.findItem(R.id.action_control).setVisible(true);
            menu.findItem(R.id.action_video_format).setVisible(true);
            menu.findItem(R.id.action_device_info).setVisible(true);
            menu.findItem(R.id.action_mirror).setVisible(true);
        } else {
            menu.findItem(R.id.action_control).setVisible(false);
            menu.findItem(R.id.action_video_format).setVisible(false);
            menu.findItem(R.id.action_device_info).setVisible(false);
            menu.findItem(R.id.action_mirror).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void capture() {
        if (mUVCCameraManager.isPreview()) {
            byte[] data = mUVCCameraManager.getCameraBytes();
            int width = mUVCCameraManager.getPreviewWidth();
            int height = mUVCCameraManager.getPreviewHeight();

            String name = System.currentTimeMillis() + "_" + width + "x" + height;
            FileUtils.writeFile(getExternalFilesDir("Camera") + "/" + name + ".bin", data);
            FileUtils.writeFile(getExternalFilesDir("Camera") + "/" + name + ".jpg", YuvTransformJpeg(data, width, height, 100));
            Toast.makeText(mContext, "图像保存成功 " + getExternalFilesDir("Camera") + "/" + name + ".jpg", Toast.LENGTH_SHORT).show();
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {

        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            Log.i(TAG, "onConnect:" + device.getManufacturerName());
            mUVCCameraManager.closeCamera();
            mUVCCameraManager.openCamera(ctrlBlock);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(mContext, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            if (mUVCCameraManager.getUVCCamera() != null && mUVCCameraManager.getUVCCamera().getUsbControlBlock() != null && device.equals(mUVCCameraManager.getUVCCamera().getUsbControlBlock().getDevice())) {
                mUVCCameraManager.closeCamera();
            }
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    private UVCCameraManager.UVCCameraCallbackX mUVCCameraCallbackX = new UVCCameraManager.UVCCameraCallbackX() {
        @Override
        public void afterOpen(UVCCamera uvcCamera) {
            Log.i(TAG, "afterOpen...");
            Log.i(TAG, "supportedSize:" + uvcCamera.getSupportedSize());
        }

        @Override
        public void beforePreview(UVCCamera uvcCamera) {
            uvcCamera.openImage();
            int venderId = 0;
            int productId = 0;
            try {
                JSONObject dataJson = new JSONObject(uvcCamera.getDescriptions());
                JSONObject descriptionJson = dataJson.getJSONObject("description");

                venderId = descriptionJson.getInt("venderId");
                productId = descriptionJson.getInt("productId");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i(TAG, "previewSizeMap.size:" + UVCCameraConfig.sUVCCameraConfigMap.size());
            UVCCameraConfig config = UVCCameraConfig.sUVCCameraConfigMap.get(uvcCamera.getDevice());
            if (config != null) {
                Size size = config.getSize();
                Log.i(TAG, "previewSize:" + size);
                config.setVid(venderId);
                config.setPid(productId);
            } else {
                config = new UVCCameraConfig();
                Size size = uvcCamera.getPreviewSize();
                config.setSize(size);
                config.setVid(venderId);
                config.setPid(productId);
                UVCCameraConfig.sUVCCameraConfigMap.put(uvcCamera.getDevice(), config);
            }
            mUVCCameraManager.setPreviewSize(config.getSize().width, config.getSize().height);
        }

        @Override
        public void afterPreview(UVCCamera uvcCamera) {
            super.afterPreview(uvcCamera);
            uvcCamera.updateCameraParams();

            StringBuffer sb = new StringBuffer();
            // 亮度
            int brightness = uvcCamera.getBrightnessDef();
            sb.append("亮度:" + brightness + ", min:" + uvcCamera.getBrightnessMin() + ", max:" + uvcCamera.getBrightnessMax() + "\n");
            // 对比度
            int contrast = uvcCamera.getContrastDef();
            sb.append("对比度:" + contrast + ", min:" + uvcCamera.getContrastMin() + ", max:" + uvcCamera.getContrastMax() + "\n");
            // 色调
            int hue = uvcCamera.getHueDef();
            sb.append("色调:" + hue + ", min:" + uvcCamera.getHueMin() + ", max:" + uvcCamera.getHueMax() + "\n");
            // 饱和度
            int saturation = uvcCamera.getSaturationDef();
            sb.append("饱和度：" + saturation + ", min:" + uvcCamera.getSaturationMin() + ", max:" + uvcCamera.getSaturationMax() + "\n");
            // 清晰度
            int sharpness = uvcCamera.getSharpnessDef();
            sb.append("清晰度：" + sharpness + ", min:" + uvcCamera.getSharpnessMin() + ", max:" + uvcCamera.getSharpnessMax() + "\n");
            // 伽玛
            int gamma = uvcCamera.getGammaDef();
            sb.append("伽玛：" + gamma + ", min:" + uvcCamera.getGammaMin() + ", max:" + uvcCamera.getGammaMax() + "\n");
            // 白平衡
            int whiteBalance = uvcCamera.getWhiteBalanceDef();
            sb.append("白平衡：" + whiteBalance + ", min:" + uvcCamera.getWhiteBalanceMin() + ", max:" + uvcCamera.getWhiteBalanceMax() + "\n");
            // 逆光补偿
            int backlightComp = uvcCamera.getBacklightCompDef();
            sb.append("逆光补偿：" + backlightComp + ", min:" + uvcCamera.getBacklightCompMin() + ", max:" + uvcCamera.getBacklightCompMax() + "\n");
            // 增益
            int gain = uvcCamera.getGainDef();
            sb.append("增益：" + gain + ", min:" + uvcCamera.getGainMin() + ", max:" + uvcCamera.getGainMax() + "\n");

            Log.i(TAG, sb.toString());

            FileUtils.writeFile(getExternalFilesDir("UVCCamera").getAbsolutePath() + "/" + uvcCamera.getUsbControlBlock().getDevice().getManufacturerName() + ".txt", sb.toString());

            runOnUiThread(() -> binding.content.previewView.setAspectRatio(uvcCamera.getPreviewSize().width / (float) uvcCamera.getPreviewSize().height));
        }

        @Override
        public void onOpened() {
            Log.i(TAG, "onOpened...");
        }

        @Override
        public void onPreview() {
            Log.i(TAG, "onPreview...");
            invalidateOptionsMenu();
            binding.content.tvConnectUSBCameraTip.setVisibility(View.GONE);
        }

        @Override
        public void onOpenError(int error) {
            Log.e(TAG, "onOpenError...");
            Toast.makeText(mContext, "open error: " + error, Toast.LENGTH_SHORT).show();
        }
    };

    private static String intToHexStr(int number) {
        String hex = Integer.toHexString(number);
        if (hex.length() < 4) {
            hex = "0" + hex;
        }

        return hex;
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

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {

    }
}