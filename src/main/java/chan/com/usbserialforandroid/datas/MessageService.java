package chan.com.usbserialforandroid.datas;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;



import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chan.com.usbserialforandroid.datas.driver.UsbSerialDriver;
import chan.com.usbserialforandroid.datas.driver.UsbSerialPort;
import chan.com.usbserialforandroid.datas.driver.UsbSerialProber;
import chan.com.usbserialforandroid.datas.notify.NotificationListener;
import chan.com.usbserialforandroid.datas.util.FLog;
import chan.com.usbserialforandroid.datas.util.HexDump;
import chan.com.usbserialforandroid.datas.util.SerialInputOutputManager;


/**
 * Created by chen on 2017/12/28.
 */
public class MessageService extends Service {
    private static final String TAG = "MessageService";

    private HandlerThread mLocalHandlerThread;
    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    private Handler mDispatchHandler;
    private List<WeakReference<NotificationListener>> notificationListeners = new ArrayList<WeakReference<NotificationListener>>();
    /**
     * 缓存消息
     */
    private List<String> mUnsolicitedCache = new ArrayList<String>();

    private final static int REFRESH_USB_DEVCIE_CHECK = 1;

    private final static int REFRESH_TIME = 5000;
    private HandlerThread mDispatchMessageHandlerThread;
    NativeService mNaticeService;
    private UsbSerialPort sPort;

    private Handler mLocalHandler;

    private UsbState mstate = UsbState.NONE;

    private UsbManager mUsbManager;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;


    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            FLog.i(TAG, e.toString());
        }

        @Override
        public void onNewData(final byte[] data) {
            synchronized (mUnsolicitedCache) {
                String message = HexDump.dumpHexString(data);
                for (WeakReference<NotificationListener> listener : notificationListeners) {
                    listener.get().onNotify(message);
                }
            }
        }
    };
    private UsbAsnckTask asnckTask;

    @Override
    public void onCreate() {
        super.onCreate();


        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mLocalHandlerThread = new HandlerThread("MSHandlerThread");
        mLocalHandlerThread.start();
        int count = 1;
        synchronized (mLocalHandlerThread) {
            while (!mLocalHandlerThread.isAlive() && count++ < 5) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    FLog.e(TAG, "onCreate() failed", e);
                    e.printStackTrace();
                }
            }
        }
        if (mLocalHandlerThread.getLooper() != null) {
            mLocalHandler = new Handler(mLocalHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case REFRESH_USB_DEVCIE_CHECK:
                            refreshfindDeviceList();
                            break;
                    }
                    super.handleMessage(msg);
                }
            };
        } else {
            FLog.e(TAG, "Handler thread can not start");
            stopSelf();
        }
        mDispatchMessageHandlerThread = new HandlerThread("MSDispathThread");
        mDispatchMessageHandlerThread.start();
        synchronized (mDispatchMessageHandlerThread) {
            while (!mDispatchMessageHandlerThread.isAlive() && count++ < 5) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    FLog.e(TAG, "onCreate() failed", e);
                    e.printStackTrace();
                }
            }
        }

        if (mDispatchMessageHandlerThread.getLooper() != null) {
            mDispatchHandler = new Handler(mDispatchMessageHandlerThread.getLooper());
        } else {
            FLog.e(TAG, "mDispatchMessageHandlerThread thread can not start");
            stopSelf();
        }
        mNaticeService = new NativeService();
        mstate = getConnectivityState();
        /**
         * 监听有USB 设备插入
         */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(MessageService.ACTION_USB_STATE);
        refreshfindDeviceList();
        registerReceiver(mUSBConnectionReceiver, intentFilter);

    }

    private UsbState getConnectivityState() {

        return mstate;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mNaticeService;
    }

    private enum UsbState {
        NONE, CONNECTED, CONNECTING;
    }

    /**
     * 检查 USB 读取设备的时候
     */
    private BroadcastReceiver mUSBConnectionReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case UsbManager.ACTION_USB_ACCESSORY_ATTACHED:
                    break;
                case UsbManager.ACTION_USB_ACCESSORY_DETACHED:
                    mstate = UsbState.NONE;
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    mstate = UsbState.NONE;
                    break;
            }
            refreshfindDeviceList();
        }
    };

    class NativeService extends Binder implements IMessageService {

        @Override
        public void registerNotificationLisener(NotificationListener listener) {
            notificationListeners.add(new WeakReference<NotificationListener>(listener));//
            synchronized (mUnsolicitedCache) {
                for (String msg : mUnsolicitedCache) {
                    firUnSolocitedMessage(msg);
                }
                mUnsolicitedCache.clear();
            }
        }

        @Override
        public void unregisterNotificationLisener(NotificationListener Listener) {
            synchronized (mUnsolicitedCache) {
                notificationListeners.remove(new WeakReference<NotificationListener>(Listener));
            }
        }
    }

    /**
     * 下发所有的数据
     *
     * @param bmsg
     */
    private void firUnSolocitedMessage(String bmsg) {
        synchronized (mUnsolicitedCache) {
            if (notificationListeners.size() == 0) {
                mUnsolicitedCache.add(bmsg);
                return;
            } else {
                for (WeakReference<NotificationListener> listener : notificationListeners) {
                    listener.get().onNotify(bmsg);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private List<UsbSerialPort> mEntries = new ArrayList<>();

    private void refreshfindDeviceList() {
        synchronized (mEntries) {
            if (asnckTask == null) {
                asnckTask = new UsbAsnckTask();
            }
        }
        synchronized (mEntries) {
            /**
             * the device mEntry size is null;
             */
            if (sPort == null || mstate == UsbState.NONE || mEntries.size() == 0) {
                asnckTask.execute();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUSBConnectionReceiver);
        try {
            sPort.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sPort = null;
        mstate = UsbState.NONE;

    }

    protected void onstartReceive() {
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            FLog.e(TAG, "No serial device.");
            mstate = UsbState.NONE;
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                FLog.e(TAG, "Opening device failed");
                return;
            }
            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                mstate = UsbState.CONNECTED;
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                FLog.e(TAG, "Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                }
                sPort = null;
                mstate = UsbState.NONE;
                return;
            }
        }
        onDeviceStateChange();
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private class UsbAsnckTask extends AsyncTask<Void, Void, List<UsbSerialPort>> {

        protected List<UsbSerialPort> doInBackground(Void... params) {
            Log.d(TAG, "Refreshing device list ...");
            SystemClock.sleep(1000);
            final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

            final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
            for (final UsbSerialDriver driver : drivers) {
                final List<UsbSerialPort> ports = driver.getPorts();
                Log.d(TAG, String.format("+ %s: %s port%s", driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                result.addAll(ports);
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<UsbSerialPort> result) {
            mEntries.clear();
            mEntries.addAll(result);
            //handler 清除数据重新开始读取数据
            if (mEntries.size() > 0) {
                sPort = mEntries.get(0);
                mstate = sPort == null ? UsbState.NONE : UsbState.CONNECTED;
                onstartReceive();
            } else {//隔一段时间检查
                mLocalHandler.removeMessages(REFRESH_USB_DEVCIE_CHECK);
                mLocalHandler.sendEmptyMessageDelayed(REFRESH_USB_DEVCIE_CHECK, REFRESH_TIME);
            }
        }
    }
}
