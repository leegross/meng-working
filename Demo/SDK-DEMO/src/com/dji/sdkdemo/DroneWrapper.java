package com.dji.sdkdemo;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIGroundStationFlyingInfoCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.StrictMath.sqrt;

/**
 * Created by leegross on 11/3/15.
 */
public class DroneWrapper {

    // current drone parameters
    private float currentAltitude;
    private float currentLatitude;
    private float currentLongitude;
    private float currentGimbalPitch;
    private float currentYaw;

    // drone home points
    private float homeLocationLatitude;
    private float homeLocationLongitude;
    private boolean getHomePointFlag;

    private DJIGroundStationTask mTask;
    private setResultToToastCallback toastCallback;
    private MyGLSurfaceView mGLView;
    private uiCallback mUiCallback;

    public DroneWrapper(MyGLSurfaceView surfaceView) {
        mGLView = surfaceView;

        initMainControllerState();

        mTask = new DJIGroundStationTask();

        initFlyingInfo();

        DJIDrone.getDjiGroundStation().setYawControlMode(DJIGroundStationTypeDef.DJINavigationFlightControlYawControlMode.Navigation_Flight_Control_Yaw_Control_Angle);
    }

    private void initFlyingInfo(){
        DJIDrone.getDjiGroundStation().setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack() {

            @Override
            public void onResult(DJIGroundStationFlyingInfo mInfo)
            {
                final StringBuffer sb = new StringBuffer();

                currentAltitude = mInfo.altitude;
                currentLatitude = (float) mInfo.droneLocationLatitude;
                currentLongitude = (float) mInfo.droneLocationLongitude;
                sb.append("drone altitude: ").append(currentAltitude).append("\n");
                sb.append("drone latitude: ").append(currentLatitude).append("\n");
                sb.append("drone longitude: ").append(currentLongitude).append("\n");
                sb.append("drone yaw: ").append(mInfo.yaw).append("\n");
                currentAltitude = mInfo.altitude;
                currentYaw = mInfo.yaw;

                mGLView.onDroneLocationAndOrientationUpdate();

                if (mUiCallback != null) {
                    mUiCallback.UICallback("ground_station_text_view", sb.toString());
                }
            }
        });
    }

    private void initMainControllerState(){
        DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack() {

            @Override
            public void onResult(DJIMainControllerSystemState state) {
                homeLocationLatitude = (float) state.homeLocationLatitude;
                homeLocationLongitude = (float) state.homeLocationLongitude;

                if (homeLocationLatitude != -1 && homeLocationLongitude != -1 && homeLocationLatitude != 0 && homeLocationLongitude != 0) {
                    getHomePointFlag = true;
                } else {
                    getHomePointFlag = false;
                }
            }

        };

        DJIDrone.getDjiMainController().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
    }

    private boolean checkGetHomePoint(){
        if(!getHomePointFlag){
            toastCallback.ToastResult("home point is not set");
        }
        return getHomePointFlag;
    }

    public interface setResultToToastCallback {
        void ToastResult(String result);
    }
    public void setToastCallback(setResultToToastCallback cb){
        toastCallback = cb;
    }

    public interface uiCallback {
        void UICallback(String type, String result);
    }
    public void setUICallback(uiCallback cb){
        mUiCallback = cb;
    }

    public void takeOff(){
        DJIDrone.getDjiGroundStation().oneKeyFly(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                String ResultsString = "take off return code =" + groundStationResult.toString();
//                toastCallback.ToastResult(ResultsString);
            }
        });
    }

    public void openGs(final float latitude, final float longitude, final float altitude, final short heading){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "openGS return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
                addWaypoint(latitude, longitude, altitude, heading);
            }

        });
    }

    public void openGs(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "openGS return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
            }

        });
    }

