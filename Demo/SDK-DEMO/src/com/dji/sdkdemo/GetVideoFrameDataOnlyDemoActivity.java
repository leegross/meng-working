package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.api.Camera.DJICameraDecodeTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.Gimbal.DJIGimbalAttitude;
import dji.sdk.api.mediacodec.DJIVideoDecoder;
import dji.sdk.interfaces.DJIGimbalUpdateAttitudeCallBack;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;

public class GetVideoFrameDataOnlyDemoActivity extends DemoBaseActivity
{
    private TextView mConnectStateTextView;

    private DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack;

    private TextView mGroundStationTextView;
    private MyGLSurfaceView mGLView;
    private DJIVideoDecoder mVideoDecoder = null;

    private DroneWrapper droneWrapper;
    private Timer mTimer;
    private double currentGimbalPitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_waypoints);

        mGLView = (MyGLSurfaceView) findViewById(R.id.surfaceview);
        droneWrapper = new DroneWrapper(mGLView);
        droneWrapper.setToastCallback(getToastCallback());

        initDecoder();

        mGLView.setDroneWrapper(droneWrapper);

        mConnectStateTextView = (TextView)findViewById(R.id.ConnectStateGsTextView);
        mGroundStationTextView = (TextView)findViewById(R.id.GroundStationInfoTV);

    }

    class Task extends TimerTask {
        //int times = 1;

        @Override
        public void run()
        {
            checkConnectState();
        }

    };

    private DroneWrapper.setResultToToastCallback getToastCallback() {
        DroneWrapper.setResultToToastCallback toast_callback = new DroneWrapper.setResultToToastCallback(){

            @Override
            public void ToastResult(String result) {
                setResultToToast(result);
            }
        };
        return toast_callback;
    }

    private void setResultToToast(String result){
        Toast.makeText(GetVideoFrameDataOnlyDemoActivity.this, result, Toast.LENGTH_SHORT).show();
    }

    private void checkConnectState(){

        GetVideoFrameDataOnlyDemoActivity.this.runOnUiThread(new Runnable(){

            @Override
            public void run()
            {
                if(DJIDrone.getDjiCamera() != null){
                    boolean bConnectState = DJIDrone.getDjiCamera().getCameraConnectIsOk();
                    if(bConnectState){
                        mConnectStateTextView.setText(R.string.camera_connection_ok);
                    }
                    else{
                        mConnectStateTextView.setText(R.string.camera_connection_break);
                    }
                }
            }
        });

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
//        mDjiGLSurfaceView.resume();
        mGLView.onResume();

        mTimer = new Timer();
        Task task = new Task();
        mTimer.schedule(task, 0, 500);

        droneWrapper.resume();

        DJIDrone.getDjiGimbal().startUpdateTimer(1000);

        super.onResume();
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onPause() {
//        mDjiGLSurfaceView.pause();
        mGLView.onPause();

        if(mTimer!=null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }

        droneWrapper.pause();

        DJIDrone.getDjiGimbal().stopUpdateTimer();

        super.onPause();
    }


    @Override
    protected void onDestroy()
    {
        if(DJIDrone.getDjiCamera() != null)
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
//        mDjiGLSurfaceView.destroy();
        super.onDestroy();
    }

    public void takeOff(View v){
        droneWrapper.takeOff();
    }

    public void addWaypoints1(View v){
        droneWrapper.openGs(droneWrapper.getHomeLocationLatitude(), droneWrapper.getHomeLocationLongitude(), 10f, (short) 0);
    }

    public void addWaypoints2(View v){
        droneWrapper.openGs(droneWrapper.getHomeLocationLatitude(), droneWrapper.getHomeLocationLongitude(), 2f, (short) 0);
    }

    public void closeGs(View v){
        droneWrapper.closeGs();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        return mGLView.onTouchEvent(e);
    }

    private void initDecoder() {
        DJIDrone.getDjiCamera().setDecodeType(DJICameraDecodeTypeDef.DecoderType.Hardware);
//        mVideoDecoder = new DJIVideoDecoder(this, surface);
        //mVideoDecoder.setRecvDataCallBack(null);
        final Context mContext = this;

        mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack(){

            @Override
            public void onResult(final byte[] videoBuffer, final int size)
            {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SurfaceTexture mSurfaceTexture = mGLView.getRenderer().getSurfaceTexture();
                        if (mSurfaceTexture == null) {
                            return;
                        }
                        if (mVideoDecoder == null) {
                            Surface mSurface = new Surface(mGLView.getRenderer().getSurfaceTexture());
                            mVideoDecoder = new DJIVideoDecoder(mContext, mSurface);
                        }

                        DJIDrone.getDjiCamera().sendDataToDecoder(videoBuffer,size);
                    }
                });

            }
        };

        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);

        DJIGimbalUpdateAttitudeCallBack mGimbalUpdateAttitudeCallBack = new DJIGimbalUpdateAttitudeCallBack() {

            DroneWrapper dw = droneWrapper;

            @Override
            public void onResult(final DJIGimbalAttitude attitude) {
                // TODO Auto-generated method stub
                //Log.d(TAG, attitude.toString());

                StringBuffer sb = new StringBuffer();
                sb.append("pitch=").append(attitude.pitch).append("\n");
                sb.append("roll=").append(attitude.roll).append("\n");
                sb.append("yaw=").append(attitude.yaw).append("\n");
                sb.append("yawAngle=").append(DJIDrone.getDjiGimbal().getYawAngle()).append("\n");
                sb.append("roll adjust=").append(attitude.rollAdjust).append("\n");

//              currentGimbalPitch = attitude.pitch;
//                dw.setCurrentGimbalPitch(attitude.pitch);

            }

        };

        DJIDrone.getDjiGimbal().setGimbalUpdateAttitudeCallBack(mGimbalUpdateAttitudeCallBack);
    }

    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     * @author      : andy.zhao
     * @param view
     * @return      : void
     */
    public void onReturn(View view){
        this.finish();
    }
}
