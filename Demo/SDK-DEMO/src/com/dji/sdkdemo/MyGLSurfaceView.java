package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static com.dji.sdkdemo.Constants.*;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.sqrt;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private DroneWrapper mDroneWrapper;

    private boolean isGestureInProgress;
    private boolean isTwoFingerGesture;

    float mPrevDelta = 0;

    float scale = 100.0f;
    private float prevX;
    private float prevY;
    private float gestStartX;
    private float gestStartY;
    private float gestStartTheta;
    private float gestStartPhi;
    private float gestStartX1;
    private float gestStartY1;
    private float gestStartX2;
    private float gestStartY2;
    private float prevX1;
    private float prevY1;
    private float prevX2;
    private float prevY2;

    private float twoFingerRotationTotal;

    private boolean dragAtConstAlt;

    private int currentGesture;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        scale = 100.0f;
        isGestureInProgress = false;
        dragAtConstAlt = false;
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
                    isTwoFingerGesture = true;
                    currentGesture = NOT_DECIDED;
                    gestStartX1 = p1x;
                    gestStartY1 = p1y;
                    gestStartX2 = p2x;
                    gestStartY2 = p2y;
                    twoFingerRotationTotal = 0;
                    isGestureInProgress = true;
                    break;
                case MotionEvent.ACTION_MOVE:

                    float rotation_angle = computeRotationAngle(p1x, p1y, p2x, p2y, prevX1, prevY1, prevX2, prevY2);

                    float finger_dist = (float) sqrt(pow(p1x-p2x, 2) + pow(p1y - p2y, 2));

                    float p1_move = (float) sqrt(pow(p1x- gestStartX1, 2) + pow(p1y- gestStartY1, 2));
                    float p2_move = (float) sqrt(pow(p2x- gestStartX2, 2) + pow(p2y- gestStartY2, 2));
                    if (currentGesture == NOT_DECIDED && max(p1_move, p2_move) < 60){
                        break;
                    } else if (currentGesture == NOT_DECIDED){
                        if (min(p1_move, p2_move) < 25){
                            currentGesture = ORBIT;
                        } else{
                            currentGesture = ZOOM_AND_TRANSLATE;
                        }
                    }
                    twoFingerRotationTotal += rotation_angle;

                    Log.d("total_rotation", " , min finger move: " + min(p1_move, p2_move) + ", max: " + max(p1_move, p2_move) + ", rotation: " + twoFingerRotationTotal);



                    float[] rotationPt = computeRotationPoint(p1x, p1y, p2x, p2y);
                    float rx = rotationPt[0];
                    float ry = rotationPt[1];

                    if (currentGesture == ZOOM_AND_TRANSLATE){
                        twoFingerDrag(p1x, p1y, p2x, p2y);

                        zoomGesture(p1x, p1y, p2x, p2y, rx, ry);
                    } else if (currentGesture == ORBIT){
                        mRenderer.moveBasedOnTwoFingerRotation(rx, ry, rotation_angle);
                    }


                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    float[] cameraTranslationV = mRenderer.getCameraTranslation();
                    float x_translate = cameraTranslationV[0];
                    float y_translate = cameraTranslationV[1];
                    float z_translate = cameraTranslationV[2];
                    float heading = mRenderer.getPhiCamera();
                    isGestureInProgress = false;

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
                    gestStartPhi = mRenderer.getPhiCamera();

                    isTwoFingerGesture = false;
                    break;
                case MotionEvent.ACTION_MOVE:
