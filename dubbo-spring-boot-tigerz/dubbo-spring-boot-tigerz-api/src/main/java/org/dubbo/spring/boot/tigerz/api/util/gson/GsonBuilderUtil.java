package org.dubbo.spring.boot.tigerz.api.util.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DateFormat;

/**
 * Created with antnest-platform
 * User: chenyuan
 * Date: 12/22/14
 * Time: 4:33 PM
 */
public class GsonBuilderUtil {

    // 使用Long类型来存储时间
    public static Gson create() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(java.util.Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG);
        gb.registerTypeAdapter(java.util.Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG);
        Gson gson = gb.create();
        return gson;
    }
}