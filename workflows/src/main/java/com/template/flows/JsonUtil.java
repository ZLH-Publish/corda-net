package com.template.flows;

import com.google.gson.Gson;

public class JsonUtil {

    public static <T> T stringToMap(Class<T> clazz,String jsonString){
        Gson gson=new Gson();
        return gson.fromJson(jsonString, clazz);
    }

    public static String toJsonString(Object obj){
        Gson gson=new Gson();
        String result=gson.toJson(obj);
        return result;
    }
}