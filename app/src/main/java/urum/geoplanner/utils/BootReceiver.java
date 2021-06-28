package urum.geoplanner.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;

import urum.geoplanner.service.LocationService;

import static urum.geoplanner.utils.Constants.CHECK_BOX_AUTO_START;
import static urum.geoplanner.utils.Constants.SWITCH_SERVICE;


public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "mytag";
    SharedPreferences sharedPreferences;
    boolean activeService;
    boolean autostartService;

    @Override
    public void onReceive(Context context, Intent intent) {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        activeService = sharedPreferences.getBoolean(SWITCH_SERVICE, false);
        autostartService = sharedPreferences.getBoolean(CHECK_BOX_AUTO_START, false);
        String action = intent.getAction();
        Log.d(TAG, "getAction: " + action);
        if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) ||
                action.equalsIgnoreCase(Intent.ACTION_REBOOT) ||
                action.equalsIgnoreCase("android.intent.action.QUICKBOOT_POWERON") ||
                action.equalsIgnoreCase("com.htc.intent.action.QUICKBOOT_POWERON") ||
                action.equalsIgnoreCase(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            if (!isServiceRunning(LocationService.class, context) & autostartService & activeService) {
                Log.d(TAG, "Service: Autorun");
                Intent serviceIntent = new Intent(context, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.d(TAG, "Service: already running or stop active ");
            }
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass, Context context) {
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
}

/*Intent App = new Intent(context, MainActivity.class);
            App.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(App);
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
            Intent intentService = new Intent(context, LocationUpdatesService.class);
            intentService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                //ContextCompat.startForegroundService(context, serviceIntent);
             */