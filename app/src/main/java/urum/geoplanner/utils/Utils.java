package urum.geoplanner.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import urum.geoplanner.R;

import static android.content.Context.LOCATION_SERVICE;
import static urum.geoplanner.utils.Constants.TAG;

public class Utils {

    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }


    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

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
    public static LatLng getLastKnownLocation(Context context) {
        LatLng location = null;
        try {
            LocationManager lm;
            lm = (LocationManager) context.getSystemService(LOCATION_SERVICE);

            boolean isGPSEnabled = false;
            boolean isNetworkEnabled = false;
            boolean isPassiveEnabled = false;

            if (lm != null) {
                isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                isPassiveEnabled = lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
            }
            Location l;
            if (isGPSEnabled || isNetworkEnabled) {
                if (isGPSEnabled) {
                    //Toast.makeText(context, "GPS_PROVIDER включен", Toast.LENGTH_SHORT).show();
                    l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else {
                    //Toast.makeText(context, "NETWORK_PROVIDER включен", Toast.LENGTH_SHORT).show();
                    l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (l != null) {
                    location = new LatLng(l.getLatitude(), l.getLongitude());
                    //Toast.makeText(context, getAddressFromLocation(location, null, context), Toast.LENGTH_SHORT).show();
                    return location;
                }
            } else {
                Toast.makeText(context, "GPS/NETWORK выключен", Toast.LENGTH_SHORT).show();
                if (isPassiveEnabled) {
                    //Toast.makeText(context, "PASSIVE_PROVIDER включен", Toast.LENGTH_SHORT).show();
                    l = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    location = new LatLng(l.getLatitude(), l.getLongitude());
                    //Toast.makeText(context, getAddressFromLocation(location, null, context), Toast.LENGTH_SHORT).show();
                }
                return location;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return location;
        }
        return location;
    }

    @SuppressLint("MissingPermission")
    public static LatLng getLastLocation(Context context) {
        final LatLng[] latLng = {getLastKnownLocation(context)};
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation().addOnSuccessListener((Activity) context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    latLng[0] = new LatLng(location.getLatitude(), location.getLongitude());
                }
            }
        });
        return latLng[0];
    }

    public static ArrayList<String> getAddressInfo(String locationName, Context context) {
        ArrayList<String> list = new ArrayList<>();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> a = geocoder.getFromLocationName(locationName.trim(), 1);
            for (int i = 0; i < a.size(); i++) {
                String city = a.get(i).getLocality();
                String street = a.get(i).getThoroughfare();
                String future = a.get(i).getFeatureName();
                if (city != null) {
                    list.add(city);
                }
                if (city != null & street != null) {
                    list.clear();
                    String[] subStr;
                    subStr = locationName.split("\\s|,");
                    for (int j = 0; j < subStr.length; j++) {
                        if (startsWithIgnoreCase(street, subStr[j]) || containsIgnoreCase(future, subStr[j]) || containsIgnoreCase(street, subStr[j])) {
                            list.add(city + "," + " " + street);
                        } else list.clear();
                    }
                }

                if (city != null & future != null) {
                    list.clear();
                    String[] subStr;
                    subStr = locationName.split("\\s|,");
                    for (int j = 0; j < subStr.length; j++) {
                        if (startsWithIgnoreCase(future, subStr[j]) || containsIgnoreCase(future, subStr[j])) {
                            if (!city.equalsIgnoreCase(future)) {
                                list.clear();
                                list.add(city + ", " + future);
                            } else if (subStr.length < 2) {
                                list.clear();
                                list.add(city);
                            }
                        } else list.clear();
                    }
                }
                if (city != null & street != null & future != null) {
                    list.clear();
                    String[] subStr;
                    subStr = locationName.split("\\s|,");
                    for (int j = 0; j < subStr.length; j++) {
                        if (startsWithIgnoreCase(street, subStr[j])) {
                            if (!street.equalsIgnoreCase(future)) {
                                list.clear();
                                list.add(city + "," + " " + street + ", " + future);
                            } else {
                                list.clear();
                                list.add(city + "," + " " + street);
                            }
                        } else list.clear();
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static StringBuilder getAddressFromLocation(LatLng addPoint, Geocoder geocoder, Context context) {

        if (geocoder == null) {
            geocoder = new Geocoder(context, Locale.getDefault());
        }
        final StringBuilder addressLine = new StringBuilder();
        try {
            List<Address> addresses;
            addresses = geocoder.getFromLocation(addPoint.latitude, addPoint.longitude, 5);
            if (addresses == null || addresses.size() == 0) {
                Toast.makeText(context, "Адрес не найден. Попробуйте ещё раз.", Toast.LENGTH_LONG).show();
            } else {
                Address address = addresses.get(0);
                for (int i = 0; i < addresses.size(); i++) {
                    if (addresses.size() == 1) {
                        break;
                    } else {
                        if (address.getLocality() != null || address.getThoroughfare() != null || address.getFeatureName() != null) {
                            address = addresses.get(i);
                            break;
                        }
                    }
                }
                if (address.getLocality() == null || address.getThoroughfare() == null || address.getFeatureName() == null) {
                    addressLine.append(round(addPoint.latitude, 5))
                            .append(",")
                            .append(context.getString(R.string.blank_space))
                            .append(round(addPoint.longitude, 5));
                } else {
                    addressLine.append(address.getLocality())
                            .append(",")
                            .append(context.getString(R.string.blank_space))
                            .append(address.getThoroughfare())
                            .append(",")
                            .append(context.getString(R.string.blank_space))
                            .append(address.getFeatureName());
                }
                return addressLine;
            }
        } catch (IOException ioException) {
            Log.d(TAG, "Сервис не работает", ioException);
            Toast.makeText(context, "Сервис не работает", Toast.LENGTH_LONG).show();
            return addressLine;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Используется неверная широта или долгота", e);
            Toast.makeText(context, "Используется неверная широта или долгота", Toast.LENGTH_LONG).show();
            return addressLine;
        }
        return addressLine;
    }
}
