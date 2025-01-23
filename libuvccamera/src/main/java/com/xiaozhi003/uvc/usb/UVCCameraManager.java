package com.xiaozhi003.uvc.usb;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.xiaozhi003.uvc.glview.UVCGLSurfaceView;
import com.xiaozhi003.uvc.util.FpsCounter;

/**
 * UVCCamera管理工具
 *
 * @author xiaozhi
 * @date 2021/3/25
 */
public class UVCCameraManager {

    private static final String TAG = UVCCameraManager.class.getSimpleName();

    private final Object mSync = new Object();

    private UVCCamera mUVCCamera;
    private String mCameraName = "";
    private Context mContext;
    private Handler mUIHandler;
    private TextureView mTextureView;
    private UVCGLSurfaceView mGLSurfaceView;
    private Surface mSurface;
    private UVCCameraCallback mUVCCameraCallback;
    private boolean isPreview = false;
    private boolean isOpening = false;
    private int mPreviewWidth = 640;
    private int mPreviewHeight = 480;
    private byte[] mCameraBytes;
    private long mStartOpenMillis = 0;
    private PreviewCallback mPreviewCallback;
    private boolean isMirror;

    FpsCounter mFpsCounter = new FpsCounter();

    /**
     * 可见光检测回调
     */
    IFrameCallback mFrameCallback = new IFrameCallback() {

        @Override
        public void onFrame(final ByteBuffer frame) {
            mFpsCounter.count();
            mFpsCounter.update();
            if (!isPreview) {
                isPreview = true;
                Log.i(TAG, mCameraName + " previewing " + (System.currentTimeMillis() - mStartOpenMillis) + "ms");
                callOnPreview();
            }

            byte[] yuv = null;
            if (frame.limit() > 0) {
                yuv = new byte[frame.limit()];
                frame.get(yuv);
            }
            mCameraBytes = yuv;
            if (mPreviewCallback != null) {
                mPreviewCallback.onPreviewFrame(yuv, mPreviewWidth, mPreviewHeight);
            }
            if (mGLSurfaceView != null) {
                mGLSurfaceView.feedData(mCameraBytes, mPreviewWidth, mPreviewHeight, UVCCamera.PIXEL_FORMAT_NV21);
            }
        }
    };

    public UVCCameraManager(Context context) {
        this.mContext = context;
        mUIHandler = new Handler(mContext.getMainLooper());
    }

    public void setCameraName(String cameraName) {
        mCameraName = cameraName;
    }

    public byte[] getCameraBytes() {
        byte[] tempCameraBytes = mCameraBytes;
        if (isMirror && tempCameraBytes != null) {
            byte[] mirrorBytes = new byte[tempCameraBytes.length];
            System.arraycopy(tempCameraBytes, 0, mirrorBytes, 0, mirrorBytes.length);
            return mirrorNV21Fast(mirrorBytes, mPreviewWidth, mPreviewHeight);
        }
        return tempCameraBytes;
    }

    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    public void setUVCCameraCallback(UVCCameraCallback UVCCameraCallback) {
        mUVCCameraCallback = UVCCameraCallback;
    }

