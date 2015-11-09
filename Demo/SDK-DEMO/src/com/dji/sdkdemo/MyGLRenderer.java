package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

/**
 * Created by leegross on 9/14/15.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private Rectangle mRectangle;
    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private float[] mRectangleRotationMatrix = new float[16];
    private final float[] mRectangleProjectionMatrix = new float[16];

    public volatile float[] mCenterVector;
    public volatile float[] mUpVector;
    public volatile float[] mSideVector;


    private SurfaceTexture mSurfaceTexture;
    private Context mContext;

    public MyGLRenderer(Context context) {
        mContext = context;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Enable depth testing
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // initialize a square
        mRectangle = new Rectangle(mContext);
        mSurfaceTexture = new SurfaceTexture(mRectangle.getTextureHandle());

        mCenterVector = new float[]{0f, 0f, 1f, 0};
        mUpVector = new float[]{0f, 1f, 0f, 0};
        mSideVector = new float[]{1f, 0f, 0f, 0};

        Matrix.setIdentityM(mRectangleRotationMatrix, 0);
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0, 0, 0, //eye
                mCenterVector[0], mCenterVector[1], mCenterVector[2], //center
                mUpVector[0], mUpVector[1], mUpVector[2]); // up

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        //mSurfaceTexture.releaseTexImage();
        mSurfaceTexture.updateTexImage();

        // move rectangle based on where the drone is
        // rectangle rotation matrix is modified when the drone moves in MyGLSurfaceView
        Matrix.multiplyMM(mRectangleProjectionMatrix, 0, mMVPMatrix, 0, mRectangleRotationMatrix, 0);

        // Draw triangle
        mRectangle.draw(mRectangleProjectionMatrix);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        float FOV = 111; //field of view in degrees of the frustrum - computed given the FOV of the camera
        double theta = toRadians(FOV/2);
        float near = (float) (ratio/sin(theta));

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, near, 30);
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public float[] getCenterVector() {
        return mCenterVector;
    }

    public void setCenter(float[] centerVector) {
        mCenterVector = centerVector;
    }

    public float[] getUpVector() {
        return mUpVector;
    }

    public void setUpVector(float[] upVector) {
        mUpVector = upVector;
    }

    public void setSideVector(float[] sideVector){
        mSideVector = sideVector;
    }

    public float[] getSideVector() {
        return mSideVector;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public float[] getRectangleRotationMatrix(){
        return mRectangleRotationMatrix;
    }

    public void setRectangleRotationMatrix(float[] newRotationMatrix){
        mRectangleRotationMatrix = newRotationMatrix;
    }
}