package urum.geoplanner.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleService;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import urum.geoplanner.R;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.ui.MainActivity;
import urum.geoplanner.utils.Constants;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static urum.geoplanner.utils.Constants.*;



public class LocationService extends LifecycleService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    private boolean mChangingConfiguration = false;

    SharedPreferences sharedPreferences;
    int updateLocationInSeconds;
    int updateLocationDistance;

    int valueSeconds;
    float valuseMeters;

    private static final int UPDATE_INTERVAL_3_SEC = 6000;
    private static final int UPDATE_INTERVAL_5_SEC = 10000;
    private static final int UPDATE_INTERVAL_10_SEC = 20000;
    private static final int UPDATE_INTERVAL_15_SEC = 30000;
    private static final float UPDATE_DISTANCE_0 = 0f;
    private static final float UPDATE_DISTANCE_1 = 1f;
    private static final float UPDATE_DISTANCE_2 = 2f;
    private static int GEO_ACCURACY = 45;


    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private LocationCallback mLocationCallback;

    private NotificationManager mNotificationManager;
    private NotificationManagerCompat notificationManager;

    private PlaceViewModel mPlaceViewModel;
    private List<Place> places;

    private Location currentLoc;

    // Сессионый справочник вхождений в области
    private Map<Integer, Boolean> inProximity = new HashMap<>();

    private ActionManager actionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager = NotificationManagerCompat.from(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        updateLocationInSeconds = Integer.parseInt(sharedPreferences.getString(LIST_SECONDS, "0"));
        updateLocationDistance = Integer.parseInt(sharedPreferences.getString(LIST_METERS, "0"));
        GEO_ACCURACY = sharedPreferences.getInt(Constants.GEO_ACCURACY, 45);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Сервис " + getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID + getString(R.string.app_name), name, NotificationManager.IMPORTANCE_MIN);
            mChannel.setShowBadge(true);
            mChannel.enableVibration(false);
            // mChannel.setAllowBubbles(false);
            mChannel.setBypassDnd(false);
            mChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        try {
            boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

            if (startedFromNotification) {
                stopSelf(startId);
                Log.i(TAG, "Сервис закрыт");
                Intent intentClose = new Intent("closeApp");
                sendBroadcast(intentClose);
            } else {
                try {
                    Log.i(TAG, "Сервис запущен");
                    places = new ArrayList<>();

                    mPlaceViewModel = new PlaceViewModel(getApplication());
                    mPlaceViewModel.getPlacesToService().observe(this, new androidx.lifecycle.Observer<List<Place>>() {
                        @Override
                        public void onChanged(@Nullable List<Place> placesUpdate) {
                            places.clear();
                            Log.d(TAG, "IN SERVICE: " + placesUpdate.toString());
                            places = placesUpdate;
                            if (!places.isEmpty()) {
                                for (Place place : places) {
                                    if (!inProximity.containsKey((int) place.getId()))
                                        inProximity.put((int) place.getId(), false);
                                }
                            }
                        }
                    });

                    actionManager = ActionManager.getInstance(this, mPlaceViewModel);

                } catch (Exception e) {
                    Log.e("mytag", "" + e.getMessage());
                }

                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        currentLoc = locationResult.getLastLocation();
                        Log.d(TAG, "lat: " + currentLoc.getLatitude() + ", lng: " + currentLoc.getLongitude() +", accuracy : " + currentLoc.getAccuracy());
                        Log.d(TAG, "GEO_ACCURACY: " + GEO_ACCURACY);
                        if (currentLoc.getAccuracy() <= GEO_ACCURACY) {
                            checkLocationInProximity(currentLoc);
                        }
                    }
                };

                switch (updateLocationInSeconds) {
                    case 0:
                    default:
                        valueSeconds = UPDATE_INTERVAL_3_SEC;
                        break;
                    case 1:
                        valueSeconds = UPDATE_INTERVAL_5_SEC;
                        break;
                    case 2:
                        valueSeconds = UPDATE_INTERVAL_10_SEC;
                        break;
                    case 3:
                        valueSeconds = UPDATE_INTERVAL_15_SEC;
                        break;
                }

                switch (updateLocationDistance) {
                    case 0:
                    default:
                        valuseMeters = UPDATE_DISTANCE_0;
                        break;
                    case 1:
                        valuseMeters = UPDATE_DISTANCE_1;
                        break;
                    case 2:
                        valuseMeters = UPDATE_DISTANCE_2;
                        break;
                }

                createLocationRequest(valueSeconds, valuseMeters);
                requestLocationUpdates();

                if (!mChangingConfiguration & !serviceIsRunningInForeground(this)) {
                    Log.i(TAG, "Сервис начал работу на переднем плане");
                    startForeground(NOTIFICATION_ID, getNotification());
                } else {
                    startService(new Intent(this, LocationService.class));
                }
            }
        } catch (
                NullPointerException e) {
            Log.e("mytag", "" + e.getMessage());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        Log.i(TAG, "Подключение клиента");
        Log.i(TAG, "Переходим на фоновый режим");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    private void checkLocationInProximity(Location location) {
        float distance;
        double lat;
        double lng;
        int radius;
        int id;

        for (Place place : places) {
            id = (int) place.getId();
            lat = place.getLatitude();
            lng = place.getLongitude();
            distance = getDistance(lat, lng, location);
            radius = place.getCondition();

            try {
                if (inProximity.get(id) != null) {
                    if (distance <= radius && !inProximity.get(id)) {
                        Log.d(TAG, "entering");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            inProximity.replace(id, true);
                        } else inProximity.put(id, true);
                        entering(place);
                    } else if (distance > radius && inProximity.get(id)) {
                        Log.d(TAG, "exiting");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            inProximity.replace(id, false);
                        } else inProximity.put(id, false);
                        exiting(place);
                    }
                }
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void createNotificationChannel(int pos) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String changeId;
            /*if (entering) {
                changeId = "enter" + id;
            } else {
                changeId = "exit" + id;
            }*/
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID + pos, channel.get(pos), NotificationManager.IMPORTANCE_HIGH);
            //mChannel.setDescription("zxc");
            mChannel.setShowBadge(true);
            mChannel.enableVibration(true);
            //mChannel.setAllowBubbles(true);
            mChannel.setBypassDnd(true);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(mChannel);
        }
    }

    private void entering(Place place) {
        String namePlace = place.getName();
        int id = (int) place.getId();
        int checkBoxEnter = place.getCheckboxEnter();
        int pos = place.getPosition();
        String number = place.getNumber();
        String sms = place.getSms();
        createNotificationChannel(pos);
        if (checkBoxEnter == 1) {
            switch (pos) {
                case 0:
                    //wakeUp(context);
                    notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Звонок на номер: " + number, pos, number, ""));
                    actionManager.makePhoneCall(number);
                    break;
                case 1:
                    // wakeUp(context);
                    notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Попытка отправить SMS на номер: " + number, pos, number, sms));
                    actionManager.sendToSms(number, sms);
                    Log.d(TAG, "Отправляем сообщение '" + sms + "' на телефон: " + number);
                    break;
                case 2:
                    // wakeUp(context);
                    Log.d(TAG, "Уведомление, канал: " + CHANNEL_ID + pos);
                    notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, sms, pos, "", ""));
                    break;
                case 3:
                    Log.d(TAG, "Выключаем звук");
                    //wakeUp(context);
                    if (mNotificationManager != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                                Intent dndIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                dndIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(dndIntent);
                            } else {
                                actionManager.onDoNotDisturbMode();
                            }
                        }
                        notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Режим 'Не беспокоить' включен", pos, "", ""));
                    }
                    break;
                case 4:
                    //wakeUp(context);
                    Log.d(TAG, "Включаем звук");
                    if (mNotificationManager != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                                Intent dndIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                dndIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(dndIntent);
                            } else {
                                actionManager.offDoNotDisturbMode();
                            }
                        }
                        notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Режим 'Не беспокоить' выключен", pos, "", ""));
                    }
                    break;
                case 5:
                    //wakeUp(context);
                    Log.d(TAG, "Включаем WiFi");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        actionManager.onWifi();
                        notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Включите Wi-Fi в настройках", pos, "", ""));
                    } else {
                        actionManager.onWifi();
                        notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Wi-Fi включен", pos, "", ""));
                    }
                    break;
                case 6:
                    //wakeUp(context);
                    Log.d(TAG, "Выключаем WiFi");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //runCommand("am start -a Android.intent.action.MAIN -n com.Android.settings/.wifi.WifiSettings");
                        actionManager.offWifi();
                        notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Выключите Wi-Fi в настройках", pos, "", ""));
                    } else {
                        actionManager.offWifi();
                        notificationManager.notify(NOTIFICATION_ID + pos, actionManager.getNotification(namePlace, true, id, "Wi-Fi выключен", pos, "", ""));
                    }
                    break;
            }
        }
    }

    private void exiting(Place place) {
        String namePlace = place.getName();
        int id = (int) place.getId();
        int checkBoxExit = place.getCheckboxExit();
        int posExit = place.getPositionExit();
        String numberExit = place.getNumberExit();
        String smsExit = place.getSmsExit();
        createNotificationChannel(posExit);
        if (checkBoxExit == 1) {
            switch (posExit) {
                case 0:
                    //wakeUp(context);
                    notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Звонок на номер: " + numberExit, posExit, numberExit, ""));
                    actionManager.makePhoneCall(numberExit);
                    break;
                case 1:
                    // wakeUp(context);
                    notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Попытка отправить SMS на номер: " + numberExit, posExit, numberExit, smsExit));
                    actionManager.sendToSms(numberExit, smsExit);
                    Log.d(TAG, "Отправляем сообщение '" + smsExit + "' на телефон: " + numberExit);
                    break;
                case 2:
                    // wakeUp(context);
                    Log.d(TAG, "Уведомление");
                    notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, smsExit, posExit, "", ""));
                    break;
                case 3:
                    Log.d(TAG, "Выключаем звук");
                    //wakeUp(context);
                    if (mNotificationManager != null) {
                        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                            Intent dndIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            dndIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(dndIntent);
                        } else {
                            actionManager.onDoNotDisturbMode();
                        }
                        notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Режим 'Не беспокоить' включен", posExit, "", ""));
                    }
                    break;
                case 4:
                    //wakeUp(context);
                    Log.d(TAG, "Включаем звук");
                    if (mNotificationManager != null) {
                        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                            Intent dndIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            dndIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(dndIntent);
                        } else {
                            actionManager.offDoNotDisturbMode();
                        }
                        notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Режим 'Не беспокоить' выключен", posExit, "", ""));
                    }
                    break;
                case 5:
                    //wakeUp(context);
                    Log.d(TAG, "Включаем WiFi");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        actionManager.onWifi();
                        notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Включите Wi-Fi в настройках", posExit, "", ""));
                    } else {
                        actionManager.onWifi();
                        notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Wi-Fi включен", posExit, "", ""));
                    }
                    break;
                case 6:
                    //wakeUp(context);
                    Log.d(TAG, "Выключаем WiFi");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //runCommand("am start -a Android.intent.action.MAIN -n com.Android.settings/.wifi.WifiSettings");
                        actionManager.offWifi();
                        notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Выключите Wi-Fi в настройках", posExit, "", ""));
                    } else {
                        actionManager.offWifi();
                        notificationManager.notify(NOTIFICATION_ID + posExit, actionManager.getNotification(namePlace, false, id, "Wi-Fi выключен", posExit, "", ""));
                    }
                    break;
            }
        }

    }

    private float getDistance(double latitude, double longitude, Location location) {
        float[] results = new float[1];
        Location.distanceBetween(latitude,
                longitude,
                location.getLatitude(),
                location.getLongitude(),
                results);
        return results[0];
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "Возвращение клиента");
        Log.i(TAG, "Переходим на фоновый режим");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Последний клиент отсоединился от сервиса");
        if (!mChangingConfiguration) {
            Log.e(TAG, "Сервис начал работу на переднем плане");
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy");
        if (mLocationCallback != null)
            removeLocationUpdates();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                    Integer.MAX_VALUE)) {
                if (getClass().getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        Log.e(TAG, "Сервис работает на переднем плане");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                FLAG_UPDATE_CURRENT);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 1,
                new Intent(this, MainActivity.class), FLAG_UPDATE_CURRENT);

        Intent openSettings = new Intent(this, MainActivity.class);
        openSettings.putExtra(FROM_NOTIFICATION_TO_SETTINGS, true);

        PendingIntent settingsPending = PendingIntent.getActivity(this, 3,
                openSettings, FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.settings_notification), settingsPending)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                        servicePendingIntent)
                .setContentTitle("Сервис запущен")
                .setOngoing(true)
                .setContentText(DateFormat.getDateTimeInstance().format(new Date()))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launch_gps))
                .setPriority(Notification.PRIORITY_MIN)
                .setWhen(System.currentTimeMillis())
                .setVibrate(new long[]{0L});

        builder.setContentIntent(activityPendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_location_notification);
            builder.setColor(getResources().getColor(R.color.myPeach));
        } else {
            builder.setSmallIcon(R.mipmap.ic_launch_gps);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID + getString(R.string.app_name));
        }
        return builder.build();
    }

    public void createLocationRequest(int interval, float meters) {
        Log.d(TAG, interval + " , " + meters);
        //mLocationRequest = new LocationRequest();
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(interval / 2);
        if (meters != 0f) {
            mLocationRequest.setSmallestDisplacement(meters);
        }
    }

    public void requestLocationUpdates() {
        Log.i(TAG, "Обновление геолокации");
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + e);
        }
    }

    public void removeLocationUpdates() {
        Log.i(TAG, "Отключение обновлений геолокации");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + e);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case LIST_SECONDS:
                updateLocationInSeconds = Integer.parseInt(sharedPreferences.getString(s, "0"));

                switch (updateLocationInSeconds) {
                    case 0:
                        removeLocationUpdates();
                        valueSeconds = UPDATE_INTERVAL_3_SEC;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                    case 1:
                        removeLocationUpdates();
                        valueSeconds = UPDATE_INTERVAL_5_SEC;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                    case 2:
                        removeLocationUpdates();
                        valueSeconds = UPDATE_INTERVAL_10_SEC;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                    case 3:
                        removeLocationUpdates();
                        valueSeconds = UPDATE_INTERVAL_15_SEC;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                }
                break;
            case LIST_METERS:
                updateLocationDistance = Integer.parseInt(sharedPreferences.getString(s, "0"));
                switch (updateLocationDistance) {
                    case 0:
                        removeLocationUpdates();
                        valuseMeters = UPDATE_DISTANCE_0;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                    case 1:
                        removeLocationUpdates();
                        valuseMeters = UPDATE_DISTANCE_1;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                    case 2:
                        removeLocationUpdates();
                        valuseMeters = UPDATE_DISTANCE_2;
                        createLocationRequest(valueSeconds, valuseMeters);
                        requestLocationUpdates();
                        break;
                }
                break;

            case Constants.GEO_ACCURACY:
                GEO_ACCURACY = sharedPreferences.getInt(s, 45);
                break;
        }
    }
}
