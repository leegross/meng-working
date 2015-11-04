package com.dji.sdkdemo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.api.Camera.DJICameraDecodeTypeDef.DecoderType;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.mediacodec.DJIVideoDecoder;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;

public class PreviewDemoHwDecodeActivity extends DemoBaseActivity
{

    private static final String TAG = "PreviewDemoHwDecodeActivity";
    
    private DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = null;
    
    private TextView mConnectStateTextView;
    private Timer mTimer;
    
    private DJIVideoDecoder mVideoDecoder = null;

    private MyGLSurfaceView mGLView;

    
    class Task extends TimerTask {
        //int times = 1;

        @Override
        public void run() 
        {
            //Log.d(TAG ,"==========>Task Run In!");
            checkConnectState(); 
        }

    };
    private void checkConnectState(){
        
        PreviewDemoHwDecodeActivity.this.runOnUiThread(new Runnable(){

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
                    
//                    if(recvData){
//                        mConnectStateTextView.setTextColor(PreviewDemoActivity.this.getResources().getColor(R.color.blue));
//                    }
//                    else{
//                        mConnectStateTextView.setTextColor(PreviewDemoActivity.this.getResources().getColor(R.color.red));
//                    }
                }
            }
        });
        
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_hw_demo);

        mGLView = (MyGLSurfaceView) findViewById(R.id.surfaceview);
        initDecoder();
         
        mConnectStateTextView = (TextView)findViewById(R.id.ConnectStatePreviewTextView);

    }
    
    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        mTimer = new Timer();
        Task task = new Task();
        mTimer.schedule(task, 0, 500);

        super.onResume();
    }
    
    @Override
    protected void onPause()
    {
        // TODO Auto-generated method stub
        if(mTimer!=null) {            
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }

        super.onPause();
    }
    
    @Override
    protected void onStop()
    {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        try
        {
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (mVideoDecoder != null) {
            mVideoDecoder.stopVideoDecoder();
            mVideoDecoder = null;
        }
        
        super.onDestroy();
    }

    /** 
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG ,"onReturn");  
        this.finish();
    }

    /**
     * Description : init decoder
     */
    private void initDecoder() {
        DJIDrone.getDjiCamera().setDecodeType(DecoderType.Hardware);
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

    }
}
