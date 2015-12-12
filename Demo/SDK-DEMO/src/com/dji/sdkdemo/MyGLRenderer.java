package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.dji.sdkdemo.util.OperationsHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.Matrix.perspectiveM;
import static com.dji.sdkdemo.Constants.*;
import static com.dji.sdkdemo.Constants.FRUST_NEAR;
import static com.dji.sdkdemo.Constants.HORIZONTAL_FOV;
import static com.dji.sdkdemo.util.OperationsHelper.addArrays;
import static com.dji.sdkdemo.util.OperationsHelper.floatEquals;
import static com.dji.sdkdemo.util.OperationsHelper.*;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.toDegrees;

/**
 * Created by leegross on 9/14/15.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private float[] mMVPMatrix = new float[16];
    private float[] mMVPProjectorMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];

    private float camera_theta; // rotation of camera in vertical direction
    private float camera_phi; // rotation of camera in horizontal direction
    private float projector_theta;
    private float projector_phi;

    private boolean camera_theta_initialized;
    private boolean camera_phi_initialized;

    private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private Hemisphere mHemisphere;

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

        camera_theta = -45;
        camera_phi = 0;
        projector_theta = -45;
        projector_phi = 0;
        camera_theta_initialized = false;
        camera_phi_initialized = false;

        cameraTranslationV = new float[]{0, STARTING_ALTITUDE, 0, 0};
        projectorTranslationV = new float[]{0, STARTING_ALTITUDE, 0, 0};
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mMVPMatrix = transformMatrix(camera_theta, camera_phi, cameraTranslationV); // transform camera matrix
        mMVPProjectorMatrix = transformMatrix(projector_theta, projector_phi, projectorTranslationV); // transform projector matrix

        mSurfaceTexture.updateTexImage();

        // Draw hemisphere
        mHemisphere.render(mMVPMatrix, mMVPProjectorMatrix);
    }

    private float[] transformMatrix(float theta, float phi, float[] translationV){
        float[] mCamera = new float[16];

        // Set the projector position (View matrix)
        Matrix.setIdentityM(mCamera, 0);

        Matrix.translateM(mCamera, 0, translationV[0], translationV[1], translationV[2]);

        float[] rotationMatrix = getCameraRotationMatrix(theta, phi);
        mCamera = multiplyMM(mCamera, rotationMatrix);

        float[] mViewMatrix = getInverse(mCamera);

        // Calculate the projection and view transformation
        return OperationsHelper.multiplyMM(mProjectionMatrix, mViewMatrix);
    }

    public float[] getCameraRotationMatrix(float theta, float phi){
        float[] rotationM = new float[16];
        Matrix.setIdentityM(rotationM, 0);

        Matrix.rotateM(rotationM, 0, phi, 0, 1, 0);
        Matrix.rotateM(rotationM, 0, theta, 1, 0, 0);

        return rotationM;
    }

    public void updateCameraZoom(float zoom_scale){
        //compute rotation matrices
        float[] rotationMatrix = getCameraRotationMatrixForZoom(camera_theta, camera_phi);

        float[] translateV = new float[4];
        Matrix.multiplyMV(translateV, 0, rotationMatrix, 0, new float[]{0, 0, zoom_scale, 1}, 0);
        if (!passPendingBoundsCheck(translateV)) {
         return;
        }
        float[] temp = addArrays(cameraTranslationV, translateV);

//        cameraTranslationV = temp;
        cameraTranslationV = clipTranslationVector(temp);
    }

    private boolean passPendingBoundsCheck(float[] translateDeltaV) {
        if (( floatEquals(cameraTranslationV[1], MIN_ALTITUDE) && cameraTranslationV[1] + translateDeltaV[1] < MIN_ALTITUDE) ||
            ( floatEquals(cameraTranslationV[0], -MAX_DIST) && cameraTranslationV[0] + translateDeltaV[0] < -MAX_DIST) ||
            ( floatEquals(cameraTranslationV[0], MAX_DIST) && cameraTranslationV[0] + translateDeltaV[0] > MAX_DIST) ||
            ( floatEquals(cameraTranslationV[2], -MAX_DIST) && cameraTranslationV[2] + translateDeltaV[2] < -MAX_DIST) ||
            ( floatEquals(cameraTranslationV[2], MAX_DIST) && cameraTranslationV[2] + translateDeltaV[2] > MAX_DIST)){
            return false;
        }
        return true;
    }

    private float[] clipTranslationVector(float[] temp){
        float clip_ratio;
        if (temp[1] < MIN_ALTITUDE) {
            clip_ratio = MIN_ALTITUDE/temp[1];
            temp[0] = clip_ratio * temp[0];
            temp[1] = MIN_ALTITUDE;
            temp[2] = clip_ratio * temp[2];
        }

        if (temp[0] < -MAX_DIST) {
            clip_ratio = -MAX_DIST/temp[0];
            temp[0] =  -MAX_DIST;
            temp[1] = clip_ratio * temp[1];
            temp[2] = clip_ratio * temp[2];
        } else if (temp[0] > MAX_DIST) {
            clip_ratio = MAX_DIST/temp[0];
            temp[0] = MAX_DIST;
            temp[1] = clip_ratio * temp[1];
            temp[2] = clip_ratio * temp[2];
        }

        if (temp[2] < -MAX_DIST) {
            clip_ratio = -MAX_DIST/temp[2];
            temp[0] = clip_ratio * temp[0];
            temp[1] = clip_ratio * temp[1];
            temp[2] = -MAX_DIST;
        } else if (temp[2] > MAX_DIST) {
            clip_ratio = MAX_DIST/temp[2];
            temp[0] = clip_ratio * temp[0];
            temp[1] = clip_ratio * temp[1];
            temp[2] = MAX_DIST;
        }
        return temp;
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        float ratio = ASPECT_RATIO;
        float near = FRUST_NEAR;
        float fov_y = HORIZONTAL_FOV / ratio;
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

    public float[] getCameraTranslation(){
        return cameraTranslationV;
    }

    public void setProjectorTranslationV(float x, float y, float z){
        projectorTranslationV = new float[]{x, y, z, 0};
    }

    public float[] getProjectorTranslation(){
        return projectorTranslationV;
    }

    public void resetCameraParameters(){
        camera_phi = projector_phi;
        camera_theta = projector_theta;
        System.arraycopy(projectorTranslationV, 0, cameraTranslationV, 0, projectorTranslationV.length);
    }

    public void updateCameraRotation(float p1x, float p1y, float p2x, float p2y, float theta, float phi){
        // get world coordinates from screen coordinates
//        float[] p1_r = screenPointToRelativeWorldDirection(0, SURFACE_VERTICAL_CENTER); // relative direction
//        float[] p2_w = screenPointToWorldDirection(0, SURFACE_VERTICAL_CENTER, theta, phi); // actual direction - given the orientation of the drone
        float[] p1_r = screenPointToRelativeWorldDirection(p1x, p1y); // relative direction
        float[] p2_w = screenPointToWorldDirection(p2x, p2y, theta, phi); // actual direction - given the orientation of the drone

        // change to 3D and normalize p1_r and p2
        p1_r = new float[]{p1_r[0], p1_r[1], p1_r[2]};
        p2_w = new float[]{p2_w[0], p2_w[1], p2_w[2]};
        p1_r = normalizeV(p1_r);
        p2_w = normalizeV(p2_w);

        // get phi
        // project on x and z plane to to find the yaw angle
        float[] p1_xz = new float[]{p1_r[0], 0, p1_r[2]};
        float[] p2_xz = new float[]{p2_w[0], 0, p2_w[2]};
        p1_xz = normalizeV(p1_xz);
        p2_xz = normalizeV(p2_xz);
        float phi1 = (float) toDegrees(atan2(p1_xz[0], -p1_xz[2]));
        float phi2 = (float) toDegrees(atan2(p2_xz[0], -p2_xz[2]));
        float new_phi = phi2 - phi1;

        // apply new phi to p1
        p1_r = new float[]{p1_r[0], p1_r[1], p1_r[2], 1}; // switch to homogeneous coords
        float[] p1_ = multiplyMV(getRotationMatrix(-new_phi, new float[]{0, 1, 0}), p1_r);
        p1_ = new float[]{p1_[0], p1_[1], p1_[2]};
        p1_ = normalizeV(p1_);

        // get theta
        float theta1 = (float) toDegrees(atan2(p1_[1], -p1_[2]));
        float theta2 = (float) toDegrees(atan2(p2_w[1], -p2_w[2]));
        float new_theta = theta2 - theta1;

//        float[] p1_yz = new float[]{0, p1_r[1], p1_r[2]};
//        float[] p2_yz = new float[]{0, p2_w[1], p2_w[2]};
//        p1_yz = normalizeV(p1_yz);
//        p2_yz = normalizeV(p2_yz);
//        float theta1 = (float) toDegrees(atan2(p1_yz[1], -p1_yz[2]));//(float) toDegrees(acos(dot(p1_xz, new float[]{0, 0, -1})));
//        float theta2 = (float) toDegrees(atan2(p2_yz[1], -p2_yz[2]));//(float) toDegrees(acos(dot(p2_xz, new float[]{0, 0, -1})));
//        float new_theta = theta2 - theta1;
//
//        // apply new theta to p1
//        p1_r = new float[]{p1_r[0], p1_r[1], p1_r[2], 1}; // switch to homogeneous coords
//        float[] p1_ = multiplyMV(getRotationMatrix(new_theta%360, new float[]{1, 0, 0}), p1_r);
//        p1_ = new float[]{p1_[0], p1_[1], p1_[2]};
//        p1_ = normalizeV(p1_);
//
//        // get theta
//        float phi1 = (float) toDegrees(atan2(p1_[0], -p1_[2]));//(float) toDegrees(acos(dot(p1_, new float[]{0, 0, -1})));
//        float phi2 = (float) toDegrees(atan2(p2_w[0], -p2_w[2]));//(float) toDegrees(acos(dot(p2_w, new float[]{0, 0, -1})));
//        float new_phi = phi2 - phi1;

        camera_phi = new_phi;
        camera_theta = -new_theta;
    }

    // world direction = C * P^-1 * v
    // where C is the camera matrix,
    // P^-1 is the inverse of the projection matrix
    // and v is the scaled screen coordinates vector (x and y range from -1 to 1)
    private float[] screenPointToWorldDirection(float x, float y, float theta, float phi){
        float[] v = {(2.0f * x/GL_SURFACE_WIDTH - 1.0f), -(2.0f * y / GL_SURFACE_HEIGHT - 1.0f), 0, 1};

        float[] mInverseProjectionMatrix = getInverse(mProjectionMatrix);
        float[] world_v = multiplyMV(mInverseProjectionMatrix, v);

        float[] cameraRotationMatrix = getCameraRotationMatrix(theta, phi);
        float[] cameraInvRotationMatrix = getInverse(cameraRotationMatrix);
        world_v = OperationsHelper.multiplyMV(cameraInvRotationMatrix, world_v);

        return world_v;
    }

    // relative world direction = P^-1 * v
    // P^-1 is the inverse of the projection matrix
    // and v is the scaled screen coordinates vector (x and y range from -1 to 1)
    private float[] screenPointToRelativeWorldDirection(float x, float y){
        float[] v = {(2.0f * x/GL_SURFACE_WIDTH - 1.0f), -(2.0f * y/GL_SURFACE_HEIGHT - 1.0f), 0, 1};
        float[] proj_inv = getInverse(mProjectionMatrix);

        float[] world_v = multiplyMV(proj_inv, v);
        return world_v;
    }

    private float[] getCameraRotationMatrixForZoom(float theta, float phi){
        float[] rotateThetaMatrix = new float[16];
        float[] rotatePhiMatrix = new float[16];

        Matrix.setRotateM(rotatePhiMatrix, 0, phi%360, 0, 1, 0);
        Matrix.setRotateM(rotateThetaMatrix, 0, theta%360, 1, 0, 0);

        float[] rotationMatrix = new float[16];
        Matrix.multiplyMM(rotationMatrix, 0, rotatePhiMatrix, 0, rotateThetaMatrix, 0);
        return rotationMatrix;
    }

    public String cameraInfoToString(){
        final StringBuffer sb = new StringBuffer();

        sb.append("camera_x: ").append(cameraTranslationV[0]).append("\n");
        sb.append("camera_y: ").append(cameraTranslationV[1]).append("\n");
        sb.append("camera_z: ").append(cameraTranslationV[2]).append("\n");
        sb.append("camera pitch: ").append(camera_theta).append("\n");
        sb.append("camera yaw: ").append(camera_phi).append("\n");

        return sb.toString();
    }

    public String projectorInfoToString(){
        final StringBuffer sb = new StringBuffer();

        sb.append("projector_x: ").append(projectorTranslationV[0]).append("\n");
        sb.append("projector_y: ").append(projectorTranslationV[1]).append("\n");
        sb.append("projector_z: ").append(projectorTranslationV[2]).append("\n");
        sb.append("projector pitch: ").append(projector_theta).append("\n");
        sb.append("projector yaw: ").append(projector_phi).append("\n");

        return sb.toString();
    }
}