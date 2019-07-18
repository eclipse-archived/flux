package org.eclipse.flux.core.util;

import org.eclipse.core.resources.IMarker;
import org.json.JSONArray;
import org.json.JSONException;
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
    
    public static JSONArray toJSON(IMarker[] markers) throws JSONException{
        JSONArray objects = new JSONArray();
        for(IMarker marker : markers){
            JSONObject object = new JSONObject();
            object.put("description", marker.getAttribute("message", ""));
            object.put("line", marker.getAttribute("lineNumber", 0));
            switch(marker.getAttribute("severity", IMarker.SEVERITY_WARNING)){
                case IMarker.SEVERITY_WARNING:
                    object.put("severity", marker.getAttribute("severity", "warning"));
                    break;
                case IMarker.SEVERITY_ERROR:
                    object.put("severity", marker.getAttribute("severity", "error"));
                    break;
            }
            object.put("start", marker.getAttribute("charStart", 0));
            object.put("end", marker.getAttribute("charEnd", 0));
            objects.put(object);
        }
        return objects;
    }
}
