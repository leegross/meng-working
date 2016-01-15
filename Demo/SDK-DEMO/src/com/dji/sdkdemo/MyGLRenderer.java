package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.dji.sdkdemo.util.OperationsHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.Matrix.perspectiveM;
import static android.opengl.Matrix.rotateM;
import static com.dji.sdkdemo.Constants.*;
import static com.dji.sdkdemo.Constants.FRUST_NEAR;
import static com.dji.sdkdemo.Constants.HORIZONTAL_FOV;
import static com.dji.sdkdemo.util.OperationsHelper.addArrays;
import static com.dji.sdkdemo.util.OperationsHelper.floatEquals;
import static com.dji.sdkdemo.util.OperationsHelper.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.pow;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.toDegrees;

/**
 * Created by leegross on 9/14/15.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

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

        camera_theta = 0;//-89.999f;
        camera_phi = 180;
        projector_theta = 0;//-89.999f;
        projector_phi = 180;
        camera_theta_initialized = false;
        camera_phi_initialized = false;

        cameraTranslationV = new float[]{0, STARTING_ALTITUDE, 0, 1};
        projectorTranslationV = new float[]{0, STARTING_ALTITUDE, 0, 1};
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] mMVPMatrix = transformMatrix(camera_theta, camera_phi, cameraTranslationV);
        float[] mMVPProjectorMatrix = transformMatrix(projector_theta, projector_phi, projectorTranslationV);

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
        float[] thetaRotationMatrix = getRotationMatrix(theta, new float[]{1, 0, 0, 0});
        float[] phiRotationMatrix = getRotationMatrix(phi, new float[]{0, 1, 0, 0});
        float[] rotationM = multiplyMM(phiRotationMatrix, thetaRotationMatrix);

        return rotationM;
    }

    public void moveBasedOnCameraZoom(float p1x, float p1y, float p2x, float p2y, float prev_p1x, float prev_p1y, float prev_p2x, float prev_p2y){

        float[] prev_p1 = getWorlPositionRelativeToCamera(prev_p1x, prev_p1y);
        float[] prev_p2 = getWorlPositionRelativeToCamera(prev_p2x, prev_p2y);


        float[] disp_xz = solveFor2DZoomDisplacement(prev_p1[0], -prev_p1[2], prev_p2[0], -prev_p2[2], p1x, p2x, GL_SURFACE_WIDTH, FRUST_NEAR_SCALE_X, 1.0f);
        float[] disp_yz = solveFor2DZoomDisplacement(prev_p1[1], -prev_p1[2], prev_p2[1], -prev_p2[2], p1y, p2y, GL_SURFACE_HEIGHT, FRUST_NEAR_SCALE_Y, -1.0f);

        float disp_z;
        if (abs(disp_xz[1]) < .00000001) disp_z = disp_yz[1];
        else disp_z = disp_xz[1];

        float[] dispV = new float[]{-disp_xz[0], -disp_yz[0], disp_z, 1};

        dispV = boundZoomAtMaxMag(dispV);

        float[] cameraRotationM = getCameraRotationMatrix(camera_theta, camera_phi);
        float[] translateV = multiplyMV(cameraRotationM, dispV);

        if (!passPendingBoundsCheck(translateV)) return;

        float[] temp = addArrays(cameraTranslationV, translateV);

////        cameraTranslationV = temp;
        temp[3] =  1;
        cameraTranslationV = clipTranslationVector(temp);
    }

    private float[] boundZoomAtMaxMag(float[] zoomV){
        float[] result = zoomV.clone();
        // cap zoom at a maximum magnitude
        float[] dispV3D = new float[]{zoomV[0], zoomV[1], zoomV[2]};
        float mag = magnitude(dispV3D);
//        Log.d("zoom", mag + "");
        if (mag > .03){
            float[] new_disp_3d = scaleVtoMag(dispV3D, .03f);
            result = new float[]{new_disp_3d[0], new_disp_3d[1], new_disp_3d[2], 1};
//            Log.d("zoom", "update mag: " + magnitude(new_disp_3d));
        }
        return result;
    }

    private float[] getWorlPositionRelativeToCamera(float x, float y){
        float[] world_p = getWorldPoint(x, y);
        float[] p_relative_to_position = new float[]{world_p[0] - cameraTranslationV[0], world_p[1] - cameraTranslationV[1], world_p[2] - cameraTranslationV[2], 1};

        float[] rotationM = getCameraRotationMatrix(camera_theta, camera_phi);
        float[] rotationInvM = getInverse(rotationM);
        float[] p_relative_to_orientation = multiplyMV(rotationInvM, p_relative_to_position);

        return p_relative_to_orientation;
    }

    private float[] solveFor2DZoomDisplacement(float a1, float a2, float b1, float b2, float p1, float p2, float dimension_size, float frust_near_scale, float sign){
        // scale all points to range between -1 and 1
        float alpha_ = sign * (2.0f * p1/dimension_size - 1.0f) * frust_near_scale;
        float beta_ = sign * (2.0f * p2/dimension_size - 1.0f) * frust_near_scale;

        if (abs(p1 - p2) < 0.0000001) {
            Log.d("zoomError", "impossible case reached");
            float alpha = (a1 / a2) * FRUST_NEAR;
            return new float[]{alpha_ - alpha, 0.0f};
        }

        float a1_ = -alpha_ * (FRUST_NEAR * (-a1 + b1) + beta_ * (a2 - b2))/(FRUST_NEAR * (alpha_ - beta_));
        float a2_ = (FRUST_NEAR * (a1 - b1) + beta_ * (-a2 + b2))/(alpha_ - beta_);

        return new float[]{a1_ - a1, a2_ - a2};
    }

    public float[] getDirectionAnglesOfPoint(float midx, float midy) {

        float[] p1 = screenPointToWorldDirection(SURFACE__HORIZONTAL_CENTER, SURFACE_VERTICAL_CENTER); // camera center
        float[] p2 = screenPointToWorldDirection(midx, midy); // midpoint of fingers

        // change to 3D and normalize p1 and p2
        p1 = new float[]{p1[0], p1[1], p1[2]};
        p2 = new float[]{p2[0], p2[1], p2[2]};
        p1 = normalizeV(p1);
        p2 = normalizeV(p2);

        float[] phi_theta = computeDeltaPhiAndThetaBetweenTwoUnitVectors(p1, p2);

        // compute angles of midpoint
        float midpt_phi = camera_phi - phi_theta[0];
        float midpt_theta = camera_theta + phi_theta[1];
        midpt_theta = max(midpt_theta, -89.999f);
        return new float[]{midpt_phi, midpt_theta};
    }

    public float[] getWorldPoint(float x, float y){
        float[] phi_theta = getDirectionAnglesOfPoint(x, y);
        float phi = phi_theta[0];
        float theta = phi_theta[1];
        float r = (float) abs(cameraTranslationV[1] * tan(toRadians(90 - theta)));

        float px = (float) (cameraTranslationV[0] - r * sin(toRadians(phi)));
        float pz = (float) (cameraTranslationV[2] - r * cos(toRadians(phi)));

        // if the pt is above the horizon, make the radius the max radius (radius of the sphere)
        float mag = magnitude(new float[]{px, 0, pz});
        if (mag - SPHERE_RADIUS > -.001 || theta > -.001){
            float[] p = projectHorizonPtOnSphereRadius(px, pz);
            px = p[0];
            pz = p[1];
        }

        return new float[]{px, 0, pz};
    }

    private float[] projectHorizonPtOnSphereRadius(float x_p, float z_p){
        float x_d = cameraTranslationV[0];
        float z_d = cameraTranslationV[2];
        float r = SPHERE_RADIUS;

        float x_candidate1;
        float x_candidate2;
        float z_candidate1;
        float z_candidate2;

        if (abs(x_p - x_d) < .0001) {
            x_candidate1 = x_d;
            z_candidate1 = (float) sqrt(pow(r, 2) - pow(x_d, 2));

            x_candidate2 = x_d;
            z_candidate2 = (float) -sqrt(pow(r, 2) - pow(x_d, 2));
        } else {
            float m = (z_p - z_d) / (x_p - x_d);

            x_candidate1 = (float) ((sqrt(-pow(z_d - x_d * m, 2) + pow(m * r, 2) + pow(r, 2)) - m * (z_d - x_d * m)) / (m * m + 1));
            z_candidate1 = (float) ((m * sqrt(-pow(z_d - x_d * m, 2) + pow(m * r, 2) + pow(r, 2)) + z_d - x_d * m) / (m * m + 1));

            x_candidate2 = (float) ((-sqrt(-pow(z_d - x_d * m, 2) + pow(m * r, 2) + pow(r, 2)) - m * (z_d - x_d * m)) / (m * m + 1));
            z_candidate2 = (float) ((-m * sqrt(-pow(z_d - x_d * m, 2) + pow(m * r, 2) + pow(r, 2)) + z_d - x_d * m) / (m * m + 1));
        }

        // choose candiate based on which one falls between the drone location and the point
        float dist1 = (float) sqrt(pow(x_candidate1 - x_p, 2) + pow(z_candidate1 - z_p, 2));
        float dist2 = (float) sqrt(pow(x_candidate1 - x_p, 2) + pow(z_candidate1 - z_p, 2));
        if (dist1 < dist2){
            return new float[]{x_candidate1, z_candidate1};
        } else {
            return new float[]{x_candidate2, z_candidate2};
        }

    }

    // x and y are the screen coordinates that I want to rotate about
    // beta is the angle I want to rotate by
    public void moveBasedOnTwoFingerRotation(float x, float y, float beta){
        float[] rotationPt = getWorldPoint(x, y);

        //if rotation pt is past the horizon, don't rotate
        if (magnitude(rotationPt) - SPHERE_RADIUS > -.001){
            return;
        }

        // get translation matrices
        float[] translationM = new float[16];
        Matrix.setIdentityM(translationM, 0);
        Matrix.translateM(translationM, 0, rotationPt[0], rotationPt[1], rotationPt[2]);
        float[] translationInv = getInverse(translationM);

        // shift the point
        float[] tempCameraTransV = cameraTranslationV.clone();
        tempCameraTransV = multiplyMV(translationInv, tempCameraTransV);

        // rotate about y axis
        float[] rotationM = getRotationMatrix(beta, new float[]{0, 1, 0, 1});
        tempCameraTransV = multiplyMV(rotationM, tempCameraTransV);

        // shift back
        cameraTranslationV = multiplyMV(translationM, tempCameraTransV);

        camera_phi += beta;
    }

    private boolean passPendingBoundsCheck(float[] translateDeltaV) {
        if (( (floatEquals(cameraTranslationV[1], MIN_ALTITUDE)|| cameraTranslationV[1] < MIN_ALTITUDE) && cameraTranslationV[1] + translateDeltaV[1] < cameraTranslationV[1]) ||
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

    public void resetCameraParameters(){
        camera_phi = projector_phi;
        camera_theta = projector_theta;
        System.arraycopy(projectorTranslationV, 0, cameraTranslationV, 0, projectorTranslationV.length);
    }

    public void updateCameraRotation(float p1x, float p1y, float p2x, float p2y, float gest_start_y, float gest_start_theta){
        // get world coordinates from screen coordinates
        float[] p1 = screenPointToWorldDirection(p1x, p1y); // relative direction
        float[] p2 = screenPointToWorldDirection(p2x, p2y); // actual direction - given the orientation of the drone
//        float[] p1 = screenPointToWorldDirection(SURFACE__HORIZONTAL_CENTER, GL_SURFACE_HEIGHT); // relative direction
//        float[] p2 = screenPointToWorldDirection(SURFACE__HORIZONTAL_CENTER, SURFACE_VERTICAL_CENTER); // actual direction - given the orientation of the drone
//
        // change to 3D and normalize p1 and p2
        p1 = new float[]{p1[0], p1[1], p1[2]};
        p2 = new float[]{p2[0], p2[1], p2[2]};
        p1 = normalizeV(p1);
        p2 = normalizeV(p2);

        float new_phi = 0;
        float new_theta = 0;

        float fov_y = HORIZONTAL_FOV / ASPECT_RATIO;

        // if we're below 60 degrees, only allow pitch
        // otherwise compute points to both yaw and pitch
        float theta1 = -(gest_start_y-SURFACE_VERTICAL_CENTER)/GL_SURFACE_HEIGHT * fov_y + gest_start_theta;
        if ( false){//theta1 < -60){
            new_phi = 0;
            new_theta = -(p2y - p1y)/GL_SURFACE_HEIGHT * fov_y;
        } else {
            int num_iter = 0;
//            while (num_iter < 2) {
                float[] phi_theta_p1__ = computeDeltaPhiAndThetaBetweenTwoUnitVectors(p1, p2);
                new_phi = phi_theta_p1__[0];
                new_theta = phi_theta_p1__[1];
                p1 = new float[] {phi_theta_p1__[2], phi_theta_p1__[3], phi_theta_p1__[4]};
//                camera_phi += new_phi;
//                camera_theta = max(camera_theta - new_theta, -89.999f);
                num_iter += 1;
//            }
        }

        camera_phi += new_phi;
        camera_theta = max(camera_theta - new_theta, -89.999f);
    }

    // returns delta angles phi, theta and a vector p1__
    // phi and theta are the relative angles between p1 and p2
    // p1__ is where p1 moves to after rotating the camera by phi and theta
    // (p1__ doesn't always reach p2 because there is some error)
    private float[] computeDeltaPhiAndThetaBetweenTwoUnitVectors(float[] p1, float[] p2){
        // get phi
        // project on x and z plane to to find the yaw angle
        float[] p1_xz = new float[]{p1[0], 0, p1[2]};
        float[] p2_xz = new float[]{p2[0], 0, p2[2]};
        p1_xz = normalizeV(p1_xz);
        p2_xz = normalizeV(p2_xz);
        float phi1 = (float) toDegrees(atan2(p1_xz[0], -p1_xz[2]));
        float phi2 = (float) toDegrees(atan2(p2_xz[0], -p2_xz[2]));
        float new_phi = phi2 - phi1;

        // apply new phi to p1
        p1 = new float[]{p1[0], p1[1], p1[2], 1}; // switch to homogeneous coords
        float[] phiRotationMatrix = getRotationMatrix(new_phi, new float[]{0, 1, 0});
        float[] p1_ = multiplyMV(phiRotationMatrix, p1);
        p1_ = new float[]{p1_[0], p1_[1], p1_[2]};
        p1_ = normalizeV(p1_);

        // get theta
        float theta1 = (float) toDegrees(atan2(p1_[1], sqrt(pow(p1_[2], 2) + pow(p1_[0], 2))));
        float theta2 = (float) toDegrees(atan2(p2[1], sqrt(pow(p2[2], 2) + pow(p2[0], 2))));
        float new_theta = theta2 - theta1;

        float[] u = multiplyMV(phiRotationMatrix, new float[]{1, 0, 0, 1});
        float[] thetaRotationMatrix = getRotationMatrix(-new_theta, new float[]{u[0], u[1], u[2]});
        p1_ = new float[]{p1_[0], p1_[1], p1_[2], 1};
        float[] p1__ = multiplyMV(thetaRotationMatrix, p1_);
        p1__ = new float[]{p1__[0], p1__[1], p1__[2]};
        p1__ = normalizeV(p1__);

        float[] phi_theta_p1__ = new float[]{new_phi, new_theta, p1__[0], p1__[1], p1__[2]};
        return phi_theta_p1__;
    }

    // world direction = C * P^-1 * v
    // where C is the camera matrix,
    // P^-1 is the inverse of the projection matrix
    // and v is the scaled screen coordinates vector (x and y range from -1 to 1)
    private float[] screenPointToWorldDirection(float x, float y){
        float[] v = {(2.0f * x/GL_SURFACE_WIDTH - 1.0f), -(2.0f * y / GL_SURFACE_HEIGHT - 1.0f), 0, 1};

        float[] mInverseProjectionMatrix = getInverse(mProjectionMatrix);
        float[] world_v = multiplyMV(mInverseProjectionMatrix, v);

        float[] cameraRotationM = getCameraRotationMatrix(camera_theta, camera_phi);
        world_v = OperationsHelper.multiplyMV(cameraRotationM, world_v);

        return world_v;
    }

    // relative world direction = P^-1 * v
    // P^-1 is the inverse of the projection matrix
    // and v is the scaled screen coordinates vector (x and y range from -1 to 1)
    // the output is relative to the current orientation of the camera
    private float[] screenPointToRelativeDirection(float x, float y){
        float[] v = {(2.0f * x/GL_SURFACE_WIDTH - 1.0f), -(2.0f * y / GL_SURFACE_HEIGHT - 1.0f), 0, 1};

        float[] mInverseProjectionMatrix = getInverse(mProjectionMatrix);
        float[] world_v = multiplyMV(mInverseProjectionMatrix, v);
        return world_v;
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