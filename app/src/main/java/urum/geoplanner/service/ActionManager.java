package urum.geoplanner.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import urum.geoplanner.R;
import urum.geoplanner.db.entities.Place;
import urum.geoplanner.db.entities.PlaceLog;
import urum.geoplanner.ui.MainActivity;
import urum.geoplanner.ui.PlaceActivity;
import urum.geoplanner.viewmodel.PlaceViewModel;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static urum.geoplanner.utils.Constants.CHANNEL_ID;
import static urum.geoplanner.utils.Constants.DELIVER_SMS_FLAG;
import static urum.geoplanner.utils.Constants.FROM_NOTIFICATION_TO_PLACEACTIVITY;
import static urum.geoplanner.utils.Constants.ID_FROM_NOTIFICATION;
import static urum.geoplanner.utils.Constants.NUMBER_DELIVER;
import static urum.geoplanner.utils.Constants.NUMBER_SENT;
import static urum.geoplanner.utils.Constants.SENT_SMS_FLAG;
import static urum.geoplanner.utils.Constants.SMS_BODY;
import static urum.geoplanner.utils.Constants.SMS_DELIVER;
import static urum.geoplanner.utils.Constants.SMS_SENT;
import static urum.geoplanner.utils.Constants.TAG;


@SuppressWarnings("MissingPermission")
public class ActionManager {

    ReceiverManager receiverManager;
    private Context context;

    @SuppressLint("StaticFieldLeak")
    private static ActionManager instance;
    private PlaceViewModel placeViewModel;

    public static ActionManager getInstance(Context context, PlaceViewModel placeViewModel) {
        if (instance == null) {
            instance = new ActionManager();
            instance.context = context;
            instance.placeViewModel = placeViewModel;
        }
        return instance;
    }

