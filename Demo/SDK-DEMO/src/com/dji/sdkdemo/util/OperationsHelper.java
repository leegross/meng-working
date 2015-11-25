package com.dji.sdkdemo.util;

import static java.lang.Math.abs;

/**
 * Created by leegross on 11/23/15.
 */
public class OperationsHelper {
    public static boolean floatEquals(float v1, float v2) {
        if (abs(v1 - v2) < .00001) {
            return true;
        }
        return false;
    }

    public static float[] addArrays(float[] a1, float[] a2){
        if (a1.length != a2.length) {
            return new float[0];
        }
        float[] sum = new float[a1.length];
        for (int i = 0; i < a1.length; i++) {
            sum[i] = a1[i] + a2[i];
        }
        return sum;
    }

}
