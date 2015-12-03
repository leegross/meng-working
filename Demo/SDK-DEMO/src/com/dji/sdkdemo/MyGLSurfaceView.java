package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static com.dji.sdkdemo.Constants.*;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.tan;
import static java.lang.StrictMath.toRadians;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private DroneWrapper mDroneWrapper;

    float H_CENTER = SURFACE__HORIZONTAL_CENTER;
    float V_CENTER = SURFACE_VERTICAL_CENTER;
    private final float TOUCH_SCALE_FACTOR = (float) (tan(toRadians(HORIZONTAL_FOV/2)) * FRUST_NEAR /H_CENTER);

    private int minGimbalPitchAngle;
    private int maxGimbalPitchAngle;

    float phi_at_gest_start;
    float theta_at_gest_start;
    float x_at_gest_start;
    float y_at_gest_start;

    private boolean isGestureInProgress;

    float mPrevDelta = 0;

    float scale = 100.0f;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        scale = 100.0f;
        isGestureInProgress = false;

        phi_at_gest_start = 0;
        theta_at_gest_start = 0;
        x_at_gest_start = 0;
        y_at_gest_start = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        isGestureInProgress = true;
        float x = e.getX();
        float y = e.getY();

        //handle multi touch event
        if (e.getPointerCount() > 1 && false) {
            float p1x = e.getX(0);
            float p1y = e.getY(0);
            float p2x = e.getX(1);
            float p2y = e.getY(1);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    float delta = (float) sqrt(pow(p1x - p2x, 2) + pow(p1y - p2y, 2));
                    float zoom_scale = (mPrevDelta - delta)/scale;
                    mRenderer.updateCameraZoom(zoom_scale);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    float[] cameraTranslationV = mRenderer.getCameraTranslation();
                    float x_translate = cameraTranslationV[0];
                    float y_translate = cameraTranslationV[1];
                    float z_translate = cameraTranslationV[2];
                    float heading = mRenderer.getPhiCamera();
                    mDroneWrapper.setNewGPSCoordinates(x_translate, y_translate, z_translate, heading);

                    break;
            }

            mPrevDelta = (float) sqrt(pow(p1x - p2x, 2.0f) + pow(p1y - p2y, 2.0f));

        }
        // handle single touch event
        else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    phi_at_gest_start = mRenderer.getPhiCamera();
                    theta_at_gest_start = mRenderer.getThetaCamera();
                    x_at_gest_start = x;
                    y_at_gest_start = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    mRenderer.updateCameraRotation(x_at_gest_start, y_at_gest_start, x, y, theta_at_gest_start, phi_at_gest_start);
                    break;
                case MotionEvent.ACTION_UP:
                    mDroneWrapper.setYawAngle(mRenderer.getPhiCamera());
                    mDroneWrapper.setGimbalPitch((int) mRenderer.getThetaCamera());

                    isGestureInProgress = false;
                    break;
            }
        }

        return true;
    }

    private float clipPitchAngle(float thetaY){
        // Y is limited to [-90, 0] so don't move past those
        if (mRenderer.getThetaCamera() > maxGimbalPitchAngle &&
                mRenderer.getThetaCamera() - thetaY > maxGimbalPitchAngle) {
            thetaY = (float) 0;
        } else if (mRenderer.getThetaCamera() < minGimbalPitchAngle &&
                mRenderer.getThetaCamera() - thetaY < minGimbalPitchAngle) {
            thetaY = (float) 0;
        } else if (mRenderer.getThetaCamera() - thetaY > maxGimbalPitchAngle) {
            thetaY = mRenderer.getThetaCamera() - maxGimbalPitchAngle;
        } else if (mRenderer.getThetaCamera() - thetaY < minGimbalPitchAngle) {
            thetaY = mRenderer.getThetaCamera() - minGimbalPitchAngle;
        }

        return thetaY;
    }

    // called by drone wrapper when we receive new orientation update
    public void onDroneLocationAndOrientationUpdate() {
        float currentGimbalPitch = mDroneWrapper.getCurrentGimbalPitch();
        float currentYaw = mDroneWrapper.getCurrentYaw();
        float x = mDroneWrapper.getCurrentLongitudeInMeters();
        float y = mDroneWrapper.getCurrentAltitude();
        float z = mDroneWrapper.getCurrentLatitudeInMeters();

        mRenderer.setProjectorRotationAngles(currentGimbalPitch, currentYaw);
        mRenderer.setProjectorTranslationV(x, y, z);

        if (mDroneWrapper.droneUpdatesAreInitialized() && !mRenderer.isCameraPhiInitailized()) {
            mRenderer.setInitialCameraPhi(currentYaw);
        }
        if (mDroneWrapper.droneUpdatesAreInitialized() && !mRenderer.isCameraThetaInitialized()) {
            mRenderer.setInitialCameraTheta(currentGimbalPitch);
        }
    }

    public void setDroneWrapper(DroneWrapper droneWrapper) {
        mDroneWrapper = droneWrapper;
        minGimbalPitchAngle = (int) mDroneWrapper.getGimbalMinPitchAngle();
        maxGimbalPitchAngle = (int) mDroneWrapper.getGimbalMaxPitchAngle();
    }

    public MyGLRenderer getRenderer() {
        return mRenderer;
    }

    public void updateZoomScale(float new_scale) {
        scale = new_scale;
    }

    public boolean isGestureInProgress() {
        return isGestureInProgress;
    }
}