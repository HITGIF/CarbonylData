package carbonylgroup.com.carbonyldata;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class getSetting {

    public static String readStringSetting(String pref, Context context) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String setting = settings.getString(pref, "NULL");
        return setting;
    }

    public static Boolean readBooleanSetting(String pref, Context context) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean setting = settings.getBoolean(pref, true);
        return setting;
    }

    public static int readIntSetting(String pref, Context context) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int setting = Integer.parseInt(settings.getString(pref, "300000"));
        return setting;
    }
}
