package com.example.qrcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class HistoryEntry {
    String content;
    String path;
    long dateInMillis;

    HistoryEntry(String _content, String _filename, long _dateInMillis) {
        content = _content;
        path = _filename;
        dateInMillis = _dateInMillis;
    }

    HistoryEntry(String jsonString) {
        try {
            fromJSONObject(new JSONObject(jsonString));
        } catch (JSONException e) {
            content = "";
            path = "";
            dateInMillis = 0;
        }
    }

    HistoryEntry(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    private void fromJSONObject(JSONObject jsonObject) {
        content = jsonObject.optString("content");
        path = jsonObject.optString("path");
        dateInMillis = jsonObject.optLong("dateInMillis");
    }

    public String getVisibleDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateInMillis);
        return SimpleDateFormat.getDateInstance(DateFormat.MEDIUM).format(c.getTime())
                .concat(", ")
                .concat(SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime()));
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("content", content)
                    .put("path", path)
                    .put("dateInMillis", dateInMillis);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }
}
