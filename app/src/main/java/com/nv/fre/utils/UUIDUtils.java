package com.nv.fre.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Created by nover on 2016/1/16 0016.
 */
public class UUIDUtils {
    private static final String PER_NAME = "NV";
    private static final String KEY_UUID = "NV_UUID";
    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PER_NAME, Context.MODE_PRIVATE);
    }
    public static String getUUID(Context context) {
        String uuid = getPref(context).getString(KEY_UUID, null);
        if(uuid == null) {
            uuid = UUID.randomUUID().toString();
            getPref(context).edit().putString(KEY_UUID, uuid).commit();
        }
        return uuid;
    }
}
