package org.dubbo.spring.boot.tigerz.api.util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.dubbo.spring.boot.tigerz.api.util.gson.DateDeserializer;
import org.dubbo.spring.boot.tigerz.api.util.gson.DateSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class GsonUtil {
    
    private static Gson gson = create();
    
    public static Gson create() {
        GsonBuilder gb = new GsonBuilder();
        // Gson默认是不兼容NaN的加上下面这句话就可以了。但也让很多问题暴露不出来。所以暂时不加
        // gb.serializeSpecialFloatingPointValues();
        gb.registerTypeAdapter(java.util.Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG);
        gb.registerTypeAdapter(java.util.Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG);
        Gson gson = gb.create();
        return gson;
    }
    
    /**
     * 简单单一的对象反序列化。如果转成List等需要用另一个函数
     * @param json
     * @param classOfT
     * @return
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    
    /**
     * 如果要转成List对象，要使用这个方法
     * @param json
     * @param t
     * @return
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> t) {
        List<T> list = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonArray jsonarray = parser.parse(json).getAsJsonArray();
        for (JsonElement element : jsonarray) {
            list.add(gson.fromJson(element, t));
        }
        return list;
    }
    
    public static <T> String toJson(T t) {
        return gson.toJson(t);
    }

}
