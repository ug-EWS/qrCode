package com.example.qrcode;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

public class Json {
    Json() {

    }
    public static String valueOf(ArrayList<String> list) {
        return new Gson().toJson(list);
    }

    public static String valueOf(HashMap<String, String> map) {
        return new Gson().toJson(map);
    }

    public static ArrayList<String> toList(String json) {
        return new Gson().fromJson(json, new TypeToken<ArrayList<String>>(){});
    }

    public static HashMap<String, String> toMap(String json) {
        return new Gson().fromJson(json, new TypeToken<HashMap<String, String>>(){});
    }
}