    public void setPreviewSize(int previewWidth, int previewHeight) {
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public boolean isOpen() {
        return mUVCCamera != null;
    }

    public UVCCamera getUVCCamera() {
        return mUVCCamera;
    }

    public String getCameraName() {
        return mCameraName;
    }

    public boolean isMirror() {
        return isMirror;
    }

    public void setMirror(boolean mirror) {
        isMirror = mirror;
    }

    public FpsCounter getFpsCounter() {
        return mFpsCounter;
    }

    /**
     * 打开摄像头
     *
     * @param ctrlBlock
     */
    public void openCamera(final USBMonitor.UsbControlBlock ctrlBlock) {
        if (mUVCCamera != null) {
            Log.e(TAG, mCameraName + " camera has opened...");
            return;
        }
        if (isOpening) {
            Log.e(TAG, mCameraName + " camera isOpening...");
            return;
        }
        isOpening = true;
        new Thread(() -> {
            synchronized (mSync) {
                if (mUVCCamera != null) {
                    Log.e(TAG, mCameraName + " camera has opened...");
                    return;
                }
                final UVCCamera camera = new UVCCamera();
                mStartOpenMillis = System.currentTimeMillis();
                int code = camera.openWithError(ctrlBlock);
                if (code < 0) {
                    callOnOpenError(code, 0);
                    return;
                }
                Log.i(TAG, mCameraName + " camera.open: [" + numToHex16(ctrlBlock.getVenderId()) + ":" + numToHex16(ctrlBlock.getProductId()) + "]");
                if (mUVCCameraCallback != null && mUVCCameraCallback instanceof UVCCameraCallbackX) {
                    ((UVCCameraCallbackX) mUVCCameraCallback).afterOpen(camera);
                }

                mUVCCamera = camera;
                startPreview();
            }
        }).start();
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (mUVCCamera == null) {
            return;
        }
        synchronized (mSync) {
            if (mUVCCameraCallback != null && mUVCCameraCallback instanceof UVCCameraCallbackX) {
                ((UVCCameraCallbackX) mUVCCameraCallback).beforePreview(mUVCCamera);
            }

            try {
                Log.i(TAG, "preview [" + mPreviewWidth + ", " + mPreviewHeight + "]");
                mUVCCamera.setPreviewSize(mPreviewWidth, mPreviewHeight, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);

                if (mTextureView == null) {
                    callOnOpened(0);
                } else {
                    SurfaceTexture st = mTextureView.getSurfaceTexture();
                    Log.i(TAG, "mTextureView.getSurfaceTexture() = " + st);
                    if (st == null) {
                        Log.e(TAG, mCameraName + " st is null, wait 500ms...");
                        sleep(500);
                        st = mTextureView.getSurfaceTexture();
                        Log.i(TAG, "st = " + st);
                    }

                    if (st != null) {
                        mSurface = new Surface(st);
                        callOnOpened(0);
                    } else {
                        mUVCCamera.destroy();
                        mUVCCamera = null;
                        callOnOpenError(-400, 0);
                        return;
                    }
                }
            } catch (final IllegalArgumentException e) {
                Log.i(TAG, e.getMessage());
                // fallback to YUV mode
                try {
                    mPreviewWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
                    mPreviewHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
                    mUVCCamera.setPreviewSize(mPreviewWidth, mPreviewHeight, UVCCamera.DEFAULT_PREVIEW_MODE, UVCCamera.DEFAULT_BANDWIDTH);
                } catch (final IllegalArgumentException e1) {
                    mUVCCamera.destroy();
                    callOnOpenError(-200, 0);
                    return;
                }
            }


            if (mUVCCamera != null) {
                mUVCCamera.setPreviewDisplay(mSurface);
                mUVCCamera.setFrameCallback(mFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
                mUVCCamera.startPreview();
                if (mUVCCameraCallback != null && mUVCCameraCallback instanceof UVCCameraCallbackX) {
                    ((UVCCameraCallbackX) mUVCCameraCallback).afterPreview(mUVCCamera);
                }
            }
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mUVCCamera == null) {
            return;
        }
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
                isPreview = false;
                if (mGLSurfaceView != null) {
                    byte[] blackYuv = new byte[mPreviewWidth * mPreviewHeight * 3 / 2];
                    Arrays.fill(blackYuv, mPreviewWidth * mPreviewHeight, blackYuv.length, (byte) 0x80);
                    mGLSurfaceView.feedData(blackYuv, mPreviewWidth, mPreviewHeight, UVCCamera.PIXEL_FORMAT_NV21);
                }
            }
        }
    }

    /**
     * 关闭摄像头
     */
    public void closeCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                stopPreview();
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                }
                isPreview = false;
                mUVCCamera = null;
                mCameraBytes = null;
                mPreviewCallback = null;
                Log.e(TAG, mCameraName + " camera close...");
            }
            if (mSurface != null) {
                mSurface.release();
                mSurface = null;
            }
        }
    }

    /**
     * 设置预览view
     *
     * @param textureView
     */
    public void setDisplayView(TextureView textureView) {
        if (textureView != null) {
            mTextureView = textureView;
        }
    }

    public void setDisplayView(UVCGLSurfaceView glSurfaceView) {
        if (glSurfaceView != null) {
            mGLSurfaceView = glSurfaceView;
        }
    }

    private void callOnOpened(long delayMillis) {
        isOpening = false;
        if (mUVCCameraCallback != null) {
            mUIHandler.postDelayed(() -> mUVCCameraCallback.onOpened(), delayMillis);
        }
    }

    private void callOnPreview() {
        if (mUVCCameraCallback != null) {
            mUIHandler.postDelayed(() -> mUVCCameraCallback.onPreview(), 0);
        }
    }

    private void callOnOpenError(int error, long delayMillis) {
        isOpening = false;
        if (mUVCCameraCallback != null) {
            mUIHandler.postDelayed(() -> mUVCCameraCallback.onOpenError(error), delayMillis);
        }
    }

    public interface UVCCameraCallback {

        void onOpened();

        void onPreview();

        void onOpenError(int error);
    }

    public abstract static class UVCCameraCallbackX implements UVCCameraCallback {
        public abstract void afterOpen(UVCCamera uvcCamera);

        public void beforePreview(UVCCamera uvcCamera) {
        }

        public void afterPreview(UVCCamera camera) {
        }
    }

    public interface PreviewCallback {
        void onPreviewFrame(byte[] data, int width, int height);
    }

    protected void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String numToHex16(int b) {
        return String.format("%04x", b);
    }

    public static byte[] mirrorNV21Fast(byte[] data, int width, int height) {
        int i;
        int j;
        for (i = 0; i < height; ++i) {
            for (j = 0; j < width / 2; ++j) {
                byte tempY = data[i * width + j];
                data[i * width + j] = data[(i + 1) * width - 1 - j];
                data[(i + 1) * width - 1 - j] = tempY;
            }
        }

        for (i = height; i < height * 3 / 2; ++i) {
            for (j = 0; j < width / 4; ++j) {
                byte tempV = data[i * width + j * 2];
                byte tempU = data[i * width + j * 2 + 1];
                data[i * width + j * 2] = data[(i + 1) * width - j * 2 - 2];
                data[i * width + j * 2 + 1] = data[(i + 1) * width - j * 2 - 1];
                data[(i + 1) * width - j * 2 - 2] = tempV;
                data[(i + 1) * width - j * 2 - 1] = tempU;
            }
        }

        return data;
    }
}
