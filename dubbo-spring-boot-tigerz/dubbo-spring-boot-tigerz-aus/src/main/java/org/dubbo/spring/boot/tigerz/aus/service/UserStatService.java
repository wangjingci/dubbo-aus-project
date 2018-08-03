package org.dubbo.spring.boot.tigerz.aus.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.dubbo.spring.boot.tigerz.api.util.GsonUtil;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;
import org.dubbo.spring.boot.tigerz.aus.constant.RedisKey;
import org.dubbo.spring.boot.tigerz.aus.entity.BrowseHouseHistory;
import org.dubbo.spring.boot.tigerz.aus.entity.HouseListInfo;
import org.dubbo.spring.boot.tigerz.aus.entity.HouseListInfo.HouseSimpleInfo;
import org.dubbo.spring.boot.tigerz.aus.entity.SellingHouse;
import org.dubbo.spring.boot.tigerz.aus.entity.UserStat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoDao;
import com.tigerz.easymongo.MongoDaoImpl;
import com.tigerz.easymongo.MongoService;

@Service
public class UserStatService {
    
    private MongoService mongoService = new MongoService("general_manager");
    
    private MongoService mongoServiceForSellingHouse = MongoService.getInstance();
    
    @Autowired
    private ESTransportService eSTransportService;
    
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(UserStatService.class);
    

    @SuppressWarnings("static-access")
    public List<BrowseHouseHistory> getBrowseHistory(String id) throws InstantiationException, IllegalAccessException {
        
        // 获取1个月内的浏览历史，按照时间倒排序
        Date date=new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(calendar.MONTH,-1);//把日期往后增加一天.整数往后推,负数往前移动
        date=calendar.getTime(); //这个时间就是日期往后推一天的结果
        long startTime = date.getTime();
        
        BasicDBObject query = new BasicDBObject();
        query.append("uid", id);
        query.append("update_time", new BasicDBObject("$gte",startTime));
        
        BasicDBObject sort = new BasicDBObject("update_time",-1);
        
        return mongoService.find(query, sort, BrowseHouseHistory.class);
    }
    
    public void deleteBrowseHistory(String uid, String houseId) {
        BasicDBObject query = new BasicDBObject();
        query.append("house_id", houseId);
        query.append("uid", uid);
        mongoService.deleteOne(query, BrowseHouseHistory.class);
    }
    
    public void saveUserFollowHouse(String uid, Map<String, Object> data) {
        String userFollowHouse = "user_follow_house";
        Document doc = new Document(data);
        doc.append("uid", uid);
        doc.append("update_time", System.currentTimeMillis());
        mongoService.insertDoc(doc,userFollowHouse);
    }
    
    public List<Document> getUserFollowHouse(String id) {
        String userFollowHouse = "user_follow_house";
        BasicDBObject query = new BasicDBObject();
        query.append("uid", id);
        MongoDaoImpl mongoManager = mongoService.getMongoManager();
        List<Document> docs = mongoManager.find(mongoManager.getCollection(userFollowHouse), query);
        // 去掉ID，这个不需要返回给客户端
        if (docs != null) {
            for (Document house : docs) {
                house.remove("_id");
            }
        }
        return docs;
    }
    
    public void deleteUserFollowHouse(String uid, String houseId) {
        String userFollowHouse = "user_follow_house";
        BasicDBObject query = new BasicDBObject();
        query.append("uid", uid);
        query.append("houseId", houseId);
        MongoDaoImpl mongoManager = mongoService.getMongoManager();
        mongoManager.delete(mongoManager.getCollection(userFollowHouse), query);
    }
    
    public void saveUserSearchRecord(String id,String keyword, int level, String name, String interval, String displayName, Map<String,Object> filter) {
        String searchRecord = "search_record_user_save";
        Document doc = new Document();
        doc.append("uid", id);
        doc.append("keyword", keyword);
        doc.append("level", level);
        doc.append("name", name);
        doc.append("interval", interval);
        doc.append("displayName", displayName);
        Document filterDoc = new Document(filter);
        doc.append("filter", filterDoc);
        mongoService.insertDoc(doc, searchRecord);
    }
    
