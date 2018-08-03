package org.dubbo.spring.boot.tigerz.gm.dao;

import org.dubbo.spring.boot.tigerz.api.util.IPUtils;
import org.dubbo.spring.boot.tigerz.gm.entity.Ip2locationDb11;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;


@Repository
public class IP2LocationDAOImpl implements IP2LocationDAO {
    //@Autowired
    //private MongoTemplate mongoTemplate;
    
    private MongoService mongoService = new MongoService("ip2location"); 
    private Logger logger = org.slf4j.LoggerFactory.getLogger(IP2LocationDAOImpl.class);

    @Override
    public Ip2locationDb11 findLocationByIP(String IP) {
        long ipNumber = IPUtils.Dot2LongIP(IP);
        String queryFormat = "{ ip_from : { $lte : %d }, ip_to:{ $gte : %d } }";
        String queryString =  String.format(queryFormat,ipNumber,ipNumber);
        BasicDBObject query = BasicDBObject.parse(queryString);
        Ip2locationDb11 entity = null;
        try {
            logger.info("start IP find");
            entity = mongoService.findOne(query, Ip2locationDb11.class);
            logger.info("stop IP find");
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } 
        return entity;
    }
    
    
}
