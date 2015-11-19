package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.Matrix.perspectiveM;

/**
 * Created by leegross on 9/14/15.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mMVPProjectorMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mProjectorViewMatrix = new float[16];
    private final float[] mCamera = new float[16];
    private final float[] mProjector = new float[16];

    private float theta_camera; // rotation of camera in vertical direction
    private float phi_camera; // rotation of camera in horizontal direction
    private float theta_projector;
    private float phi_projector;

    private boolean theta_camera_initialized;
    private boolean phi_camera_initialized;

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
        mHemisphere = new Hemisphere(mContext);
        mSurfaceTexture = new SurfaceTexture(mHemisphere.getTextureHandle());

        theta_camera = 0;
        phi_camera = 0;
        theta_projector = 0;
        phi_projector = 0;
        theta_camera_initialized = false;
        phi_camera_initialized = false;
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        transformMatrix(mMVPMatrix, theta_camera, phi_camera); // transform camera matrix
        transformMatrix(mMVPProjectorMatrix, theta_projector, phi_projector); // transform projector matrix

        mSurfaceTexture.updateTexImage();

        // Draw hemisphere
        mHemisphere.render(mMVPMatrix, mMVPProjectorMatrix);
    }

    private void transformMatrix(float[] m, float theta, float phi){
        // Set the projector position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0, .5f, 0, //eye
                0, -.5f, 10, //center
                0, 1, 0); // up

        Matrix.invertM(mCamera, 0, mViewMatrix, 0);
        Matrix.rotateM(mCamera, 0, phi%360, 0, 1, 0); // rotate in horizontal direction about y axis
        Matrix.rotateM(mCamera, 0, theta%360, 1, 0, 0); // rotate in vertical direction about x direction
        Matrix.invertM(mViewMatrix, 0, mCamera, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(m, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        float ratio = Constants.ASPECT_RATIO;
        float near = Constants.FRUST_NEAR;
        float fov_y = Constants.HORIZONTAL_FOV/ratio;
        perspectiveM(mProjectionMatrix, 0, fov_y, ratio, near, 1000.0f);
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

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void updateCameraRotationAngles(float theta, float phi) {
        theta_camera -= theta; // vertical directions are inverted
        phi_camera += phi;
    }

    public void updateProjectorRotationAngles(float theta, float phi) {
        theta_projector -=theta;
        phi_projector += phi;
    }

    public void setProjectorRotationAngles(float theta, float phi) {
        theta_projector = theta;
        phi_projector = phi;
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

    public void setInitialCameraTheta(float theta){
        theta_camera = theta;
        theta_camera_initialized = true;
    }

    public void setInitialCameraPhi(float phi){
        phi_camera = phi;
        phi_camera_initialized = false;
    }

    public boolean isCameraThetaInitialized(){
        return theta_camera_initialized;
    }

    public boolean isCameraPhiInitailized(){
        return phi_camera_initialized;
    }
}