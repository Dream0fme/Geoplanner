package urum.geoplanner.service;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.google.android.gms.maps.model.LatLng;

import static urum.geoplanner.utils.Utils.getLastLocation;

public class LastLocation extends LiveData<LatLng> {

    Context context;
    @SuppressLint("StaticFieldLeak")
    private static LastLocation instance;

    public static LastLocation getInstance(LatLng location, Context context) {
        if (instance == null) {
            instance = new LastLocation(location, context);
        }
        return instance;
    }

    private LastLocation(LatLng value, Context context) {
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
