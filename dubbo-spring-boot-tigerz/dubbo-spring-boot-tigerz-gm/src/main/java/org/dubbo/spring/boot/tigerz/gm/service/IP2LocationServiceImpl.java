package org.dubbo.spring.boot.tigerz.gm.service;

import java.util.EnumMap;

import org.dubbo.spring.boot.tigerz.api.service.IP2LocationService;
import org.dubbo.spring.boot.tigerz.gm.dao.IP2LocationDAO;
import org.dubbo.spring.boot.tigerz.gm.entity.Ip2locationDb11;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;

@Service(
        version = "1.0.0",
        application = "${dubbo.application.id}",
        protocol = "${dubbo.protocol.id}",
        registry = "${dubbo.registry.id}"
)
public class IP2LocationServiceImpl implements IP2LocationService{
    
    @Autowired
    private IP2LocationDAO ip2LocationDAO;

    @Override
    public EnumMap<IPInfoProperty, String> transfer(String IP) {
        // 需要mongo的DAO服务，需要redis服务。 目前还没做任何缓存
        Ip2locationDb11 ip2Location11 = ip2LocationDAO.findLocationByIP(IP);
        
        EnumMap<IPInfoProperty, String> info = new EnumMap<IPInfoProperty,String>(IPInfoProperty.class);
        if (ip2Location11 != null) {
            info.put(IPInfoProperty.CODE, ip2Location11.getCountryCode());
            info.put(IPInfoProperty.REGION, ip2Location11.getRegionName());
        } else {
            info = null;
        }
        return info;
    }
    
}
