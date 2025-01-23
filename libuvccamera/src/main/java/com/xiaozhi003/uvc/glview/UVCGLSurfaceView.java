package com.xiaozhi003.uvc.glview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.xiaozhi003.uvc.usb.UVCCamera;
import com.xiaozhi003.uvc.widget.UVCPreview;

public class UVCGLSurfaceView extends android.opengl.GLSurfaceView implements UVCPreview {

    private static final String TAG = UVCGLSurfaceView.class.getSimpleName();

    private GLRenderer renderer;

    private boolean isMirror = false;

    private double mRequestedAspect = -1.0;

    public UVCGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public UVCGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        renderer = new GLRenderer();

        // Set the Renderer for drawing on the UVCGLSurfaceView
        setRenderer(renderer);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Render the view only when there is a change in the drawing data
        setRenderMode(android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * 设置渲染的YUV数据的宽高
     *
     * @param width  宽度
     * @param height 高度
     */
    public void setYuvDataSize(int width, int height) {
        Log.d(TAG, "setYuvDataSize");
        renderer.setYuvDataSize(width, height);
    }

    /**
     * 填充预览YUV格式数据
     *
     * @param yuvData yuv格式的数据
     * @param type    YUV数据的格式 0 -> I420  1 -> NV12  2 -> NV21
     */
    public void feedData(byte[] yuvData, int width, int height, int type) {
        if (yuvData == null) {
            return;
        }
        int transType = 0;
        switch (type) {
            case UVCCamera.PIXEL_FORMAT_I420:
                transType = 0;
                break;
            case UVCCamera.PIXEL_FORMAT_NV21:
                transType = 2;
                break;
            case UVCCamera.PIXEL_FORMAT_NV12:
                transType = 1;
                break;
            default:
                break;
        }

        renderer.feedData(yuvData, width, height, transType);
        // 请求渲染新的YUV数据
        requestRender();
    }

    /**
     * 填充预览YUV格式数据
     *
     * @param yuvData yuv格式的数据
     * @param type    YUV数据的格式 0 -> I420  1 -> NV12  2 -> NV21
     */
    public void feedData(byte[] yuvData, int type) {
        if (yuvData == null) {
            return;
        }
        renderer.feedData(yuvData, type);
        // 请求渲染新的YUV数据
        requestRender();
    }

    @Override
    public void setMirror(boolean isMirror) {
        this.isMirror = isMirror;
        renderer.setMirror(isMirror);
    }

    @Override
    public boolean isMirror() {
        return isMirror;
    }

    @Override
    public void rotate(int angle) {
        if (renderer != null) {
            renderer.rotate(angle);
        }
    }

    @Override
    public void setAspectRatio(final int width, final int height) {
        setAspectRatio(width / (double) height);
    }

    @Override
    public double getAspectRatio() {
        return mRequestedAspect;
    }

    @Override
    public void setAspectRatio(final double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mRequestedAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            final int horizPadding = getPaddingLeft() + getPaddingRight();
            final int vertPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horizPadding;
            initialHeight -= vertPadding;

            final double viewAspectRatio = (double) initialWidth / initialHeight;
            final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) > 0.01) {
                if (aspectDiff > 0) {
                    // width priority decision
                    initialHeight = (int) (initialWidth / mRequestedAspect);
                } else {
                    // height priority decision
                    initialWidth = (int) (initialHeight * mRequestedAspect);
                }
                initialWidth += horizPadding;
                initialHeight += vertPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
