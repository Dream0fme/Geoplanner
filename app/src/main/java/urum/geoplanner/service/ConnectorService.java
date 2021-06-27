package urum.geoplanner.service;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceManager;

import urum.geoplanner.ui.ActivityHome;
import urum.geoplanner.ui.MainActivity;

import static urum.geoplanner.utils.Constants.switchService;
import static urum.geoplanner.utils.Constants.TAG;

public class ConnectorService implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private LocationService mService = null;
    private boolean mBound = false;

    private final Context context;
    private SharedPreferences sharedPreferences;
    private boolean activeService;

    public ConnectorService(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        activeService = sharedPreferences.getBoolean(switchService, true);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void createConn() {
        //Log.d(TAG, "ON_CREATE");
        if (context instanceof ActivityHome & activeService) {
            startService();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void connect() {
        //Log.d(TAG, "ON_START");
        bindService();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void reconnect() {
        //Log.d(TAG, "ON_RESUME");
        bindService();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void disconnect() {
        //Log.d(TAG, "ON_STOP");
        unbindService();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        //Log.d(TAG, "ON_DESTROY");
        if (context instanceof MainActivity) {
            //stopService();
        }
    }

    public void bindService() {
        if (!mBound & activeService) {
            context.bindService(new Intent(context, LocationService.class), mServiceConnection, 0);
            if (!isMyServiceRunning(LocationService.class)) {
                context.bindService(new Intent(context, LocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    public void unbindService() {
        if (isMyServiceRunning(LocationService.class) & mBound & activeService) {
            context.unbindService(mServiceConnection);
            mBound = false;
        }
    }

    public void startService() {
        if (!isMyServiceRunning(LocationService.class)) {
            context.startService(new Intent(context, LocationService.class));
            //context.bindService(new Intent(context, LocationService.class), mServiceConnection, 0);
        }
    }

    public void stopService() {
        if (isMyServiceRunning(LocationService.class)) {
            context.stopService(new Intent(context, LocationService.class));
        }
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case switchService:
                activeService = sharedPreferences.getBoolean(s, true);
                if (activeService) {
                    startService();
                    bindService();
                } else {
                    stopService();
                }
                break;
        }
    }
}
