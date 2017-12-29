package chan.com.usbserialforandroid.datas;


import chan.com.usbserialforandroid.datas.notify.NotificationListener;

/**
 * Created by chen on 2017/12/28.
 */
public interface IMessageService {

    void registerNotificationLisener(NotificationListener listener);

    void unregisterNotificationLisener(NotificationListener Listener);

}
