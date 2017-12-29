package chan.com.usbserialforandroid.datas.notify;


/**
 * Created by chen on 2017/12/28.
 */
public interface NotificationListener {
    /**
     * please make sure do not update Ui in this function
     * because this function doesn't run in UI thread.
     *
     * @param noti
     */
    public void onNotify(String noti);
}
