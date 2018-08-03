package org.dubbo.spring.boot.tigerz.api.util;

public class IPUtils {
    
    // IP字符串转Long型，用于查找计算等
    public static long Dot2LongIP(String ipstring) {
        String[] ipAddressInArray = ipstring.split("\\.");
        long result = 0;
        long ip = 0;
        for (int x = 3; x >= 0; x--) {
            ip = Long.parseLong(ipAddressInArray[3 - x]);
            result |= ip << (x << 3);
        }
        return result;
    }
}
