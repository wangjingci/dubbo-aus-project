package org.dubbo.spring.boot.tigerz.aus.service;

import org.bson.types.ObjectId;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;
import org.dubbo.spring.boot.tigerz.aus.constant.RedisKey;
import org.dubbo.spring.boot.tigerz.aus.entity.RentingHouse;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;

@Service
public class RentingHouseService {
    
    private MongoService mongoService = MongoService.getInstance();
    
    public RentingHouse getRentingHouse(String id, String lang) {
        Gson gson = new Gson();
        String keyWord = RedisKey.GET_RENTING_HOUSE + "_" + id;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return gson.fromJson(redisValue, RentingHouse.class);
        }
        
        BasicDBObject query = new BasicDBObject("_id",new ObjectId(id));
        try {
            RentingHouse rentingHouse = mongoService.findOne(query, RentingHouse.class);
            if (rentingHouse != null) {
                RedisUtils.setex(keyWord, gson.toJson(rentingHouse), 60*60*24);
            }
            return rentingHouse;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
}
