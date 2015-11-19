package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by leegross on 9/14/15.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private Rectangle mRectangle;
    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mMVPProjectorMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mProjectorViewMatrix = new float[16];
    private final float[] mCamera = new float[16];
    private final float[] mProjector = new float[16];

    private float[] mRectangleRotationMatrix = new float[16];
    private final float[] mRectangleProjectionMatrix = new float[16];

    public volatile float[] mCenterVector;
    public volatile float[] mUpVector;
    public volatile float[] mSideVector;

    private float theta_camera; // rotation of camera in vertical direction
    private float phi_camera; // rotation of camera in horizontal direction
    private float theta_projector;
    private float phi_projector;


    private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private Hemisphere mHemisphere;

    public MyGLRenderer(Context context) {
        mContext = context;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // initialize a square
        //mRectangle = new Rectangle(mContext);
        mHemisphere = new Hemisphere(mContext);
        //mSurfaceTexture = new SurfaceTexture(mRectangle.getTextureHandle());
        mSurfaceTexture = new SurfaceTexture(mHemisphere.getTextureHandle());

        mCenterVector = new float[]{0f, 0f, 1f, 1f};
        mUpVector = new float[]{0f, 1f, 0f, 1f};
        mSideVector = new float[]{1f, 0f, 0f, 1f};

        Matrix.setIdentityM(mRectangleRotationMatrix, 0);

        theta_camera = 0;
        phi_camera = 0;
        theta_projector = 0;
        phi_projector = 0;
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0, .1f, 0, //eye
                mCenterVector[0], mCenterVector[1], mCenterVector[2], //center
                mUpVector[0], mUpVector[1], mUpVector[2]); // up

        Matrix.invertM(mCamera, 0, mViewMatrix, 0);
        Matrix.rotateM(mCamera, 0, theta_camera, 1, 0, 0); // rotate in vertical direction about x direction
        Matrix.rotateM(mCamera, 0, phi_camera, 0, 1, 0); // rotate in horizontal direction about y axis
        Matrix.invertM(mViewMatrix, 0, mCamera, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        //mSurfaceTexture.releaseTexImage();
        mSurfaceTexture.updateTexImage();

        // move rectangle based on where the drone is
        // rectangle rotation matrix is modified when the drone moves in MyGLSurfaceView
        Matrix.multiplyMM(mRectangleProjectionMatrix, 0, mMVPMatrix, 0, mRectangleRotationMatrix, 0);

        createProjectorMatrix();

        // Draw triangle
        //mRectangle.draw(mRectangleProjectionMatrix);
        mHemisphere.render(mMVPMatrix, mMVPProjectorMatrix);
    }

    private void createProjectorMatrix(){
        // Set the projector position (View matrix)
        Matrix.setLookAtM(mProjectorViewMatrix, 0,
                0, .1f, 0, //eye
                mCenterVector[0], mCenterVector[1], mCenterVector[2], //center
                mUpVector[0], mUpVector[1], mUpVector[2]); // up

        Matrix.invertM(mProjector, 0, mProjectorViewMatrix, 0);
        Matrix.rotateM(mProjector, 0, theta_projector, 1, 0, 0); // rotate in vertical direction about x direction
        Matrix.rotateM(mProjector, 0, phi_projector, 0, 1, 0); // rotate in horizontal direction about y axis
        Matrix.invertM(mProjectorViewMatrix, 0, mProjector, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPProjectorMatrix, 0, mProjectionMatrix, 0, mProjectorViewMatrix, 0);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        float ratio = Constants.ASPECT_RATIO;
        GLES20.glViewport(0, 0, width, height);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        float near = Constants.TABLET_Z;
        frustumFoV(mProjectionMatrix, Constants.HORIZONTAL_FOV, ratio, near, 1000.0f);

    }

    public static void frustumFoV(float[] matrix, float horizontal_fov, float ratio, float near, float far) {
        float right = (float) (near * Math.tan(StrictMath.toRadians(horizontal_fov/2)));
        float left = -right;
        float top = right/ratio;
        float bottom = -top;
        Matrix.frustumM(matrix, 0, left, right, bottom, top, near, far);
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

    public void updateCameraRotationAngles(float theta, float phi) {
        theta_camera -= theta; // vertical directions are inverted
        phi_camera += phi;
    }

    public void updateProjectorRotationAngles(float theta, float phi) {
        theta_projector -=theta;
        phi_camera += phi;
    }

    public float getThetaCamera(){
        return theta_camera;
    }

    public float getPhiCamera(){
        return phi_camera;
    }

    public float getThetaProjector(){
        return theta_projector;
    }

    public float getPhiProjector(){
        return phi_projector;
    }

}