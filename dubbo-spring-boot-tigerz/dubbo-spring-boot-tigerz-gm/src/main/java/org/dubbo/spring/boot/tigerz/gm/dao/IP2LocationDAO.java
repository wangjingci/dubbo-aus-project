package org.dubbo.spring.boot.tigerz.gm.dao;

import org.dubbo.spring.boot.tigerz.gm.entity.Ip2locationDb11;

public interface IP2LocationDAO {
    
    Ip2locationDb11 findLocationByIP(String IP);
}
