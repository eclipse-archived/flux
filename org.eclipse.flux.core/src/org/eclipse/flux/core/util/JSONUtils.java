package org.eclipse.flux.core.util;

import org.json.JSONObject;

public class JSONUtils {
    public static String getString(JSONObject object, String key, String defaultValue){
        try {
            return object.getString(key);
        }
        catch (Exception e) {
            return defaultValue;
        }
    }
}
