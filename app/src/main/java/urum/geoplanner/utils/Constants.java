package urum.geoplanner.utils;

import java.util.HashMap;
import java.util.Map;

public final class Constants {
    public static final String TAG = "mytag";
    public static final String CLOSEAPPINTENTFILTER = "closeApp";
    public static final String PACKAGE_NAME = "urum.geoplanner";

    /* Constants for Notifications, bundle, Intents*/
    public static final String CHANNEL_ID = "channel_";
    public static final int NOTIFICATION_ID = 10;
    public static final String SENT_SMS_FLAG = "SENT_SMS";
    public static final String DELIVER_SMS_FLAG = "DELIVER_SMS";
    public static final String ID_FROM_NOTIFICATION = "id_notification";
    public static final String ID = "id";
    public static final String FROM_MAPS = "fromMaps";
    public static final String ADDRESS_FROM_MAPS = "addressFromMaps";
    public static final String LATITUDE_FROM_PLACEACTIVITY = "latitudeFromPlaceActivity";
    public static final String LONGITUDE_FROM_PLACEACTIVITY = "longitudeFromPlaceActivity";
    public static final String LATITUDE_FROM_MAPS = "latitudeFromMaps";
    public static final String LONGITUDE_FROM_MAPS = "longitudeFromPlaceMaps";
    public static final String FROM_MAIN = "fromMain";
    public static final String FROM_NOTIFICATION_TO_SETTINGS = "fromNotificationToSettings";
    public static final String FROM_NOTIFICATION_TO_PLACEACTIVITY = "fromNotificationToPlaceActivity";
    public static final String FROM_ACTIVITY_PLACE = "fromActivityPlace";
    public static final String NUMBER_SENT = "numberSent";
    public static final String SMS_SENT = "smsSent";
    public static final String NUMBER_DELIVER = "numberDeliver";
    public static final String SMS_DELIVER = "smsDeliver";
    public static final String SMS_BODY = "sms_body";

    /* Constants for permissions */
    public static final int LOCATION_PERMISSIONS = 10;
    public static final int REQUEST_CHECK_SETTINGS = 110;
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /* Constants for settings */
    public static final String LIST_SECONDS = "listSeconds";
    public static final String LIST_METERS = "listMeters";
    public static final String GEO_ACCURACY = "geo_accuracy";
    public static final String SWITCH_SERVICE = "switchService";
    public static final String CHECK_BOX_AUTO_START = "checkBoxAutoStart";
    public static final String MAX_RADIUS = "maxRadius";


    /* Channel notifications */
    public static final Map<Integer, String> channel = new HashMap<Integer, String>() {{
        put(0, "Уведомление о звонке");
        put(1, "Уведомление об отправке SMS");
        put(2, "Уведомление");
        put(3, "Уведомление о включении режима 'Не беспокоить'");
        put(4, "Уведомление о выключении режима 'Не беспокоить'");
        put(5, "Уведомление о включении Wi-Fi");
        put(6, "Уведомление о выключении Wi-Fi");
    }};
}
