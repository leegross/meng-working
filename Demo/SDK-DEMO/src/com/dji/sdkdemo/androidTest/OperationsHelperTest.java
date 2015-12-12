package com.dji.sdkdemo.androidTest;

import static android.util.FloatMath.sqrt;
import static com.dji.sdkdemo.util.OperationsHelper.*;

/**
 * Created by leegross on 12/4/15.
 */
public class OperationsHelperTest {

    public static boolean testNormalize(){
        float[] v = {1.0f, 1.0f, 1.0f};
        float[] normv = normalizeV(v);
        float m = sqrt(3.0f);
        if (normv[0] == 1.0/m && normv[1] == 1.0/m && normv[2] == 1.0/m){
            return true;
        }
        return false;
    }

    public static void main(String[] args){

        testNormalize();
    }
}
