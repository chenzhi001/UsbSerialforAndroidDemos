package chan.com.usbserialforandroid.datas.util;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static android.util.Log.getStackTraceString;

/**
 * Created by 28851274 on 7/27/15.
 */
public final class FLog {
    private final static String LOG_TAG = "FLog";

    /**
     * Pls set to "true" if you want to save none log in SD card and print none log in ADB.
     * Otherwise set to "false".
     * Pls set to false always except in your debug test.
     */
    private final static boolean NO_LOG = false;

    //The max of log file is 2M bytes.
    private final static int LOG_FILE_SIZE_MAX = 2 * 1024 * 1024;
    //The max of log buffer size is 8K bytes.
    private final static int LOG_BUFFER_SIZE_MAX = 8 * 1024;

    //Internal log buffer
    private static byte[] buffer = new byte[LOG_BUFFER_SIZE_MAX];
    private static int mPos = 0;

    private static String mLogPath = "/usb_service/log/";

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
    private static SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS");

    public static int v(String tag, String msg) {
        if (NO_LOG) return 0;
        if (tag == null || msg == null) return 0;

        saveLogs(tag, msg, null);
        return Log.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        if (NO_LOG) return 0;
        if (tag == null || tr == null) return 0;

        if (msg == null) {
            saveLogs(tag, "verbose info", tr);
            return Log.v(tag, "verbose info", tr);
        } else {
            saveLogs(tag, msg, tr);
            return Log.v(tag, msg, tr);
        }

    }

    public static int d(String tag, String msg) {
        if (NO_LOG) return 0;
        if (tag == null || msg == null) return 0;

        saveLogs(tag, msg, null);
        return Log.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        if (NO_LOG) return 0;
        if (tag == null || tr == null) return 0;

        if (msg == null) {
            saveLogs(tag, "debug info", tr);
            return Log.d(tag, "debug info", tr);
        } else {
            saveLogs(tag, msg, tr);
            return Log.d(tag, msg, tr);
        }
    }

    public static int i(String tag, String msg) {
        if (NO_LOG) return 0;
        if (tag == null || msg == null) return 0;

        saveLogs(tag, msg, null);
        return Log.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        if (NO_LOG) return 0;
        if (tag == null || tr == null) return 0;

        if (msg == null) {
            saveLogs(tag, "information info", tr);
            return Log.i(tag, "information info", tr);
        } else {
            saveLogs(tag, msg, tr);
            return Log.i(tag, msg, tr);
        }
    }

    public static int w(String tag, String msg) {
        if (NO_LOG) return 0;
        if (tag == null || msg == null) return 0;

        saveLogs(tag, msg, null);
        return Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        if (NO_LOG) return 0;
        if (tag == null || tr == null) return 0;

        if (msg == null) {
            saveLogs(tag, "wrong info", tr);
            return Log.w(tag, "wrong info", tr);
        } else {
            saveLogs(tag, msg, tr);
            return Log.w(tag, msg, tr);
        }
    }

    public static int e(String tag, String msg) {
        if (NO_LOG) return 0;
        if (tag == null || msg == null) return 0;

        saveLogs(tag, msg, null);
        return Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (NO_LOG) return 0;
        if (tag == null || tr == null) return 0;

        if (msg == null) {
            saveLogs(tag, "error info", tr);
            return Log.e(tag, "error info", tr);
        } else {
            saveLogs(tag, msg, tr);
            return Log.e(tag, msg, tr);
        }
    }

    protected static void saveLogs(String tag, String msg, Throwable tr) {
        StringBuilder sb = new StringBuilder(tag);
        sb.append(": ");
        sb.append(msg);
        sb.append("\n");

        //Also save throwable string if tr is not null.
        if (tr != null) {
            String str = getStackTraceString(tr);
            sb.append(str);
        }

        //long t1 = System.currentTimeMillis();
        internalLog(sb);
        //long t2 = System.currentTimeMillis();
        //Log.i(LOG_TAG, "saveLogs():Cost of format log and save log is " + (t2 - t1) + " ms!");
    }


    /**
     * Produce internal log format and save to SD card.
     *
     * @param msg
     */
    private synchronized static void internalLog(StringBuilder msg) {

        //add time header
        String timeStamp = simpleDateFormat.format(new Date());
        msg.insert(0, timeStamp);
        byte[] contentArray = msg.toString().getBytes();
        int length = contentArray.length;
        int srcPos = 0;

        if (mPos == LOG_BUFFER_SIZE_MAX) {
            //Flush internal buffer
            flushInternalBuffer();
        }

        if (length > buffer.length) {
            //Strongly flush the current buffer no matter whether it is full
            flushInternalBuffer();

            //Flush all msg string to sd card
            while (length > buffer.length) {

                System.arraycopy(contentArray, srcPos, buffer, mPos, buffer.length);

                flushInternalBuffer();

                length -= buffer.length;
                srcPos += buffer.length;

            }
        } else if (length == buffer.length) {
            flushInternalBuffer();

            //Copy contents to buffer
            System.arraycopy(contentArray, 0, buffer, mPos, length);
            flushInternalBuffer();
            length = 0;
        }

        if (length < buffer.length && length > 0) {
            if ((mPos + length) > buffer.length) {
                flushInternalBuffer();

                //Copy contents to buffer
                System.arraycopy(contentArray, srcPos, buffer, mPos, length);
                mPos += length;

            } else if ((mPos + length) == buffer.length) {
                //Add content to buffer
                System.arraycopy(contentArray, srcPos, buffer, mPos, length);
                mPos += length;

                flushInternalBuffer();

            } else {
                //Add content to buffer
                System.arraycopy(contentArray, srcPos, buffer, mPos, length);
                mPos += length;

            }

        }

    }

