package org.dubbo.spring.boot.tigerz.aus.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.dubbo.spring.boot.tigerz.api.service.GmDubboService;
import org.dubbo.spring.boot.tigerz.api.util.GsonUtil;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;
import org.dubbo.spring.boot.tigerz.aus.constant.RedisKey;
import org.dubbo.spring.boot.tigerz.aus.entity.SellingHouse;
import org.dubbo.spring.boot.tigerz.aus.entity.SellingSoldHouse;
import org.dubbo.spring.boot.tigerz.aus.entity.SuburbRank;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;
import com.tigerz.easymongo.util.Assert;


@Service
public class SellingHouseService {
    
//    @Reference(version = "1.0.0",
//            application = "${dubbo.application.id}",
//            url = "dubbo://localhost:12345")
//    private GmDubboService gmDubboService;
    
    private MongoService mongoService = MongoService.getInstance();
    
    public SellingHouse getSellingHouse(String id, String lang) {
        // TODO handle multiple language
        
        Gson gson = new Gson();
        String keyWord = RedisKey.GET_SELLING_HOUSE + "_" + id;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return gson.fromJson(redisValue, SellingHouse.class);
        }
        
        BasicDBObject query = new BasicDBObject("_id",new ObjectId(id));
        try {
            SellingHouse sellingHouse = mongoService.findOne(query, SellingHouse.class);
            if (sellingHouse != null) {
                
                sellingHouse = setDisplay(sellingHouse, lang);
                
                RedisUtils.setex(keyWord, gson.toJson(sellingHouse), 60*60*24);
            }
            return sellingHouse;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public SellingSoldHouse getSoldHouse(String id, String lang) {
        // TODO handle multiple language
        
        Gson gson = new Gson();
        String keyWord = RedisKey.GET_SOLD_HOUSE + "_" + id;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return gson.fromJson(redisValue, SellingSoldHouse.class);
        }
        
        BasicDBObject query = new BasicDBObject("_id",new ObjectId(id));
        try {
            SellingSoldHouse sellingSoldHouse = mongoService.findOne(query, SellingSoldHouse.class);
            if (sellingSoldHouse != null) {
                
                //SellingSoldHouse = setDisplay(SellingSoldHouse, lang);
                
                RedisUtils.setex(keyWord, gson.toJson(sellingSoldHouse), 60*60*24);
            }
            return sellingSoldHouse;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public List<SellingHouse> getNearbyHouse(ArrayList<Double> point)
            throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        String keyWord = RedisKey.GET_NEARBY_HOUSE + "_" + point.toString();
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return GsonUtil.fromJson(redisValue, List.class);
        }
        
        int limit = 12;
        BasicDBObject basicQuery = new BasicDBObject();
        basicQuery.put("status", "sale");
        List<SellingHouse> list = mongoService.findByNear("base_point", basicQuery, point, limit, SellingHouse.class);
        if (list != null) {
            RedisUtils.setex(keyWord, GsonUtil.toJson(list), 60*60*24*1);
        }                
        return list;
    }
    
    
    
