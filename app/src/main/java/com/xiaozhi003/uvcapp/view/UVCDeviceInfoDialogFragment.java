package com.xiaozhi003.uvcapp.view;

import android.app.Dialog;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.xiaozhi003.uvc.usb.UVCCameraManager;
import com.xiaozhi003.uvcapp.config.UVCCameraConfig;
import com.xiaozhi003.uvcapp.databinding.FragmentDeviceInfoBinding;

public class UVCDeviceInfoDialogFragment extends DialogFragment {

    private static final String TAG = UVCDeviceInfoDialogFragment.class.getSimpleName();

    private FragmentDeviceInfoBinding mBinding;
    private UsbDevice mUsbDevice;
    private UVCCameraConfig mUVCCameraConfig;

    public UVCDeviceInfoDialogFragment(UsbDevice usbDevice) {
        mUsbDevice = usbDevice;
        mUVCCameraConfig = UVCCameraConfig.sUVCCameraConfigMap.get(usbDevice);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = FragmentDeviceInfoBinding.inflate(getLayoutInflater());

        updateDialogUI();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(mBinding.getRoot());
        return builder.create();
    }

    private void updateDialogUI() {
        mBinding.devNameTv.setText("设备名：" + mUsbDevice.getManufacturerName());
        if (mUVCCameraConfig != null) {
            mBinding.devIdTv.setText("vid&pid：" + UVCCameraManager.numToHex16(mUVCCameraConfig.getVid()) + "&" + UVCCameraManager.numToHex16(mUVCCameraConfig.getPid()));
        } else {
            mBinding.devIdTv.setText("vid&pid：" + UVCCameraManager.numToHex16(mUsbDevice.getVendorId()) + "&" + UVCCameraManager.numToHex16(mUsbDevice.getProductId()));
        }
    }
}