//                    gestStartX = SURFACE__HORIZONTAL_CENTER;
//                    gestStartY = SURFACE_VERTICAL_CENTER;
//                    x = SURFACE__HORIZONTAL_CENTER;
//                    y =  0;
                    if (isTwoFingerGesture) break;
                    mRenderer.updateCameraRotation(gestStartX, gestStartY, x, y, gestStartTheta, gestStartPhi);

                    break;
                case MotionEvent.ACTION_UP:
                    isGestureInProgress = false;
                    if (isTwoFingerGesture) break;
                    mDroneWrapper.setYawAngle(mRenderer.getPhiCamera());
                    mDroneWrapper.setGimbalPitch((int) mRenderer.getThetaCamera());

                    break;
            }

            prevX = x;
            prevY = y;
        }

        return true;
    }

    private void twoFingerDrag(float p1x, float p1y, float p2x, float p2y){
        if (dragAtConstAlt){
            mRenderer.moveBasedOnTwoFingerDragAtConstAlt((prevX1 + prevX2)/2.0f, (prevY1 + prevY2)/2.0f, (p1x+p2x)/2.0f, (p1y+p2y)/2.0f);
        } else {
            mRenderer.moveBasedOnTwoFingerDrag((prevX1 + prevX2)/2.0f, (prevY1 + prevY2)/2.0f, (p1x+p2x)/2.0f, (p1y+p2y)/2.0f);
        }
    }

    private void zoomGesture(float p1x, float p1y, float p2x, float p2y, float rx, float ry){
        if (dragAtConstAlt){ // don't zoom if cost altitude flag is enabled
            return;
        }
        // if fixed pt for two finger rotation is p1
        // else fixed pt for two finger rotation is p2
        if (abs(rx - p1x) < .00000001 && abs(ry - p1y) < .00000001 ){
            float mag_prev = (float) (sqrt(pow(prevX2 - prevX1, 2) + pow(prevY2 - prevY1, 2)));
            float angle_current = (float) toDegrees(atan2(p2y - p1y, p2x - p1x));

            // project the fixed pt onto the line of the current pts
            // add the second prev pt at the angle of the prev pt to the new fixed pt
            float m = (p2y - p1y)/(p2x - p1x);
            float new_prev_p1x = (prevY1 - p1y + 1/m * prevX1 + m * p1x)/(1/m + m);
            if (abs(m) < .00001){
                new_prev_p1x = prevX1;
            }
            float new_prev_p1y = p1y + m * (new_prev_p1x - p1x);
            if (abs(p2x - p1x) < .0001){
                new_prev_p1x = p1x;
                new_prev_p1y = prevY1;
            }
            float new_prev_p2x = (float) (new_prev_p1x + mag_prev * cos(toRadians(angle_current)));
            float new_prev_p2y = (float) (new_prev_p1y + mag_prev * sin(toRadians(angle_current)));

            // slide by avg translation
            float slide_x = ((p2x - new_prev_p2x) + (p1x - new_prev_p1x))/2.0f;
            float slide_y = ((p2y - new_prev_p2y) + (p1y - new_prev_p1y))/2.0f;
            new_prev_p1x += slide_x;
            new_prev_p1y += slide_y;
            new_prev_p2x += slide_x;
            new_prev_p2y += slide_y;

            mRenderer.moveBasedOnCameraZoom(p1x, p1y, p2x, p2y, new_prev_p1x, new_prev_p1y, new_prev_p2x, new_prev_p2y);
        } else {
            float mag_prev = (float) (sqrt(pow(prevX2 - prevX1, 2) + pow(prevY2 - prevY1, 2)));
            float angle_current = (float) toDegrees(atan2(p2y - p1y, p2x - p1x));

            // project the fixed pt onto the line of the current pts
            // add the second prev pt at the angle of the prev pt to the new fixed pt
            float m = (p2y - p1y)/(p2x - p1x);
            float new_prev_p2x = (prevY2 - p2y + 1/m * prevX2 + m * p2x)/(1/m + m);
            if (abs(m) < .00001){
                new_prev_p2x = prevX2;
            }
            float new_prev_p2y = p2y + m * (new_prev_p2x - p2x);
            if (abs(p2x - p1x) < .0001){
                new_prev_p2x = p2x;
                new_prev_p2y = prevY2;
            }
            float new_prev_p1x = (float) (new_prev_p2x - mag_prev * cos(toRadians(angle_current)));
            float new_prev_p1y = (float) (new_prev_p2y - mag_prev * sin(toRadians(angle_current)));

            // slide by avg translation
            float slide_x = ((p2x - new_prev_p2x) + (p1x - new_prev_p1x))/2.0f;
            float slide_y = ((p2y - new_prev_p2y) + (p1y - new_prev_p1y))/2.0f;
            new_prev_p1x += slide_x;
            new_prev_p1y += slide_y;
            new_prev_p2x += slide_x;
            new_prev_p2y += slide_y;

            mRenderer.moveBasedOnCameraZoom(p1x, p1y, p2x, p2y, new_prev_p1x, new_prev_p1y, new_prev_p2x, new_prev_p2y);
        }
    }

    private float computeRotationAngle(float x1, float y1, float x2, float y2, float prevx1, float prevy1, float prevx2, float prevy2){
        // compute rotation angle
        float[] fixed_p = computeRotationPoint(x1, y1, x2, y2);

//        float[] prev_world_p;
//        float[] world_p;
//        if (abs(fixed_p[0] - x1) < .0001 && abs(fixed_p[1] - y1) < .0001){
//            prev_world_p = mRenderer.getWorldPoint(prevx2, prevy2);
//            world_p = mRenderer.getWorldPoint(x2, y2);
//        } else {
//            prev_world_p = mRenderer.getWorldPoint(prevx1, prevy1);
//            world_p = mRenderer.getWorldPoint(x1, y1);
//        }
//
//        float[] fixed_world_p = mRenderer.getWorldPoint(fixed_p[0], fixed_p[1]);
//        float angle_start = (float) atan2(fixed_world_p[2] - prev_world_p[2], fixed_world_p[0] - prev_world_p[0]);
//        float angle_end = (float) atan2(fixed_world_p[2] - world_p[2], fixed_world_p[0] - world_p[0]);
//        float rotation_angle = (float) toDegrees(angle_end - angle_start);

        float rotation_angle;
        if (abs(fixed_p[0] - x1) < .0001 && abs(fixed_p[1] - y1) < .0001){
            float angle_start = (float) atan2(fixed_p[1] - prevy2, fixed_p[0] - prevx2);
            float angle_end = (float) atan2(fixed_p[1] - y2, fixed_p[0] - x2);
            rotation_angle = (float) toDegrees(angle_end - angle_start);
        } else {
            float angle_start = (float) atan2(fixed_p[1] - prevy1, fixed_p[0] - prevx1);
            float angle_end = (float) atan2(fixed_p[1] - y1, fixed_p[0] - x1);
            rotation_angle = (float) toDegrees(angle_end - angle_start);
        }

        if (rotation_angle < -180){
            rotation_angle += 360;
        }

        rotation_angle = rotation_angle %360;

        return rotation_angle;
    }

    private float[] computeRotationPoint(float x1, float y1, float x2, float y2){
//        float p1_move = (float) sqrt(pow(x1- gestStartX1, 2) + pow(y1- gestStartY1, 2));
//        float p2_move = (float) sqrt(pow(x2- gestStartX2, 2) + pow(y2- gestStartY2, 2));
        float p1_move = (float) sqrt(pow(x1- prevX1, 2) + pow(y1- prevY1, 2));
        float p2_move = (float) sqrt(pow(x2- prevX2, 2) + pow(y2- prevY2, 2));

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

    public void setDragAtConstAlt(boolean b){
        dragAtConstAlt = b;
    }
}