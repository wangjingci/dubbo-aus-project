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
package org.dubbo.spring.boot.tigerz.aus.controller;
import com.alibaba.dubbo.config.annotation.Reference;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.dubbo.spring.boot.tigerz.api.entity.User;
import org.dubbo.spring.boot.tigerz.api.enums.StatOperation;
import org.dubbo.spring.boot.tigerz.api.service.GmDubboService;
import org.dubbo.spring.boot.tigerz.api.service.UserService;
import org.dubbo.spring.boot.tigerz.api.util.GsonUtil;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;
import org.dubbo.spring.boot.tigerz.aus.constant.RedisKey;
import org.dubbo.spring.boot.tigerz.aus.dto.FluzzySearchResponse;
import org.dubbo.spring.boot.tigerz.aus.dto.StandardResponse;
import org.dubbo.spring.boot.tigerz.aus.entity.BrowseHouseHistory;
import org.dubbo.spring.boot.tigerz.aus.entity.HouseListInfo;
import org.dubbo.spring.boot.tigerz.aus.entity.HouseListInfo.HouseSimpleInfo;
import org.dubbo.spring.boot.tigerz.aus.entity.NewsList;
import org.dubbo.spring.boot.tigerz.aus.entity.RentingHouse;
import org.dubbo.spring.boot.tigerz.aus.entity.School;
import org.dubbo.spring.boot.tigerz.aus.entity.SellingHouse;
import org.dubbo.spring.boot.tigerz.aus.entity.SellingSoldHouse;
import org.dubbo.spring.boot.tigerz.aus.service.ESTransportService;
import org.dubbo.spring.boot.tigerz.aus.service.NewsService;
import org.dubbo.spring.boot.tigerz.aus.service.UserStatService;
import org.dubbo.spring.boot.tigerz.aus.service.RentingHouseService;
import org.dubbo.spring.boot.tigerz.aus.service.SellingHouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * General Managerment Controller (REST)
 *
 * @author wangjingci@126.com
 * @see
 * @since 1.0.0
 */
@CrossOrigin(origins = "*", maxAge = 3600)  //跨域
@RestController
public class PropertyController {
    
    private final Logger logger = LoggerFactory.getLogger(PropertyController.class);
    @Autowired
    private SellingHouseService sellingHouseService;
    
    @Autowired
    private RentingHouseService rentingHouseService;
    
    @Autowired
    private ESTransportService eSTransportService;
    
    @Autowired
    private UserStatService userStatService;
    
    @Autowired
    private NewsService newsService;
    
    
    /**
     * @Reference定义了远程提供服务的地址
     */
    @Reference(version = "1.0.0",
            application = "${dubbo.application.id}",
            //check = false,
            //async = true,
            //sent = false,
            //url = "dubbo://localhost:12345"
            registry = "${dubbo.registry.id}"
            )  // 采用直连方式,会绕过注册中心,多地址用分号隔开
            
    private GmDubboService gmDubboService;
    
    @Reference(version = "1.0.0",
            application = "${dubbo.application.id}",
            //check = false,
            registry = "${dubbo.registry.id}")
    private UserService userService;
    
    /**
     * 测试Dubbo服务
     * @param name
     * @return
     */
    @RequestMapping(value = "dubboTest/{name}", method = RequestMethod.GET)
    public String sayHello(@ApiParam(defaultValue="wjc") @PathVariable("name") String name) {
        System.out.println("===》启动sayHello");
        return gmDubboService.sayHello(name);
    }
    
//    /**
//     * 暂时测试该功能是否可用
//     * @param token
//     * @return
//     */
//    @RequestMapping(value = "getUserByToken/{token}", method = RequestMethod.GET)
//    public User getUserByToken(@ApiParam(value="token") @PathVariable("token") String token) {
//        return userService.getUserByToken(token);
//    }
    
