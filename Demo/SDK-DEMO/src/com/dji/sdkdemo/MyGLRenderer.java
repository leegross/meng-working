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
    private final float[] mCamera = new float[16];

    private float camera_theta; // rotation of camera in vertical direction
    private float camera_phi; // rotation of camera in horizontal direction
    private float projector_theta;
    private float projector_phi;

    private boolean camera_theta_initialized;
    private boolean camera_phi_initialized;

    private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private Hemisphere mHemisphere;

    private float camera_zoom_scale;

    private float[] cameraTranslationV;
    private float[] projectorTranslationV;

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

        camera_theta = 0;
        camera_phi = 0;
        projector_theta = 0;
        projector_phi = 0;
        camera_theta_initialized = false;
        camera_phi_initialized = false;

        camera_zoom_scale = 0;

        cameraTranslationV = new float[]{0, Constants.STARTING_ALTITUDE, 0, 0};
        projectorTranslationV = new float[]{0, Constants.STARTING_ALTITUDE, 0, 0};
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] current_camera_translation = getCameraTranslationAfterTempZoom();

        transformMatrix(mMVPMatrix, camera_theta, camera_phi, current_camera_translation); // transform camera matrix
        transformMatrix(mMVPProjectorMatrix, projector_theta, projector_phi, projectorTranslationV); // transform projector matrix

        mSurfaceTexture.updateTexImage();

        // Draw hemisphere
        mHemisphere.render(mMVPMatrix, mMVPProjectorMatrix);
    }

    private void transformMatrix(float[] m, float theta, float phi, float[] translationV){
        // Set the projector position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0, 0, 0, //eye
                0, 0, 10, //center
                0, 1, 0); // up

        Matrix.invertM(mCamera, 0, mViewMatrix, 0);
        Matrix.rotateM(mCamera, 0, phi % 360, 0, 1, 0); // rotate in horizontal direction about y axis
        Matrix.rotateM(mCamera, 0, theta%360, 1, 0, 0); // rotate in vertical direction about x direction
        Matrix.translateM(mCamera, 0, translationV[0], translationV[1], translationV[2]);
        Matrix.invertM(mViewMatrix, 0, mCamera, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(m, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    private float[] pendingZoom(float theta, float phi, float zoom_scale){
        //compute rotation matrices
        float[] rotateThetaMatrix = new float[16];
        float[] rotatePhiMatrix = new float[16];

        Matrix.setRotateM(rotateThetaMatrix, 0, theta, 1, 0, 0);
        Matrix.setRotateM(rotatePhiMatrix, 0, phi, 0, 1, 0);

        float[] rotationMatrix = new float[16];
        Matrix.multiplyMM(rotationMatrix, 0, rotatePhiMatrix, 0, rotateThetaMatrix, 0);

        float[] translateV = new float[4];
        Matrix.multiplyMV(translateV, 0, rotationMatrix, 0, new float[]{0, 0, zoom_scale, 1}, 0);
        return translateV;
    }

    public float[] getCameraTranslationAfterTempZoom(){
        float[] translation = addArrays(cameraTranslationV, pendingZoom(camera_theta, camera_phi, camera_zoom_scale));

        // bound translation
        // if we're beyond bounds for any of the dimensions, don't move
//        float alt = translation[1];
//        float x = translation[0];
//        float z = translation[2];
//        if (alt < Constants.MIN_ALTITUDE || alt > Constants.MAX_ALTITUDE ||
//            x < -Constants.MAX_DIST || x > Constants.MAX_DIST ||
//            z < -Constants.MAX_DIST || z > Constants.MAX_DIST) {
//            return translationV;
//        }

        return translation;
    }

    private float[] addArrays(float[] a1, float[] a2){
        if (a1.length != a2.length) {
            return new float[0];
        }
        float[] sum = new float[a1.length];
        for (int i = 0; i < a1.length; i++) {
            sum[i] = a1[i] + a2[i];
        }
        return sum;
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
        camera_theta -= theta; // vertical directions are inverted
        camera_phi += phi;
    }

    public void updateProjectorRotationAngles(float theta, float phi) {
        projector_theta -=theta;
        projector_phi += phi;
    }

    public void setProjectorRotationAngles(float theta, float phi) {
        projector_theta = theta;
        projector_phi = phi;
    }

    public float getThetaCamera(){
        return camera_theta;
    }

    public float getPhiCamera(){
        return camera_phi;
    }

    public float getThetaProjector(){
        return projector_theta;
    }

    public float getPhiProjector(){
        return projector_phi;
    }

    public void setInitialCameraTheta(float theta){
        camera_theta = theta;
        camera_theta_initialized = true;
    }

    public void setInitialCameraPhi(float phi){
        camera_phi = phi;
        camera_phi_initialized = true;
    }

    public boolean isCameraThetaInitialized(){
        return camera_theta_initialized;
    }

    public boolean isCameraPhiInitailized(){
        return camera_phi_initialized;
    }

    public void updateCurrentCameraZoom(float scale) {
        camera_zoom_scale += scale;
    }

    public float[] getCameraTranslation(){
        return cameraTranslationV;
    }

    public void finalizeCurrentCameraZoom(){
        cameraTranslationV = addArrays(cameraTranslationV, pendingZoom(camera_theta, camera_phi, camera_zoom_scale));
        camera_zoom_scale = 0;
    }

    public void setProjectorTranslationV(float x, float y, float z){
        projectorTranslationV = new float[]{x, y, z, 0};
    }

    public float[] getProjectorTranslation(){
        return projectorTranslationV;
    }
}