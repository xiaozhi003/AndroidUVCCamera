package com.xiaozhi003.uvc.glview;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = GLRenderer.class.getSimpleName();
    private GLProgram mProgram;
    // GLSurfaceView宽度
    private int mScreenWidth = 0;
    // GLSurfaceView高度
    private int mScreenHeight = 0;
    // 预览YUV数据宽度
    private int mVideoWidth = 0;
    // 预览YUV数据高度
    private int mVideoHeight = 0;
    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private float[] vPMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    // y分量数据
    private ByteBuffer y = ByteBuffer.allocate(0);
    // u分量数据
    private ByteBuffer u = ByteBuffer.allocate(0);
    // v分量数据
    private ByteBuffer v = ByteBuffer.allocate(0);
    // uv分量数据
    private ByteBuffer uv = ByteBuffer.allocate(0);
    // YUV数据格式 0 -> I420  1 -> NV12  2 -> NV21
    private int type = 0;
    // 标识GLSurfaceView是否准备好
    private boolean hasVisibility = false;
    private boolean isMirror = false;
    private int mAngle = 0;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 配置OpenGL ES 环境
        mProgram = new GLProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

//        mScreenWidth = 640;
//        mScreenHeight = 480;

        mScreenWidth = width;
        mScreenHeight = height;
        float ratio = 1f * width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        if (isMirror) {
            Matrix.frustumM(projectionMatrix, 0, -1, 1, -1, 1, 3f, 7f);
        } else {
            Matrix.frustumM(projectionMatrix, 0, 1, -1, -1, 1, 3f, 7f);
        }

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            createBuffers(mVideoWidth, mVideoHeight);
        }
        hasVisibility = true;
        Log.d(TAG, "ratio = " + ratio);
        Log.d(TAG, "screenSize [" + mScreenWidth + ", " + mScreenHeight + "]");
        Log.d(TAG, "onSurfaceChanged [" + mVideoWidth + ", " + mVideoHeight + "]");
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        synchronized (this) {
            if (y.capacity() > 0) {
                y.position(0);
                if (type == 0) {
                    u.position(0);
                    v.position(0);
                    mProgram.feedTextureWithImageData(y, u, v, mVideoWidth, mVideoHeight);
                } else {
                    uv.position(0);
                    mProgram.feedTextureWithImageData(y, uv, mVideoWidth, mVideoHeight);
                }
                // Redraw background color
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                /**
                 * 参考 https://blog.csdn.net/jamesshaoya/article/details/54342241 地址设置
                 */
                // Set the camera position (View matrix)
                Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0.0f);

                // Calculate the projection and view transformation
                Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

                try {
                    long start = System.currentTimeMillis();
                    mProgram.drawTexture(vPMatrix, type);
//                    Log.i(TAG, "drawTexture 耗时：" + (System.currentTimeMillis() - start) + "ms");
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        }
    }

    /**
     * 设置渲染的YUV数据的宽高
     *
     * @param width  宽度
     * @param height 高度
     */
    public void setYuvDataSize(int width, int height) {
        if (width > 0 && height > 0) {
            // 调整比例
            createBuffers(width, height);

            // 初始化容器
            if (width != mVideoWidth || height != mVideoHeight) {
                this.mVideoWidth = width;
                this.mVideoHeight = height;
                int yarraySize = width * height;
                int uvarraySize = yarraySize / 4;
                synchronized (this) {
                    y = ByteBuffer.allocate(yarraySize);
                    u = ByteBuffer.allocate(uvarraySize);
                    v = ByteBuffer.allocate(uvarraySize);
                    uv = ByteBuffer.allocate(uvarraySize * 2);
                }
            }
        }
    }

    /**
     * 调整渲染纹理的缩放比例
     *
     * @param width  YUV数据宽度
     * @param height YUV数据高度
     */
    private void createBuffers(int width, int height) {
        if (mScreenWidth > 0 && mScreenHeight > 0) {
            float f1 = 1f * mScreenHeight / mScreenWidth;
            float f2 = 1f * height / width;
            if (f1 == f2) {
                mProgram.createBuffers(GLProgram.squareVertices);
            } else if (f1 < f2) {
                float widthScale = f1 / f2;
                mProgram.createBuffers(
                        new float[]{
                                -widthScale,
                                -1.0f,
                                widthScale,
                                -1.0f,
                                -widthScale,
                                1.0f,
                                widthScale,
                                1.0f
                        }
                );

            } else {
                float heightScale = f2 / f1;
                mProgram.createBuffers(
                        new float[]{
                                -1.0f,
                                -heightScale,
                                1.0f,
                                -heightScale,
                                -1.0f,
                                heightScale,
                                1.0f,
                                heightScale
                        }
                );
            }
        }
    }

    public void setMirror(boolean mirror) {
        isMirror = mirror;
        if (isMirror) {
            if (mAngle == 0) {
                Matrix.frustumM(projectionMatrix, 0, -1, 1, -1, 1, 3f, 7f);
            } else if (mAngle == 180) {
                Matrix.frustumM(projectionMatrix, 0, -1, 1, 1, -1, 3f, 7f);
            }
        } else {
            if (mAngle == 0) {
                Matrix.frustumM(projectionMatrix, 0, 1, -1, -1, 1, 3f, 7f);
            } else if (mAngle == 180) {
                Matrix.frustumM(projectionMatrix, 0, 1, -1, 1, -1, 3f, 7f);
            }
        }
    }

    public void rotate(int angle) {
        mAngle = angle;
        if (isMirror) {
            if (mAngle == 0) {
                Matrix.frustumM(projectionMatrix, 0, -1, 1, -1, 1, 3f, 7f);
            } else if (mAngle == 180) {
                Matrix.frustumM(projectionMatrix, 0, -1, 1, 1, -1, 3f, 7f);
            }
        } else {
            if (mAngle == 0) {
                Matrix.frustumM(projectionMatrix, 0, 1, -1, -1, 1, 3f, 7f);
            } else if (mAngle == 180) {
                Matrix.frustumM(projectionMatrix, 0, 1, -1, 1, -1, 3f, 7f);
            }
        }
    }

    /**
     * 预览YUV格式数据
     *
     * @param yuvdata yuv格式的数据
     * @param type    YUV数据的格式 0 -> I420  1 -> NV12  2 -> NV21
     */
    public void feedData(byte[] yuvdata, int width, int height, int type) {
        setYuvDataSize(width, height);

        synchronized (this) {
            if (hasVisibility) {
                this.type = type;
                if (type == 0) {
                    y.clear();
                    u.clear();
                    v.clear();
                    y.put(yuvdata, 0, mVideoWidth * mVideoHeight);
                    u.put(yuvdata, mVideoWidth * mVideoHeight, mVideoWidth * mVideoHeight / 4);
                    v.put(yuvdata, mVideoWidth * mVideoHeight * 5 / 4, mVideoWidth * mVideoHeight / 4);
                } else {
                    y.clear();
                    uv.clear();
                    y.put(yuvdata, 0, mVideoWidth * mVideoHeight);
                    uv.put(yuvdata, mVideoWidth * mVideoHeight, mVideoWidth * mVideoHeight / 2);
                }
            }
        }
    }

    /**
     * 预览YUV格式数据
     *
     * @param yuvdata yuv格式的数据
     * @param type    YUV数据的格式 0 -> I420  1 -> NV12  2 -> NV21
     */
    public void feedData(byte[] yuvdata, int type) {
        synchronized (this) {
            if (hasVisibility) {
                this.type = type;
                if (type == 0) {
                    y.clear();
                    u.clear();
                    v.clear();
                    y.put(yuvdata, 0, mVideoWidth * mVideoHeight);
                    u.put(yuvdata, mVideoWidth * mVideoHeight, mVideoWidth * mVideoHeight / 4);
                    v.put(yuvdata, mVideoWidth * mVideoHeight * 5 / 4, mVideoWidth * mVideoHeight / 4);
                } else {
                    y.clear();
                    uv.clear();
                    y.put(yuvdata, 0, mVideoWidth * mVideoHeight);
                    uv.put(yuvdata, mVideoWidth * mVideoHeight, mVideoWidth * mVideoHeight / 2);
                }
            }
        }
    }

}
