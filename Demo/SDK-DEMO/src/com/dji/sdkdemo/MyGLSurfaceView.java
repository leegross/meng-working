package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static com.dji.sdkdemo.Constants.*;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.sqrt;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private DroneWrapper mDroneWrapper;

    private boolean isGestureInProgress;

    float mPrevDelta = 0;

    float scale = 100.0f;
    private float prevX;
    private float prevY;
    private float gestStartX;
    private float gestStartY;
    private float gestStartTheta;
    private float gestStartX1;
    private float gestStartY1;
    private float gestStartX2;
    private float gestStartY2;
    private float prevX1;
    private float prevY1;
    private float prevX2;
    private float prevY2;

    private boolean twoFingerRotationStarted;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        scale = 100.0f;
        isGestureInProgress = false;
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
        if (e.getPointerCount() > 1) {
            float p1x = e.getX(0);
            float p1y = e.getY(0);
            float p2x = e.getX(1);
            float p2y = e.getY(1);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    gestStartX1 = p1x;
                    gestStartY1 = p1y;
                    gestStartX2 = p2x;
                    gestStartY2 = p2y;
                    twoFingerRotationStarted = false;
                    break;
                case MotionEvent.ACTION_MOVE:

//                    p1x = SURFACE__HORIZONTAL_CENTER - 1/6.0f * GL_SURFACE_WIDTH;
//                    p1y = SURFACE_VERTICAL_CENTER - 1/4.0f * GL_SURFACE_HEIGHT;
//                    p2x = SURFACE__HORIZONTAL_CENTER + 1/6.0f * GL_SURFACE_WIDTH;
//                    p2y = SURFACE_VERTICAL_CENTER - 1/4.0f * GL_SURFACE_HEIGHT;
//                    prevX1 = SURFACE__HORIZONTAL_CENTER - 1/6.0f * GL_SURFACE_WIDTH;
//                    prevY1 = SURFACE_VERTICAL_CENTER;
//                    prevX2 = SURFACE__HORIZONTAL_CENTER + 1/6.0f * GL_SURFACE_WIDTH;
//                    prevY2 = SURFACE_VERTICAL_CENTER;

                    float rotation_angle = computeRotationAngle(p1x, p1y, p2x, p2y, prevX1, prevY1, prevX2, prevY2);
                    float[] rotationPt = computeRotationPoint(p1x, p1y, p2x, p2y);
                    float rx = rotationPt[0];
                    float ry = rotationPt[1];
//                    mRenderer.moveBasedOnTwoFingerRotation(rx, ry,rotation_angle);

//                    mRenderer.moveBasedOnCameraZoom(
//                            SURFACE__HORIZONTAL_CENTER - 1/6.0f * GL_SURFACE_WIDTH, SURFACE_VERTICAL_CENTER,
//                            SURFACE__HORIZONTAL_CENTER + 1/6.0f * GL_SURFACE_WIDTH, SURFACE_VERTICAL_CENTER + 1/4.0f * GL_SURFACE_HEIGHT,
//                            SURFACE__HORIZONTAL_CENTER - 1/6.0f * GL_SURFACE_WIDTH, SURFACE_VERTICAL_CENTER + 1/4.0f * GL_SURFACE_HEIGHT,
//                            SURFACE__HORIZONTAL_CENTER + 1/6.0f * GL_SURFACE_WIDTH, SURFACE_VERTICAL_CENTER + 1/4.0f * GL_SURFACE_HEIGHT
//                    );
                    float on_screen_rotation_angle = computeOnScreenRotationAngle(p1x, p1y, p2x, p2y, gestStartX1, gestStartY1, gestStartX2, gestStartY2);

                    // if fixed pt for two finger rotation is p1
                    // else fixed pt for two finger rotation is p2
                    if (abs(rx - p1x) < .00000001 && abs(ry - p1y) < .00000001 ){
                        float mag_prev = (float) (sqrt(pow(prevX2 - prevX1, 2) + pow(prevY2 - prevY1, 2)));
                        float angle_current = (float) toDegrees(atan2(p2y - p1y, p2x - p1x));

                        float new_prev_p2x = (float) (prevX1 + mag_prev * cos(toRadians(angle_current)));
                        float new_prev_p2y = (float) (prevY1 + mag_prev * sin(toRadians(angle_current)));
                        mRenderer.moveBasedOnCameraZoom(p1x, p1y, p2x, p2y, prevX1, prevY1, new_prev_p2x, new_prev_p2y);
                    } else {
                        float mag_prev = (float) (sqrt(pow(prevX2 - prevX1, 2) + pow(prevY2 - prevY1, 2)));
                        float angle_current = (float) toDegrees(atan2(p2y - p1y, p2x - p1x));

                        float new_prev_p1x = (float) (prevX2 - mag_prev * cos(toRadians(angle_current)));
                        float new_prev_p1y = (float) (prevY2 - mag_prev * sin(toRadians(angle_current)));
                        mRenderer.moveBasedOnCameraZoom(p1x, p1y, p2x, p2y, new_prev_p1x, new_prev_p1y, prevX2, prevY2);
                    }

//                    mRenderer.moveBasedOnCameraZoom(p1x, p1y, p2x, p2y, prevX1, prevY1, prevX2, prevY2);

                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    float[] cameraTranslationV = mRenderer.getCameraTranslation();
                    float x_translate = cameraTranslationV[0];
                    float y_translate = cameraTranslationV[1];
                    float z_translate = cameraTranslationV[2];
                    float heading = mRenderer.getPhiCamera();

                    mDroneWrapper.setNewGPSCoordinates(x_translate, y_translate, z_translate, heading);

                    if (e.getActionIndex() == 0){
                        prevX = p2x;
                        prevY = p2y;
                    } else{
                        prevX = p1x;
                        prevY = p1y;
                    }

                    break;
            }

            prevX1 = p1x;
            prevY1 = p1y;
            prevX2 = p2x;
            prevY2 = p2y;
            mPrevDelta = (float) sqrt(pow(p1x - p2x, 2.0f) + pow(p1y - p2y, 2.0f));

        }
        // handle single touch event
        else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    gestStartX = x;
                    gestStartY = y;
                    gestStartTheta = mRenderer.getThetaCamera();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mRenderer.updateCameraRotation(prevX, prevY, x, y, gestStartY, gestStartTheta);
                    break;
                case MotionEvent.ACTION_UP:
                    mDroneWrapper.setYawAngle(mRenderer.getPhiCamera());
                    mDroneWrapper.setGimbalPitch((int) mRenderer.getThetaCamera());
                    isGestureInProgress = false;

                    break;
            }

            prevX = x;
            prevY = y;
        }

        return true;
    }

    private float computeOnScreenRotationAngle(float x1, float y1, float x2, float y2, float prevx1, float prevy1, float prevx2, float prevy2){
        // compute rotation angle
        float p1_move = (float) sqrt(pow(x1- prevx1, 2) + pow(y1- prevy1, 2));
        float p2_move = (float) sqrt(pow(x2- prevx2, 2) + pow(y2- prevy2, 2));

        float rotation_angle;
        if (p1_move < p2_move){
            float fixed_x = prevx1;
            float fixed_y = prevy1;

            float angle_start = (float) atan2(fixed_y - prevy2, fixed_x - prevx2);
            float angle_end = (float) atan2(fixed_y - y2, fixed_x - x2);
            rotation_angle = (float) toDegrees(angle_end - angle_start);
        } else {
            float fixed_x = prevx2;
            float fixed_y = prevy2;

            float angle_start = (float) atan2(fixed_y - prevy1, fixed_x - prevx1);
            float angle_end = (float) atan2(fixed_y - y1, fixed_x - x1);
            rotation_angle = (float) toDegrees(angle_end - angle_start);
        }

        if (rotation_angle < -180){
            rotation_angle += 360;
        }

        return rotation_angle;
    }

    private float computeRotationAngle(float x1, float y1, float x2, float y2, float prevx1, float prevy1, float prevx2, float prevy2){
        // compute rotation angle
        float p1_move = (float) sqrt(pow(x1- prevx1, 2) + pow(y1- prevy1, 2));
        float p2_move = (float) sqrt(pow(x2- prevx2, 2) + pow(y2- prevy2, 2));

//        float rotation_angle;
        float[] prev_world_p;
        float[] world_p;
        float[] fixed_world_p;
        if (p1_move < p2_move){
            float fixed_x = prevx1;//(x1+ prevx1)/2.0f;
            float fixed_y = prevy1;//(y1+ prevy1)/2.0f;

            prev_world_p = mRenderer.getWorldPoint(prevx2, prevy2);
            world_p = mRenderer.getWorldPoint(x2, y2);
            fixed_world_p = mRenderer.getWorldPoint(fixed_x, fixed_y);
        } else {
            float fixed_x = prevx2;//(x2 + prevx2)/2.0f;
            float fixed_y = prevy2;//(y2 + prevy2)/2.0f;

            prev_world_p = mRenderer.getWorldPoint(prevx1, prevy1);
            world_p = mRenderer.getWorldPoint(x1, y1);
            fixed_world_p = mRenderer.getWorldPoint(fixed_x, fixed_y);
        }

        float angle_start = (float) atan2(fixed_world_p[2] - prev_world_p[2], fixed_world_p[0] - prev_world_p[0]);
        float angle_end = (float) atan2(fixed_world_p[2] - world_p[2], fixed_world_p[0] - world_p[0]);
        float rotation_angle = (float) toDegrees(angle_end - angle_start);

        if (rotation_angle < -180){
            rotation_angle += 360;
        }

        return rotation_angle;
    }

    private float[] computeRotationPoint(float x1, float y1, float x2, float y2){
        float p1_move = (float) sqrt(pow(x1- gestStartX1, 2) + pow(y1- gestStartY1, 2));
        float p2_move = (float) sqrt(pow(x2- gestStartX2, 2) + pow(y2- gestStartY2, 2));

        float fixed_x;
        float fixed_y;
        if (p1_move < p2_move){
            fixed_x = x1;
            fixed_y = y1;
        } else {
            fixed_x = x2;
            fixed_y = y2;
        }

        return new float[]{fixed_x, fixed_y};
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