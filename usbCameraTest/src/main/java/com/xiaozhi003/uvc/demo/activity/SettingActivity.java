package com.xiaozhi003.uvc.demo.activity;

import android.hardware.usb.UsbDevice;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import com.eyecool.uvc.demo.R;
import com.xiaozhi003.uvc.demo.adapter.PreviewSizeAdapter;
import com.xiaozhi003.uvc.demo.config.UVCCameraConfig;
import com.xiaozhi003.uvc.usb.Size;
import com.xiaozhi003.uvc.usb.UVCCameraManager;

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = SettingActivity.class.getSimpleName();
    public static final String EXTRA_USB_DEVICE = "cn.eyecool.uvc.EXTRA_USB_DEVICE";
    public static final String EXTRA_PREVIEW_SIZE = "cn.eyecool.uvc.EXTRA_PREVIEW_SIZE";

    private TextView mDevNameTv;
    private TextView mDevIdTv;
    private Switch mMirrorSwitch;
    private Spinner mPreviewResolutionSp;
    private PreviewSizeAdapter mPreviewSizeAdapter;
    private TextView mSavePathTv;

    private List<Size> mPreviewSizeList;
    private UsbDevice mUsbDevice;
    private Size mSelectSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mDevNameTv = findViewById(R.id.devNameTv);
        mDevIdTv = findViewById(R.id.devIdTv);
        mMirrorSwitch = findViewById(R.id.mirrorSwitch);
        mPreviewResolutionSp = findViewById(R.id.previewResolutionSp);
        mSavePathTv = findViewById(R.id.savePathTv);

        Object obj = getIntent().getSerializableExtra(EXTRA_PREVIEW_SIZE);
        mUsbDevice = getIntent().getParcelableExtra(EXTRA_USB_DEVICE);
        if (obj != null) {
            mPreviewSizeList = (List<Size>) obj;
            mPreviewSizeAdapter = new PreviewSizeAdapter(this, mPreviewSizeList);
            mPreviewResolutionSp.setAdapter(mPreviewSizeAdapter);
        }
        mPreviewResolutionSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSelectSize = mPreviewSizeAdapter.getItem(position);
                Log.i(TAG, "select:" + mSelectSize);
                UVCCameraConfig config = new UVCCameraConfig();
                config.setSize(mSelectSize);
                UVCCameraConfig.sUVCCameraConfigMap.put(mUsbDevice, config);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        UVCCameraConfig config = UVCCameraConfig.sUVCCameraConfigMap.get(mUsbDevice);
        if (mPreviewSizeList != null && config != null) {
            Size size = config.getSize();
            for (int i = 0; i < mPreviewSizeList.size(); i++) {
                if (mPreviewSizeList.get(i).width == size.width && mPreviewSizeList.get(i).height == size.height) {
                    mPreviewResolutionSp.setSelection(i);
                    break;
                }
            }
        }

        if (mUsbDevice != null) {
            mDevNameTv.setText("设备名：" + mUsbDevice.getManufacturerName());
            mDevIdTv.setText("vid&pid：" + UVCCameraManager.numToHex16(mUsbDevice.getVendorId()) + "&" + UVCCameraManager.numToHex16(mUsbDevice.getProductId()));
        }
        mSavePathTv.setText("图像存储路径：" + getExternalFilesDir("Camera") + "/");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, getIntent());
        finish();
    }
}