    /**
     * Flush internal buffer to SD card and then clear buffer to 0.
     */
    private static void flushInternalBuffer() {

        //Strongly set the last byte to "0A"(new line)
        if (mPos < LOG_BUFFER_SIZE_MAX) {
            buffer[LOG_BUFFER_SIZE_MAX - 1] = 10;
        }

        long t1, t2;

        //Save buffer to SD card.
        t1 = System.currentTimeMillis();
        writeToSDCard();
        //calculate write file cost
        t2 = System.currentTimeMillis();
        Log.i(LOG_TAG, "internalLog():Cost of write file to SD card is " + (t2 - t1) + " ms!");

        //flush buffer.
        Arrays.fill(buffer, (byte) 0);
        mPos = 0;
    }


    /**
     * Flush buffer date to SD card.
     * <p>
     * This method is used ont only internal, but also used external.
     * UI developer should invoke this method explicitly to strongly flush buffer to SD card file
     * when the application is going to die, no matter whether the internal buffer is full {@link FLog#LOG_BUFFER_SIZE_MAX} .
     * </p>
     */
    public static void writeToSDCard() {
        File logDir = new File(StorageUtil.getAbsoluteSdcardPath() + mLogPath);
        if (!logDir.exists()) {
            logDir.setWritable(true);
            boolean ret = logDir.mkdirs();
            Log.i(LOG_TAG, "writeToSDCard(): create log dir: " + logDir.getAbsolutePath() + " " + ret);
        }

        if (!logDir.canWrite()) {
            logDir.setWritable(true);
        }

        //Find the last modified file
        File lastFile = getLastModifiedFile(logDir);

        //Write to last modified file
        writeToLastFile(logDir, lastFile);
    }

    /**
     * Flush logs in {@link FLog#buffer} to last modified file.
     * <p>
     * Write to last modified file directly if <code>buffer.length+lastFile.length()</code>
     * is smaller than {@link FLog#LOG_FILE_SIZE_MAX}. Otherwise it should create another new
     * file to flush buffer.
     * </p>
     *
     * @param logDir   log file folder
     * @param lastFile last modified file
     */
    private static void writeToLastFile(File logDir, File lastFile) {
        if (lastFile == null) {
            Log.e(LOG_TAG, "writeToLastFile(): File is null. lastFile= " + lastFile);
        }

        if ((buffer.length + lastFile.length()) > LOG_FILE_SIZE_MAX) {
            //New another file to flush buffer logs.
            File file = new File(logDir, fileNameFormat.format(new Date()) + ".txt");
            lastFile = file;

        }

        String fileName = lastFile.getAbsolutePath();
        Log.i(LOG_TAG, "writeToLastFile(): fileName = " + fileName);
        try {
            String bufferStr = new String(buffer, "UTF-8");
            appendFileSdCardFile(fileName, bufferStr);
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "writeToLastFile(): Change from byte[] to String failed!");
            e.printStackTrace();
        }

    }

    /**
     * Get last modified file in Log folder <code>logDir</code>.
     *
     * @param logDir Log folder
     * @return last modified file if exist. null if <code>logDir</code> is invalid.
     */
    private static File getLastModifiedFile(File logDir) {
        File[] files = logDir.listFiles();
        if (files == null) {
            Log.e(LOG_TAG, "getLastModifiedFile(): This file dir is invalid. logDir= " + logDir.getAbsolutePath());
            return null;
        }
        //Create a new file if no file exists in this folder
        if (files.length == 0) {
            File file = new File(logDir, fileNameFormat.format(new Date()) + ".txt");
            return file;
        }

        //Find the last modified file
        long lastModifiedTime = 0;
        File lastModifiedFile = null;
        for (File f : files) {
            if (lastModifiedTime <= f.lastModified()) {
                lastModifiedTime = f.lastModified();
                lastModifiedFile = f;
            }
        }
        return lastModifiedFile;
    }


    /**
     * Write to a file <code>fileName</code> with content <code>writeStr</code>.
     *
     * @param fileName file name must be an absolute path name.
     * @param writeStr content to be written.
     */
    public static void writeFileSdCardFile(String fileName, String writeStr) {

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(fileName);
            byte[] bytes = writeStr.getBytes();

            fout.write(bytes);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * Append <code>content</code> to a SD card file.
     *
     * @param fileName a SD card file would be written.
     * @param content  content want to be written.
     */
    public static void appendFileSdCardFile(String fileName, String content) {
        FileWriter writer = null;
        try {
            //Open a file writer and write a file with appending format with "true".
            writer = new FileWriter(fileName, true);
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
