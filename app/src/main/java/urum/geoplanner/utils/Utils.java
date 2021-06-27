package urum.geoplanner.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static android.content.Context.LOCATION_SERVICE;

public class Utils {
    public static NavController findNavController(@NonNull Fragment fragment) {
        View view = fragment.getView();
        if (view != null) {
            return Navigation.findNavController(view);
        } else return null;
    }

    public static void enableLayout(ViewGroup layout, boolean enable) {
        layout.setEnabled(enable);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                enableLayout((ViewGroup) child, enable);
            } else {
                child.setEnabled(enable);
            }
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @SuppressLint("MissingPermission")
    public static LatLng getLastLocation(Context context) {
        LatLng location = null;
        try {
            LocationManager lm;
            lm = (LocationManager) context.getSystemService(LOCATION_SERVICE);

            boolean isGPSEnabled = false;
            boolean isNetworkEnabled = false;

            if (lm != null) {
                isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
            Location l;
            if (isGPSEnabled || isNetworkEnabled) {
                if (isGPSEnabled) {
                    l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else {
                    l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (l != null) {
                    location = new LatLng(l.getLatitude(), l.getLongitude());
                    return location;
                }
            } else {
                Toast.makeText(context, "GPS/NETWORK выключен", Toast.LENGTH_SHORT).show();
                l = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                location = new LatLng(l.getLatitude(), l.getLongitude());
                return location;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return location;
        }
        return location;
    }
}
