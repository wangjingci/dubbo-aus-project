package org.dubbo.spring.boot.tigerz.gm.service;

import org.bson.types.ObjectId;
import org.dubbo.spring.boot.tigerz.gm.entity.SellingHouse;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;

@Service
public class SellingHouseService {
     private MongoService mongoService = new MongoService("australia_crawdbler");
     public SellingHouse getAusSellingHouse(String id, String lang) {
           BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
           try {
               SellingHouse e = (SellingHouse)this.mongoService.findOne(query, SellingHouse.class);
                   return e;    
           } catch (InstantiationException arg4) {
               arg4.printStackTrace();
           } catch (IllegalAccessException arg5) {
               arg5.printStackTrace();
           }
           return null;
     }
           
}