    @ApiOperation(value = "获取可售房源详情页基本信息", notes = "SellingHouse的Id") 
    @RequestMapping(value = "getSellingHouseBaseInfo/{id}/{lang}", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getSellingHouseBaseInfo(HttpServletRequest request,
            @ApiParam(defaultValue="5a9fb7bb49f75640e4e512d6")  @PathVariable("id") String id,
            @ApiParam(defaultValue="cn")  @PathVariable("lang") String lang) throws Exception {
        SellingHouse sellingHouse = sellingHouseService.getSellingHouse(id, lang);
        sendViewStat(request,sellingHouse);
        StandardResponse sr = new StandardResponse();
        sr.setData(sellingHouse);
        return sr;
    }
    
    @ApiOperation(value = "获取已售房源基本信息", notes = "SellingSoldHouse的Id") 
    @RequestMapping(value = "getSoldHouseBaseInfo/{id}/{lang}", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getSoldHouseBaseInfo(HttpServletRequest request,
            @ApiParam(defaultValue="5aa1f54faebec106d4c9e85b")  @PathVariable("id") String id,
            @ApiParam(defaultValue="cn")  @PathVariable("lang") String lang) throws Exception {
        SellingSoldHouse soldHouse = sellingHouseService.getSoldHouse(id, lang);
        //sendViewStat(request,sellingSoldHouse);
        StandardResponse sr = new StandardResponse();
        sr.setData(soldHouse);
        return sr;
    }
    
    @ApiOperation(value = "获取可租房源详情页基本信息", notes = "RentingHouse的Id") 
    @RequestMapping(value = "getRentingHouseBaseInfo/{id}/{lang}", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getRentingHouseBaseInfo(
            @ApiParam(defaultValue="5aab445d49f75607c4eb866d")  @PathVariable("id") String id,
            @ApiParam(defaultValue="cn")  @PathVariable("lang") String lang) throws Exception {
        //logger.info("getRentingHouseBaseInfo");
        RentingHouse rentingHouse = rentingHouseService.getRentingHouse(id, lang);
        StandardResponse sr = new StandardResponse();
        sr.setData(rentingHouse);
        return sr;
    }
    
    @ApiOperation(value = "获取附近可售房源信息", notes = "获取附近可售房源信息，按照距离返回30个数据")
    @RequestMapping(value = "/getNearbyHouse",method = RequestMethod.POST)
    public @ResponseBody StandardResponse getNearbyHouse(@ApiParam(defaultValue = "{\"basePoint\":[174.6167169 ,-36.89577225]}") @RequestBody Map<String,ArrayList<Double>> basePoint) throws Exception {
        ArrayList<Double> point = basePoint.get("basePoint");
        List<SellingHouse> list = sellingHouseService.getNearbyHouse(point);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "获取附近成交房源信息", notes = "获取附近成交房源信息，按照距离返回N个数据")
    @RequestMapping(value = "/getNearbyDealedHouse",method = RequestMethod.POST)
    public @ResponseBody StandardResponse getNearbySoldHouse(@ApiParam(value = "{\"basePoint\":[174.6167169 ,-36.89577225]}") @RequestBody Map<String,ArrayList<Double>> basePoint) throws Exception {
        ArrayList<Double> point = basePoint.get("basePoint");
        List<SellingSoldHouse> list = sellingHouseService.getNearbySoldHouse(point);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "获取24小降价房源", notes = "24小时降价房源") 
    @RequestMapping(value = "getReducePriceHouse", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getReducePriceHouse() {
        String keyWord = RedisKey.GET_REDUCE_PRICE_HOUSE;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        List<HouseSimpleInfo> houseList = null;
        if(redisValue != null){
            houseList = GsonUtil.fromJson(redisValue, List.class);
            StandardResponse sr = new StandardResponse();
            sr.setData(houseList);
            return sr;
        }
        
        int limit = 6;
        Map<String, Object> filter = new HashMap<>();
        filter.put("isReduceIn24", true);
        List<HouseListInfo> list = eSTransportService.searchHouseListByAreaWithFilter("selling","Australia", 0, 0, "default", filter);

        if (list != null && list.size() > 0) {
            HouseListInfo houseListInfo = list.get(0);
            if (houseListInfo.getHouseInfo().size() > limit) {
                houseList = houseListInfo.getHouseInfo().subList(0, limit);
            } else {
                houseList = houseListInfo.getHouseInfo();
            }
        }
        
        StandardResponse sr = new StandardResponse();
        sr.setData(houseList);
        
        if (houseList != null) {
            RedisUtils.setex(keyWord, GsonUtil.toJson(houseList), 60*60*8);
        } 
        
        return sr;
    }
    
    @ApiOperation(value = "个性化推荐房源", notes = "个性化推荐") 
    @RequestMapping(value = "getRecommendHouse", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getRecommendHouse(HttpServletRequest request) throws InstantiationException, IllegalAccessException {
        String uid = getUserId(request);
        //uid = "79e11c1d-db62-4217-866a-83e05178039f";
        List<HouseSimpleInfo> list = userStatService.getRecommendHouse(uid);
        
        // 如果没有任何用户信息，则给用户推荐热门房源
        if (list == null) {
            int recommendNum = 6;
            List<HouseListInfo> houselist = eSTransportService.searchHouseListByAreaWithFilter("selling","Australia", 0, 0, "default", null);
            HouseListInfo listInfo = houselist.get(0);
            list = listInfo.getHouseInfo().subList(0, recommendNum);
        }

        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "按照问题推荐房源", notes = "按照问题推荐房子") 
    @RequestMapping(value = "getRecommendHouse", method = RequestMethod.POST)
    public @ResponseBody StandardResponse getRecommendHouse(HttpServletRequest request, 
            @ApiParam(value = "{\"q\":\"q1\",\"location\":\"Alexandria\",\"level\":4,\"budget\":874000}") @RequestBody Map<String, Object> question) {
        User user= getLoginedUser(request);
        String userId = null;
        if (user != null) {
            userId = user.get_id();
        }
        List<HouseSimpleInfo> list = userStatService.getRecommendHouse(question,userId);
        
        // 如果没有任何用户信息，则给用户推荐热门房源
        if (list == null) {
            int recommendNum = 6;
            List<HouseListInfo> houselist = eSTransportService.searchHouseListByAreaWithFilter("selling","Australia", 0, 0, "default", null);
            HouseListInfo listInfo = houselist.get(0);
            list = listInfo.getHouseInfo().subList(0, recommendNum);
        }

        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "获取首页统计排行榜", notes = "数据排行榜") 
    @RequestMapping(value = "getSuburbRank", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getSuburbRank() {
        StandardResponse sr = new StandardResponse();
        sr.setData(sellingHouseService.getSuburbRank());
        return sr;
    }
    
    @RequestMapping(value = "getNewsList/{state}/{pageCount}/{page}", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getNewsList(
            @ApiParam(value="all")  @PathVariable("state") String state,
            @ApiParam(value="30")  @PathVariable("pageCount") int pageCount,
            @ApiParam(value="1")  @PathVariable("page") int page) throws Exception {
        List<NewsList> newsList = newsService.getNewsList(page, state, pageCount);
        StandardResponse sr = new StandardResponse();
        sr.setData(newsList);
        return sr;
    }
    
    @RequestMapping(value = "getOneNews/{id}", method = RequestMethod.GET)
    public @ResponseBody StandardResponse getOneNews(
            @ApiParam(value="5b1f8bea49f7562af49b4887")  @PathVariable("id") String id) throws Exception {
        NewsList newsList = newsService.getOneNews(id);
        StandardResponse sr = new StandardResponse();
        sr.setData(newsList);
        return sr;
    }
    
    
    
    @Deprecated
    @ApiOperation(value = "按照区域查找房源-Deprecated", notes = "以区域为条件查找房源")
    @RequestMapping(value = "/searchHouseByArea",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse searchHouseByArea(@ApiParam(value = "{\"scope\":\"all\",\"name\":\"Spencer Park\",\"level\":4,\"page\":0,\"sort\":\"newest\",\"bedroom\":{\"all\":true,\"one\":false,\"two\":false,\"three\":false,\"four\":false,\"more\":false}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        String scope = (String)houseQuery.get("scope");
        String name = (String)houseQuery.get("name");
        int level = (int)houseQuery.get("level");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        @SuppressWarnings("unchecked")
        Map<String,Boolean> bedroomFilter = (Map<String,Boolean>)houseQuery.get("bedroom");
        List<HouseListInfo> list = eSTransportService.searchHouseByArea(scope,name,level,page,sort,bedroomFilter);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "搜索地图房源", notes = "以区域为条件查找地图房源")
    @RequestMapping(value = "/searchHouseByAreaWithFilter",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse searchHouseByAreaWithFilter (
            HttpServletRequest request,
            @ApiParam(value = "{\"cate\":\"selling\",\"name\":\"Australia\",\"level\":0,\"page\":0,\"sort\":\"default\",\"filter\":{\"bed\":\"1-3\"}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        String cate = (String)houseQuery.get("cate");
        if (cate == null || cate.length() == 0) {
            cate = "selling";
        }
        String name = (String)houseQuery.get("name");
        int level = (int)houseQuery.get("level");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        @SuppressWarnings("unchecked")
        Map<String,Object> filter = (Map<String,Object>)houseQuery.get("filter");
        List<HouseListInfo> list = eSTransportService.searchHouseByAreaWithFilter(cate,name,level,page,sort,filter);
        if (list != null && !list.isEmpty()) {
            User user = getLoginedUser(request);
            if (user != null) {
                userStatService.signUserIsFollow(user.get_id(), list);
            }
        }
        sendSearchStat(request, name, level, filter);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "搜索列表房源，不带地图信息", notes = "以区域为条件查找房源")
    @RequestMapping(value = "/searchHouseListByAreaWithFilter",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse searchHouseListByAreaWithFilter (
            HttpServletRequest request,
            @ApiParam(value = "{\"name\":\"Australia\",\"level\":0,\"page\":0,\"sort\":\"default\",\"filter\":{\"bed\":\"1-3\"}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        String cate = (String)houseQuery.get("cate");
        if (cate == null) {
            cate = "selling";
        }
        String name = (String)houseQuery.get("name");
        int level = (int)houseQuery.get("level");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        @SuppressWarnings("unchecked")
        Map<String,Object> filter = (Map<String,Object>)houseQuery.get("filter");
        List<HouseListInfo> list = eSTransportService.searchHouseListByAreaWithFilter(cate,name,level,page,sort,filter);
        sendSearchStat(request, name, level, filter);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @Deprecated
    @ApiOperation(value = "按照地图查找房源", notes = "以地图为条件查找房源")
    @RequestMapping(value = "/searchHouseByMap",method = RequestMethod.POST)
    public @ResponseBody StandardResponse searchHouseByMap(@ApiParam(value = "{\"zoom\":14,\"bounds\":[174.93427726168602,-36.9125568784113,174.87822982211082,-36.9531727613143],\"page\":0,\"sort\":\"default\",\"isAllHouse\":false,\"bedroom\":{\"all\":false,\"one\":false,\"two\":true,\"three\":false,\"four\":false,\"more\":false}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        int zoom = (int)houseQuery.get("zoom");
        @SuppressWarnings("unchecked")
        ArrayList<Double> bounds = (ArrayList<Double>)houseQuery.get("bounds");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        @SuppressWarnings("unchecked")
        Map<String,Boolean> bedroomFilter = (Map<String,Boolean>)houseQuery.get("bedroom");
        List<HouseListInfo> list = eSTransportService.searchHouseByMap(zoom,bounds,page,sort,bedroomFilter);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "按照地图及过滤条件查找房源", notes = "以地图为条件查找房源")
    @RequestMapping(value = "/searchHouseByMapWithFilter",method = RequestMethod.POST)
    public @ResponseBody StandardResponse searchHouseByMapWithFilter(
            HttpServletRequest request,
            @ApiParam(value = "{\"cate\":\"selling\",\"zoom\":15,\"bounds\":[149.15165099475098,-35.158540855409555,149.12418517443848,-35.17278450203736],\"page\":0,\"sort\":\"default\",\"filter\":{},\"includeSchool\":false,\"schoolFilter\":{}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        String cate = (String)houseQuery.get("cate");
        if (cate == null || cate.length() == 0) {
            cate = "selling";
        }
        int zoom = (int)houseQuery.get("zoom");
        ArrayList<Double> bounds = (ArrayList<Double>)houseQuery.get("bounds");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        Map<String,Object> filter = (Map<String,Object>)houseQuery.get("filter");
        List<HouseListInfo> list = eSTransportService.searchHouseByMapWithFilter(cate,zoom,bounds,page,sort,filter);
        if (list != null && !list.isEmpty()) {
            User user = getLoginedUser(request);
            if (user != null) {
                userStatService.signUserIsFollow(user.get_id(), list);
            }
        }
        
        if (filter != null && filter.size() > 0) {
            sendSearchStat(request, null, 0, filter);
        }
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "智能联想提示", notes = "根据用户数据的关键词给出联想提示")
    @RequestMapping(value = "/searchHintInFuzzy",method = RequestMethod.POST)
    public @ResponseBody StandardResponse searchHintInFuzzy(@ApiParam(value = "{\"content\":\"Central North Island\"}") @RequestBody Map<String,String> queryContent) throws Exception {
        String content = queryContent.get("content");
        String cate = queryContent.get("cate");
        if (cate == null || cate.length() == 0) {
            cate = "selling";
        }
        List<FluzzySearchResponse> list = eSTransportService.searchHintInFuzzy(content, cate);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @Deprecated
    @ApiOperation(value = "模糊搜索", notes = "按照关键词模糊搜索")
    @RequestMapping(value = "/searchInFuzzy",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse searchInFuzzy(@ApiParam(value = "{\"content\":\"Spencer Park\",\"page\":0,\"sort\":\"newest\",\"bedroom\":{\"all\":true,\"one\":false,\"two\":false,\"three\":false,\"four\":false,\"more\":false}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        String content = (String)houseQuery.get("content");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        @SuppressWarnings("unchecked")
        Map<String,Boolean> bedroomFilter = (Map<String,Boolean>)houseQuery.get("bedroom");
        List<HouseListInfo> list = eSTransportService.searchInFuzzy(content,page,sort,bedroomFilter);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "模糊搜索", notes = "按照关键词模糊搜索")
    @RequestMapping(value = "/searchInFuzzyWithFilter",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse searchInFuzzyWithFilter(
            HttpServletRequest request,
            @ApiParam(value = "{\"cate\":\"selling\",\"content\":\"Spencer Park\",\"page\":0,\"sort\":\"newest\",\"filter\":{\"bed\":\"1-3\"}}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        String cate = (String)houseQuery.get("cate");
        String content = (String)houseQuery.get("content");
        int page = (int)houseQuery.get("page");
        String sort = (String)houseQuery.get("sort");
        @SuppressWarnings("unchecked")
        Map<String,Object> filter = (Map<String,Object>)houseQuery.get("filter");
        List<HouseListInfo> list = eSTransportService.searchInFuzzyWithFilter(cate,content,page,sort,filter);
        if (list != null && !list.isEmpty()) {
            User user = getLoginedUser(request);
            if (user != null) {
                userStatService.signUserIsFollow(user.get_id(), list);
            }
        }
        
        sendSearchStat(request, null, 0, filter);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    @ApiOperation(value = "搜索某个学校的学区房", notes = "通过学区房ID搜索这个学校周边住房")
    @RequestMapping(value = "searchSchoolHouse/{id}", method = RequestMethod.GET)
    public @ResponseBody StandardResponse searchSchoolHouse(
            @ApiParam(value="5aced0db49f75627b4846c09")  @PathVariable("id") String id) throws Exception {
        List<HouseListInfo> list = eSTransportService.searchSchoolHouseById(id);
        StandardResponse sr = new StandardResponse();
        sr.setData(list);
        return sr;
    }
    
    
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "在地图上搜索学校", notes = "地图上查找学校")
    @RequestMapping(value = "/searchSchoolByMapWithFilter",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse searchSchoolByMapWithFilter(@ApiParam(value = "{\"points\":[],\"sort\":\"default\",\"filter\":{\"level\":\"Elementary\",\"type\":\"Public\"}}") @RequestBody Map<String,Object> schoolQuery) throws Exception {
        ArrayList<Double> points = (ArrayList<Double>)schoolQuery.get("points");
        String sort = (String)schoolQuery.get("sort");
        Map<String,Object> filter = (HashMap<String,Object>)schoolQuery.get("filter");
        List<School> schoolList = eSTransportService.searchSchoolByMapWithFilter(points, sort, filter);
        StandardResponse sr = new StandardResponse();
        sr.setData(schoolList);
        return sr;
    }
    
    
    @ApiOperation(value = "获取多个对比房源", notes = "多个房源进行对比，目前无上限要求")
    @RequestMapping(value = "/getSeveralSellingHouse",method = RequestMethod.POST)           
    public @ResponseBody StandardResponse getSeveralSellingHouse(@ApiParam(value = "{\"ids\":[\"5a9fb7bb49f75640e4e512d5\",\"5a9fb7bb49f75640e4e512d7\"]}") @RequestBody Map<String,Object> houseQuery) throws Exception {
        @SuppressWarnings("unchecked")
        ArrayList<String> ids = houseQuery==null?null:(ArrayList<String>)houseQuery.get("ids");
        List<SellingHouse> sellingHouses = sellingHouseService.getSeveralSellingHouse(ids);
        StandardResponse sr = new StandardResponse();
        sr.setData(sellingHouses);
        return sr;
    }
    
    @SuppressWarnings("unchecked")
    @ApiOperation(value = "保存用户搜索条件", notes = "保存用户搜索条件")
    @RequestMapping(value = "/saveUserSearchRecord",method = RequestMethod.POST) 
    public @ResponseBody StandardResponse saveUserSearchRecord(
            HttpServletRequest request,
            HttpSession session,
            @ApiParam(value = "{\"keyword\":\"Sydney\",\"level\":4,\"name\":\"my search\",\"interval\":\"day\",\"displayName\":\"Sydney, NSW, 2000\",\"filter\":{}}") @RequestBody Map<String,Object> searchRecord) {
        
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String id = getUserId(request);
        String keyword = (String)searchRecord.get("keyword");
        int level = (Integer)searchRecord.get("level");
        String name = (String)searchRecord.get("name");
        String interval = (String)searchRecord.get("interval");
        String displayName = (String)searchRecord.get("displayName");
        Map<String, Object> filter = (Map<String,Object>)searchRecord.get("filter");
        
        userStatService.saveUserSearchRecord(id, keyword, level,name,interval,displayName, filter);
        
        return sr;
    }
    
    @ApiOperation(value = "获取用户的搜索条件", notes = "仅限登陆用户")
    @RequestMapping(value = "/getUserSearchRecord",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse getUserSearchRecord(
            HttpServletRequest request,
            HttpSession session) {
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String id = getUserId(request);
        List<Document> searchRecords = userStatService.getUserSearchRecord(id);
        sr.setData(searchRecords);
        return sr;
    }
    
    @ApiOperation(value = "删除用户搜索条件", notes = "删除用户搜索条件")
    @RequestMapping(value = "/deleteUserSearchRecord/{sid}",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse deleteUserSearchRecord(
            HttpServletRequest request,
            @ApiParam(value="5aced0db49f75627b4846c09")  @PathVariable("sid") String sid
            ) {
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        String id = getUserId(request);
        userStatService.deleteUserSearchRecord(id,sid);
        return sr;
    }
    
    @ApiOperation(value = "获取用户浏览历史", notes = "仅限登陆用户")
    @RequestMapping(value = "/getBrowseHistory",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse getBrowseHistory(
            HttpServletRequest request,
            HttpSession session) throws InstantiationException, IllegalAccessException {
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String id = getUserId(request);
        List<BrowseHouseHistory> browseRecords = userStatService.getBrowseHistory(id);
        sr.setData(browseRecords);
        return sr;
    }
    
    @ApiOperation(value = "删除用户浏览历史记录", notes = "删除用户浏览历史记录")
    @RequestMapping(value = "/deleteBrowseHistory/{houseId}",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse deleteBrowseHistory(
            HttpServletRequest request,
            HttpSession session,
            @ApiParam(value="5aced0db49f75627b4846c09")  @PathVariable("houseId") String houseId) {
        
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String uid = getUserId(request);
        //TODO 如果用户没有关注这个房子，这里将会发生什么？
        userStatService.deleteBrowseHistory(uid, houseId);
        return sr;
    }
    
    @ApiOperation(value = "保存用户关注的房源", notes = "保存用户关注房源的基本信息")
    @RequestMapping(value = "/saveUserFollowHouse",method = RequestMethod.POST) 
    public @ResponseBody StandardResponse saveUserFollowHouse(
            HttpServletRequest request,
            HttpSession session,
            @ApiParam(value = "{houseId,mainPic,price，address,bed,bath,car 几个属性}") @RequestBody Map<String, Object> houseBasicInfo) {
        
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String id = getUserId(request);
        userStatService.saveUserFollowHouse(id, houseBasicInfo);
        return sr;
    }
    
    @ApiOperation(value = "删除用户关注的房源", notes = "删除用户关注房源的基本信息")
    @RequestMapping(value = "/deleteUserFollowHouse/{houseId}",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse deleteUserFollowHouse(
            HttpServletRequest request,
            HttpSession session,
            @ApiParam(value="5aced0db49f75627b4846c09")  @PathVariable("houseId") String houseId) {
        
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String uid = getUserId(request);
        //TODO 如果用户没有关注这个房子，这里将会发生什么？
        userStatService.deleteUserFollowHouse(uid, houseId);
        return sr;
    }
    
    @ApiOperation(value = "获取用户关注的房源列表", notes = "获取用户关注房源列表")
    @RequestMapping(value = "/getUserFollowHouse",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse getUserFollowHouse(
            HttpServletRequest request,
            HttpSession session) {
        StandardResponse sr = new StandardResponse();
        if (getLoginedUser(request) == null) {
            sr.setIsOK(false);
            sr.setErrorMessage("Please login first !");
            return sr;
        }
        
        String id = getUserId(request);
        List<Document> houses = userStatService.getUserFollowHouse(id);
        
        sr.setData(houses);
        return sr;
    }
    
    @ApiOperation(value = "获得Title，Desc等", notes = "SEO的title，desc，keyword")
    @RequestMapping(value = "/getTDK/{keyword}/{level}",method = RequestMethod.GET) 
    public @ResponseBody StandardResponse getTDK(
            @ApiParam(value="Victoria")  @PathVariable("keyword") String name,
            @ApiParam(value="1")  @PathVariable("level") Integer level) {
        
        StandardResponse sr = new StandardResponse();
        sr.setData(userStatService.getTDK(name, level));
        return sr;
    }
    
    
    
    
    private enum UType {
        USER_ID,
        CANVAS_ID,
        SESSION_ID;
    }
    
    
    
    /**
     * 包括area搜索与地图搜素
     * 1. Area
     *    只有level是suburb的搜索才会被统计，但所有的level的搜索都会作为搜索记录记录
     * 2. Map
     *    只有含有搜索条件的搜索才会被记录下来
     * @param request
     * @param name
     * @param level
     * @param filter
     */
    private void sendSearchStat(HttpServletRequest request, String name, int level, Map<String, Object> filter) {
        if (gmDubboService == null) return;
        
        if (isValidReqeust(request) == false) return;
        
        long startTime = System.currentTimeMillis();
        String IP = request.getRemoteAddr();
        
        HttpSession session = request.getSession();
        if (session == null) return;
//        if (session.isNew()) {
//            session.setMaxInactiveInterval(-1);
//        }
        User user = (User)session.getAttribute("user");
        String uid = null;
        int utype = UType.SESSION_ID.ordinal();
        if (user == null) {
            // 获取canvasId
            Cookie[] cookies = request.getCookies();
            String canvasId = null;
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if(StringUtils.equals(cookie.getName(), "canvas")) {
                        canvasId = cookie.getValue();
                    }
                }
            }
            
            if (canvasId != null) {
                uid = canvasId;
                utype = UType.CANVAS_ID.ordinal();
            } else {
                uid = session.getId();
                utype = UType.SESSION_ID.ordinal();
            }
            
        } else {
            uid = user.get_id();
            utype = UType.USER_ID.ordinal();
        }
        
        Map<String,Object> map = new HashMap<String,Object>();
        
        
        
        if (!StringUtils.isEmpty(name) && level == 4) {
            map.put("search_suburb",name);
        }
        
        String price = (String)filter.get("price");
        if (price != null) {
            map.put("search_price",price);
        }
        
        String bed = (String)filter.get("bed");
        if (bed != null) {
            map.put("search_bed",bed);
        }
        
        String bath = (String)filter.get("bath");
        if (bath != null) {
            map.put("search_bath",bath);
        }
        
        String parking = (String)filter.get("parking");
        if (parking != null) {
            map.put("search_parking",parking);
        }
        
        @SuppressWarnings("unchecked")
        List<String> propertyType = (List<String>)filter.get("propertyType");
        if (propertyType != null) {
            map.put("search_property_type",propertyType);
        }
        
        @SuppressWarnings("unchecked")
        List<String> feature = (List<String>)filter.get("feature");
        if (propertyType != null) {
            map.put("search_feature",feature);
        }
        
        Boolean isNew = (Boolean)filter.get("isNew");
        if (isNew != null) {
            map.put("is_new", isNew);
        }
        
        Boolean isReduceIn24 = (Boolean)filter.get("isReduceIn24");
        if (isReduceIn24 != null) {
            map.put("is_reduce_In_24", isReduceIn24);
        }
        
        map.put("ip", IP);
        
        
        
        try {
            gmDubboService.statUserInfo(uid, utype, StatOperation.SEARCH, map);
        } catch (Exception e){
            logger.error("通过dubbo传送消息失败！\n" + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        logger.debug("sendStat in search cost time:{}", endTime - startTime);
        
        
    }
    
    
    private void sendViewStat(HttpServletRequest request, SellingHouse sellingHouse) {
        if (sellingHouse == null || gmDubboService == null) return;
        
        if (isValidReqeust(request) == false) {
            return ;
        }
        
        String IP = request.getRemoteAddr();
        
        HttpSession session = request.getSession();
        if (session == null) return;
//        if (session.isNew()) {
//            session.setMaxInactiveInterval(-1);
//        }
        User user = (User)session.getAttribute("user");
        String uid = null;
        int utype = UType.SESSION_ID.ordinal();
        if (user == null) {
            // 获取canvasId
            Cookie[] cookies = request.getCookies();
            String canvasId = null;
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if(StringUtils.equals(cookie.getName(), "canvas")) {
                        canvasId = cookie.getValue();
                    }
                }
            }
            
            if (canvasId != null) {
                uid = canvasId;
                utype = UType.CANVAS_ID.ordinal();
            } else {
                uid = session.getId();
                utype = UType.SESSION_ID.ordinal();
            }
            
        } else {
            uid = user.get_id();
            utype = UType.USER_ID.ordinal();
        }
        
        long startTime = System.currentTimeMillis();
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("house_id", sellingHouse.get_id());
        map.put("bed", sellingHouse.getBeds());
        map.put("bath", sellingHouse.getBaths());
        map.put("parking", sellingHouse.getParking());
        map.put("house_price", sellingHouse.getHousePrice());
        map.put("price", sellingHouse.getPrice());
        map.put("street", sellingHouse.getStreet());
        map.put("suburb", sellingHouse.getSuburb());
        map.put("ip", IP);
        map.put("main_pic", sellingHouse.getHousePicMain());
        map.put("address", sellingHouse.getAddress());
        
        try {
            gmDubboService.statUserInfo(uid, utype, StatOperation.VIEW, map);
        } catch (Exception e){
            logger.error("通过dubbo传送消息失败！\n" + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        logger.debug("sendStat in Sellinghouse cost time:{}", endTime - startTime);
    }
    
    private boolean isValidReqeust(HttpServletRequest request) {
        if (request == null) return false;
        String  userAgent  =   request.getHeader("User-Agent");
        logger.debug("userAgent:" + userAgent);
        if (userAgent != null) {
            if (userAgent.contains("PhantomJS")) {
                return false;
            }
        }
        
        return true;
    }
    
    private String getUserId(HttpServletRequest request) {
        /**
         * 如果用户登录，则返回用户真正的ID
         * 如果用户没有登录，首先看有没有canvas返回
         * 什么都有，就返回SessionID
         */
        
        HttpSession session = request.getSession();
        User user = (User)session.getAttribute("user");
        String uid = null;
        if (user == null) {
            // 获取canvasId
            Cookie[] cookies = request.getCookies();
            String canvasId = null;
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if(StringUtils.equals(cookie.getName(), "canvas")) {
                        canvasId = cookie.getValue();
                    }
                }
            }
            
            if (canvasId != null) {
                uid = canvasId;
            } else {
                uid = session.getId();
            }
            
        } else {
            uid = user.get_id();
        }
        
        return uid;
    }
    
    private User getLoginedUser(HttpServletRequest request) {
//        Cookie[] cookies = request.getCookies();
//        for (Cookie cookie : cookies) {
//            System.out.println("cookie name:" + cookie.getName() + ", value:" + cookie.getValue() );
//        }
        HttpSession session = request.getSession();
        System.out.println("session id:" + session.getId());
        User user = (User)session.getAttribute("user");
        return user;
    }

}
