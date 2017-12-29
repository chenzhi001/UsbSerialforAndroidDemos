package chan.com.usbserialforandroid.datas;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;

import chan.com.usbserialforandroid.datas.notify.NotificationListener;
import chan.com.usbserialforandroid.datas.notify.ResponseListener;
import chan.com.usbserialforandroid.datas.util.FLog;


/**
 * Created by chen on 2017/12/28.
 */
public class UserService {

    private final String TAG = "UserService";
    private Context mContext;
    private String mTerminalId;
    private IMessageService mNativeService;
    private UsbManager mUSbManger;
    private ServiceConnection mConnection;
    private Object o = new Object();
    private MessagParser messagParser;
    int bound = 0;

    public UserService(Context mContext, ResponseListener mResoponselisener, String mTerminalId) {
        this.mContext = mContext;
        this.mTerminalId = mTerminalId;
        this.mUSbManger = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        this.messagParser = new MessagParser(mResoponselisener);
        if (mNativeService == null) {
            bindMessageService();
        }
    }

    /**
     * bindService
     */

    public void bindMessageService() {
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(mContext, MessageService.class);
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mNativeService = (IMessageService) service;
                registerNotificationLisener(listener);
                bound++;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                bound = 0;
                unregisterNotificationLisener(listener);
            }
        };
        boolean bindOK = mContext.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        if (bindOK) {
            FLog.i(TAG, bindOK + "----");
        } else {
            bindMessageService();
        }
    }

    private void registerNotificationLisener(NotificationListener listener) {
        mNativeService.registerNotificationLisener(listener);
    }

    private void unregisterNotificationLisener(NotificationListener listener) {
        mNativeService.unregisterNotificationLisener(listener);
    }

    /**
     * destory the method
     */
    public void onDestory() {
        unregisterNotificationLisener(listener);
        mContext.unbindService(mConnection);
        unregisterNotificationLisener(listener);
        mNativeService = null;
        mConnection = null;
    }

    /**
     * not promise the currentThread is ui thread;
     */
    private NotificationListener listener = new NotificationListener() {
        @Override
        public void onNotify(String noti) {
            synchronized (o) {
                if (messagParser != null) {
                    messagParser.parserJson(noti);
                }
            }
        }
    };
}