    public List<Document> getUserSearchRecord(String id) {
        String searchRecord = "search_record_user_save";
        BasicDBObject query = new BasicDBObject();
        query.append("uid", id);
        MongoDaoImpl mongoManager = mongoService.getMongoManager();
        List<Document> docs = mongoManager.find(mongoManager.getCollection(searchRecord), query);
        if (docs != null) {
            for (Document doc : docs) {
                ObjectId oid = (ObjectId)doc.get("_id");
                String strId = oid.toString();
                doc.put("_id", strId);
            }
        }
        return docs;
    }
    
    public void deleteUserSearchRecord(String uid, String sid) {
        String searchRecord = "search_record_user_save";
        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(sid));
        query.append("uid", uid);
        MongoDaoImpl mongoManager = mongoService.getMongoManager();
        mongoManager.delete(mongoManager.getCollection(searchRecord), query);
    }
    
    
    public List<HouseSimpleInfo> getRecommendHouse(Map<String,Object> question,String userId) {
        String q = (String)question.get("q");
        if (q == null) return null;
        if (q.equals("q1")) {
            String location = (String)question.get("location");
            int level = (Integer)question.get("level");
            int budget = (Integer)question.get("budget");
            return getRecommendHouseByQ1(location,level,budget);
        } else if (q.equals("q2")) {
            
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<HouseSimpleInfo> getRecommendHouseByQ1(String location,int level, int budget) {
        // 获取位置坐标点
        String index = "aus_index_area";
        BasicDBObject query = new BasicDBObject();
        query.append("name", location);
        query.append("level", level);
        List<Document> areas = eSTransportService.simpleSearch(index, query, 1, true);
        ArrayList<Double> center = null;
        if (areas != null && !areas.isEmpty()) {
            Document doc = areas.get(0);
            center = (ArrayList<Double>)doc.get("base_point");
        }
                
        // 获取坐标点周边10公里，价格在预算上下10%的房子
        if (center == null) return null;
        int recommendCount = 50;
        List<HouseSimpleInfo> list = eSTransportService.searchSellingHouseByPointAndPrice(center, budget,recommendCount);
        return list;
    }
    
    /**
     * 获取个性化推荐房源
     * 首先确定中心点
     * 1. 如果有搜索，就按照最新的搜索位置来
     * 2. 如果没有搜索，就按照最新看的房子位置来
     * 
     * 然后是确定期望价格
     * 1. 首先是看看有没有搜索价格
     * 2. 没有搜素就按照最新的房子价格来
     * @param uid
     * @return
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @SuppressWarnings("unchecked")
    public List<HouseSimpleInfo> getRecommendHouse(String uid) throws InstantiationException, IllegalAccessException {
        BasicDBObject query = new BasicDBObject();
        query.put("uid", uid);
        UserStat userStat = mongoService.findOne(query, UserStat.class);
        if (userStat == null) {
            return null;
        }
        
        ArrayList<Double> centerPoint = null;
        if (userStat.getSearchSuburbNew() != null && userStat.getSearchSuburbNew().length() > 0) {
            String index = "aus_index_area";
            BasicDBObject suburbPointQuery = new BasicDBObject();
            suburbPointQuery.append("name", userStat.getSearchSuburbNew());
            suburbPointQuery.append("level", 4);
            List<Document> areas = eSTransportService.simpleSearch(index, query, 1, true);
            if (areas != null && !areas.isEmpty()) {
                Document doc = areas.get(0);
                centerPoint = (ArrayList<Double>)doc.get("base_point");
            }
        } else if (userStat.getViewNew() != null && userStat.getViewNew().length() > 0) {
            String index = "aus_index_sellinghouse";
            BasicDBObject houseQuery = new BasicDBObject();
            houseQuery.append("_id", userStat.getViewNew());
            List<Document> houses = eSTransportService.simpleSearch(index, houseQuery, 1, true);
            if (houses != null && !houses.isEmpty()) {
                Document doc = houses.get(0);
                centerPoint = (ArrayList<Double>)doc.get("base_point");
            }
        } 
        
        if (centerPoint == null ) {
            logger.debug("该用户即没有搜索记录，也没有看过的房源，无法为其推荐房源");
            return null;
        }
        
        Integer centerPrice = null;
        
        // 如果搜索里有价格就用搜索的价格
        String price = userStat.getSearchPriceNew(); // 100 - any
        if (price != null && price.length() > 0) {
            String[] strs = price.split("-");
            if (strs.length >= 2) {
                String from = strs[0];
                String to = strs[1];
                if (!StringUtils.isNumeric(from) && !StringUtils.isNumeric(to)) {
                    centerPrice = null;
                } else if (StringUtils.isNumeric(from) && StringUtils.isNumeric(to)) {
                    centerPrice = (int)(Integer.valueOf(from) + Integer.valueOf(to))/2;
                } else if (StringUtils.isNumeric(from) && !StringUtils.isNumeric(to)) {
                    centerPrice = (int)(Integer.valueOf(from) * 1.1);
                } else if (!StringUtils.isNumeric(from) && StringUtils.isNumeric(to)) {
                    centerPrice = (int)(Integer.valueOf(to) * 0.9);
                }
            }
        }
        
        // 如果搜索没有价格，就用最近看的房子的价格
        if (centerPrice == null) {
            centerPrice = userStat.getHousePriceNew();
        }
        
        List<HouseSimpleInfo> list = null;
        // 如果只搜了位置却没有明确的价格信息，
        if (centerPrice != null) {
            int recommendCount = 6;
            list = eSTransportService.searchSellingHouseByPointAndPrice(centerPoint, centerPrice,recommendCount);
        } else {
            list = null;
        }
        
        
        return list;
    }
    
    /**
     * 过去的推荐算法，太过于精准，导致经常找不到房子，而且找到的房子也不一定适合
     * @param uid
     * @return
     */
    public List<HouseSimpleInfo> getRecommendHouseOld(String uid) {
        int recommendNum = 6;
        BasicDBObject query = new BasicDBObject();
        query.put("uid", uid);
        
        try {
            UserStat userStat = mongoService.findOne(query, UserStat.class);
            if (userStat != null && (userStat.getSearchSuburbNew()!=null || userStat.getViewNew()!=null)) {
                // 如果有该用户信息，则通过用户关注的suburb等搜索推荐房源
                // 暂时简单做，即获取用户关注的suburb，价格，bed，bath等数据，去搜索一个出来
                // 1. 如果有search_suburb优先用search， 如果没有search，用房源suburb
                // 2. search内部，如果搜索某个suburb超过50%， 则用这个，超不过则用最新

                
                // 1.找到搜索次数最多的suburb，及其占比如果占比超过50%，则使用，不超过则使用最新的search条件
                // 1.1 如果没有suburb，那就用view来替换
                // 2.然后是bed，room，price等关键条件
                // 3.用这些条件去搜索
                
                String suburb = null;
                String bed = null;
                String bath = null;
                String price = null;
                String likeHouse = null;
                List<HouseSimpleInfo> resultList = new LinkedList<>();
                
                // Suburb 参数
                String searchSuburbNew = userStat.getSearchSuburbNew();
                if (com.alibaba.dubbo.common.utils.StringUtils.isNotEmpty(searchSuburbNew)) {
                    // 如果有search过，那就研究使用哪个search
                    Document searchSuburbDoc = userStat.getSearchSuburb();
                    MaxKey searchSuburbMaxKey = getMaxkey(searchSuburbDoc);
                    if (searchSuburbMaxKey.rate > 0.5) {
                        suburb = searchSuburbMaxKey.key;
                    } else {
                        suburb = searchSuburbNew;
                    }

                }
                
                if (suburb == null && userStat.getViewNew() != null) {
                    Document suburbDoc = userStat.getSuburb();
                    MaxKey suburbMaxKey = getMaxkey(suburbDoc);
                    if (suburbMaxKey.rate > 0.5) {
                        suburb = suburbMaxKey.key;
                    } else {
                        suburb = userStat.getViewNew();
                    } 
                }
                
                // Bed 参数
                bed = userStat.getSearchBedNew();
                if (bed == null || userStat.getBedNew() != null) {
                    Document bedDoc = userStat.getBed();
                    Set<Integer> bedNumSet = new HashSet<>();
                    Set<String> keys = bedDoc.keySet();
                    for (String key : keys) {
                        int bedNum = bedDoc.getInteger(key);
                        bedNumSet.add(bedNum);
                    }
                    bed = "";
                    for (Integer num : bedNumSet) {
                        if (bed.length() == 0) {
                            bed = bed + num;
                        } else {
                            bed = bed + "," + num;
                        }
                    }
                }
                
                // Bath 参数
                bath = userStat.getSearchBathNew();
                if (bath == null || userStat.getBathNew() != null) {
                    Document bathDoc = userStat.getBath();
                    Set<Integer> bathNumSet = new HashSet<>();
                    Set<String> keys = bathDoc.keySet();
                    for (String key : keys) {
                        int bathNum = bathDoc.getInteger(key);
                        bathNumSet.add(bathNum);
                    }
                    bath = "";
                    for (Integer num : bathNumSet) {
                        if (bath.length() == 0) {
                            bath = bath + num;
                        } else {
                            bath = bath + "," + num;
                        }
                    }
                }
                
                // Price 参数
                double priceRate = 0.1;
                price = userStat.getSearchPriceNew();
                if (bath == null || userStat.getHousePriceNew() != null) {
                    Document priceDoc = userStat.getHousePrice();
                    Set<String> priceSet = priceDoc.keySet();
                    Integer max = 0;
                    Integer min = 0;
                    for (String priceValue : priceSet) {
                        Integer priceInt = Integer.valueOf(priceValue);
                        if (priceInt > max) max = priceInt;
                        if (priceInt < min) min = priceInt;
                    }
                    if (priceSet.size() == 1) priceRate = 0.2;
                    int min2 = (int)(min * (1 - priceRate));
                    int max2 = (int)(max * (1 + priceRate));
                    price = min2 + "-" + max2;
                }
                
                // 获取喜欢的房子
                Document viewDoc = userStat.getView();
                if (viewDoc != null) {
                    MaxKey viewMaxKey = getMaxkey(viewDoc);
                    if (viewMaxKey.rate > 0.5) {
                        likeHouse = viewMaxKey.key;
                    } else {
                        String newView = userStat.getViewNew();
                        Set<String> set = viewDoc.keySet();
                        for (String id : set) {
                            if (!id.equals(newView)) {
                                likeHouse = id;
                                break;
                            }
                        }
                        if (likeHouse == null) likeHouse = newView;
                    }
                }
                
                HouseSimpleInfo likeSimpleInfo = null;
                if (likeHouse != null) {
                    BasicDBObject queryHouse = new BasicDBObject("_id",likeHouse);
                    List<Document> docList = eSTransportService.simpleSearch(ESTransportService.INDEX_SELLING_HOUSE, queryHouse, 1, true);
                    if (docList != null && docList.size() > 0) {
                        Document doc = docList.get(0);
                        likeSimpleInfo = parseToHouseSimpleInfo(doc);
                    }
                }

                
                // 查找用户感兴趣房源
                Map<String,Object> filter = new HashMap<>();
                if (bed != null) {
                    filter.put("bed", bed);
                }
                if (bath != null) {
                    filter.put("bath", bath);
                }
                if (price != null) {
                    filter.put("price", price);
                }
                
                List<HouseListInfo> list = eSTransportService.searchHouseListByAreaWithFilter("selling",suburb, 4, 0, "default", filter);
                if (list != null && list.size() > 0) {
                    HouseListInfo listInfo = list.get(0);
                    List<HouseSimpleInfo> simpleInfoList = listInfo.getHouseInfo();
                    if (simpleInfoList != null && simpleInfoList.size() > 0) {
                        // 搜到了数据开始填充
                        if (likeSimpleInfo != null && simpleInfoList.size() > recommendNum -1) {
                            for (int i = 0; i < (recommendNum -1) ;i++) {
                                resultList.add(simpleInfoList.get(i));
                            }
                            resultList.add(likeSimpleInfo);
                        } else if (likeSimpleInfo == null && simpleInfoList.size() > recommendNum) {
                            for (int i = 0; i < recommendNum ;i++) {
                                resultList.add(simpleInfoList.get(i));
                            }
                        } else if (likeSimpleInfo != null && simpleInfoList.size() <= recommendNum -1) {
                            for (int i = 0; i < simpleInfoList.size() ;i++) {
                                resultList.add(simpleInfoList.get(i));
                            }
                            resultList.add(likeSimpleInfo);
                        } else if (likeSimpleInfo == null && simpleInfoList.size() <= recommendNum) {
                            for (int i = 0; i < simpleInfoList.size() ;i++) {
                                resultList.add(simpleInfoList.get(i));
                            }
                        }
                    } else if (likeSimpleInfo != null) {
                        resultList.add(likeSimpleInfo);
                    }
                }
                
                // 如果没有填充满，很可能是搜索条件太苛刻，采用search宽泛搜索
                if (resultList.size() < recommendNum) {
                    List<HouseListInfo> moreList = eSTransportService.searchHouseListByAreaWithFilter("selling",suburb, 4, 0, "default", null);
                    if (moreList != null && moreList.size() > 0) {
                        HouseListInfo listInfo = moreList.get(0);
                        List<HouseSimpleInfo> simpleInfoList = listInfo.getHouseInfo();
                        // 新搜到的东西有可能是重复的  只要不重复，总数不超就往里放
                        if (simpleInfoList != null && simpleInfoList.size() > 0) {
                            for (HouseSimpleInfo simpleInfo: simpleInfoList) {
                                if (resultList.size() < recommendNum) {
                                    boolean houseExist = false;
                                    for (HouseSimpleInfo result: resultList) {
                                        if (result.get_id().equals(simpleInfo.get_id())) {
                                            houseExist = true;
                                            break;
                                        }
                                    }
                                    
                                    if (houseExist == false) {
                                        resultList.add(simpleInfo);
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                        
                    }
                }
                
                
                
                // 如果没填充满，就直接搜索他最喜欢的房子周边的房子，但只包括价格筛选
                if (resultList.size() < recommendNum) {
                    if (likeSimpleInfo != null && likeSimpleInfo.getBasePoint() != null) {
                        int diff = recommendNum - resultList.size();
                        BasicDBObject basicQuery = new BasicDBObject();
                        basicQuery.append("status", "sale");
                        
                        // 设置价格范围
                        if (price != null && price.contains("-")){
                            String priceFrom = price.split("-")[0];
                            String priceTo = price.split("-")[1];
                            BasicDBObject priceCondition = new BasicDBObject();
                            priceCondition.append("$gte",Integer.valueOf(priceFrom));
                            priceCondition.append("$lte",Integer.valueOf(priceTo));
                            basicQuery.append("house_price", priceCondition);
                        }
                        
                        List<SellingHouse> sellingList = mongoServiceForSellingHouse.findByNear("base_point", basicQuery, likeSimpleInfo.getBasePoint(),  diff+1, SellingHouse.class);
                        //List<SellingHouse> sellingList = sellingHouseService.getNearbyHouse(likeSimpleInfo.getBasePoint(), basicQuery, diff+1);
                        for (SellingHouse sellingHouse : sellingList) {
                            if (!sellingHouse.get_id().equals(likeSimpleInfo.get_id())) {
                                if (resultList.size() < recommendNum) {
                                    HouseSimpleInfo one =  new HouseSimpleInfo(sellingHouse);
                                    resultList.add(one);
                                }
                            }
                        }
                    }
                }
                
                
                
                // 如果还没有填满，就拿曾经看过的房子充数，还是不够就算了
                if (resultList.size() < recommendNum) {
                    Document allSawHouse = userStat.getView();
                    if (allSawHouse != null) {
                        Set<String> ids = allSawHouse.keySet();
                        int diff = recommendNum - resultList.size();
                        List<String> toSearchList = new LinkedList<String>();
                        for (String houseId : ids) {
                            if (diff > 0) {
                                if (!houseId.equals(likeHouse)) {
                                    toSearchList.add(houseId);
                                    diff --;
                                }
                            } else {
                                break;
                            }
                            
                        }
                        if (toSearchList.size() > 0) {
                            BasicDBObject searchQuery = new BasicDBObject();
                            for (String toSearchId : toSearchList) {
                                searchQuery.append("_id", toSearchId);
                            }
                            List<Document> docList = eSTransportService.simpleSearch(ESTransportService.INDEX_SELLING_HOUSE, searchQuery, -1, false);
                            if (docList != null && docList.size() > 0) {
                                for (Document doc : docList) {
                                    resultList.add(parseToHouseSimpleInfo(doc));
                                }
                            }
                        }
                    }
                    
                }
                
                return resultList;
                
            }

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String,Object> getTDK(String keyword, int level) {
        String keyWord = RedisKey.GET_TDK + "_" + keyword + "_" + level;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return GsonUtil.fromJson(redisValue, Map.class);
        }
        
       MongoDao mongoManager = mongoServiceForSellingHouse.getMongoManager();
       BasicDBObject query = new BasicDBObject();
       query.append("level", level);
       query.append("name", keyword);
       List<Document> docList = mongoManager.find(mongoManager.getCollection("tdk"), query, null, 0, 1);
       
       Map<String,Object> result = new HashMap<>();
       if (docList != null && docList.size() > 0) {
           Document doc = docList.get(0);
           result.put("title", (String)doc.get("title"));
           result.put("description", (String)doc.get("description"));
           //System.out.println(doc.keySet().toString());
           List<String> keywords = (ArrayList<String>)doc.get("kewords");
           result.put("keyword", keywords.get(0));
           RedisUtils.setex(keyWord, GsonUtil.toJson(result), 60*60*24*365);
           
           return result;
       } else {
           return null;
       }
       
    }
    
    public void signUserIsFollow(String uid, List<HouseListInfo> list) {
        if (list == null || list.isEmpty() ) {
            return;
        }
        
        System.out.println(list.toString());
        HouseListInfo houseListInfo = list.get(0);
        List<HouseSimpleInfo> simpeHouseList = houseListInfo.getHouseInfo();
        if (simpeHouseList == null || simpeHouseList.isEmpty()) {
            return;
        }
        
        List<Document> docs = getUserFollowHouse(uid);
        Set<String> ids = new HashSet<>();
        for (Document doc : docs) {
            ids.add((String)doc.getString("houseId"));
        }
        
        for (HouseSimpleInfo houseSimpleInfo : simpeHouseList) {
            String id = houseSimpleInfo.get_id();
            if (ids.contains(id)) {
                houseSimpleInfo.setIsFollowed(true);
            }
        }
    }
    
    public static class MaxKey {
        public String key;
        public double rate;
    }
    
    
    private MaxKey getMaxkey(Document doc) {
        MaxKey maxKey = new MaxKey();
        Set<String> keys = doc.keySet();
        int total = 0;
        String currMaxKey = null;
        int currMaxValue = 0;
        for (String key : keys) {
            int value = doc.getInteger(key, 0);
            if (value > currMaxValue) {
                currMaxKey = key;
                currMaxValue = value;
            }
            total = total + value;
        }
        maxKey.key = currMaxKey;
        maxKey.rate = doc.getInteger(currMaxKey, 0) * 1.0 /total;
        
        return maxKey;
    }
    
    @SuppressWarnings("unchecked")
    private HouseSimpleInfo parseToHouseSimpleInfo(Document houseDoc) {
        HouseSimpleInfo houseSimpleInfo = new HouseSimpleInfo();
        
        houseSimpleInfo.set_id((String)houseDoc.get("_id"));
        houseSimpleInfo.setBathroom((Integer)houseDoc.get("baths"));
        houseSimpleInfo.setBedroom((Integer)houseDoc.get("beds"));
        houseSimpleInfo.setParking((Integer)houseDoc.get("parking"));
        // TODO 这个图片是domain的图，暂时先用.我们的首页是house_main_image_path
        houseSimpleInfo.setHouseMainImagePath((String)houseDoc.get("house_pic_main"));
        houseSimpleInfo.setImages((ArrayList<String>)houseDoc.get("images"));
        houseSimpleInfo.setHousePrice((String)houseDoc.get("price"));
        houseSimpleInfo.setPrice((Integer)houseDoc.get("house_price"));
        String dateStr = (String)houseDoc.get("created_on");
        if(dateStr != null){
            long dateLong = Long.valueOf(dateStr);
            houseSimpleInfo.setListedDate(dateLong);
        }
        ArrayList<Double> point = (ArrayList<Double>)houseDoc.get("base_point");

        houseSimpleInfo.setBasePoint(point);
        houseSimpleInfo.setStatus((String)houseDoc.get("status"));
        houseSimpleInfo.setTitle((String)houseDoc.get("headline"));
        houseSimpleInfo.setSuburb((String)houseDoc.get("suburb"));
        houseSimpleInfo.setAddress((String)houseDoc.get("address"));
        houseSimpleInfo.setTagList((HashMap<String,Object>)houseDoc.get("tag_list"));
        
        return houseSimpleInfo;
    }
    

}
