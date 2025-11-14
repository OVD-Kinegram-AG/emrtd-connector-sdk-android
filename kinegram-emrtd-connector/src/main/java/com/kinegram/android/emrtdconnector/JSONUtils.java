package com.kinegram.android.emrtdconnector;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class JSONUtils {
    static List<String> optStringList(JSONObject obj, String name) throws JSONException {
        JSONArray array = obj.optJSONArray(name);
        if (array == null) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(array.getString(i));
        }
        return result;
    }

    static String[] optStringArray(JSONObject obj, String name) throws JSONException {
        List<String> stringList = optStringList(obj, name);
        return stringList != null ? stringList.toArray(new String[0]) : null;
    }

    static int[] optIntArray(JSONObject obj, String name) throws JSONException {
        JSONArray array = obj.optJSONArray(name);
        if (array == null) {
            return null;
        }
        int[] result = new int[array.length()];
        for (int i = 0; i < array.length(); i++) {
            result[i] = array.getInt(i);
        }
        return result;
    }

    static List<byte[]> optByteArrayList(JSONObject obj, String name) throws JSONException {
        List<String> stringList = optStringList(obj, name);
        if (stringList == null) {
            return null;
        }
        List<byte[]> result = new ArrayList<>();
        for (String s : stringList) {
            result.add(decodeB64(s));
        }
        return result;
    }

    static Boolean optBool(JSONObject obj, String name) throws JSONException {
        return obj.has(name) ? obj.getBoolean(name) : null;
    }

    static byte[] decodeB64(String s) {
        return s != null ? Base64.decode(s, Base64.DEFAULT) : null;
    }
}
