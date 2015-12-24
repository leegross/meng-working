import static com.dji.sdkdemo.Constants.EARTHS_RADIUS_IN_METERS;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.toRadians;

/**
 * Created by leegross on 12/24/15.
 */
public class Temp {
    public static void main(String [] args){
        double homeLocationLongitude = 113.953640;
        double homeLocationLatitude = 22.537018;
        double new_long = homeLocationLongitude + toDegrees(atan2(1.0, EARTHS_RADIUS_IN_METERS));
        double new_lat = homeLocationLatitude - toDegrees(atan2(1.0, EARTHS_RADIUS_IN_METERS));
        double delta_x = (EARTHS_RADIUS_IN_METERS * tan(toRadians(new_long - homeLocationLongitude)));
        System.out.println("new long: " + new_long);
        System.out.println("new lat: " + new_lat);
        System.out.println("delta x: " + delta_x);

        System.out.println("atan(180): " + atan2(1, 1));

    }
}
