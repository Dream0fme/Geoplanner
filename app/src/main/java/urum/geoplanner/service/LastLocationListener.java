package urum.geoplanner.service;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.google.android.gms.maps.model.LatLng;

import static urum.geoplanner.utils.Utils.getLastLocation;

public class LastLocationListener extends LiveData<LatLng> {

    Context context;
    @SuppressLint("StaticFieldLeak")
    private static LastLocationListener instance;

    public static LastLocationListener getInstance(LatLng location, Context context) {
        if (instance == null) {
            instance = new LastLocationListener(location, context);
        }
        return instance;
    }

    private LastLocationListener(LatLng value, Context context) {
        super(value);
        this.context = context;
    }

    @Override
    public void onActive() {
        setValue(getLastLocation(context));
    }

    @Override
    public void onInactive() {

    }
}
