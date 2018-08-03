/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dubbo.spring.boot.tigerz.gm.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;


import org.bson.Document;
import org.dubbo.spring.boot.tigerz.api.enums.StatOperation;
import org.dubbo.spring.boot.tigerz.api.service.GmDubboService;

import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;

/**
 * Default {@link DemoService}
 *
 * @author wangjingci@126.com
 * @see DemoService
 * @since 1.0.0
 */
@Service(
        version = "1.0.0",
        application = "${dubbo.application.id}",
        protocol = "${dubbo.protocol.id}",
        registry = "${dubbo.registry.id}"
)
public class GmDubboServiceImpl implements GmDubboService {
    
    MongoService mongoService = MongoService.getInstance();
    private ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(10);
    //private static final Logger logger = org.slf4j.LoggerFactory.getLogger(GmDubboServiceImpl.class);
    
    
    @Override
    public String sayHello(String name) {
        System.out.println("===》调用到Provider Sayhello");
        return "Hello, " + name + " (From GM Service)";
    }
    
    
    public void statUserInfo(final String uid, final int utype, final StatOperation operation, final Map<String,Object> data) {
        
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                switch (operation) {
                case VIEW:
                    saveUserBrowseHistory(uid, utype, operation, data);
                    statUserViewData(uid, utype, operation, data);
                    break;
                case SEARCH:
                    saveUserSearchHistory(uid, utype, operation, data);
                    statUserSearchData(uid, utype, operation, data);
                default:
                    break;
                }
            }
            
        });
        
    }
    
    private synchronized void statUserSearchData(String uid, int utype, StatOperation operation, Map<String,Object> data) {
        String statCollName = "user_stat";
        BasicDBObject query = new BasicDBObject();
        query.put("uid", uid);
        Document doc = new Document(data);
        Document statData = mongoService.findOne(query, statCollName);
        if (statData == null) {
            // 如果是新用户
            // 登录次数 login_days:3  last_login_date:
            
            statData = new Document();
            statData.append("uid", uid);
            statData.append("utype", utype);
            statData.append("update_search_time",System.currentTimeMillis());
            statData.append("search_count",1);
            
            Date date = new Date();
            java.text.SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = simpleDateFormat.format(date);
            statData.append("last_login_date", dateStr);
            statData.append("login_days", 1);
            List<String> loginTime = new ArrayList<String>();
            java.text.SimpleDateFormat detailedDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String detailedTime = detailedDateFormat.format(date);
            loginTime.add(detailedTime);
            statData.append("login_time", loginTime);
            
            
            
            /**
             * 目前只记录suburb，别的搜索不管
             * bed,bath等只记录最新的值，因为考虑只会用到最新的值
             */
            String suburb = (String)doc.get("search_suburb");
            if (suburb != null) {
                Document suburbDoc = new Document();
                suburbDoc.append(suburb, 1);
                statData.append("search_suburb", suburbDoc);
                statData.append("search_suburb_new", suburb);
            }
            
            String price = (String)doc.get("search_price");
            if (price != null) {
                statData.append("search_price_new", price);
            }
            
            String bed = (String)doc.get("search_bed");
            if (bed != null) {
                statData.append("search_bed_new", bed);
            }
            
            String bath = (String)doc.get("search_bath");
            if (bath != null) {
                statData.append("search_bath_new", bath);
            }
            
            String parking = (String)doc.get("search_parking");
            if (parking != null) {
                statData.append("search_parking_new", parking);
            }
            
            @SuppressWarnings("unchecked")
            List<String> propertyType = (List<String>)doc.get("search_property_type");
            if (propertyType != null && !propertyType.isEmpty()) {
                statData.append("search_property_type_new", propertyType);
            }
            
            @SuppressWarnings("unchecked")
            List<String> feature = (List<String>)doc.get("search_feature");
            if (feature != null && !feature.isEmpty()) {
                statData.append("search_feature_new", feature);
            }
            
            Boolean isNew = (Boolean)doc.get("is_new");
            if (isNew != null) {
                statData.append("is_new", isNew);
            }
            
            Boolean isReduceIn24 = (Boolean)doc.get("is_reduce_In_24");
            if (isReduceIn24 != null) {
                statData.append("is_reduce_In_24", isReduceIn24);
            }
            
            String IP = doc.getString("ip");
            if (IP != null && IP.length() > 0) {
                statData.append("IP", IP);
            }
            
            mongoService.insertDoc(statData,statCollName);
            
        } else {
            
            long currentTime = System.currentTimeMillis();
            statData.put("update_view_time",currentTime);
            
            Date date = new Date();
            java.text.SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = simpleDateFormat.format(date);
            String lastLoginDate = (String)statData.get("last_login_date");
            if (!com.alibaba.dubbo.common.utils.StringUtils.isEquals(dateStr, lastLoginDate)) {
                statData.put("last_login_date", dateStr);
                Integer loginDays = (Integer)statData.get("login_days");
                loginDays ++;
                statData.put("login_days", loginDays);
                
                @SuppressWarnings("unchecked")
                List<String> loginTime = (List<String>)statData.get("login_time");
                java.text.SimpleDateFormat detailedDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String detailedTime = detailedDateFormat.format(date);
                loginTime.add(detailedTime);
                statData.put("login_time", loginTime);
            }
            
            Integer count = (Integer)statData.get("search_count");
            if (count != null) {
                count ++ ;
                statData.put("search_count", count);
            } else {
                statData.put("search_count", 1);
            }
            
            String suburb = (String)doc.get("search_suburb");
            if (suburb != null) {
                Document suburbDoc = (Document)statData.get("search_suburb");
                if (suburbDoc == null) {
                    suburbDoc = new Document();
                } 
                
                Integer suburbCount = (Integer)suburbDoc.get(suburb);
                if (suburbCount == null) {
                    suburbDoc.append(suburb, 1);
                } else {
                    suburbCount ++;
                    suburbDoc.append(suburb, suburbCount);
                }
                // Document的replace方法不是没有就会添加，而是如果原来没有就啥也不干，太坑了
                statData.put("search_suburb", suburbDoc);
                statData.put("search_suburb_new", suburb);
            }
            
            String price = (String)doc.get("search_price");
            if (price != null) {
                statData.put("search_price_new", price);
            }
            
            String bed = (String)doc.get("search_bed");
            if (bed != null) {
                statData.put("search_bed_new", bed);
            }
            
            String bath = (String)doc.get("search_bath");
            if (bath != null) {
                statData.put("search_bath_new", bath);
            }
            
            String parking = (String)doc.get("search_parking");
            if (parking != null) {
                statData.put("search_parking_new", parking);
            }
            
            @SuppressWarnings("unchecked")
            List<String> propertyType = (List<String>)doc.get("search_property_type");
            if (propertyType != null && !propertyType.isEmpty()) {
                statData.put("search_property_type_new", propertyType);
            }
            
            @SuppressWarnings("unchecked")
            List<String> feature = (List<String>)doc.get("search_feature");
            if (feature != null && !feature.isEmpty()) {
                statData.put("search_feature_new", feature);
            }
            
            Boolean isNew = (Boolean)doc.get("is_new");
            if (isNew != null) {
                statData.put("is_new", isNew);
            }
            
            Boolean isReduceIn24 = (Boolean)doc.get("is_reduce_In_24");
            if (isReduceIn24 != null) {
                statData.put("is_reduce_In_24", isReduceIn24);
            }
            
            String IP = doc.getString("ip");
            if (IP != null && IP.length() > 0) {
                statData.put("IP", IP);
            }
            
            BasicDBObject saveData = new BasicDBObject();
            for (String key : statData.keySet()) {
                saveData.append(key, statData.get(key));
            }
            
            
            mongoService.update(query, saveData, statCollName);
            
        }
        
    }
    
    private synchronized void statUserViewData(String uid, int utype, StatOperation operation, Map<String,Object> data) {
        String statCollName = "user_stat";
        BasicDBObject query = new BasicDBObject();
        query.put("uid", uid);
        Document doc = new Document(data);
        Document statData = mongoService.findOne(query, statCollName);
        if (statData == null) {
            // 如果是新用户
            statData = new Document();
            statData.append("uid", uid);
            statData.append("utype", utype);
            statData.append("update_view_time",System.currentTimeMillis());
            
            Date date = new Date();
            java.text.SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = simpleDateFormat.format(date);
            statData.append("last_login_date", dateStr);
            statData.append("login_days", 1);
            
            String houseId = (String)doc.get("house_id");
            if (houseId != null) {
                Document viewHouse = new Document();
                viewHouse.append(houseId,1);
                statData.append("view", viewHouse);
                statData.append("view_new", houseId);
                statData.append("view_count", 1);
            }

            
            String suburb = (String)doc.get("suburb");
            if (suburb != null) {
                Document suburbDoc = new Document();
                suburbDoc.append(suburb, 1);
                statData.append("suburb", suburbDoc);
                statData.append("suburb_new", suburb);
            }
            
            
            String street = (String)doc.get("street");
            if (street != null) {
                Document streetDoc = new Document();
                streetDoc.append(street, 1);
                statData.append("street", streetDoc);
                statData.append("street_new", street);
            }
            
            Integer price = (Integer)doc.get("house_price");
            if (price != null) {
                Document priceDoc = new Document();
                priceDoc.append(price.toString(), 1);
                statData.append("house_price", priceDoc);
                statData.append("house_price_new", price);
            }
            
            Integer bed = (Integer)doc.get("bed");
            if (bed != null) {
                Document bedDoc = new Document();
                bedDoc.append(bed.toString(), 1);
                statData.append("bed", bedDoc);
                statData.append("bed_new", bed);
            }
            
            Integer bath = (Integer)doc.get("bath");
            if (bath != null) {
                Document bathDoc = new Document();
                bathDoc.append(bath.toString(), 1);
                statData.append("bath", bathDoc);
                statData.append("bath_new", bath);
            }
            
            Integer parking = (Integer)doc.get("parking");
            if (parking != null) {
                Document parkingDoc = new Document();
                parkingDoc.append(parking.toString(), 1);
                statData.append("parking", parkingDoc);
                statData.append("parking_new", parking);
            }
            
            String IP = doc.getString("ip");
            if (IP != null && IP.length() > 0) {
                statData.append("IP", IP);
            }

            mongoService.insertDoc(statData,statCollName);
            
        } else {
            // 如果是老用户
            
            Date date = new Date();
            java.text.SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = simpleDateFormat.format(date);
            String lastLoginDate = (String)statData.get("last_login_date");
            if (!com.alibaba.dubbo.common.utils.StringUtils.isEquals(dateStr, lastLoginDate)) {
                statData.put("last_login_date", dateStr);
                Integer loginDays = (Integer)statData.get("login_days");
                loginDays ++;
                statData.put("login_days", loginDays);
                
                @SuppressWarnings("unchecked")
                List<String> loginTime = (List<String>)statData.get("login_time");
                java.text.SimpleDateFormat detailedDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String detailedTime = detailedDateFormat.format(date);
                loginTime.add(detailedTime);
                statData.put("login_time", loginTime);
            }
            
            Long updateTime = (Long)statData.get("update_view_time");
            long currentTime = System.currentTimeMillis();
            if (updateTime != null && currentTime - updateTime < 10000) {
                // 如果同一房源两次浏览时间小于10秒，认为他在刷新，不记录统计
                String houseId = (String)doc.get("house_id");
                String viewNew = (String)statData.get("view_new");
                if (com.alibaba.dubbo.common.utils.StringUtils.isEquals(viewNew, houseId)) {
                    return ;
                }
            }
            statData.put("update_view_time",currentTime);
            
            String houseId = (String)doc.get("house_id");
            if (houseId != null) {
                Document viewDoc = (Document)statData.get("view");
                if (viewDoc == null) {
                    viewDoc = new Document();
                }
                Integer value = (Integer)viewDoc.get(houseId);
                if (value != null) {
                    value++;
                    viewDoc.put(houseId, value);
                } else {
                    viewDoc.append(houseId, 1);
                }
                statData.put("view", viewDoc);
                statData.put("view_new", houseId);
                
                Integer viewCount = (Integer)statData.get("view_count");
                if (viewCount != null) {
                    viewCount++;
                    statData.put("view_count", viewCount);
                } else {
                    statData.put("view_count", 1);
                }
                
            }
            
            String suburb = (String)doc.get("suburb");
            if (suburb != null) {
                Document suburbDoc = (Document)statData.get("suburb");
                if (suburbDoc == null) {
                    suburbDoc = new Document();
                }
                Integer value = (Integer)suburbDoc.get(suburb);
                if (value != null) {
                    value++;
                    suburbDoc.put(suburb, value);
                } else {
                    suburbDoc.append(suburb, 1);
                }
                
                statData.put("suburb", suburbDoc);
                statData.put("suburb_new", suburb);
            }
            
            String street = (String)doc.get("street");
            if (street != null) {
                Document streetDoc = (Document)statData.get("street");
                if (streetDoc == null) {
                    streetDoc = new Document();
                }
                Integer value = (Integer)streetDoc.get(street);
                if (value != null) {
                    value++;
                    streetDoc.put(street, value);
                } else {
                    streetDoc.append(street, 1);
                }
                statData.put("street", streetDoc);
                statData.put("street_new", street);
            }
            
            Integer price = (Integer)doc.get("house_price");
            if (price != null) {
                Document priceDoc = (Document)statData.get("house_price");
                if (priceDoc == null) {
                    priceDoc = new Document();
                }
                Integer value = (Integer)priceDoc.get(price.toString());
                if (value != null) {
                    value++;
                    priceDoc.put(price.toString(), value);
                } else {
                    priceDoc.append(price.toString(), 1);
                }
                statData.put("house_price", priceDoc);
                statData.put("house_price_new", price);
            }
            
            Integer bed = (Integer)doc.get("bed");
            if (bed != null) {
                Document bedDoc = (Document)statData.get("bed");
                if (bedDoc == null) {
                    bedDoc = new Document();
                }
                Integer value = (Integer)bedDoc.get(bed.toString());
                if (value != null) {
                    value++;
                    bedDoc.put(bed.toString(), value);
                } else {
                    bedDoc.append(bed.toString(), 1);
                }
                statData.put("bed", bedDoc);
                statData.put("bed_new", bed);
            }
            
            Integer bath = (Integer)doc.get("bath");
            if (bath != null) {
                Document bathDoc = (Document)statData.get("bath");
                if (bathDoc == null) {
                    bathDoc = new Document();
                }
                Integer value = (Integer)bathDoc.get(bath.toString());
                if (value != null) {
                    value++;
                    bathDoc.put(bath.toString(), value);
                } else {
                    bathDoc.append(bath.toString(), 1);
                }
                statData.put("bath", bathDoc);
                statData.put("bath_new", bath);
            }
            
            Integer parking = (Integer)doc.get("parking");
            if (parking != null) {
                Document parkingDoc = (Document)statData.get("parking");
                if (parkingDoc == null) {
                    parkingDoc = new Document();
                }
                Integer value = (Integer)parkingDoc.get(parking.toString());
                if (value != null) {
                    value++;
                    parkingDoc.put(parking.toString(), value);
                } else {
                    parkingDoc.append(parking.toString(), 1);
                }

                statData.put("parking", parkingDoc);
                statData.put("parking_new", parking);
            }
            
            String IP = doc.getString("ip");
            if (IP != null && IP.length() > 0) {
                statData.put("IP", IP);
            }
            
            BasicDBObject saveData = new BasicDBObject();
            for (String key : statData.keySet()) {
                saveData.append(key, statData.get(key));
            }
            
            mongoService.update(query, saveData, statCollName);
            
        }
        
    }
    
    private void saveUserBrowseHistory(String uid, int utype, StatOperation operation, Map<String,Object> data) {
        String historyCollName = "browse_house_history";
        String houseId = (String)data.get("house_id");
        BasicDBObject query = new BasicDBObject();
        query.append("uid", uid);
        query.append("house_id", houseId);
        
        
        Document house =mongoService.findOne(query, historyCollName);
        if (house != null) {
            // 该用户之前浏览过这个房源
            long newTime = System.currentTimeMillis();
            BasicDBObject update = new BasicDBObject();
            update.append("update_time", newTime);
            mongoService.update(query, update, historyCollName);
        } else {
            // 从没没浏览过
            Document doc = new Document(data);
            doc.append("uid", uid);
            doc.append("utype", utype);
            doc.append("update_time", System.currentTimeMillis());
            mongoService.insertDoc(doc,historyCollName);
        }
    }
    
    // 目标存下来的数据并没有使用，因为数据都是偷偷记录用户的搜索行为
    private void saveUserSearchHistory(String uid, int utype, StatOperation operation, Map<String,Object> data) {
        String historyCollName = "search_history_system_record";
        Document doc = new Document(data);
        doc.append("uid", uid);
        doc.append("utype", utype);
        doc.append("update_time", System.currentTimeMillis());
        mongoService.insertDoc(doc,historyCollName);
    }
    
    
    
    

}