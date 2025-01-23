package com.xiaozhi003.uvcapp.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import com.xiaozhi003.uvc.usb.Size;
import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvcapp.adapter.ResolutionSizeAdapter;
import com.xiaozhi003.uvcapp.config.UVCCameraConfig;
import com.xiaozhi003.uvcapp.databinding.FragmentVideoFormatBinding;

public class UVCVideoFormatDialogFragment extends DialogFragment {

    private UVCCamera mUVCCamera;

    private FragmentVideoFormatBinding mBinding;

    private ResolutionSizeAdapter mResolutionSizeAdapter;
    private List<Size> mResolutionSizeList;
    private Size mSelectSize;

    private OnVideoFormatSelectListener mOnVideoFormatSelectListener;

    public UVCVideoFormatDialogFragment(UVCCamera uvcCamera) {
        mUVCCamera = uvcCamera;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mBinding = FragmentVideoFormatBinding.inflate(getLayoutInflater());

        updateDialogUI();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Video Format")
                .setView(mBinding.getRoot())
                .setPositiveButton("OK", (dialog, which) -> {
                    UVCCameraConfig config = new UVCCameraConfig();
                    config.setSize(mSelectSize);
                    UVCCameraConfig.sUVCCameraConfigMap.put(mUVCCamera.getDevice(), config);
                    if (mOnVideoFormatSelectListener != null) {
                        mOnVideoFormatSelectListener.onFormatSelect(mSelectSize);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {

                });
        return builder.create();
    }

    private void updateDialogUI() {
        mResolutionSizeList = mUVCCamera.getSupportedSizeList();
        if (mResolutionSizeList != null) {
            mResolutionSizeAdapter = new ResolutionSizeAdapter(requireActivity(), mResolutionSizeList);
            mBinding.resolutionSp.setAdapter(mResolutionSizeAdapter);
            mBinding.resolutionSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mSelectSize = mResolutionSizeAdapter.getItem(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        UVCCameraConfig config = UVCCameraConfig.sUVCCameraConfigMap.get(mUVCCamera.getDevice());
        if (mResolutionSizeList != null && config != null) {
            Size size = config.getSize();
            for (int i = 0; i < mResolutionSizeList.size(); i++) {
                if (mResolutionSizeList.get(i).width == size.width && mResolutionSizeList.get(i).height == size.height) {
                    mBinding.resolutionSp.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBinding = null;
        mUVCCamera = null;
    }

    public void setOnVideoFormatSelectListener(OnVideoFormatSelectListener listener) {
        this.mOnVideoFormatSelectListener = listener;
    }

    public interface OnVideoFormatSelectListener {
        void onFormatSelect(Size size);
    }
}
