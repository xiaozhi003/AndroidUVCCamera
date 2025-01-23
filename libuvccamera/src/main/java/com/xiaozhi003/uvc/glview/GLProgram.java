package com.xiaozhi003.uvc.glview;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * 创建者     likunlun
 * 创建时间   2019/3/26 17:23
 * 描述	      desc
 */
public class GLProgram {

    private static final String TAG = GLProgram.class.getSimpleName();
    /**
     * 顶点着色器程序
     * vertex shader在每个顶点上都执行一次，通过不同世界的坐标系转化定位顶点的最终位置。
     * 它可以传递数据给fragment shader，如纹理坐标、顶点坐标，变换矩阵等
     */
    public static String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 texCoord;" +
                    "varying vec2 tc;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  tc = texCoord;" +
                    "}";

//    public static float squareVertices[] = {  //rotate 90
//            1.0f,  1.0f,//rt
//            1.0f, -1.0f,//rb
//            -1.0f,  1.0f,//lt
//            -1.0f, -1.0f,//lb
//    };

//        public static float squareVertices[] = {  //rotate 270
//            -1.0f,  1.0f,//rt
//            -1.0f, -1.0f,//rb
//            1.0f,  1.0f,//lt
//            1.0f, -1.0f,//lb
//    };
    /**
     * 片段着色器程序
     * fragment shader在每个像素上都会执行一次，通过插值确定像素的最终显示颜色
     */
    public static String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D samplerY;" +
                    "uniform sampler2D samplerU;" +
                    "uniform sampler2D samplerV;" +
                    "uniform sampler2D samplerUV;" +
                    "uniform int yuvType;" +
                    "varying vec2 tc;" +
                    "void main() {" +
                    "  vec4 c = vec4((texture2D(samplerY, tc).r - 16./255.) * 1.164);" +
                    "  vec4 U; vec4 V;" +
                    "  if (yuvType == 0){" +
                    "    U = vec4(texture2D(samplerU, tc).r - 128./255.);" +
                    "    V = vec4(texture2D(samplerV, tc).r - 128./255.);" +
                    "  } else if (yuvType == 1){" +
                    "    U = vec4(texture2D(samplerUV, tc).r - 128./255.);" +
                    "    V = vec4(texture2D(samplerUV, tc).a - 128./255.);" +
                    "  } else {" +
                    "    U = vec4(texture2D(samplerUV, tc).a - 128./255.);" +
                    "    V = vec4(texture2D(samplerUV, tc).r - 128./255.);" +
                    "  } " +
                    "  c += V * vec4(1.596, -0.813, 0, 0);" +
                    "  c += U * vec4(0, -0.392, 2.017, 0);" +
                    "  c.a = 1.0;" +
                    "  gl_FragColor = c;" +
                    "}";
    static float[] squareVertices = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,}; // fullscreen
    private int mProgram;
    private IntBuffer mPlanarTextureHandles = IntBuffer.wrap(new int[3]);
    private int[] mSampleHandle = new int[3];
    // handles
    private int mPositionHandle = -1;
    private int mCoordHandle = -1;
    private int mVPMatrixHandle = -1;
    // vertices buffer
    private ByteBuffer mVertexBuffer = null;
    private ByteBuffer mCoordBuffer = null;
    // whole-texture
    private float[] mCoordVertices = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};

    public GLProgram() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        Log.d(TAG, "vertexShader = $vertexShader \n fragmentShader = $fragmentShader");

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();
        if (mProgram != 0) {
            checkGlError("glCreateProgram");
            // add the vertex shader to program
            GLES20.glAttachShader(mProgram, vertexShader);

            // add the fragment shader to program
            GLES20.glAttachShader(mProgram, fragmentShader);

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(mProgram);
        }

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.w(TAG, "Could not link program: ${GLES20.glGetProgramInfoLog(mProgram)}");
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }

        Log.d(TAG, "mProgram = $mProgram");

        checkGlError("glCreateProgram");

        // 生成纹理句柄
        GLES20.glGenTextures(3, mPlanarTextureHandles);

        checkGlError("glGenTextures");
    }

    /**
     * create shader with given source.
     */
    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public void drawTexture(float[] mvpMatrix, int type) {

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
        /*
         * get handle for "vPosition" and "a_texCoord"
         */
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        if (mPositionHandle == -1) {
            throw new RuntimeException("Could not get attribute location for vPosition");
        }
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 8, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // 传纹理坐标给fragment shader
        mCoordHandle = GLES20.glGetAttribLocation(mProgram, "texCoord");
        if (mCoordHandle == -1) {
            throw new RuntimeException("Could not get attribute location for a_texCoord");
        }
        GLES20.glVertexAttribPointer(mCoordHandle, 2, GLES20.GL_FLOAT, false, 8, mCoordBuffer);
        GLES20.glEnableVertexAttribArray(mCoordHandle);

        // get handle to shape's transformation matrix
        mVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mVPMatrixHandle, 1, false, mvpMatrix, 0);

        //传纹理的像素格式给fragment shader
        int yuvType = GLES20.glGetUniformLocation(mProgram, "yuvType");
        checkGlError("glGetUniformLocation yuvType");
        GLES20.glUniform1i(yuvType, type);

        //type: 0是I420, 1是NV12
        int planarCount = 0;
        if (type == 0) {
            //I420有3个平面
            planarCount = 3;
            mSampleHandle[0] = GLES20.glGetUniformLocation(mProgram, "samplerY");
            mSampleHandle[1] = GLES20.glGetUniformLocation(mProgram, "samplerU");
            mSampleHandle[2] = GLES20.glGetUniformLocation(mProgram, "samplerV");
        } else {
            //NV12、NV21有两个平面
            planarCount = 2;
            mSampleHandle[0] = GLES20.glGetUniformLocation(mProgram, "samplerY");
            mSampleHandle[1] = GLES20.glGetUniformLocation(mProgram, "samplerUV");
        }
        for (int i = 0; i < planarCount; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPlanarTextureHandles.get(i));
            GLES20.glUniform1i(mSampleHandle[i], i);
        }

        // 调用这个函数后，vertex shader先在每个顶点执行一次，之后fragment shader在每个像素执行一次，
        // 绘制后的图像存储在render buffer中
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFinish();

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mCoordHandle);
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量分开存储的（I420）
     *
     * @param yPlane YUV数据的Y分量
     * @param uPlane YUV数据的U分量
     * @param vPlane YUV数据的V分量
     * @param width  YUV图片宽度
     * @param height YUV图片高度
     */
    public void feedTextureWithImageData(ByteBuffer yPlane, ByteBuffer uPlane, ByteBuffer vPlane, int width, int height) {
        //根据YUV编码的特点，获得不同平面的基址
        textureYUV(yPlane, width, height, 0);
        textureYUV(uPlane, width / 2, height / 2, 1);
        textureYUV(vPlane, width / 2, height / 2, 2);
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量交叉存储的（NV12、NV21）
     *
     * @param yPlane  YUV数据的Y分量
     * @param uvPlane YUV数据的UV分量
     * @param width   YUV图片宽度
     * @param height  YUV图片高度
     */
    public void feedTextureWithImageData(ByteBuffer yPlane, ByteBuffer uvPlane, int width, int height) {
        //根据YUV编码的特点，获得不同平面的基址
        textureYUV(yPlane, width, height, 0);
        textureNV12(uvPlane, width / 2, height / 2, 1);
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量分开存储的（I420）
     *
     * @param imageData YUV数据的Y/U/V分量
     * @param width     YUV图片宽度
     * @param height    YUV图片高度
     */
    private void textureYUV(ByteBuffer imageData, int width, int height, int index) {
        // 将纹理对象绑定到纹理目标
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPlanarTextureHandles.get(index));
        // 设置放大和缩小时，纹理的过滤选项为：线性过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 设置纹理X,Y轴的纹理环绕选项为：边缘像素延伸
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        // 加载图像数据到纹理，GL_LUMINANCE指明了图像数据的像素格式为只有亮度，虽然第三个和第七个参数都使用了GL_LUMINANCE，
        // 但意义是不一样的，前者指明了纹理对象的颜色分量成分，后者指明了图像数据的像素格式
        // 获得纹理对象后，其每个像素的r,g,b,a值都为相同，为加载图像的像素亮度，在这里就是YUV某一平面的分量值
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE, width, height, 0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE, imageData
        );
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量交叉存储的（NV12、NV21）
     *
     * @param imageData YUV数据的UV分量
     * @param width     YUV图片宽度
     * @param height    YUV图片高度
     */
    private void textureNV12(ByteBuffer imageData, int width, int height, int index) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mPlanarTextureHandles.get(index));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_LUMINANCE_ALPHA, width, height, 0,
                GLES20.GL_LUMINANCE_ALPHA,
                GLES20.GL_UNSIGNED_BYTE, imageData
        );
    }

    /**
     * 创建两个缓冲区用于保存顶点 -> 屏幕顶点和纹理顶点
     *
     * @param vert 屏幕顶点数据
     */
    public void createBuffers(float[] vert) {
        mVertexBuffer = ByteBuffer.allocateDirect(vert.length * 4);
        mVertexBuffer.order(ByteOrder.nativeOrder());
        mVertexBuffer.asFloatBuffer().put(vert);
        mVertexBuffer.position(0);

        if (mCoordBuffer == null) {
            mCoordBuffer = ByteBuffer.allocateDirect(mCoordVertices.length * 4);
            mCoordBuffer.order(ByteOrder.nativeOrder());
            mCoordBuffer.asFloatBuffer().put(mCoordVertices);
            mCoordBuffer.position(0);
        }

//        Log.d(TAG, "createBuffers vertice_buffer $mVertexBuffer  coord_buffer $mCoordBuffer");
    }

    /**
     * 检查GL操作是否有error
     *
     * @param op 当前检查前所做的操作
     */
    private void checkGlError(String op) {
        int error = GLES20.glGetError();
        while (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "***** $op: glError $error");
            error = GLES20.glGetError();
        }
    }
}
