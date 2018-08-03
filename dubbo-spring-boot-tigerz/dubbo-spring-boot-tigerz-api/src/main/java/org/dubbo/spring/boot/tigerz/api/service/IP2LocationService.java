package org.dubbo.spring.boot.tigerz.api.service;

import java.util.EnumMap;

public interface IP2LocationService {
    
    enum IPInfoProperty {
        CODE,COUNTRY,REGION
    }

    // 返回CODE REGION
    EnumMap<IPInfoProperty,String> transfer(String IP);
    
}