    BroadcastReceiver deliverReceiver = new BroadcastReceiver() {
        private static final String CHANNEL = "channel_03";
        private static final int NOTIFICATION = 30;

        @Override
        public void onReceive(Context c, Intent in) {
            String number = in.getStringExtra(NUMBER_DELIVER);
            String sms = in.getStringExtra(SMS_DELIVER);
            NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Уведомление о доставке сообщения";
                NotificationChannel mChannel =
                        new NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationManager.createNotificationChannel(mChannel);
            }
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "Сообщение доставлено!");
                    if (mNotificationManager != null) {
                        //wakeUp(c);
                        mNotificationManager.notify(NOTIFICATION, getNotification(c, "", number, ""));
                    }
                    break;
                default:
                    Log.d(TAG, "Сообщение НЕ доставлено!");
                    if (mNotificationManager != null) {
                        //wakeUp(c);
                        mNotificationManager.notify(NOTIFICATION, getNotification(c, "НЕ ", number, sms));
                    }
                    break;
            }
        }

        private Notification getNotification(Context context, String result, String number, String sms) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 0,
                    intent, 0);

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("СООБЩЕНИЕ " + result + "ДОСТАВЛЕНО!");
            bigTextStyle.bigText("На номер: " + number + '\n' + '\n' + addDateNow());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_launch, context.getString(R.string.launch_activity),
                            activityPendingIntent)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setContentTitle("СООБЩЕНИЕ " + result + "ДОСТАВЛЕНО!")
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setStyle(bigTextStyle)
                    .setWhen(System.currentTimeMillis());

            Intent action;
            PendingIntent actionPendingIntent;
            if (result.equals("")) {
                action = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                action.putExtra(SMS_BODY, sms);
                action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actionPendingIntent = PendingIntent.getActivity(context, 0,
                        action, 0);
                builder.addAction(R.drawable.ic_launch, "Отправить SMS", actionPendingIntent);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP & Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                builder.setSmallIcon(R.drawable.ic_location_notification);
                builder.setColor(context.getResources().getColor(R.color.myPeach));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //builder.setSmallIcon(R.mipmap.ic_launch_gps);
                builder.setSmallIcon(R.drawable.ic_location_notification);
                builder.setColor(context.getResources().getColor(R.color.myPeach));
            } else {
                builder.setSmallIcon(R.mipmap.ic_launch_gps);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL);
            }
            return builder.build();
        }
    };

    BroadcastReceiver sentReceiver = new BroadcastReceiver() {
        private static final String CHANNEL = "channel_02";
        private static final int NOTIFICATION = 20;

        @Override
        public void onReceive(Context c, Intent in) {
            String number = in.getStringExtra(NUMBER_SENT);
            String sms = in.getStringExtra(SMS_SENT);
            NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Уведомление о отправке сообщения";
                NotificationChannel mChannel =
                        new NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);
                if (mNotificationManager != null) {
                    mNotificationManager.createNotificationChannel(mChannel);
                }
            }
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "Сообщение отправлено!");
                    if (mNotificationManager != null) {
                        mNotificationManager.notify(NOTIFICATION, getNotification(c, "", number, ""));
                    }
                    break;
                default:
                    Log.d(TAG, "Сообщение НЕ отправлено!");
                    if (mNotificationManager != null) {
                        mNotificationManager.notify(NOTIFICATION, getNotification(c, "НЕ ", number, sms));
                    }
                    break;
            }
        }

        private Notification getNotification(Context context, String result, String number, String sms) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 0,
                    intent, 0);

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("СООБЩЕНИЕ " + result + "ОТПРАВЛЕНО!");
            bigTextStyle.bigText("На номер: " + number + '\n' + '\n' + addDateNow());
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_launch, context.getString(R.string.launch_activity),
                            activityPendingIntent)
                    .setContentTitle("СООБЩЕНИЕ " + result + "ОТПРАВЛЕНО!")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setStyle(bigTextStyle)
                    .setWhen(System.currentTimeMillis());

            Intent action;
            PendingIntent actionPendingIntent;
            if (result.equals("")) {
                action = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                action.putExtra(SMS_BODY, sms);
                action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actionPendingIntent = PendingIntent.getActivity(context, 0,
                        action, 0);
                builder.addAction(R.drawable.ic_launch, "Отправить SMS", actionPendingIntent);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP & Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                builder.setSmallIcon(R.drawable.ic_location_notification);
                builder.setColor(context.getResources().getColor(R.color.myPeach));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setSmallIcon(R.drawable.ic_location_notification);
                builder.setColor(context.getResources().getColor(R.color.myPeach));
            } else {
                builder.setSmallIcon(R.mipmap.ic_launch_gps);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL); // Channel ID
            }
            return builder.build();
        }
    };

    // Костыль какой-то но чет не продумал раньше
    private void addToLog(String info, String timeText, int id) {

        Place place = placeViewModel.getPlace(id);

        PlaceLog placeLog = new PlaceLog();
        placeLog.setName(place.getName());
        placeLog.setAddress(place.getAddress());
        placeLog.setCondition(place.getCondition());
        placeLog.setSms(info);
        placeLog.setDate(timeText);

        placeViewModel.insertToLog(placeLog);
    }

    public Notification getNotification(String namePlace, boolean proximity, int id, String msg, int pos, String number, String sms) {

        String enter;
        if (proximity) {
            enter = ("Вы рядом с местом '" + namePlace + "'").trim();
        } else {
            enter = ("Вы ушли из места '" + namePlace + "'").trim();
        }

        Date currentDate = new Date();
        DateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String timeText = timeFormat.format(currentDate);

        if (proximity) {
            if (pos == 2) {
                addToLog("Уведомление при входе: " + msg, timeText, id);
            } else {
                addToLog("При входе: " + msg, timeText, id);
            }
        } else {
            if (pos == 2) {
                addToLog("Уведомление при выходе: " + msg, timeText, id);
            } else {
                addToLog("При выходе: " + msg, timeText, id);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        Intent editPlace = new Intent(context, PlaceActivity.class);
        editPlace.putExtra(ID_FROM_NOTIFICATION, (long) id);
        editPlace.putExtra(FROM_NOTIFICATION_TO_PLACEACTIVITY, true);

        PendingIntent editIntent = PendingIntent.getActivity(context, 4,
                editPlace, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 5,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(enter);
        bigTextStyle.bigText(msg + '\n' + '\n' + timeText);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .addAction(R.drawable.ic_launch, context.getString(R.string.edit_place_notification),
                        editIntent)
                .setAutoCancel(true)
                .setContentTitle(enter)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setStyle(bigTextStyle)
                .setShowWhen(true)
//                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        builder.setContentIntent(activityPendingIntent);

        Intent action;
        PendingIntent actionPendingIntent;
        switch (pos) {
            case 0:
                action = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actionPendingIntent = PendingIntent.getActivity(context, 0,
                        action, 0);
                builder.addAction(R.drawable.ic_launch, "Позвонить ещё раз", actionPendingIntent);
                break;
            case 1:
                action = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
                action.putExtra("sms_body", sms);
                action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actionPendingIntent = PendingIntent.getActivity(context, 0,
                        action, 0);
                builder.addAction(R.drawable.ic_launch, "Редактировать SMS", actionPendingIntent);
                break;
            case 5:
            case 6:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    action = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                } else {
                    action = new Intent(Settings.ACTION_WIFI_SETTINGS);
                }
                action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actionPendingIntent = PendingIntent.getActivity(context, 0,
                        action, 0);
                builder.addAction(R.drawable.ic_launch, "Открыть настройки Wi-Fi", actionPendingIntent);
                break;
            case 2:
            case 3:
            case 4:
            default:
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP & Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            builder.setSmallIcon(R.drawable.ic_location_notification);
            builder.setColor(context.getResources().getColor(R.color.myPeach));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //builder.setSmallIcon(R.mipmap.ic_launch_gps);
            builder.setSmallIcon(R.drawable.ic_location_notification);
            builder.setColor(context.getResources().getColor(R.color.myPeach));
        } else {
            builder.setSmallIcon(R.mipmap.ic_launch_gps);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID + pos);
        }
        return builder.build();
    }

    public void makePhoneCall(String s) {
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + s));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }

    public void sendToSms(String number, String sms) {
        Intent sentIn = new Intent(SENT_SMS_FLAG);
        sentIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sentIn.putExtra(NUMBER_SENT, number);
        sentIn.putExtra(SMS_SENT, sms);
        final PendingIntent sentPIn = PendingIntent.getBroadcast(context, 0,
                sentIn, 0);

        Intent deliverIn = new Intent(DELIVER_SMS_FLAG);
        deliverIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        deliverIn.putExtra(NUMBER_DELIVER, number);
        deliverIn.putExtra(SMS_DELIVER, sms);
        final PendingIntent deliverPIn = PendingIntent.getBroadcast(context, 0,
                deliverIn, 0);
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, sms, sentPIn,
                deliverPIn);
        Log.d(TAG, "Попытка отправить сообщение на номер: " + number);
        receiverManager.registerReceiver(sentReceiver, new IntentFilter(SENT_SMS_FLAG));
        receiverManager.registerReceiver(deliverReceiver, new IntentFilter(DELIVER_SMS_FLAG));
    }

    public void onDoNotDisturbMode() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Log.d(TAG, "Включаю DND");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            } else {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }
    }

    public void offDoNotDisturbMode() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Log.d(TAG, "Выключаю DND");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            } else {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        }
    }

    public void onWifi() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) {
                Log.d(TAG, "Включаю Wi-Fi");
                wifiManager.setWifiEnabled(true);
            }
        }
    }


    public void offWifi() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
                Log.d(TAG, "Выключаю Wi-Fi");
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    public String addDateNow() {
        return DateFormat.getDateTimeInstance().format(new Date());
    }

    public static class ReceiverManager {
        private List<BroadcastReceiver> receivers = new ArrayList<>();
        private ReceiverManager ref;
        private Context context;

        private ReceiverManager(Context context) {
            this.context = context;
        }

        public synchronized ReceiverManager init(Context context) {
            if (ref == null) ref = new ReceiverManager(context);
            return ref;
        }

        public void registerReceiver(BroadcastReceiver receiver, IntentFilter intentFilter) {
            receivers.add(receiver);
            context.registerReceiver(receiver, intentFilter);
        }

        public boolean isReceiverRegistered(BroadcastReceiver receiver) {
            boolean registered = receivers.contains(receiver);
            return registered;
        }

        public void unregisterReceiver(BroadcastReceiver receiver) {
            if (isReceiverRegistered(receiver)) {
                receivers.remove(receiver);
                context.unregisterReceiver(receiver);
            }
        }

    }
}
