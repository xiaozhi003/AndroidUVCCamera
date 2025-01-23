package com.xiaozhi003.uvcapp.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvcapp.R;
import com.xiaozhi003.uvcapp.databinding.FragmentBottomDialogBinding;

public class UVCBottomDialogFragment extends BottomSheetDialogFragment {

    private UVCCamera mUVCCamera;

    private FragmentBottomDialogBinding mBinding;

    public UVCBottomDialogFragment(UVCCamera uvcCamera) {
        mUVCCamera = uvcCamera;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.MyBottomSheetDialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentBottomDialogBinding.inflate(inflater, container, false);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 亮度
        mBinding.brightnessTv.setText(mUVCCamera.getRealBrightness() + "");
        mBinding.brightnessBar.setMax(mUVCCamera.getBrightnessMax() - mUVCCamera.getBrightnessMin());
        mBinding.brightnessBar.setProgress(mUVCCamera.getRealBrightness() - mUVCCamera.getBrightnessMin());
        mBinding.brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.brightnessTv.setText((progress + mUVCCamera.getBrightnessMin()) + "");
                mUVCCamera.setRealBrightness((progress + mUVCCamera.getBrightnessMin()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 对比度
        mBinding.contrastTv.setText(mUVCCamera.getRealContrast() + "");
        mBinding.contrastBar.setMax(mUVCCamera.getContrastMax() - mUVCCamera.getContrastMin());
        mBinding.contrastBar.setProgress(mUVCCamera.getRealContrast() - mUVCCamera.getContrastMin());
        mBinding.contrastBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.contrastTv.setText((progress + mUVCCamera.getContrastMin()) + "");
                mUVCCamera.setRealContrast(progress + mUVCCamera.getContrastMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 色调
        mBinding.hueTv.setText(mUVCCamera.getRealHue() + "");
        mBinding.hueBar.setMax(mUVCCamera.getHueMax() - mUVCCamera.getHueMin());
        mBinding.hueBar.setProgress(mUVCCamera.getRealHue() - mUVCCamera.getHueMin());
        mBinding.hueBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.hueTv.setText((progress + mUVCCamera.getHueMin()) + "");
                mUVCCamera.setRealHue(progress + mUVCCamera.getHueMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 饱和度
        mBinding.saturationTv.setText(mUVCCamera.getRealSaturation() + "");
        mBinding.saturationBar.setMax(mUVCCamera.getSaturationMax() - mUVCCamera.getSaturationMin());
        mBinding.saturationBar.setProgress(mUVCCamera.getRealSaturation() - mUVCCamera.getSaturationMin());
        mBinding.saturationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.saturationTv.setText((progress + mUVCCamera.getSaturationMin()) + "");
                mUVCCamera.setRealSaturation(progress + mUVCCamera.getSaturationMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 清晰度
        mBinding.sharpnessTv.setText(mUVCCamera.getRealSharpness() + "");
        mBinding.sharpnessBar.setMax(mUVCCamera.getSharpnessMax() - mUVCCamera.getSharpnessMin());
        mBinding.sharpnessBar.setProgress(mUVCCamera.getRealSharpness() - mUVCCamera.getSharpnessMin());
        mBinding.sharpnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.sharpnessTv.setText((progress + mUVCCamera.getSharpnessMin()) + "");
                mUVCCamera.setRealSharpness(progress + mUVCCamera.getSharpnessMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 伽玛
        mBinding.gammaTv.setText(mUVCCamera.getRealGamma() + "");
        mBinding.gammaBar.setMax(mUVCCamera.getGammaMax() - mUVCCamera.getGammaMin());
        mBinding.gammaBar.setProgress(mUVCCamera.getRealGamma() - mUVCCamera.getGammaMin());
        mBinding.gammaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.gammaTv.setText(progress + mUVCCamera.getGammaMin() + "");
                mUVCCamera.setRealGamma(progress + mUVCCamera.getGammaMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 白平衡
        mBinding.whiteBalanceTv.setText(mUVCCamera.getRealWhiteBalance() + "");
        mBinding.whiteBalanceBar.setMax(mUVCCamera.getWhiteBalanceMax() - mUVCCamera.getWhiteBalanceMin());
        mBinding.whiteBalanceBar.setProgress(mUVCCamera.getRealWhiteBalance() - mUVCCamera.getWhiteBalanceMin());
        mBinding.whiteBalanceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.whiteBalanceTv.setText(progress + mUVCCamera.getWhiteBalanceMin() + "");
                mUVCCamera.setRealWhiteBalance(progress + mUVCCamera.getWhiteBalanceMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // 逆光补偿
        mBinding.backlightCompTv.setText(mUVCCamera.getRealBacklightComp() + "");
        mBinding.backlightCompBar.setMax(mUVCCamera.getBacklightCompMax() - mUVCCamera.getBacklightCompMin());
        mBinding.backlightCompBar.setProgress(mUVCCamera.getRealBacklightComp() - mUVCCamera.getBacklightCompMin());
        mBinding.backlightCompBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.backlightCompTv.setText(progress + mUVCCamera.getBacklightCompMin() + "");
                mUVCCamera.setRealBacklightComp(progress + mUVCCamera.getBacklightCompMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // 增益
        mBinding.gainTv.setText(mUVCCamera.getRealGain() + "");
        mBinding.gainBar.setMax(mUVCCamera.getGainMax() - mUVCCamera.getGainMin());
        mBinding.gainBar.setProgress(mUVCCamera.getRealGain() - mUVCCamera.getGainMin());
        mBinding.gainBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBinding.gainTv.setText(progress + mUVCCamera.getGainMin() + "");
                mUVCCamera.setRealGain(progress + mUVCCamera.getGainMin());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mBinding.resetCameraControlBtn.setOnClickListener(v -> {
            mUVCCamera.resetBrightness();
            mUVCCamera.resetContrast();
            mUVCCamera.resetHue();
            mUVCCamera.resetSaturation();
            mUVCCamera.resetSharpness();
            mUVCCamera.resetGamma();
            mUVCCamera.resetWhiteBalance();
            mUVCCamera.resetBacklightComp();
            mUVCCamera.resetGain();

            mBinding.brightnessTv.setText(mUVCCamera.getRealBrightness() + "");
            mBinding.brightnessBar.setProgress(mUVCCamera.getRealBrightness() - mUVCCamera.getBrightnessMin());
            mBinding.contrastTv.setText(mUVCCamera.getRealContrast() + "");
            mBinding.contrastBar.setProgress(mUVCCamera.getRealContrast() - mUVCCamera.getContrastMin());
            mBinding.hueTv.setText(mUVCCamera.getRealHue() + "");
            mBinding.hueBar.setProgress(mUVCCamera.getRealHue() - mUVCCamera.getHueMin());
            mBinding.saturationTv.setText(mUVCCamera.getRealSaturation() + "");
            mBinding.saturationBar.setProgress(mUVCCamera.getRealSaturation() - mUVCCamera.getSaturationMin());
            mBinding.sharpnessTv.setText(mUVCCamera.getRealSharpness() + "");
            mBinding.sharpnessBar.setProgress(mUVCCamera.getRealSharpness() - mUVCCamera.getSharpnessMin());
            mBinding.gammaTv.setText(mUVCCamera.getRealGamma() + "");
            mBinding.gammaBar.setProgress(mUVCCamera.getRealGamma() - mUVCCamera.getGammaMin());
            mBinding.whiteBalanceTv.setText(mUVCCamera.getRealWhiteBalance() + "");
            mBinding.whiteBalanceBar.setProgress(mUVCCamera.getRealWhiteBalance() - mUVCCamera.getWhiteBalanceMin());
            mBinding.backlightCompTv.setText(mUVCCamera.getRealBacklightComp() + "");
            mBinding.backlightCompBar.setProgress(mUVCCamera.getRealBacklightComp() - mUVCCamera.getBacklightCompMin());
            mBinding.gainTv.setText(mUVCCamera.getRealGain() + "");
            mBinding.gainBar.setProgress(mUVCCamera.getRealGain() - mUVCCamera.getGainMin());
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUVCCamera = null;
        mBinding = null;
    }
}