//    private void addWaypoint(double latitude, double longitude, float altitude, short heading){
//        mTask.RemoveAllWaypoint();
//        DJIGroundStationWaypoint mWayPoint1 = new DJIGroundStationWaypoint(latitude, longitude);
//        mWayPoint1.action.actionRepeat = 1;
//        mWayPoint1.altitude = (currentAltitude+altitude)/2;
//        mWayPoint1.heading = 0;
//        mWayPoint1.actionTimeout = 10;
//        mWayPoint1.turnMode = 1;
//        mWayPoint1.dampingDistance = 1.5f;
//        mWayPoint1.hasAction = true;
//
//        mWayPoint1.addAction(DJIGroundStationTypeDef.GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, 0);
//
//
//        mTask.addWaypoint(mWayPoint1);
//
//        DJIGroundStationWaypoint mWayPoint2 = new DJIGroundStationWaypoint(latitude, longitude);
//        mWayPoint2.action.actionRepeat = 1;
//        mWayPoint2.altitude = altitude;
//        mWayPoint2.heading = 0;
//        mWayPoint2.actionTimeout = 10;
//        mWayPoint2.turnMode = 1;
//        mWayPoint2.dampingDistance = 1.5f;
//        mWayPoint2.hasAction = true;
//
//        mWayPoint2.addAction(DJIGroundStationTypeDef.GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, heading);
//
//
//        mTask.addWaypoint(mWayPoint2);
//
//        mTask.finishAction = DJIGroundStationTypeDef.DJIGroundStationFinishAction.None;
//        mTask.movingMode = DJIGroundStationTypeDef.DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
//        mTask.pathMode = DJIGroundStationTypeDef.DJIGroundStationPathMode.Point_To_Point;
//        mTask.wayPointCount = mTask.getAllWaypoint().size();
//
//        uploadWaypoint();
//    }

    private void addWaypoint(float latitude, float longitude, float altitude, short heading){
        // there is a limitation that we must have two waypoints
        // and that each waypoint must be at least 2 meters away from the previous waypoint
        // therefore, for every waypoint we want to set, we set a midpoint along the way
        // if the midpoint is less than two meters away from the current location,
        // we make the drone fly two meters away and then back to where it needs to go

        // check if lat and log are at least 4 meters away from current location
        // if they are, just give the first waypoint the midpoint coordinates
        // otherwise, give the second waypoint an altitude that is 2 meters above the second waypoint
        float dist = distInMeters(latitude, longitude, latitude, currentLatitude, currentLongitude, currentAltitude);
        float lat_midpoint;
        float long_midpoint;
        float alt_midpoint;
//        if (dist < 4) {
            lat_midpoint = (currentLatitude + latitude)/2.0f;
            long_midpoint = (currentLongitude + longitude)/2.0f;
            alt_midpoint = (currentAltitude+altitude)/2 + 2.0f;
//        } else {
//            lat_midpoint = (currentLatitude + latitude)/2.0f;
//            long_midpoint = (currentLongitude + longitude)/2.0f;
//            alt_midpoint = (currentAltitude+altitude)/2;
//        }

        mTask.RemoveAllWaypoint();

        DJIGroundStationWaypoint mWayPoint1 = createWaypoint(lat_midpoint, long_midpoint, alt_midpoint, heading);
        mTask.addWaypoint(mWayPoint1);


        DJIGroundStationWaypoint mWayPoint2 = createWaypoint(latitude, longitude, altitude, heading);
        mTask.addWaypoint(mWayPoint2);

        setWaypointTaskSettings();

        uploadWaypoint();
    }

    private float distInMeters(float lat1, float long1, float alt1, float lat2, float long2, float alt2){
        float lat_diff_in_meters = (lat1 - lat2)/Constants.GPS_SCALE;
        float long_diff_in_meters = (long1 - long2)/Constants.GPS_SCALE;
        return (float) sqrt(pow(lat_diff_in_meters, 2.0) + pow(long_diff_in_meters, 2.0) + pow(alt1 - alt2, 2.0));
    }

    private DJIGroundStationWaypoint createWaypoint(float latitude, float longitude, float altitude, float heading) {
        DJIGroundStationWaypoint waypoint = new DJIGroundStationWaypoint(latitude, longitude);
        waypoint.action.actionRepeat = 1;
        waypoint.altitude = altitude;
//        waypoint.heading = 0;
        waypoint.actionTimeout = 10;
        waypoint.turnMode = 1;
        waypoint.dampingDistance = 1.5f;
        waypoint.hasAction = false;

//        waypoint.addAction(DJIGroundStationTypeDef.GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, (int) heading);
        return waypoint;
    }

    private void setWaypointTaskSettings(){
        mTask.finishAction = DJIGroundStationTypeDef.DJIGroundStationFinishAction.None;
        mTask.movingMode = DJIGroundStationTypeDef.DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
        mTask.pathMode = DJIGroundStationTypeDef.DJIGroundStationPathMode.Point_To_Point;
        mTask.wayPointCount = mTask.getAllWaypoint().size();
    }

    private void uploadWaypoint(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().uploadGroundStationTask(mTask, new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "upload waypoint return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
                startTask();
            }
        });

    }

    private void startTask(){
        if(!checkGetHomePoint()) return;
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "start task return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
            }
        });
    }

    public void closeGs(final float latitude, final float longitude, final float altitude, final short heading){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "close gs return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
                openGs(latitude, longitude, altitude, heading);
            }

        });
    }

    public void closeGs(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "close gs return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
            }

        });
    }

    public void setGimbalPitch(int angle){
        // clip gimbal pitch between -90 and 0
        angle = (int) max(getGimbalMinPitchAngle(), angle);
        angle = (int) min(getGimbalMaxPitchAngle(), angle);
        final int finalAngle = angle;
        new Thread()
        {
            public void run()
            {
                DJIGimbalRotation mPitch = new DJIGimbalRotation(true, false,true, finalAngle);

                DJIDrone.getDjiGimbal().updateGimbalAttitude(mPitch,null,null);

            }
        }.start();
    }

    // sets the angle the drone is facing relative to north
    public void setYawAngle(float angle){
        angle = -angle%360;
        if (angle < -180){
            angle += 360;
        } else if(angle > 180){
            angle -= 360;
        }
        final float finalAngle = angle;
        openGs();
        new Thread()
        {
            public void run()
            {
                DJIDrone.getDjiGroundStation().sendFlightControlData(finalAngle, 0, 0, 0, new DJIExecuteResultCallback() {
                    @Override
                    public void onResult(DJIError djiError) {}
                });

            }
        }.start();
    }

    public void resume(){

        DJIDrone.getDjiMainController().startUpdateTimer(100);
        DJIDrone.getDjiGroundStation().startUpdateTimer(1000);
    }

    public void pause(){

        DJIDrone.getDjiMainController().stopUpdateTimer();
        DJIDrone.getDjiGroundStation().stopUpdateTimer();
    }

    public boolean droneUpdatesAreInitialized(){
        if (getCurrentLongitude() != 0 && getCurrentLatitude() != 0) {
            return true;
        }
        return false;
    }

    public float getCurrentAltitude(){
        return currentAltitude;
    }

    public float getCurrentLatitude(){
        return currentLatitude;
    }

    public float getCurrentLongitude(){
        return currentLongitude;
    }

    public float getHomeLocationLatitude(){
        return homeLocationLatitude;
    }

    public float getHomeLocationLongitude(){
        return homeLocationLongitude;
    }

    public float getCurrentGimbalPitch(){
        return currentGimbalPitch;
    }

    public float getGimbalMinPitchAngle(){
        return -90;//DJIDrone.getDjiGimbal().getGimbalPitchMinAngle();
    }

    public float getGimbalMaxPitchAngle(){
        return 0;//DJIDrone.getDjiGimbal().getGimbalPitchMaxAngle();
    }

    public void setCurrentGimbalPitch(float pitch){
        currentGimbalPitch = pitch;
        mGLView.onDroneLocationAndOrientationUpdate();
    }

    public float getCurrentYaw(){
        return -currentYaw;
    }

    // receives new coordinates in meters
    public void setNewGPSCoordinates(float x, float y, float z) {
        // bound parameters
//        altitude = max(Constants.MIN_ALTITUDE, altitude);
//        altitude = min(Constants.MAX_ALTITUDE, altitude);
//        latitude = max(-Constants.MAX_DIST, latitude);
//        latitude = min(Constants.MAX_DIST, latitude);
//        longitude = max(-Constants.MAX_DIST, longitude);
//        longitude = min(Constants.MAX_DIST, longitude);

        // convert longitude and latitude to gps coordinates
        float converted_lat = homeLocationLatitude - metersToLat(z);
        float converted_long = homeLocationLongitude - metersToLong(x);

        flyToNewWaypoint(converted_lat, converted_long, y);
    }

    private void flyToNewWaypoint(float latitude, float longitude, float altitude) {
        closeGs(latitude, longitude, altitude, (short) 0);
    }

    private float metersToLong(float longitude_in_meters) {
        return longitude_in_meters * Constants.GPS_SCALE;
    }

    private float metersToLat(float latitude_in_meters) {
        return latitude_in_meters * Constants.GPS_SCALE;
    }

    public float getCurrentLongitudeInMeters(){
        return -(currentLongitude - homeLocationLongitude)/Constants.GPS_SCALE;
    }

    public float getCurrentLatitudeInMeters(){
        return -(currentLatitude - homeLocationLatitude)/Constants.GPS_SCALE;
    }
}
