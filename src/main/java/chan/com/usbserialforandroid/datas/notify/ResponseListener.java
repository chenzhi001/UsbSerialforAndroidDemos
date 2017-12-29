package chan.com.usbserialforandroid.datas.notify;


import chan.com.usbserialforandroid.datas.beans.DevcieMessage;
import chan.com.usbserialforandroid.datas.beans.Location;
import chan.com.usbserialforandroid.datas.beans.UnKnowMessage;

/**
 * <ul>
 * Listener for notify response message.
 * </ul>
 * <ul>
 * To get response message, you have to make sure use
 * to send request. Otherwise you don't receive response for ever.
 * </ul>
 *
 * @author 28851274
 */
public interface ResponseListener {

    /**
     * Response message notification.<br>
     * Note: please make sure do not update UI in this function, because this
     * function doesn't run in UI thread.
     * <p/>
     * response gps location or GGA sigal location
     */
    public void onResponselocation(Location location);

    /**
     * response Devices message
     *
     */
    public void onResponseDevicesInfo(DevcieMessage devcieMessage);

    /**
     * unKnow Message
     *
     */
    public void onResponseUnknow(UnKnowMessage errorMessage);


}
