package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static java.lang.Math.atan2;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.tan;
import static java.lang.StrictMath.toDegrees;
import static java.lang.StrictMath.toRadians;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private DroneWrapper mDroneWrapper;

    float H_CENTER = Constants.SURFACE__HORIZONTAL_CENTER;
    float V_CENTER = Constants.SURFACE_VERTICAL_CENTER;
    private final float TOUCH_SCALE_FACTOR = (float) (tan(toRadians(Constants.HORIZONTAL_FOV/2)) * Constants.FRUST_NEAR /H_CENTER);//.011f;//180f/320;

    private float mPreviousX;
    private float mPreviousY;

    private int minGimbalPitchAngle;
    private int maxGimbalPitchAngle;

    float mPrevDelta = 0;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        //handle multi touch event
        if (e.getPointerCount() > 1) {
            float p1x = e.getX(0);
            float p1y = e.getY(0);
            float p2x = e.getX(1);
            float p2y = e.getY(1);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    float delta = (float) sqrt(pow(p1x - p2x, 2) + pow(p1y - p2y, 2));
                    float zoom_scale = (mPrevDelta - delta)/10.0f;
                    mRenderer.updateCurrentCameraZoom(zoom_scale);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    int i = 1 - e.getActionIndex(); // index of the finger that is left
                    mPreviousX = e.getX(i);
                    mPreviousY = e.getY(i);

                    mRenderer.finalizeCurrentCameraZoom();
                    float[] cameraTranslationV = mRenderer.getCameraTranslation();
                    float x_translate = cameraTranslationV[0];
                    float y_translate = cameraTranslationV[1];
                    float z_translate = cameraTranslationV[2];
                    mDroneWrapper.setNewGPSCoordinates(x_translate, y_translate, z_translate);

                    break;
            }

            mPrevDelta = (float) sqrt(pow(p1x - p2x, 2.0f) + pow(p1y - p2y, 2.0f));

        }
        // handle single touch event
        else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:

                    float thetaX1 = (float) atan2((x - H_CENTER) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                    float thetaX2 = (float) atan2((H_CENTER - mPreviousX) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                    float thetaX = (float) toDegrees(thetaX1 + thetaX2);

                    float thetaY1 = (float) atan2((V_CENTER - y) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                    float thetaY2 = (float) atan2((mPreviousY - V_CENTER) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                    float thetaY = (float) toDegrees(thetaY1 + thetaY2);

                    thetaY = clipPitchAngle(thetaY);

                    mRenderer.updateCameraRotationAngles(thetaY, thetaX);

                    break;
                case MotionEvent.ACTION_UP:

                    mDroneWrapper.setYawAngle(mRenderer.getPhiCamera());
                    mDroneWrapper.setGimbalPitch((int) mRenderer.getThetaCamera());

                    break;
            }

            mPreviousX = x;
            mPreviousY = y;
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
    public void onDroneOrientationUpdate() {
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
}