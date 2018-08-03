package org.dubbo.spring.boot.tigerz.gm.manager;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dubbo.spring.boot.tigerz.api.util.MD5Util;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;


public class SessionManager {
    
    public static final String COOKIE_TOKEN = "token";
    public static final String COOKIE_TOKEN_TEMP = "tempToken";
    
    private static final int TIME_OUT = Integer.MAX_VALUE;
    private static final String TEMP_SEED = "TempSeed";

    
    public static String generateToken(String userId) {
        String seed = userId + System.currentTimeMillis();
        return MD5Util.getMD5(seed);
    }
    
    public static String generateTempToken() {
        String seed = TEMP_SEED + System.currentTimeMillis();
        return MD5Util.getMD5(seed);
    }
    
    public static void setAttribute(String key,String value) {
        RedisUtils.setex(key,value,TIME_OUT);
    }
    
    public static void setAttributeTimeOut(String key,String value,int seconds) {
        RedisUtils.setex(key,value,seconds);
    }
    
    public static String getAttribute(String key) {
        return RedisUtils.getKeyAsString(key);
    }

    public static void removeAttribute(String key) {
        RedisUtils.del(key);
    }
    
    public static void addTokenToCookie(HttpServletResponse resp,String token) throws Exception  {
        Cookie mycookie = new Cookie(COOKIE_TOKEN,token); 
        // 永不过期，除非清理缓存
        mycookie.setMaxAge(TIME_OUT); 
        resp.addCookie(mycookie);
    }
    
    public static void addTempTokenToCookie(HttpServletResponse resp,String token) throws Exception  {
        Cookie mycookie = new Cookie(COOKIE_TOKEN_TEMP,token); 
        // 永不过期，除非清理缓存
        mycookie.setMaxAge(TIME_OUT); 
        resp.addCookie(mycookie);
    }
    
    public static boolean checkToken(String token) {
        String tokenInfo = getAttribute(token);
        return StringUtils.isNotEmpty(tokenInfo);
    }
    
    
}
