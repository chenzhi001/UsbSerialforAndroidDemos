package chan.com.usbserialforandroid.datas.util;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageUtil {

	
	public static final String CT_S_Sdcard_Sign_Storage_emulated = "storage/emulated/";
	public static final String CT_S_Sdcard_Sign_Storage_sdcard = "storage/sdcard";
	
	private static String CD_S_SdcardPath = "";
	private static String CD_S_SdcardPathAbsolute = "";

	public static String getSdcardPath() {
		if (TextUtils.isEmpty(CD_S_SdcardPath))
			CD_S_SdcardPath = Environment.getExternalStorageDirectory()
					.getPath();

		CD_S_SdcardPath = checkAndReplaceEmulatedPath(CD_S_SdcardPath);

		return CD_S_SdcardPath;
	}

	public static String getAbsoluteSdcardPath() {
		if (TextUtils.isEmpty(CD_S_SdcardPathAbsolute))
			CD_S_SdcardPathAbsolute = Environment.getExternalStorageDirectory().getAbsolutePath();

		CD_S_SdcardPathAbsolute = checkAndReplaceEmulatedPath(CD_S_SdcardPathAbsolute);

		return CD_S_SdcardPathAbsolute;
	}

	public static File getSdcardPathFile() {
		return new File(getSdcardPath());
	}

	public static String checkAndReplaceEmulatedPath(String strSrc) {

		Pattern p = Pattern.compile("/?storage/emulated/\\d{1,2}");
		Matcher m = p.matcher(strSrc);
		if (m.find()) {
			strSrc = strSrc.replace(
					CT_S_Sdcard_Sign_Storage_emulated,
					CT_S_Sdcard_Sign_Storage_sdcard);
		}
		return strSrc;
	}
}
