package com.xiaozhi003.uvc.util;

import android.util.Log;

import com.xiaozhi003.uvc.usb.UVCCamera;

public class CameraControl {

    private static final String TAG = CameraControl.class.getSimpleName();

    //转动马达 角度传感器封装 add
    private MotorReversalCallback mmotorReversalCallback;
    private MotorForwardCallback mmotorForwardCallback;

    private boolean mIsFacePreviewing;

    private UVCCamera mUVCFaceCamera;

    public CameraControl(UVCCamera uvcCamera) {
        mUVCFaceCamera = uvcCamera;
    }

    public void setFacePreviewing(boolean facePreviewing) {
        mIsFacePreviewing = facePreviewing;
    }

    private void sleep(long delay) {
        if (delay < 0)
            return;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void MotorForwardFwdRun(int num, final MotorForwardCallback callback) {
        if (mUVCFaceCamera != null && mIsFacePreviewing) {
            byte[] data = DataTypeConversionUtil.shortToByteArray((short) num);
            mUVCFaceCamera.WriteToASIC((short) 0x0d45, data[0]);
            mUVCFaceCamera.WriteToASIC((short) 0x0d46, data[1]);
            mUVCFaceCamera.WriteToASIC((short) 0x0d4f, (byte) 0x01);

            int mill = AngleSensorUtil.MotortotalTime / AngleSensorUtil.totalstep * num;
        }
        if (callback != null) {
            callback.done();
        }
//        Log.d(TAG, "转动后角度：" + ReadAnglesensor());
    }

    public synchronized void motorReverseFwsRun(int num, final MotorReversalCallback callback) {
        if (mUVCFaceCamera != null && mIsFacePreviewing) {
            byte[] data = DataTypeConversionUtil.shortToByteArray((short) num);

            mUVCFaceCamera.WriteToASIC((short) 0x0d47, data[0]);
            mUVCFaceCamera.WriteToASIC((short) 0x0d48, data[1]);
            mUVCFaceCamera.WriteToASIC((short) 0x0d50, (byte) 0x01);
            int mill = AngleSensorUtil.MotortotalTime / AngleSensorUtil.totalstep * num;
        }
        if (callback != null) {
            callback.done();
        }
//        Log.d(TAG, "转动后角度：" + ReadAnglesensor());
    }


    public int ReadAddrValue(short addr) {
        int ret = 0xff;
        if (mUVCFaceCamera == null || !mIsFacePreviewing) {
            return ret;
        }
        byte value = mUVCFaceCamera.ReadToASIC(addr);
        ret = DataTypeConversionUtil.unsignedByteToInt(value);
        return ret;

    }

    public void setAngleszero() {
        if (mUVCFaceCamera == null || !mIsFacePreviewing) {
            return;
        }
        mUVCFaceCamera.WriteToASIC((short) 0x0d44, (byte) 0x01);
        mUVCFaceCamera.WriteToASIC((short) 0x0d53, (byte) 0x01);
    }

    public void setRedLight(boolean onoff) {
        if (mUVCFaceCamera == null || !mIsFacePreviewing) {
            return;
        }
        if (onoff) {
            mUVCFaceCamera.WriteToASIC((short) 0x0d4b, (byte) 0x01);
        } else {
            mUVCFaceCamera.WriteToASIC((short) 0x0d4b, (byte) 0x00);
        }
        mUVCFaceCamera.WriteToASIC((short) 0x0d54, (byte) 0x01);

    }

    public void setGreenLight(boolean onoff) {
        if (mUVCFaceCamera == null || !mIsFacePreviewing) {
            return;
        }
        if (onoff) {
            mUVCFaceCamera.WriteToASIC((short) 0x0d4c, (byte) 0x01);
        } else {
            mUVCFaceCamera.WriteToASIC((short) 0x0d4c, (byte) 0x00);
        }
        mUVCFaceCamera.WriteToASIC((short) 0x0d55, (byte) 0x01);

    }

    public void setBlueLight(boolean onoff) {
        if (mUVCFaceCamera == null || !mIsFacePreviewing) {
            return;
        }
        if (onoff) {
            mUVCFaceCamera.WriteToASIC((short) 0x0d4d, (byte) 0x01);
        } else {
            mUVCFaceCamera.WriteToASIC((short) 0x0d4d, (byte) 0x00);
        }
        mUVCFaceCamera.WriteToASIC((short) 0x0d56, (byte) 0x01);

    }

    public int ReadAnglesensor() {
        int angle = 0;
        if (mUVCFaceCamera == null || !mIsFacePreviewing) {
            return angle;
        }

        mUVCFaceCamera.WriteToASIC((short) 0x05fe, (byte) 0x01);
        sleep(30);//添加延时，否则读取错误
        byte[] data = new byte[2];
        data[0] = mUVCFaceCamera.ReadToASIC((short) 0x0d42);
        data[1] = mUVCFaceCamera.ReadToASIC((short) 0x0d43);
        int value = DataTypeConversionUtil.byte2int(data);
        angle = (int) (value * 360.0 / 0xffff);
        if (angle >= 355) {
            angle = 0;
        }
        return angle;

    }

    public synchronized void initAngularSensorData(final MotorInitCallback callback) {
        Log.i(TAG, "initAngularSensorData...");

        int angleSensor = ReadAnglesensor();
        Log.d(TAG, "转动前角度： " + angleSensor);

        int num = (int) (angleSensor - 27) * 1000 / 307;

        Log.i(TAG, "转动角度: " + num);
        if (num < 0) {
            MotorForwardFwdRun(Math.abs(num), () -> {
                if (callback != null) {
                    callback.done(num);
                }
            });
        } else {
            motorReverseFwsRun(Math.abs(num), () -> {
                if (callback != null) {
                    callback.done(num);
                }
            });
        }
    }

    public synchronized void initAngularSensorData() {
        Log.i(TAG, "initAngularSensorData...");

        int angleSensor = ReadAnglesensor();
        Log.d(TAG, "转动前角度： " + angleSensor);

        int num = (int) (angleSensor - 27) * 1000 / 307;

        Log.i(TAG, "转动角度: " + num);
        if (num < 0) {
            MotorForwardFwdRun(Math.abs(num), () -> {
                sleep(500);

                Log.d(TAG, "转动后角度： " + ReadAnglesensor());
            });
        } else {
            motorReverseFwsRun(Math.abs(num), () -> {
                sleep(500);

                Log.d(TAG, "转动后角度： " + ReadAnglesensor());
            });
        }
    }

    /**
     * 校准
     */
    public synchronized void calibration() {
        Log.i(TAG, "校准中...");
        motorReverseFwsRun(200, () -> {
            sleep(3000);

            setAngleszero();
        });
    }

    public interface MotorForwardCallback {
        void done();
    }

    public interface MotorReversalCallback {
        void done();
    }

    public interface MotorInitCallback {
        void done(int num);
    }
}
