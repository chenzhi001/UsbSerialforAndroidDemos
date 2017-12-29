package chan.com.usbserialforandroid.datas;

import android.util.Log;



import java.util.regex.Pattern;

import chan.com.usbserialforandroid.datas.beans.DevcieMessage;
import chan.com.usbserialforandroid.datas.beans.Location;
import chan.com.usbserialforandroid.datas.beans.UnKnowMessage;
import chan.com.usbserialforandroid.datas.notify.ResponseListener;

/**
 * Created by chen on 2017/12/28.
 */
public class MessagParser {

    private static final String TAG = "BDMsgParse";
    private static final String regRMC = "\\$[^,]{2}RMC(,[^,]*){12,13}";
    private static final String regGGA = "\\$[^,]{2}GGA(,[^,]*){14,15}";
    private static final String regICI = "\\$BDICI(,[^,]*){8}";
    private static final String regTXR = "\\$BDTXR(,[^,]*){5,10}";
    private static final String regFKI = "\\$BDFKI,TXA(,[^,]*){1,10}";
    private static final String regBSI = "\\$BDBSI(,[^,]*){12}";
    private static MessageType messagettype;
    private static Pattern patterRMC = Pattern.compile(regRMC);
    private static Pattern patterGGA = Pattern.compile(regGGA);
    private static Pattern patterICI = Pattern.compile(regICI);
    private static Pattern patterTXR = Pattern.compile(regTXR);
    private static Pattern patterFKI = Pattern.compile(regFKI);
    private static Pattern patterBSI = Pattern.compile(regBSI);
    private static boolean ICI_OK;
    private ResponseListener listener;

    public MessagParser(ResponseListener listener) {
        this.listener = listener;
    }

    private Object o = new Object();

    public void parserJson(String Message) {

        String messages[] = Message.split("\n");
        for (String message : messages) {
            removeallMessag(message);
        }
    }

    public void removeallMessag(String message) {
        MessageType type = getMessagettype(message);
    }


    public MessageType getMessagettype(String message) {

        if (patterRMC.matcher(message).matches()) {
            handleRMC(message);
            return MessageType.RMC;
        }
        if (patterGGA.matcher(message).matches()) {
            handleGGA(message);
            return MessageType.GGA;
        }
        if (patterICI.matcher(message).matches()) {
            handleBDICI(message);
            return MessageType.ICI;
        }
        if (patterTXR.matcher(message).matches()) {
            return MessageType.TXR;
        }
        if (patterFKI.matcher(message).matches()) {
            return MessageType.FKI;
        }
        if (patterBSI.matcher(message).matches()) {
            return MessageType.BSI;
        } else {
            synchronized (o) {
                if (listener != null) {
                    UnKnowMessage unkonwMessage = new UnKnowMessage();
                    unkonwMessage.setTYPE(MessageType.UNKNOWN);
                    unkonwMessage.setmEcontent(message);
                    listener.onResponseUnknow(unkonwMessage);
                }
            }
            return MessageType.UNKNOWN;
        }
    }


    private void handleBDICI(String message) {
        String s = message.substring(message.indexOf("ICI"));
        String values[] = s.split(",");
        String format = String.format("服务频度: %s; 通信等级: %s ", values[5], values[6]);
        this.ICI_OK = true;
        if (listener != null) {
            DevcieMessage devcieMessgae = new DevcieMessage();
            devcieMessgae.setTYPE(MessageType.ICI);
            devcieMessgae.setmContent(format);
            listener.onResponseDevicesInfo(devcieMessgae);
        }
    }

    /**
     * 描述定位数据，本语句包含与接收定位，测试相关的数据
     *
     * @param message
     */
    private static Location mLocation = new Location(0.0, 0.0, 0.0, 0.0, 0, 0.0);

    private void handleGGA(String message) {
        String s = message.substring(message.indexOf("GGA"));
        String[] values = s.split(",");
        int sat_num = Integer.parseInt(values[7]);// 视野内的卫星数量
        if (sat_num <= 0) {
            // return;
        } else if (src == SRC.all || src == SRC.gga) {
            try {
                double lat = getDegree(values[2]);
                double lon = getDegree(values[4]);
                double hei = Double.parseDouble(values[9]);

                synchronized (o) {
                    mLocation.setTYPE(MessageType.GGA);
                    mLocation.setmLat(lat);
                    mLocation.setmLng(lon);
                    mLocation.setmHeidght(hei);
                    if (listener != null) {
                        listener.onResponselocation(mLocation);
                    }
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "NumberFormat error:" + message);
            }
        }
    }

    /**
     * 简单的导航传输数据
     *
     * @param message
     */
    private SRC src = SRC.all;

    private void handleRMC(String message) {
        String s = message.substring(message.indexOf("RMC"));
        String values[] = s.split(",");

        if ("A".equals(values[2])) {// 检查信息是否有效
            if (src == SRC.all || src == SRC.rmc) {
                try {
                    double lat = getDegree(values[3]);
                    double lon = getDegree(values[5]);
                    boolean bdir = false;
                    double bering = 0.0;
                    if (!"".equals(values[8])) {
                        bering = getDegree(values[8]);
                        bdir = true;
                    }
                    boolean bspe = false;
                    double speed = 0.0;
                    if (!"".equals(values[7])) {
                        speed = Double.parseDouble(values[7]);
                        bspe = true;
                    }

                    synchronized (o) {
                        mLocation.setTYPE(MessageType.RMC);
                        mLocation.setmLat(lat);
                        mLocation.setmLng(lon);
                        mLocation.setmBering(bering);
                        mLocation.setmSpeed(speed);
                        if (listener != null) {
                            listener.onResponselocation(mLocation);
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.d(TAG, "NumberFormat error:" + message);
                }
            }
        }
    }

    private double getDegree(String s) {
        double value = Double.parseDouble(s);
        int degree = ((int) value / 100);
        double minute = (value / 100 - degree) * 100 / 60;
        return degree + minute;
    }
}