    @SuppressWarnings("unchecked")
    public List<SellingSoldHouse> getNearbySoldHouse(ArrayList<Double> point)
            throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        String keyWord = RedisKey.GET_NEARBY_SOLD_HOUSE + "_" + point.toString();
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return GsonUtil.fromJson(redisValue, List.class);
        }
        
        int limit = 12;
        //List<SellingSoldHouse> list = mongoService.findByNear("base_point", null, point, limit,SellingSoldHouse.class);
        //BasicDBObject query = new BasicDBObject();
        List<SellingSoldHouse> list = mongoService.findByNearWithDistance("selling_sold_house", null, point, limit, SellingSoldHouse.class);
        java.util.Collections.sort(list, new java.util.Comparator<SellingSoldHouse>() {
            @Override
            public int compare(SellingSoldHouse o1, SellingSoldHouse o2) {
                String price1 = o1.getPrice();
                String price2 = o2.getPrice();
                if (price1 != null && price1.contains("$") && price2 != null && price2.contains("$")) {
                    return Double.compare(o1.getDistance(), o2.getDistance());
                } else if ((price1 == null || !price1.contains("$")) && (price2 == null || !price2.contains("$"))) {
                    return Double.compare(o1.getDistance(), o2.getDistance());
                } else if (price1 != null && price1.contains("$")) {
                    return -1;
                } 
                return 1;
            }
            
        });
        
        RedisUtils.setex(keyWord, GsonUtil.toJson(list), 60*60*24*1);
        return list;
    }
    
    public List<SellingHouse> getSeveralSellingHouse(ArrayList<String> ids) {
        Assert.notNull(ids, "房源ID列表不能是kong");
        
        List<BasicDBObject> mList = new ArrayList<BasicDBObject>();
        for (String id : ids) {
            BasicDBObject query = new BasicDBObject();
            query.put("_id", new ObjectId(id));
            mList.add(query);
        }
        
        BasicDBObject query = new BasicDBObject();
        query.put("$or", mList);
        
        try {
            List<SellingHouse> sellingHouses = mongoService.find(query, SellingHouse.class);
            return sellingHouses;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String,Object> getSuburbRank() {
        String keyWord = RedisKey.GET_SUBURB_RANK;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return GsonUtil.fromJson(redisValue, Map.class);
        }
        
        int rankNum = 10;
        Map<String, Object> suburbRank = new HashMap<>();
       
        try {
            List<SuburbRank> list = mongoService.findAll(SuburbRank.class);
            java.util.Collections.sort(list, new java.util.Comparator<SuburbRank>() {
                @Override
                public int compare(SuburbRank o1, SuburbRank o2) {
                    if (o1.getRentYield() != null && o2.getRentYield() != null) {
                        return -Double.compare(o1.getRentYield(), o2.getRentYield());
                    } else if (o1.getRentYield() == null && o2.getRentYield() == null) {
                        return 0;
                    } else if (o1.getRentYield() != null && o2.getRentYield() == null) {
                        return -1;
                    } else if (o1.getRentYield() == null && o2.getRentYield() != null) {
                        return 1;
                    }
                    return 0;
                }
            });
            
            List<SuburbRank> rentList = new LinkedList<>();
            for (int i = 0; i < rankNum; i++) {
                rentList.add(list.get(i));
            }
            suburbRank.put("rentList", rentList);
            System.out.println(rentList.toString());
            
            java.util.Collections.sort(list, new java.util.Comparator<SuburbRank>() {
                @Override
                public int compare(SuburbRank o1, SuburbRank o2) {
                    if (o1.getAnnualGrowth() != null && o2.getAnnualGrowth() != null) {
                        return -Double.compare(o1.getAnnualGrowth(), o2.getAnnualGrowth());
                    } else if (o1.getAnnualGrowth() == null && o2.getAnnualGrowth() == null) {
                        return 0;
                    } else if (o1.getAnnualGrowth() != null && o2.getAnnualGrowth() == null) {
                        return -1;
                    } else if (o1.getAnnualGrowth() == null && o2.getAnnualGrowth() != null) {
                        return 1;
                    }
                    return 0;
                    
                }
            });
            List<SuburbRank> annualGrowthList = new LinkedList<>();
            for (int i = 0; i < rankNum; i++) {
                annualGrowthList.add(list.get(i));
            }
            suburbRank.put("annualGrowthList", annualGrowthList);
            
            
        } catch (InstantiationException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (suburbRank != null)  RedisUtils.setex(keyWord, GsonUtil.toJson(suburbRank), 60*60*24*30);
        
        return suburbRank;
    }
    
    private SellingHouse setDisplay(SellingHouse sellingHouse, String lang) {
        
        // 设置价格显示为首字母大写，其他字母小写
        String price = sellingHouse.getPrice();
        if (price != null && price.length() > 1) {
            String newPrice = price.substring(0, 1).toUpperCase() + price.substring(1).toLowerCase();
            sellingHouse.setPrice(newPrice);
        }
        return sellingHouse;
    }
    
    
    
}
