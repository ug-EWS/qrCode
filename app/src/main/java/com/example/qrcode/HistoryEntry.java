package com.example.qrcode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class HistoryEntry {
    String content;
    String path;
    long dateInMillis;

    HistoryEntry(String _content, String _filename, long _dateInMillis) {
        content = _content;
        path = _filename;
        dateInMillis = _dateInMillis;
    }
    HistoryEntry(String json) {
        HashMap<String, String> map = Json.toMap(json);
        content = map.get("content");
        path = map.get("path");
        dateInMillis = Long.parseLong(map.get("dateInMillis"));
    }

    public String getVisibleDate(String pattern) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dateInMillis);
        return new SimpleDateFormat(pattern).format(c.getTime());
    }

    public String getJson() {
        HashMap<String, String> map = new HashMap<>();
        map.put("content", content);
        map.put("path", path);
        map.put("dateInMillis", ((Long)dateInMillis).toString());
        return Json.valueOf(map);
    }
}
