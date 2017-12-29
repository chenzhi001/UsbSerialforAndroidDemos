package chan.com.usbserialforandroid.datas.beans;


import chan.com.usbserialforandroid.datas.BaseMessage;

/**
 * Created by chen on 2017/12/29.
 */
public class Location extends BaseMessage {

    private double mLng;

    private double mLat;


    private double mSpeed;

    private double mHeidght;

    private int mStarts;

    public Location(double mLng, double mLat, double mSpeed, double mHeidght, int mStarts, double mBering) {

        this.mLng = mLng;
        this.mLat = mLat;
        this.mSpeed = mSpeed;
        this.mHeidght = mHeidght;
        this.mStarts = mStarts;
        this.mBering = mBering;
    }

    public double getmLng() {
        return mLng;
    }

    public void setmLng(double mLng) {
        this.mLng = mLng;
    }

    public double getmLat() {
        return mLat;
    }

    public void setmLat(double mLat) {
        this.mLat = mLat;
    }

    public double getmHeidght() {
        return mHeidght;
    }

    public void setmHeidght(double mHeidght) {
        this.mHeidght = mHeidght;
    }

    public double getmSpeed() {
        return mSpeed;
    }

    public void setmSpeed(double mSpeed) {
        this.mSpeed = mSpeed;
    }

    public int getmStarts() {
        return mStarts;
    }

    public void setmStarts(int mStarts) {
        this.mStarts = mStarts;
    }

    public double getmBering() {
        return mBering;
    }

    public void setmBering(double mBering) {
        this.mBering = mBering;
    }

    private double mBering;
}
