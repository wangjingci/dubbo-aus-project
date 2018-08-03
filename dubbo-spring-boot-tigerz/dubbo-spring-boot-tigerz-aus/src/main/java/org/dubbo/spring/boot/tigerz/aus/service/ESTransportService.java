package org.dubbo.spring.boot.tigerz.aus.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.bson.Document;
import org.dubbo.spring.boot.tigerz.api.util.GsonUtil;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;
import org.dubbo.spring.boot.tigerz.aus.constant.RedisKey;
import org.dubbo.spring.boot.tigerz.aus.dto.FluzzySearchResponse;
import org.dubbo.spring.boot.tigerz.aus.entity.HouseListInfo;
import org.dubbo.spring.boot.tigerz.aus.entity.HouseListInfo.HouseSimpleInfo;
import org.dubbo.spring.boot.tigerz.aus.entity.School;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.util.Assert;


/**
 * 搜索服务
 * 为区域表，可售房源表等提供搜索服务。
 * <ul>
 * <li>智能联想功能</li>
 * <li>模糊查询功能</li>
 * <li>按照区域查询</li>
 * <li>按照地图查询</li>
 * </ul>
 * ESTransportService
 * @Desc: 这是描述
 * @Company: TigerZ
 * @author Wang Jingci
 * @date 2018年5月1日 下午9:48:47
 */

@Service
public class ESTransportService {
    
    private static final int POSTCODE_LENGTH = 4;
    public static final String INDEX_SELLING_HOUSE = "aus_index_sellinghouse";
    public static final String INDEX_RENTING_HOUSE = "aus_index_rentinghouse";
    public static final String INDEX_SOLD_HOUSE = "aus_index_soldhouse";
    public static final String INDEX_AREA = "aus_index_area";
    public static final String INDEX_SCHOOL = "aus_index_school";
    private static final String SCRIPT_SCORE_FILE = "WeightedSort";
    private static final Logger logger = LoggerFactory.getLogger(ESTransportService.class);
    
    private TransportClient client;
    private final int maxSizePerTime = 10000;  //最多搜索返回2000个对象
    private final HashSet<String> stateSet = new HashSet<String>();

    
    protected enum AreaLevel {
        COUNTRY,STATE,REGION,AREA,SUBURB,HOUSE;
    }
    
    
    
    public ESTransportService(){
        Settings settings = Settings.builder()
                .put("cluster.name", "tigerz-cluster").put("client.transport.sniff", true).build();
        
        client = new PreBuiltTransportClient(settings);
        try {
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("www.tigerz.nz"),9300)); //www.tigerz.com
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        
        stateSet.add("VIC");
        stateSet.add("NSW");
        stateSet.add("QLD");
        stateSet.add("SA");
        stateSet.add("WA");
        stateSet.add("ACT");
        stateSet.add("NT");
        stateSet.add("TAS");
        
        logger.info("ES service is ready");
    }
    
    /**
     * 搜索房源列表，只获取list信息，不处理地图信息
     * @param name
     * @param level
     * @param page
     * @param sort
     * @param filter
     * @return
     */
    public List<HouseListInfo> searchHouseListByAreaWithFilter(String cate, String name, int level,int page, String sort,Map<String,Object> filter ) {
        return doSearchHouseByAreaWithFilter(cate, name, level, page, sort, filter, true);
    }
    
    /**
     * 通过区域信息按照过滤条件，获得地图信息及房源列表信息
     * @param name
     * @param level
     * @param page
     * @param sort
     * @param filter 包括多个过滤条件，分为数字类型，范围类型, 是否类型</br>
     *          数字范围表示：bedroom: 1-3 ，或 2 或 1-any<br>
     *          范围类型表示：property: house, apartment, units</br>
     *          是否类型表示：isNew:false
     * @return
     */
    public List<HouseListInfo> searchHouseByAreaWithFilter(String cate,String name, int level,int page, String sort,Map<String,Object> filter ) {
        return doSearchHouseByAreaWithFilter(cate, name, level, page, sort, filter, false);
    }
    
    private List<HouseListInfo> doSearchHouseByAreaWithFilter(String cate, String name, int level,int page, String sort,Map<String,Object> filter, boolean listOnly ) {

        
        /**
         * 设计思路：
         * 返回的数据包括房源列表与地图圈列表。
         * 当传入的level是1,2,3的时候，代表用户想看区域视图
         *     此时优先查询area表，找到要显示的子区域构建mapInfo，呈现位置及房源数量
         *     然后在查到子区域中，逐个寻找里面的房源构建simpleHouseInfo，同时分页显示
         *     最后要查这个父区域获取的中心点坐标。这一步也可以考虑用房源最多的子区域替代啊？
         *     总结：一共查询了三次，考虑优化！
         * 当传入的level是4的时候，代表用户想看具体suburb下的房源
         *     查询selling表，找到房子的具体位置，构建mapInfo，同时构建houseInfo信息
         *     查询父区域获得中心点坐标
         *     总结：一共查询了2次，也考考虑优化一下！
         * 
         * 传入参数：
         * 1. 用户传来的level视为用户想看这个level下面的数据，比如是0，代表用户想看state层级数据
         * 2. 函数areaLeve=level+1,代表给用户看的信息是由什么元素组成，比如2,则代表由region数据组成
         */
        // 简单搜索做缓存
        if ((filter == null || filter.isEmpty())  && page < 5 && sort.equals("default")) {
            String keyWord = RedisKey.SEARCH_BY_AREA + "_" + name + "_" + level + "_" + page + "_" + listOnly  + "_" + cate;
            String redisValue = RedisUtils.getKeyAsString(keyWord);
            if(redisValue != null){
                List<HouseListInfo> list = GsonUtil.fromJsonToList(redisValue, HouseListInfo.class);
                return list;
            }
        }
        
        BoolQueryBuilder filterQuery = buildFilter(filter);
        
        int areaLevel = level +1;
        String fatherName = name;
        long countPerPage = 20;
        
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setMapLevel(areaLevel);
        String houseIndex = INDEX_SELLING_HOUSE;
        if (cate.equals("renting")) {
            houseIndex = "aus_index_rentinghouse";
        } else if (cate.equals("sold")) {
            houseIndex = "aus_index_soldhouse";
        }
        
        if(areaLevel == 1) {
            // 说明查询的是State行政区，地图的左侧应该呈现大的圆圈
            // 1. 装载左侧地图数据
            if (listOnly == false) {
                QueryBuilder faterNameQuery = QueryBuilders.termQuery("father_name.raw", fatherName); 
                QueryBuilder levelQuery =QueryBuilders.termQuery("level", areaLevel);
                QueryBuilder mapQuery = QueryBuilders.boolQuery().must(faterNameQuery).must(levelQuery);
                
                SearchResponse searchResponse = client.prepareSearch(INDEX_AREA)
                        .setQuery(mapQuery).setSize(maxSizePerTime).execute().actionGet();
                
                SearchHits hits = searchResponse.getHits(); 
                houseListInfo.setMapListFromSearchHits(hits, areaLevel);
            }
            
            // 2. 装载右侧list数据
            QueryBuilder fieldQuery = QueryBuilders.matchAllQuery();
            QueryBuilder listQuery = filterQuery.must(fieldQuery);

            SearchRequestBuilder searchRequestBuilder = client.prepareSearch(houseIndex)   
                    .setFrom(page*(int)countPerPage).setSize((int)countPerPage);
            
            addSorter(searchRequestBuilder,listQuery,sort,cate);
            
            SearchResponse searchResponseList = searchRequestBuilder.execute().actionGet();
            
            SearchHits listHits = searchResponseList.getHits(); 
            houseListInfo.setHouseListFromSearchHits(listHits,0,countPerPage);
            
            long totalProp = listHits.getTotalHits();
            houseListInfo.setPropNum(totalProp);
            houseListInfo.setCurPage(page);
            houseListInfo.setMaxPage(totalProp/countPerPage);
            houseListInfo.setFatherName(fatherName);
            
        } else {
            // 说明关键字是非State行政区，用户应该看到房源数据，要到Sellinghouse里面查询
            
            // 1. 直接查找房源数据，最多1000
            String field = "suburb.raw";
            if(level == 1){
                field = "state.raw";
            }else if(level == 2){
                // TODO region域还没在数据库里，也没在搜索里
                field = "region.raw";
            }else if(level == 3){
             // TODO area域还没在数据库里，也没在搜索里
                field = "area.raw";
            }else if(level == 4){
                field = "suburb.raw";
            }
            
            QueryBuilder localityQuery = QueryBuilders.termQuery(field, fatherName);
            
            QueryBuilder mapQuery = filterQuery.must(localityQuery);
            int maxHouseCount = 1000;
            
            SearchRequestBuilder searchRequestBuilder = client.prepareSearch(houseIndex)   
                    .setFrom(0).setSize(maxHouseCount);
            
            addSorter(searchRequestBuilder,mapQuery,sort,cate);
            
            SearchResponse searchResponseList = searchRequestBuilder.execute().actionGet();

            SearchHits hits = searchResponseList.getHits(); 
            
            // 2. 装载左侧Map数据，所有的返回都装载进去
            if (listOnly == false) {
                houseListInfo.setMapListFromSearchHits(hits, areaLevel);
            }
            
            // 3. 装载右侧List数据，只装载一页
            houseListInfo.setHouseListFromSearchHits(hits,page,countPerPage);
            
            long totalProp = hits.getTotalHits();
            houseListInfo.setPropNum(totalProp);
            houseListInfo.setCurPage(page);
            long minProp = (totalProp > maxHouseCount) ? maxHouseCount:totalProp;
            houseListInfo.setMaxPage(minProp/countPerPage);
            houseListInfo.setFatherName(fatherName);
            
        }
        
        // 在父区域表里获得中心点坐标,区域中位价等信息
        QueryBuilder NameQuery =QueryBuilders.termQuery("name.raw", name);
        QueryBuilder levelQuery =QueryBuilders.termQuery("level", level);
        QueryBuilder nameBoolQuery = QueryBuilders.boolQuery().must(NameQuery).must(levelQuery);
        
        SearchResponse searchResponseByName = client.prepareSearch(INDEX_AREA)
                .setQuery(nameBoolQuery).execute().actionGet();
        SearchHits namehits = searchResponseByName.getHits(); 
        for (SearchHit hit : namehits.getHits())
        {
            Map<String, Object> result = hit.getSource();
            @SuppressWarnings("unchecked")
            ArrayList<Double> point = (ArrayList<Double>)result.get("base_point");
            houseListInfo.setBasePoint(point);
            houseListInfo.setFatherName((String)result.get("father_name"));
            houseListInfo.setAreaMidPrice((Double)result.get("valuer_median"));
        }
        houseListInfo.setSearchName(name);

        // 只有一个数据，但为了保持返回数据一致性，保证成数组
        ArrayList<HouseListInfo> list = new ArrayList<HouseListInfo>();
        list.add(houseListInfo);
        
        if ((filter == null || filter.isEmpty()) && page < 5 && sort.equals("default")) {
            String keyWord = RedisKey.SEARCH_BY_AREA + "_" + name + "_" + level + "_" + page + "_" + listOnly  + "_" + cate;
            RedisUtils.setex(keyWord, GsonUtil.toJson(list), 60*60*3);
        }
        
        return list;
        
    }
    
    /**
     * 通过区域信息获得地图信息及部分房源列表信息
     * @deprecated 仅适用于老接口，新接口请使用 @see {@link #searchHouseByAreaWithFilter(String, int, int, String, Map)}
     * @param name
     * @param level
     * 具体的level country:0,stat:1, region:2, area:3, suburb:4, house:5
     * @param page
     * @param sort
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<HouseListInfo> searchHouseByArea(String scope, String name, int level,int page, String sort,Map<String,Boolean> bedroomFilter ){ 
        /**
         * 设计思路：
         * 返回的数据包括房源列表与地图圈列表。
         * 当传入的level是1,2,3的时候，代表用户想看区域视图
         *     此时优先查询area表，找到要显示的子区域构建mapInfo，呈现位置及房源数量
         *     然后在查到子区域中，逐个寻找里面的房源构建simpleHouseInfo，同时分页显示
         *     最后要查这个父区域获取的中心点坐标。这一步也可以考虑用房源最多的子区域替代啊？
         *     总结：一共查询了三次，考虑优化！
         * 当传入的level是4的时候，代表用户想看具体suburb下的房源
         *     查询selling表，找到房子的具体位置，构建mapInfo，同时构建houseInfo信息
         *     查询父区域获得中心点坐标
         *     总结：一共查询了2次，也考考虑优化一下！
         * 
         * 传入参数：
         * 1. 用户传来的level视为用户想看这个level下面的数据，比如是0，代表用户想看state层级数据
         * 2. 函数areaLeve=level+1,代表给用户看的信息是由什么元素组成，比如2,则代表由region数据组成
         */
        
        Boolean allScope = false;
        if(scope.equals("all")){
            allScope = true;
        }
        int areaLevel = level +1;
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setMapLevel(areaLevel);
        
        //bedroom过滤条件查询
        ArrayList<Integer> bedroom = new ArrayList<Integer>();
        Boolean bedroomIsAll = true;
        if(bedroomFilter != null){
            bedroomIsAll = bedroomFilter.get("all");
        }
        if(!bedroomIsAll){
            if(bedroomFilter.get("one")){
                bedroom.add(1);
            }
            if(bedroomFilter.get("two")){
                bedroom.add(2);
            }
            if(bedroomFilter.get("three")){
                bedroom.add(3);
            }
            if(bedroomFilter.get("four")){
                bedroom.add(4);
            }
            if(bedroomFilter.get("more")){
                bedroom.add(5);
                bedroom.add(6);
                bedroom.add(7);
                bedroom.add(8);
                bedroom.add(9);
                bedroom.add(10);
            }
        }
        
        //获取地图信息
        String fatherName = name;
        QueryBuilder mapQuery = null;
        String mapIndexName = null;
        if(areaLevel < 5){
            // 说明查询的关键字是area及以上的行政区， 所以到区域表里面查询区域数据构建MapInfo 
            QueryBuilder faterNameQuery = QueryBuilders.termQuery("father_name.raw", fatherName); 
            QueryBuilder levelQuery =QueryBuilders.termQuery("level", areaLevel);
            mapQuery = QueryBuilders.boolQuery().must(faterNameQuery).must(levelQuery);
            
            mapIndexName = INDEX_AREA;
        } else {
            // 说明关键字是suburb，需要到Sellinghouse里面查询数据，构建MapInfo与HouseListInfo
            QueryBuilder localityQuery = QueryBuilders.termQuery("suburb.raw", fatherName);
            
            if(bedroomIsAll){
                //如果bedroom没有做过滤，则不用加入这个搜索条件
                mapQuery = QueryBuilders.boolQuery().must(localityQuery);
            }else{
                //r如果bedroom做了限制，查询的时候也要做限制
                QueryBuilder bedroomQuery =QueryBuilders.termsQuery("beds", bedroom);
                mapQuery = QueryBuilders.boolQuery().must(localityQuery).must(bedroomQuery);
            }
            mapIndexName = INDEX_SELLING_HOUSE;
        }
        SearchResponse searchResponse = client.prepareSearch(mapIndexName)
                                                .setQuery(mapQuery).setSize(maxSizePerTime).execute().actionGet();
        
        SearchHits hits = searchResponse.getHits(); 
        houseListInfo.setMapListFromSearchHits(hits, areaLevel);
        
        // 在父区域表里获得中心点坐标,区域中位价等信息
        QueryBuilder nameBoolQuery = null;
        if(allScope){
            QueryBuilder NameQuery =QueryBuilders.termQuery("name.raw", name);
            QueryBuilder levelQuery =QueryBuilders.termQuery("level", level);
            nameBoolQuery = QueryBuilders.boolQuery().must(NameQuery).must(levelQuery);
        }else{
            QueryBuilder NameQuery =QueryBuilders.termQuery("name.raw", name);
            QueryBuilder levelQuery =QueryBuilders.termQuery("level", level);
            QueryBuilder areaQuery =QueryBuilders.termQuery("region_name", scope);
            nameBoolQuery = QueryBuilders.boolQuery().must(NameQuery).must(levelQuery).must(areaQuery);
        }

        
        SearchResponse searchResponseByName = client.prepareSearch(INDEX_AREA)
                .setQuery(nameBoolQuery).execute().actionGet();
        SearchHits namehits = searchResponseByName.getHits(); 
        for (SearchHit hit : namehits.getHits())
        {
            Map<String, Object> result = hit.getSource();
            ArrayList<Double> point = (ArrayList<Double>)result.get("base_point");
            houseListInfo.setBasePoint(point);
            houseListInfo.setFatherName((String)result.get("father_name"));
            houseListInfo.setAreaMidPrice((Double)result.get("valuer_median"));
        }
        houseListInfo.setSearchName(name);

        /**
         * 根据搜索的行政区，查找下面的房子构建SimpleHouseInfo，如果level是0则匹配所有房子
         */
        
        long countPerPage = 20;
        String field = null;
        // 如果level是0则匹配所有，但限制最多1000套
        if(level == 1){
            field = "state.raw";
        }else if(level == 2){
            // TODO region域还没在数据库里，也没在搜索里
            field = "region.raw";
        }else if(level == 3){
         // TODO area域还没在数据库里，也没在搜索里
            field = "area.raw";
        }else if(level == 4){
            field = "suburb.raw";
        }
        
        QueryBuilder listQuery = null;
        QueryBuilder fieldQuery = null;
        if (level == 0) {
            fieldQuery = QueryBuilders.matchAllQuery();
        } else {
            fieldQuery =QueryBuilders.termQuery(field, name);
        }

        if(bedroomIsAll){
            //如果bedroom没有做过滤，则不用加入这个搜索条件
            listQuery = QueryBuilders.boolQuery().must(fieldQuery);
        } else {
            //r如果bedroom做了限制，查询的时候也要做限制
            QueryBuilder bedroomQuery =QueryBuilders.termsQuery("beds", bedroom);
            listQuery = QueryBuilders.boolQuery().must(fieldQuery).must(bedroomQuery);
        }
        
        //TODO from+size据说要小于1W，这里有空需要处理换成scroller
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX_SELLING_HOUSE)   
                .setQuery(listQuery)
                .setFrom(page*(int)countPerPage).setSize((int)countPerPage);
        
        SortBuilder<?> sortor = buildSorter(sort);
        
        if (sortor != null) {
            searchRequestBuilder.setQuery(listQuery);
            searchRequestBuilder.addSort(sortor);
        } else {
            // Default 排序采用脚本方法干预分数，比如有主图，有basepoint等等，脚本在config/scripts下面
            Map<String, Object> params = new HashMap<>();  
            Script script = new Script(ScriptType.FILE,"painless",SCRIPT_SCORE_FILE, params);
            ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script); 
            searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(listQuery,scriptBuilder));
        }
        
        SearchResponse searchResponseList = searchRequestBuilder.execute().actionGet();
        
        
        
        SearchHits listHits = searchResponseList.getHits(); 
        houseListInfo.setHouseListFromSearchHits(listHits);
       
        long totalProp = listHits.getTotalHits();
        houseListInfo.setPropNum(totalProp);
        houseListInfo.setCurPage(page);
        houseListInfo.setMaxPage(totalProp/countPerPage);
        

        // 只有一个数据，但为了保持返回数据一致性，保证成数组
        ArrayList<HouseListInfo> list = new ArrayList<HouseListInfo>();
        list.add(houseListInfo);
        
        return list;
    }
    
    /**
     * 通过地图以及过滤条件查找房源
     * @param zoom
     * @param points
     * @param page
     * @param sort
     * @param filter
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public List<HouseListInfo> searchHouseByMapWithFilter(String cate, int zoom, ArrayList<Double> points,int page, String sort, Map<String,Object> filter) throws IOException{
        
        BoolQueryBuilder filterQuery = buildFilter(filter);
        
        long countPerPage = 20;
        String mapIndexName = INDEX_AREA;
        String listIndexName = INDEX_SELLING_HOUSE;
        if (cate.equals("renting")) {
            listIndexName = INDEX_RENTING_HOUSE;
        } else if (cate.equals("sold")) {
            listIndexName = INDEX_SOLD_HOUSE;
        }
        int areaLevel = 1;
        areaLevel = getAreaLevelFromZoom(zoom);
        
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setMapLevel(areaLevel);
        
        // 如果是state区域，就左侧显示圆圈，右侧显示列表
        if(areaLevel == 0 ||areaLevel == 1){
            //通过area表去搜索,搜到的内容是区域
            //注意坐标，景多默认传过来的不是top-left -》 botom-right
            QueryBuilder geoQuery = QueryBuilders.geoBoundingBoxQuery("base_point").setCorners(points.get(1), points.get(2), points.get(3), points.get(0));
            QueryBuilder levelQuery = QueryBuilders.termQuery("level", areaLevel);
            QueryBuilder mapQuery = QueryBuilders.boolQuery().must(geoQuery).must(levelQuery);
            SearchResponse searchResponse = client.prepareSearch(mapIndexName)
                    .setQuery(mapQuery).setSize(maxSizePerTime).execute().actionGet();
            
            SearchHits hits = searchResponse.getHits(); 
            
            int mapSize = houseListInfo.setMapListFromSearchHits(hits, areaLevel);
            
            
            //处理返回数据，拼出map数据信息
            String areaFieldName =  "";
            if(areaLevel == 1){
                areaFieldName = "state.raw";
            }else if(areaLevel == 2){
                areaFieldName = "region.raw";
            }else if(areaLevel == 3){
                areaFieldName = "area.raw";
            }else if(areaLevel == 4){
                areaFieldName = "suburb.raw";
            }
            
            if(mapSize > 0){
                BoolQueryBuilder listQuery = filterQuery;
                ArrayList<String> areas = new ArrayList<String>();
                for(HouseListInfo.MapInfoForArea mapInfo :(LinkedList<HouseListInfo.MapInfoForArea>)houseListInfo.getMapInfo()){
                    String name = mapInfo.getKeyword();
                    areas.add(name);
                }
                QueryBuilder termsQuery = QueryBuilders.termsQuery(areaFieldName, areas);
                listQuery.must(termsQuery);
                
                SearchRequestBuilder searchRequestBuilder = client.prepareSearch(listIndexName)   
                        .setFrom(page*(int)countPerPage)
                        .setSize((int)countPerPage);
                
                addSorter(searchRequestBuilder,listQuery,sort,cate);
                
                SearchResponse searchResponse2 = searchRequestBuilder.execute().actionGet();
                
                SearchHits listHits = searchResponse2.getHits(); 
                houseListInfo.setHouseListFromSearchHits(listHits);
                
                long totalProp = listHits.getTotalHits();
                houseListInfo.setPropNum(totalProp);
                houseListInfo.setCurPage(page);
                houseListInfo.setMaxPage(totalProp/countPerPage);
            }else{
                houseListInfo.setPropNum(0);
                houseListInfo.setCurPage(0);
                houseListInfo.setMaxPage(0);
            }


        } else {
            //通过sellinghouse去搜索,搜到的内容是房子
            QueryBuilder geoQuery = QueryBuilders.geoBoundingBoxQuery("base_point").setCorners(points.get(1), points.get(2), points.get(3), points.get(0));
            QueryBuilder mapQuery = filterQuery.must(geoQuery);
            
            int maxPerTime = 1000;
            SearchRequestBuilder searchRequestBuilder = client.prepareSearch(listIndexName); 
            searchRequestBuilder.setSize(maxPerTime);
            
            addSorter(searchRequestBuilder,mapQuery,sort,cate);
            
            SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
            SearchHits hits = searchResponse.getHits(); 
            houseListInfo.setMapListFromSearchHits(hits, areaLevel);
            
            houseListInfo.setHouseListFromSearchHits(hits,page,countPerPage);
            long totalProp = hits.getTotalHits();
            houseListInfo.setPropNum(totalProp);
            houseListInfo.setCurPage(page);
            long minProp = (totalProp < maxPerTime) ? totalProp:maxPerTime;
            houseListInfo.setMaxPage(minProp/countPerPage);
        }
        

        
        //只有一个数据，但为了保持返回数据一致性，保证成数组
        ArrayList<HouseListInfo> list = new ArrayList<HouseListInfo>();
        list.add(houseListInfo);
        return list;
    }
    
    /**
     * 通过地图搜索房源
     * @param zoom
     * @param points
     * @param page
     * @param sort
     * @param bedroomFilter
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public List<HouseListInfo> searchHouseByMap(int zoom, ArrayList<Double> points,int page, String sort, Map<String,Boolean> bedroomFilter) throws IOException{
        //地图搜索，目前不限制奥克兰区域
        //String limitedArea = "Auckland";
        long countPerPage = 20;
        String mapIndexName = INDEX_AREA;
        String listIndexName = INDEX_SELLING_HOUSE;
        int areaLevel = 1;
        areaLevel = getAreaLevelFromZoom(zoom);
        
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setMapLevel(areaLevel);
        
        //bedroom过滤条件查询
        ArrayList<Integer> bedroom = new ArrayList<Integer>();
        Boolean bedroomIsAll = true;
        if(bedroomFilter != null){
            bedroomIsAll = bedroomFilter.get("all");
        }
        if(!bedroomIsAll){
            if(bedroomFilter.get("one")){
                bedroom.add(1);
            }
            if(bedroomFilter.get("two")){
                bedroom.add(2);
            }
            if(bedroomFilter.get("three")){
                bedroom.add(3);
            }
            if(bedroomFilter.get("four")){
                bedroom.add(4);
            }
            if(bedroomFilter.get("more")){
                bedroom.add(5);
                bedroom.add(6);
                bedroom.add(7);
                bedroom.add(8);
                bedroom.add(9);
                bedroom.add(10);
            }
        }
        
        if(areaLevel == 0 ||areaLevel == 1 || areaLevel == 2 ||areaLevel == 3 || areaLevel == 4){
            //通过area表去搜索,搜到的内容是区域
            //注意坐标，景多默认传过来的不是top-left -》 botom-right ，
            QueryBuilder geoQuery = QueryBuilders.geoBoundingBoxQuery("base_point").setCorners(points.get(1), points.get(2), points.get(3), points.get(0));
            QueryBuilder levelQuery = QueryBuilders.termQuery("level", areaLevel);
            //QueryBuilder areaQuery =QueryBuilders.termQuery("region_name", limitedArea);
            QueryBuilder mapQuery = QueryBuilders.boolQuery().must(geoQuery).must(levelQuery);
            SearchResponse searchResponse = client.prepareSearch(mapIndexName)
                    .setQuery(mapQuery).setSize(maxSizePerTime).execute().actionGet();
            
            SearchHits hits = searchResponse.getHits(); 
            
            int mapSize = houseListInfo.setMapListFromSearchHits(hits, areaLevel);
            
            //处理返回数据，拼出map数据信息
            String areaFieldName =  "";
            if(areaLevel == 1){
                areaFieldName = "state.raw";
            }else if(areaLevel == 2){
                areaFieldName = "region.raw";
            }else if(areaLevel == 3){
                areaFieldName = "area.raw";
            }else if(areaLevel == 4){
                areaFieldName = "suburb.raw";
            }
            
            if(mapSize > 0){
                BoolQueryBuilder listQuery = QueryBuilders.boolQuery();
                for(HouseListInfo.MapInfoForArea mapInfo :(LinkedList<HouseListInfo.MapInfoForArea>)houseListInfo.getMapInfo()){
                    String name = mapInfo.getKeyword();
                    QueryBuilder termQuery = QueryBuilders.termQuery(areaFieldName, name);
                    listQuery.should(termQuery);
                }
                //QueryBuilder areaListQuery =QueryBuilders.termQuery("address_region.raw", limitedArea);
                //listQuery.must(areaListQuery);
                listQuery.minimumNumberShouldMatch(1); //这句话必不可少，否则should条件就会成为可有可无
                //logger.info(listQuery.toString());
                
                if(!bedroomIsAll){
                    QueryBuilder bedroomQuery =QueryBuilders.termsQuery("beds", bedroom);
                    listQuery = listQuery.must(bedroomQuery);
                }
                
                SearchRequestBuilder searchRequestBuilder = client.prepareSearch(listIndexName)   
                        .setFrom(page*(int)countPerPage)
                        .setSize((int)countPerPage);
                
                SortBuilder<?> sortor = buildSorter(sort);
                if (sortor != null) {
                    searchRequestBuilder.setQuery(listQuery);
                    searchRequestBuilder.addSort(sortor);
                } else {
                    // Default 排序采用脚本方法干预分数，比如有主图，有basepoint等等，脚本在config/scripts下面
                    Map<String, Object> params = new HashMap<>();  
                    Script script = new Script(ScriptType.FILE,"painless",SCRIPT_SCORE_FILE, params);
                    ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script); 
                    searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(listQuery,scriptBuilder));
                }
                
                SearchResponse searchResponse2 = searchRequestBuilder.execute().actionGet();
                
                SearchHits listHits = searchResponse2.getHits(); 
                houseListInfo.setHouseListFromSearchHits(listHits);
                
                long totalProp = listHits.getTotalHits();
                houseListInfo.setPropNum(totalProp);
                houseListInfo.setCurPage(page);
                houseListInfo.setMaxPage(totalProp/countPerPage);
            }else{
                houseListInfo.setPropNum(0);
                houseListInfo.setCurPage(0);
                houseListInfo.setMaxPage(0);
            }


        } else {
            //通过sellinghouse去搜索,搜到的内容是房子
            //TODO 为了实现一次搜索，这时候就需要直接排序了
            QueryBuilder geoQuery = QueryBuilders.geoBoundingBoxQuery("base_point").setCorners(points.get(1), points.get(2), points.get(3), points.get(0));
            //QueryBuilder areaQuery =QueryBuilders.termQuery("address_region.raw", limitedArea);
            QueryBuilder mapQuery = null;
            if(bedroomIsAll){
                //如果bedroom没有做过滤，则不用加入这个搜索条件
                mapQuery = QueryBuilders.boolQuery().must(geoQuery);
            }else{
                //r如果bedroom做了限制，查询的时候也要做限制
                QueryBuilder bedroomQuery =QueryBuilders.termsQuery("beds", bedroom);
                mapQuery = QueryBuilders.boolQuery().must(geoQuery).must(bedroomQuery);
            }
            
            SearchRequestBuilder searchRequestBuilder = client.prepareSearch(listIndexName)   
                    .setFrom(page*(int)countPerPage)
                    .setSize((int)countPerPage);
            
            SortBuilder<?> sortor = buildSorter(sort);
            if (sortor != null) {
                searchRequestBuilder.setQuery(mapQuery);
                searchRequestBuilder.addSort(sortor);
            } else {
                // Default 排序采用脚本方法干预分数，比如有主图，有basepoint等等，脚本在config/scripts下面
                Map<String, Object> params = new HashMap<>();  
                Script script = new Script(ScriptType.FILE,"painless",SCRIPT_SCORE_FILE, params);
                ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script); 
                searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(mapQuery,scriptBuilder));
            }
            
            SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
            
            SearchHits hits = searchResponse.getHits(); 
            
            int mapSize = houseListInfo.setMapListFromSearchHits(hits, areaLevel);
            
            System.out.println("the map info:"+mapSize);
            
            
            //处理List数据
            SearchHit[] listHits = hits.getHits();
            int pageFrom = page*(int)countPerPage;
            int pageSize = (int)countPerPage;
            int size = listHits.length;
            if(pageFrom+pageSize < size){
                size = pageFrom+pageSize ;
            } 
            houseListInfo.setHouseListFromSearchHits(hits);
            long totalProp = hits.getTotalHits();
            houseListInfo.setPropNum(totalProp);
            houseListInfo.setCurPage(page);
            houseListInfo.setMaxPage(totalProp/countPerPage);
        }
        
        
        //只有一个数据，但为了保持返回数据一致性，保证成数组
        ArrayList<HouseListInfo> list = new ArrayList<HouseListInfo>();
        list.add(houseListInfo);
        return list;
    }
    
    
    public List<School> searchSchoolByMapWithFilter(ArrayList<Double> points,String sort, Map<String,Object> filter) {
        String schoolIndex = "aus_index_school";
        // 学校过滤
        // 1. 办学类型type，包括公立，私立，宗教，其他-> Public, Private, Charter
        // 2. 年级类型level, 包括幼儿园，小学，中学，高中-> Elementary,Middle,High
        // "level":["Elementary","Middle","High"]
        // "type":["Public","Private",Charter]
        QueryBuilder geoQuery = QueryBuilders.geoBoundingBoxQuery("base_point").setCorners(points.get(1), points.get(2), points.get(3), points.get(0));
        BoolQueryBuilder mapQuery = QueryBuilders.boolQuery().must(geoQuery);
        
        if (filter != null && filter.size() > 0) {
            if (filter.get("level") != null) {
                Set<String> keywords = new HashSet<>();
                @SuppressWarnings("unchecked")
                List<String> levels = (List<String>)filter.get("level");
                if (levels.contains("Elementary")) {
                    keywords.add("Primary");
                    keywords.add("Combined");
                    keywords.add("K-12");
                    keywords.add("Primary, Secondary");
                }
                if (levels.contains("Middle")) {
                    keywords.add("Secondary");
                    keywords.add("Combined");
                    keywords.add("K-12");
                    keywords.add("Primary, Secondary");
                }
                if (levels.contains("High")) {
                    keywords.add("K-12");
                }
                
                QueryBuilder levelQuery = QueryBuilders.termsQuery("type", keywords);
                mapQuery.must(levelQuery);
            }
            
            if (filter.get("type") != null) {
                Set<String> keywords = new HashSet<>();
                @SuppressWarnings("unchecked")
                List<String> types = (List<String>)filter.get("type");
                if (types.contains("Public")) {
                    keywords.add("Government");
                }
                if (types.contains("Private")) {
                    keywords.add("Private");
                }
                if (types.contains("Charter")) {
                    keywords.add("Catholic");
                    keywords.add("Christian");
                }
                QueryBuilder typeQuery = QueryBuilders.termsQuery("sector", keywords);
                mapQuery.must(typeQuery);
            }
        }
        
        int maxSchoolCount = 5000;
        SearchResponse searchResponse = client.prepareSearch(schoolIndex)
                .setQuery(mapQuery).setSize(maxSchoolCount).execute().actionGet();
        
        SearchHits listHits = searchResponse.getHits(); 
        List<School> listSchool =  new LinkedList<>();
        for (SearchHit searchHit : listHits) {
            School school = new School();
            school.initFromSearchHit(searchHit);
            listSchool.add(school);
        }

        
        return listSchool;
    }   
    
    @SuppressWarnings("unchecked")
    public List<HouseListInfo> searchSchoolHouseById(String id) {
        
        String keyWord = RedisKey.SEARCH_SCHOOL_HOUSE_BY_ID + "_" + id;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return GsonUtil.fromJson(redisValue, List.class);
        }
        
        HouseListInfo houseListInfo = new HouseListInfo();
        
        List<School> schoolList = searchSchool(id);
        if (schoolList == null || schoolList.isEmpty()) {
            logger.error("搜索不到相应的学校,id是"+id);
            return null;
        }
        School school = schoolList.get(0);
        houseListInfo.setSchoolList(schoolList);
        // 看看返回来的是否是这个数据
        ArrayList<ArrayList<Double>> polygon = school.getPolygon();
        ArrayList<Double> basePoint = school.getBasePoint();
        LinkedList<HouseSimpleInfo> houseList = null;
        if (polygon != null && polygon.size() >= 3) {
            // 获取轮廓内的房子
            houseList = searchSchoolHouseByPolygon(polygon);
            
        } else {
            // 获取以basepoint，找5公里以内的房子
            houseList = searchSchoolHouseByCenterPoint(basePoint);
        }
        
        
        houseListInfo.setHouseInfo(houseList);
        houseListInfo.setMapInfo(houseList);
        
        houseListInfo.setBasePoint(basePoint);
        houseListInfo.setMapLevel(AreaLevel.SUBURB.ordinal());
        houseListInfo.setCurPage(0);
        houseListInfo.setMaxPage(1);
        houseListInfo.setPropNum(houseList != null?houseList.size() : 0);
        ArrayList<HouseListInfo> list = new ArrayList<>();
        list.add(houseListInfo);
        if (houseListInfo != null) {
            RedisUtils.setex(keyWord, GsonUtil.toJson(list), 60*60*6);
        }
        
        
        return list;
    }
    
    private List<School> searchSchool(String id) {
        QueryBuilder query = QueryBuilders.termQuery("_id", id);
        SearchResponse response = client.prepareSearch("aus_index_school").setTypes("aus_type_school")
                    .setQuery(query).execute().actionGet();
        
        SearchHits listHits = response.getHits(); 
        List<School> listSchool =  new LinkedList<>();
        for (SearchHit searchHit : listHits) {
            School school = new School();
            school.initFromSearchHit(searchHit);
            listSchool.add(school);
        }
        
        return listSchool;
    }
    
    private LinkedList<HouseSimpleInfo> searchSchoolHouseByPolygon(List<ArrayList<Double>> polygon) {
        long startTime = System.currentTimeMillis();
        
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // 两种方法，一个是简单的轮廓查询，一个是过滤掉大范围之外的点在查询，一会比较一下效果
        //QueryBuilders.rangeQuery("").from(from);
        
        List<GeoPoint> geoPointList = new ArrayList<>();
        for (ArrayList<Double> point : polygon) {
            GeoPoint geoPoint = new GeoPoint(point.get(1),point.get(0));
            geoPointList.add(geoPoint);
        }
        QueryBuilder polygonQuery = QueryBuilders.geoPolygonQuery("base_point", geoPointList);
        boolQuery.must(polygonQuery);
        SearchResponse searchResponseList = client.prepareSearch("aus_index_sellinghouse").setTypes("aus_type_sellinghouse").setQuery(boolQuery).setSize(maxSizePerTime).execute().actionGet();
        
        SearchHits listHits = searchResponseList.getHits(); 
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setHouseListFromSearchHits(listHits);
        
        long endTime = System.currentTimeMillis();
        System.out.println("searchSchoolHouseByPolygon cost:" + (endTime - startTime));
        return houseListInfo.getHouseInfo();
    }
    
    private LinkedList<HouseSimpleInfo> searchSchoolHouseByCenterPoint(ArrayList<Double> basePoint) {
        String distance = "1000";
        GeoPoint geoPoint = new GeoPoint(basePoint.get(1),basePoint.get(0));
        QueryBuilder distanceQuery = QueryBuilders.geoDistanceQuery("base_point").point(geoPoint).distance(distance, org.elasticsearch.common.unit.DistanceUnit.METERS);
        //
        //QueryBuilder distanceQuery = QueryBuilders.geoDistanceRangeQuery("base_point", geoPoint).to(distance).unit(org.elasticsearch.common.unit.DistanceUnit.METERS);;
        //QueryBuilder distanceQuery = QueryBuilders.geoDistanceRangeQuery("base_point", basePoint.get(1), basePoint.get(0)).to(distance).unit(org.elasticsearch.common.unit.DistanceUnit.METERS);
        SearchResponse searchResponseList = client.prepareSearch("aus_index_sellinghouse").setTypes("aus_type_sellinghouse").setQuery(distanceQuery).setSize(maxSizePerTime).execute().actionGet();
        
        SearchHits listHits = searchResponseList.getHits(); 
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setHouseListFromSearchHits(listHits);
        
        return houseListInfo.getHouseInfo();
    }
    
    public List<HouseSimpleInfo> searchSellingHouseByPointAndPrice(ArrayList<Double> centerPoint,int price, int recommendCount) {
        if (price < 0 || recommendCount < 0) {
            return null;
        }
        GeoPoint geoPoint = new GeoPoint(centerPoint.get(1),centerPoint.get(0));
        String distanceLimit = "5"; // 只考虑5公里以内的房子
        QueryBuilder distanceQuery = QueryBuilders.geoDistanceQuery("base_point").point(geoPoint).distance(distanceLimit, org.elasticsearch.common.unit.DistanceUnit.KILOMETERS);
        // offerset 是1，即1公里以内不考虑距离， 1 + 2 （sale） 公里以内可以考虑买，再远就意愿快速下降
        GaussDecayFunctionBuilder distanceDecay = ScoreFunctionBuilders.gaussDecayFunction("base_point", geoPoint, "2km", "1km");
        // offset 是price * 0.05 即5%价格浮动无所谓， scale是 price * 0.1 即超过price * 0.15 就不考虑了
        GaussDecayFunctionBuilder priceDecay = ScoreFunctionBuilders.gaussDecayFunction("house_price", price, price*0.05, price*0.05);
        
        FilterFunctionBuilder[] filterFunctionBuilders = {new FunctionScoreQueryBuilder.FilterFunctionBuilder(distanceDecay), 
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(priceDecay)
        };
        SearchResponse searchResponseList = client.prepareSearch("aus_index_sellinghouse").setTypes("aus_type_sellinghouse").setQuery(QueryBuilders.functionScoreQuery(distanceQuery,filterFunctionBuilders).boostMode(org.elasticsearch.common.lucene.search.function.CombineFunction.SUM)).setSize(recommendCount).execute().actionGet();
        
        SearchHits listHits = searchResponseList.getHits(); 
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setHouseListFromSearchHits(listHits);
        
        return houseListInfo.getHouseInfo();
    }
    
    /**
     * 智能联想. 给出相应提示
     * @param content
     * @param cate 分类，包括selling，renting，sold，默认是selling
     * @return
     */
    public List<FluzzySearchResponse> searchHintInFuzzy(String content, String cate){
        
        content = content.trim();
        
        MultiSearchResponse sr = null;
        
        if (content.length() == POSTCODE_LENGTH && StringUtils.isNumeric(content)) {
            //=如果是PostCode，直接在区域表里查
            QueryBuilder postCodeQuery = QueryBuilders.termQuery("post_code", content); 
            QueryBuilder levelQuery =QueryBuilders.termQuery("level", AreaLevel.SUBURB.ordinal());
            QueryBuilder query = QueryBuilders.boolQuery().must(levelQuery).must(postCodeQuery);
            
            SearchRequestBuilder  response = client.prepareSearch(INDEX_AREA)   
                    .setQuery(query);
            sr = client.prepareMultiSearch()
                    .add(response)
                    .get();
            
        } else if (content.length() < POSTCODE_LENGTH && StringUtils.isNumeric(content)) {
            //=如果是纯数字，依然是suburb优先，从postcode里寻找匹配
            QueryBuilder levelQuery =QueryBuilders.termQuery("level", AreaLevel.SUBURB.ordinal());
            QueryBuilder postCodeQuery = QueryBuilders.wildcardQuery("post_code", content + "*");
            QueryBuilder query = QueryBuilders.boolQuery().must(levelQuery).must(postCodeQuery);
            
            SearchRequestBuilder  response = client.prepareSearch(INDEX_AREA)   
                    .setQuery(query)
                    .setFrom(0).setSize(7);
            
            QueryBuilder addressQuery = QueryBuilders.matchPhrasePrefixQuery("address", content);
            SearchRequestBuilder response2 = client.prepareSearch(INDEX_SELLING_HOUSE)   
                    .setQuery(addressQuery)
                    .setFrom(0).setSize(7);

            sr = client.prepareMultiSearch()
                    .add(response)
                    .add(response2)
                    .get();
            
        } else {
            QueryBuilder areaQuery = QueryBuilders.matchPhrasePrefixQuery("name", content);
            FieldValueFactorFunctionBuilder levelField = ScoreFunctionBuilders.fieldValueFactorFunction("level").modifier(FieldValueFactorFunction.Modifier.LN1P).factor(10);
            
            SearchRequestBuilder  response = client.prepareSearch(INDEX_AREA)   
                    .setQuery(QueryBuilders.functionScoreQuery(areaQuery, levelField))
                    .setFrom(0).setSize(7);
            
            String indexHouse = INDEX_SELLING_HOUSE;
            if (cate.equals("renting")) {
                indexHouse = "aus_index_rentinghouse";
            } else if (cate.equals("sold")) {
                indexHouse = "aus_index_soldhouse";
            }
            QueryBuilder addressQuery = QueryBuilders.matchPhrasePrefixQuery("address", content);
            SearchRequestBuilder response2 = client.prepareSearch(indexHouse)   
                    .setQuery(addressQuery)
                    .setFrom(0).setSize(7);
            
            QueryBuilder schoolQuery = QueryBuilders.matchPhrasePrefixQuery("school_name", content);
            SearchRequestBuilder response3 = client.prepareSearch(INDEX_SCHOOL)   
                    .setQuery(schoolQuery)
                    .setFrom(0).setSize(3);

            sr = client.prepareMultiSearch()
                    .add(response)
                    .add(response2)
                    .add(response3)
                    .get();
        }
        
        
        
        List<FluzzySearchResponse> list = new ArrayList<FluzzySearchResponse>();
        
        for (MultiSearchResponse.Item item : sr.getResponses()) {
            SearchResponse lresponse = item.getResponse();
            SearchHits hits = lresponse.getHits();
            // logger.info("执行智能联想:"+content);
            // logger.info("执行用时{}秒",lresponse.getTookInMillis()/1000.0);
            
            // 先遍历School索引，让它优先把数据都填进去List里面， 剩余的位置给area,sellinghouse索引
            for (SearchHit hit : hits.getHits())
            {
                FluzzySearchResponse fs = new FluzzySearchResponse();
                fs.setSearchContent(content);
                String type = hit.getType();
                Map<String, Object> result = hit.getSource();
                double weight = 16.0; 
                if(type.equals("aus_type_school")){
                    String name = (String)result.get("school_name");
                    fs.setName(name);
                    fs.setDisplayName(name);
                    fs.setFatherName(name);
                    //logger.info("area:"+name + "===Score:"+hit.getScore());
                    Integer level = 11;  // school单独设置level
                    fs.setLevel(level);
                    fs.set_id(hit.getId());
                    Float score = hit.getScore();
                    fs.setScore(score*weight);
                    list.add(fs);
                }
                
            }
            
            // 遍历suburb
            for (SearchHit hit : hits.getHits())
            {
                if (list.size() >= 10) break;
                FluzzySearchResponse fs = new FluzzySearchResponse();
                fs.setSearchContent(content);
                String type = hit.getType();
                Map<String, Object> result = hit.getSource();
                double weight = 15.0; 
                if(type.equals("aus_type_area")){
                    String name = (String)result.get("name");
                    fs.setName(name);
                    
                    String displayName = (String)result.get("display_name");
                    fs.setDisplayName(displayName);
                    
                    //logger.info("area:"+displayName + "===Score:"+hit.getScore());
                    Integer level =(Integer)result.get("level");
                    fs.setLevel(level);
                    String fatherName = (String)result.get("father_name");
                    fs.setFatherName(fatherName);
                    Float score = hit.getScore();
                    fs.setScore(score*weight);
                    list.add(fs);
                }
                
            }
            
            // 再遍历Sellinghouse索引，填充剩余的位置
            String typeHouse = "aus_type_sellinghouse";
            if (cate.equals("renting")) {
                typeHouse = "aus_type_rentinghouse";
            } else if (cate.equals("sold")) {
                typeHouse = "aus_type_soldhouse";
            }
            for (SearchHit hit : hits.getHits())
            {
                if (list.size() >= 10) break;
                FluzzySearchResponse fs = new FluzzySearchResponse();
                fs.setSearchContent(content);
                String type = hit.getType();
                Map<String, Object> result = hit.getSource();
                double weight = 1.0; 
                if(type.equals(typeHouse)){
                    String id = hit.getId();
                    fs.set_id(id);
                    String name = (String)result.get("address");
                    fs.setName(name);
                    String displayName = (String)result.get("address");
                    fs.setDisplayName(displayName);
                    //logger.info("selling:"+displayName + "===Score:"+hit.getScore());
                    Integer level = 5;
                    fs.setLevel(level);
                    String fatherName = (String)result.get("suburb");
                    fs.setFatherName(fatherName);
                    
                    Float score = hit.getScore();
                    fs.setScore(score*weight); 
                    list.add(fs);
                }

            }

        }
        java.util.Collections.sort(list);

        return list;
    }
    
    
    /**
     * @deprecated Replaced by @see {@link #searchHintInFuzzy(String)}
     * @param content
     * @return
     */
    public List<FluzzySearchResponse> searchHintInFuzzy2(String content){
        /**
         * 设计思路：
         * 1. 如果关键词总长是4个字符并且是数字，则认为是post_code
         *    是postcode，就按照code以及level=4去term查找
         * 2. 如果是一个单词
         *    处理第一个字母大写其余小写，直接在area的name字段，以及selling的address字段做wildcard
         *    特殊情况，可能是VIC州名要全大写，也是做wildcard
         */
        //切词。判断最后是否有空格，然后删除前后空格，按照空格切成多个word
        //如果后面没有空格，则最后一个word采用wild查询
        //如果最后有空格，则每个单词都按照term查询。或者不去一个个词处理，而是整体交给es
        //获得的结果，通过type，封装返回结果。包括name，father,keyword,level 后面是用来做2查询数据的

        // 这里要考虑一下是否正确，都可以可以看做是切割关键词的作用啊
        content = QueryParser.escape(content);
        content = content.replace(',', ' ');
        int maxResult = 10;
        boolean isFinishInput = true;
        char c = content.charAt(content.length()-1);
        if(c != ' '){
            isFinishInput = false;
        }
        content = content.trim();
        String[] words = content.split("\\s+");
        
        QueryBuilder query = null;
        QueryBuilder query2 = null;
        
        boolean isPostCodeSearch = false;
        if (content.length() == POSTCODE_LENGTH && StringUtils.isNumeric(content)) {
            //=如果是PostCode，直接在区域表里查
            
            isPostCodeSearch = true;
            QueryBuilder postCodeQuery = QueryBuilders.termQuery("post_code", content); 
            QueryBuilder levelQuery =QueryBuilders.termQuery("level", AreaLevel.SUBURB.ordinal());
            query = QueryBuilders.boolQuery().must(levelQuery).must(postCodeQuery);
            
        } else if (words.length == 1 && !isFinishInput){
            //=如果只有一个单词，采用通配符+前缀的方法进行匹配=
            
            String word = content;
            //String lastWord = "*" + word.toLowerCase()+ "*";
            String upperWord = "";
            if (word.length() == 2 || word.length() == 3) {
                String up  = word.toUpperCase();
                if (stateSet.contains(up)) {
                    upperWord = up;
                } else {
                    upperWord = word.substring(0, 1).toUpperCase()+word.substring(1).toLowerCase();
                }
            } else {
                upperWord = word.substring(0, 1).toUpperCase()+word.substring(1).toLowerCase();
            }
            
            // 1. 在区域表里查
            QueryBuilder wildcardQuery = QueryBuilders.wildcardQuery("name.raw", "*"+upperWord+"*"); //wildcard参数必须小写            
            //QueryBuilder prefixQuery = QueryBuilders.prefixQuery("name.raw", upperWord).boost(3);
            //query = QueryBuilders.boolQuery().must(wildcardQuery).should(prefixQuery);
            query = wildcardQuery;
            
            // 2. 在sellinghouse表里查询
            QueryBuilder wildcardQuery2 =QueryBuilders.wildcardQuery("address.raw", "*"+upperWord+"*");
            //QueryBuilder prefixQuery2 = QueryBuilders.prefixQuery("address.raw", upperWord).boost(3);
            //QueryBuilder matchQuery2 = QueryBuilders.matchPhraseQuery("address", upperWord);
            QueryBuilder prefixQuery2 = QueryBuilders.matchPhrasePrefixQuery("address", upperWord);
            query2 = QueryBuilders.boolQuery().should(prefixQuery2).should(wildcardQuery2);
            
            //query2 = wildcardQuery2;
        } else if(words.length >1 && !isFinishInput){
            //=多个单词，lastWord前面的做match查询，lastWord做wild查询
            
            String lastWord = words[words.length-1];
            lastWord = lastWord.toLowerCase()+"*";
            String contentFuzzy = "*";
            for(int i = 0;i< words.length;i++){
                String word = words[i];
                if(word.length() >0 ){
                    word = word.substring(0, 1).toUpperCase()+word.substring(1).toLowerCase();
                    if(i == 0){
                        contentFuzzy = contentFuzzy+word;
                    }else{
                        contentFuzzy = contentFuzzy+" "+word;
                    }
                }
            }
            contentFuzzy +="*";

            String preWord = content.substring(0, content.length()-lastWord.length());
            
            //前面的词汇保证有一定match匹配度
            QueryBuilder matchQuery = QueryBuilders.matchPhraseQuery("name", preWord).slop(5);
            //后面的词汇最好出现在name里更好
            QueryBuilder wildQuery =QueryBuilders.wildcardQuery("name", lastWord);
            //如果能够完全匹配则最理想,同时增加查询条件的权重，让分数*10
            QueryBuilder wildRawQuery =QueryBuilders.wildcardQuery("name.raw", contentFuzzy).boost(10);
            query = QueryBuilders.boolQuery().must(matchQuery).should(wildQuery).should(wildRawQuery);
            
            QueryBuilder matchQuery2 = QueryBuilders.matchPhraseQuery("address", preWord).slop(5);
            QueryBuilder wildQuery2 =QueryBuilders.wildcardQuery("address", lastWord);
            QueryBuilder wildRawQuery2 =QueryBuilders.wildcardQuery("address.raw", contentFuzzy).boost(10);
            query2 =QueryBuilders.boolQuery().must(matchQuery2).should(wildQuery2).should(wildRawQuery2);
            
        } else{
            // 剩余都用match匹配
            
            QueryBuilder matchQuery = QueryBuilders.matchPhraseQuery("name", content).slop(5);
            query = QueryBuilders.boolQuery().must(matchQuery);
            
            QueryBuilder matchQuery2 = QueryBuilders.matchPhraseQuery("address", content).slop(5);
            query2 = QueryBuilders.boolQuery().must(matchQuery2);
        }
        
        MultiSearchResponse sr = null;
        if (isPostCodeSearch) {
            // PostCode搜索不限制结果数量
            SearchRequestBuilder  response = client.prepareSearch(INDEX_AREA)   
                    .setQuery(query);
            sr = client.prepareMultiSearch()
                    .add(response)
                    .get();
        } else {
            SearchRequestBuilder  response = client.prepareSearch(INDEX_AREA)   
                    .setQuery(query)
                    .setFrom(0).setSize(7);
            SearchRequestBuilder response2 = client.prepareSearch(INDEX_SELLING_HOUSE)   
                    .setQuery(query2)
                    .setFrom(0).setSize(7);

            
            sr = client.prepareMultiSearch()
                    .add(response)
                    .add(response2)
                    .get();
        }
        

        List<FluzzySearchResponse> sortedlist = new ArrayList<FluzzySearchResponse>();

        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();

        for (MultiSearchResponse.Item item : sr.getResponses()) {
            SearchResponse lresponse = item.getResponse();
            SearchHits hits = lresponse.getHits();
            // logger.info("执行智能联想:"+content);
            // logger.info("执行用时{}秒",lresponse.getTookInMillis()/1000.0);
            // 先遍历area索引，让它优先把数据都填进去List里面， 剩余的位置给sellinghouse索引
            for (SearchHit hit : hits.getHits())
            {
                FluzzySearchResponse fs = new FluzzySearchResponse();
                String type = hit.getType();
                Map<String, Object> result = hit.getSource();
                //权重，目前area权重是5，selling是1， general是1,这些权重都是经验值，尽量不要随便改
                double weight = 5.0; 
                if(type.equals("aus_type_area")){
                    String name = (String)result.get("name");
                    fs.setName(name);
                    
                    String displayName = (String)result.get("display_name");
                    fs.setDisplayName(displayName);
                    
                    logger.info("area:"+displayName + "===Score:"+hit.getScore());
                    
                    Integer level =(Integer)result.get("level");
                    fs.setLevel(level);
                    String fatherName = (String)result.get("father_name");
                    fs.setFatherName(fatherName);
                    
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("data", fs);
                    Float score = hit.getScore();
                    fs.setScore(score);             // 实际分
                    map.put("score", score*weight); // 排序分
                    list.add(map);
                }
                

            }
            
            // 再遍历Sellinghouse索引，填充剩余的位置
            for (SearchHit hit : hits.getHits())
            {
                if (list.size() > 10) break;
                FluzzySearchResponse fs = new FluzzySearchResponse();
                String type = hit.getType();
                Map<String, Object> result = hit.getSource();
                //权重，目前area权重是5，selling是1， general是1,这些权重都是经验值，尽量不要随便改
                double weight = 1.0; 
                if(type.equals("aus_type_sellinghouse")){
                    String id = hit.getId();
                    fs.set_id(id);
                    String name = (String)result.get("address");
                    fs.setName(name);
                    String displayName = (String)result.get("address");
                    fs.setDisplayName(displayName);
                    logger.info("selling:"+displayName + "===Score:"+hit.getScore());
                    Integer level = 5;
                    fs.setLevel(level);
                    String fatherName = (String)result.get("suburb");
                    fs.setFatherName(fatherName);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("data", fs);
                    Float score = hit.getScore();
                    fs.setScore(score);             // 实际分
                    map.put("score", score*weight); // 排序分
                    list.add(map);
                }

            }

        }
        
        
        while(list.size() >0){
            double maxScore = 0;
            FluzzySearchResponse maxFs = null;
            Map<String, Object> maxMap = null;
            for (Map<String, Object> map : list) {
                double score = (double)map.get("score");
                if(score > maxScore){
                    maxScore = score;
                    maxFs = (FluzzySearchResponse)map.get("data");
                    maxMap = map;
                }
            }
            
            sortedlist.add(maxFs);
            list.remove(maxMap);
            if(sortedlist.size() == maxResult){
                break;
            }
        }
        
        return sortedlist;
    }
    
    /**
     * 模糊查询同时符合过滤条件
     * @param content
     * @param page
     * @param sort
     * @param filter
     * @return
     */
    public List<HouseListInfo> searchInFuzzyWithFilter(String cate, String content, int page, String sort,Map<String,Object> filter){
        /**
         * 整体思路：
         * 1. match 'address', term 'region' & 'area'，match与term是OR的关系
         * 2. 如果term到区域，则加大量权重，保证按照区域搜索
         * 3. 所以主力就是靠match
         * 
         * 有几个问题需要思考：
         * Q. 如果用户只是想搜索suburb，但是命中了region，则在结果里看不到关键词了
         * A. 其实也能看到关键词，match会给权重，让suburb里带关键词的放在前面
         * 
         * 默认排序规则：
         * 1. 使用script_score的方式，用现有的数据计算结果，等稳定了在固化到document里
         */
        
        Assert.notNull(content, "content can't be null");
        BoolQueryBuilder filterQuery = buildFilter(filter);
        int  countPerPage = 20;
        
        String[] words = content.split("\\s+");
        String upperWords = "";
        for(int i = 0;i< words.length;i++){
            String word = words[i];
            word = word.substring(0, 1).toUpperCase()+word.substring(1).toLowerCase();
            if(i == 0){
                upperWords = word;
            }else{
                upperWords = upperWords + " " + word;
            }
        }
        
        QueryBuilder matchQuery = QueryBuilders.matchQuery("address", content);
        QueryBuilder regionTermQuery = QueryBuilders.termQuery("region.raw", upperWords).boost(10);
        QueryBuilder areaTermQuery = QueryBuilders.termQuery("area.raw", upperWords).boost(10);
        QueryBuilder query = filterQuery.should(matchQuery).should(regionTermQuery).should(areaTermQuery);
        
        
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX_SELLING_HOUSE)   
                .setFrom(page*(int)countPerPage)
                .setSize((int)countPerPage);
        
        addSorter(searchRequestBuilder,query,sort,cate);
        
        SearchResponse searchResponseList = searchRequestBuilder.execute().actionGet();
        
        long hitCount = searchResponseList.getHits().getTotalHits();
        logger.info("执行模糊搜索，搜索词是:{}",content);
        logger.info("搜索耗时:{}秒,返回数据{}个",searchResponseList.getTookInMillis()/1000.0,hitCount);
        
        
        SearchHits resultHits = searchResponseList.getHits(); 
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setMapLevel(AreaLevel.HOUSE.ordinal());
        houseListInfo.setHouseListFromSearchHits(resultHits);
        houseListInfo.setMapListFromSearchHits(resultHits, AreaLevel.HOUSE.ordinal());

        long totalProp = resultHits.getTotalHits();
        houseListInfo.setPropNum(totalProp);
        houseListInfo.setCurPage(page);
        houseListInfo.setMaxPage(totalProp/countPerPage);
        houseListInfo.setMapLevel(AreaLevel.HOUSE.ordinal());
        houseListInfo.setSearchName(content);
        if (houseListInfo.getMapInfo().size() > 0) {
            HouseListInfo.MapInfoForHouse map = (HouseListInfo.MapInfoForHouse)(houseListInfo.getMapInfo().get(0));
            houseListInfo.setBasePoint(map.getBasePoint());
        }
        
        // 只有一个数据，但为了保持返回数据一致性，保证成数组
        ArrayList<HouseListInfo> list = new ArrayList<HouseListInfo>();
        list.add(houseListInfo);
        return list;
        
    }
    
    
    /**
     * 模糊查询
     * @deprecated Replaced by {@link #searchInFuzzyWithFilter(String, int, String, Map)}
     * @param content 搜索模糊词
     * @param page 页码，从0开始
     * @param sort 缺省是default，则会有一套精准推荐算法给用户
     * @param bedroomFilter 过滤有几个房间
     * @return
     */
    public List<HouseListInfo> searchInFuzzy(String content, int page, String sort,Map<String,Boolean> bedroomFilter){
        /**
         * 整体思路：
         * 1. match 'address', term 'region' & 'area'，match与term是OR的关系
         * 2. 如果term到区域，则加大量权重，保证按照区域搜索
         * 3. 所以主力就是靠match
         * 
         * 有几个问题需要思考：
         * Q. 如果用户只是想搜索suburb，但是命中了region，则在结果里看不到关键词了
         * A. 其实也能看到关键词，match会给权重，让suburb里带关键词的放在前面
         * 
         * 默认排序规则：
         * 1. 使用script_score的方式，用现有的数据计算结果，等稳定了在固化到document里
         */
        
        Assert.notNull(content, "content can't be null");
        int  countPerPage = 20;
        
        String[] words = content.split("\\s+");
        String upperWords = "";
        for(int i = 0;i< words.length;i++){
            String word = words[i];
            word = word.substring(0, 1).toUpperCase()+word.substring(1).toLowerCase();
            if(i == 0){
                upperWords = word;
            }else{
                upperWords = upperWords + " " + word;
            }
        }
        
        QueryBuilder matchQuery = QueryBuilders.matchQuery("address", content);
        QueryBuilder regionTermQuery = QueryBuilders.termQuery("region.raw", upperWords).boost(10);
        QueryBuilder areaTermQuery = QueryBuilders.termQuery("area.raw", upperWords).boost(10);
        QueryBuilder query = QueryBuilders.boolQuery().should(matchQuery).should(regionTermQuery).should(areaTermQuery);
        
        
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX_SELLING_HOUSE)   
                .setFrom(page*(int)countPerPage)
                .setSize((int)countPerPage);
        
        SortBuilder<?> sortor = buildSorter(sort);
        
        if (sortor != null) {
            searchRequestBuilder.setQuery(query);
            searchRequestBuilder.addSort(sortor);
        } else {
            // Default 排序采用脚本方法干预分数，比如有主图，有basepoint等等，脚本在config/scripts下面
            Map<String, Object> params = new HashMap<>();  
            Script script = new Script(ScriptType.FILE,"painless",SCRIPT_SCORE_FILE, params);
            ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script); 
            searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(query,scriptBuilder));
        }
        
        SearchResponse searchResponseList = searchRequestBuilder.execute().actionGet();
        
        long hitCount = searchResponseList.getHits().getTotalHits();
        logger.info("执行模糊搜索，搜索词是:{}",content);
        logger.info("搜索耗时:{}秒,返回数据{}个",searchResponseList.getTookInMillis()/1000.0,hitCount);
        
        
        SearchHits resultHits = searchResponseList.getHits(); 
        HouseListInfo houseListInfo = new HouseListInfo();
        houseListInfo.setMapLevel(AreaLevel.HOUSE.ordinal());
        houseListInfo.setHouseListFromSearchHits(resultHits);
        houseListInfo.setMapListFromSearchHits(resultHits, AreaLevel.HOUSE.ordinal());

        long totalProp = resultHits.getTotalHits();
        houseListInfo.setPropNum(totalProp);
        houseListInfo.setCurPage(page);
        houseListInfo.setMaxPage(totalProp/countPerPage);
        houseListInfo.setMapLevel(AreaLevel.HOUSE.ordinal());
        houseListInfo.setSearchName(content);
        if (houseListInfo.getMapInfo().size() > 0) {
            HouseListInfo.MapInfoForHouse map = (HouseListInfo.MapInfoForHouse)(houseListInfo.getMapInfo().get(0));
            houseListInfo.setBasePoint(map.getBasePoint());
        }
        
        // 只有一个数据，但为了保持返回数据一致性，保证成数组
        ArrayList<HouseListInfo> list = new ArrayList<HouseListInfo>();
        list.add(houseListInfo);
        return list;
        
    }
    
    public List<Document> simpleSearch(String index, BasicDBObject query, int limit, boolean isAnd) {
        List<Document> docList = new LinkedList<>();
        Set<Entry<String, Object>> entries = query.entrySet();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        for (Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isAnd == true) {
                boolQuery.must(QueryBuilders.termQuery(key,value));
            } else {
                boolQuery.should(QueryBuilders.termQuery(key,value));
            }
        }
        if (isAnd == false) {
            boolQuery.minimumNumberShouldMatch(1);
        }
        
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index)   
                .setQuery(boolQuery);
        if (limit > 0) {
            searchRequestBuilder.setSize(limit);
        }
        SearchResponse searchResponseList = searchRequestBuilder.execute().actionGet();
        SearchHits resultHits = searchResponseList.getHits(); 
        SearchHit[] hits = resultHits.getHits();
        for (SearchHit hit : hits) {
            Map<String, Object> value = hit.getSource();
            Document doc = new Document(value);
            doc.append("_id", hit.getId());
            docList.add(doc);
        }
        
        return docList;
    }
    
    public void close() {
        this.client.close();
    }
   
    
    /**
     * 通过文档某个项来影响分数
     * 目前是一个测试例子
     * 优点是简单，缺点是只能用一项来影响评分。当然它的目的是让你自己设置这一项然后来影响评分
     */
    @SuppressWarnings("unused")
    private void searchFunctionScoreTest() {
        FieldValueFactorFunctionBuilder bedField = ScoreFunctionBuilders.fieldValueFactorFunction("beds").modifier(FieldValueFactorFunction.Modifier.LN1P).factor(10);
        SearchResponse scrollResponse = client.prepareSearch(INDEX_SELLING_HOUSE)  
                .setTypes("aus_type_sellinghouse")  
                .setQuery(QueryBuilders.functionScoreQuery(QueryBuilders.matchPhraseQuery("suburb.raw", "Spencer Park"), bedField))
                .execute().actionGet();
         
        SearchHit[] hits = scrollResponse.getHits().getHits();  
        for(int j =0;j<hits.length;j++){  
            System.out.println(hits[j].getSourceAsString());  
            System.out.println(hits[j].getScore());  
        }  
        
        
    }
    
    
    /**
     * 测试Script_Score功能
     * @param content
     * @return
     */
    @SuppressWarnings("unused")
    private void searchScriptScoreTest(){
        // 测试script_score
        
        Map<String, Object> params = new HashMap<>();  
        params.put("averageBed", 2);  
        params.put("num2", 2);  
        
        /**
         * 这是Groovy的语法，类似于javascript
            String inlineScript = "bedNum = doc['beds'].value;"  
                    + "return (bedNum/averageBed)";  
            Script script = new Script(ScriptType.INLINE, "groovy", inlineScript, params);
         */
        
        /**
         * 这是Painless内嵌语法，类似于Java，ES5.0之后，主推painless
            String inlineScript = ""
                    + "int mainPicScore = 0;"            // 有主图加2分
                    + "int imagesScore = 0;"             // 图片大于5张加1分
                    + "int priceScore = 0;"              // 有价格加1分
                    + "int basePointScore = 0;"          // 有点坐标加1分 
                    + "mainPicScore = doc['house_pic_main'].value == null?0:1;"
                    + "if (doc['images'].value instanceof List) "
                    + "imagesScore = doc['images'].value.size() > 5 ? 1:0;"
                    + "priceScore = doc['house_price'].value > 0? 1:0;"
                    + "basePointScore = doc['base_point'].value == null? 0:1;"
                    + "return mainPicScore + imagesScore + priceScore + basePointScore;"
                    ;
            Script script = new Script(ScriptType.INLINE,"painless",inlineScript, params);
        */
         
        /**
         * 这是painless的脚本使用方法
         */
        Script script = new Script(ScriptType.FILE,"painless","WeightedSort", params);
        
        ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script);  
    
        SearchRequestBuilder requestBuilder = client.prepareSearch(INDEX_SELLING_HOUSE)  
                .setTypes("aus_type_sellinghouse")  
                 .setQuery(QueryBuilders.functionScoreQuery(QueryBuilders.matchPhraseQuery("suburb.raw", "Spencer Park"),scriptBuilder));  
        
        SearchResponse response = requestBuilder.setFrom(0).setSize(5).execute().actionGet();  
        SearchHit[] hits = response.getHits().getHits();  
        for(int j =0;j<hits.length;j++){  
            System.out.println("id:" + hits[j].getId());
            System.out.println(hits[j].getSourceAsString());  
            System.out.println(hits[j].getScore());  
        }  
        
    }
    
    
    /**
     * 筛选条件.用于地图搜索，模糊搜索
     * 过滤用到的标签 
        "price":"200-1000"  // 中杠左侧如果是any，计为0， 右侧如果是any， 则整体计为 >= 0
        "bed":"1-any"       // 可以没有中杠只是数字。 如果左侧是any计为0，右侧如是any计为 >= 0
        "bath":"1-any"      // 可以没有中杠只是数字。 如果左侧是any计为0，右侧如是any计为 >= 0
        "parking":"1-any"      // 可以没有中杠只是数字。 如果左侧是any计为0，右侧如是any计为 >= 0
        "propertyType":["tag1","tag2"]
        "feature":["tag1","tag2"]
        "isNew":true
        "isReduceIn24":true
        "isExcludeOffer":true
     * @param filter
     * @return
     */
    private BoolQueryBuilder buildFilter(Map<String,Object> filter) {

        // 构造过滤器  
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        if (filter == null) return filterQuery;
        
        // Price 过滤器 price:0-any ， 不能是固定一个值，必须是范围
        if (filter.get("price") != null) {
            QueryBuilder priceFilter = null;
            String price = (String)filter.get("price");
            String[] prices = price.split("-");
            if (prices.length != 2) throw new IllegalArgumentException("价格必须有From 跟 To");
            
            int priceFrom = 0;
            if (StringUtils.isNumeric(prices[0])) {
                priceFrom = Integer.valueOf(prices[0]);;
            }
            
            if (StringUtils.isNumeric(prices[1])) {
                int priceTo = Integer.valueOf(prices[1]);
                priceFilter = QueryBuilders.rangeQuery("house_price").from(priceFrom).to(priceTo);
            } else {
                priceFilter = QueryBuilders.rangeQuery("house_price").from(priceFrom);
            }
            filterQuery.must(priceFilter);
        }
        
        // Bedroom 过滤器 bed:0-any 或 bed:2 或 2,3,5+
        if (filter.get("bed") != null) {
            QueryBuilder bedFilter = null;
            String bedStr = (String)filter.get("bed");
            String[] beds = bedStr.split("-");
            if (beds.length == 1) {
                if (bedStr.contains(",")) {
                    // 如果包含逗号则是选择数组，而且可能有既有数组, 也有范围
                    BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();
                    String[] bedArray = bedStr.split(",");
                    Set<Integer> bedIntArray = new HashSet<>();
                    for (String oneBedStr : bedArray) {
                        oneBedStr = oneBedStr.trim();
                        if (StringUtils.isNumeric(oneBedStr)) {
                            bedIntArray.add(Integer.valueOf(oneBedStr));
                        } else if (oneBedStr.contains("+")){
                            oneBedStr = oneBedStr.replace("+", "");
                            Integer oneBedInt = Integer.valueOf(oneBedStr);
                            QueryBuilder rangFilter = QueryBuilders.rangeQuery("beds").from(oneBedInt);
                            boolFilter.should(rangFilter);
                        }
                    }
                    QueryBuilder termsFilter = QueryBuilders.termsQuery("beds", bedIntArray);
                    boolFilter.should(termsFilter);
                    boolFilter.minimumNumberShouldMatch(1);
                    bedFilter = boolFilter;
                } else {
                    if (!StringUtils.isNumeric(beds[0])) {
                        throw new IllegalArgumentException("bedroom数量必须是数字");
                    }
                    bedFilter = QueryBuilders.termQuery("beds", Integer.valueOf(beds[0]));
                }
                
            } else {
                // 只是简单的范围
                int bedFrom = 0;
                if (StringUtils.isNumeric(beds[0])) {
                    bedFrom = Integer.valueOf(beds[0]);
                }
                if (StringUtils.isNumeric(beds[1])) {
                    int bedTo = Integer.valueOf(beds[1]);
                    bedFilter = QueryBuilders.rangeQuery("beds").from(bedFrom).to(bedTo);
                } else {
                    bedFilter = QueryBuilders.rangeQuery("beds").from(bedFrom);
                }
            }
            filterQuery.must(bedFilter);
        }
        
        // Bathroom 过滤器 bath:0-any 或 bath:2 或2,3,5+
        if (filter.get("bath") != null) {
            QueryBuilder bathFilter = null;
            String bathStr = (String)filter.get("bath");
            String[] baths = bathStr.split("-");
            if (baths.length == 1) {
                if (bathStr.contains(",")) {
                    // 如果包含逗号则是选择数组
                    BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();
                    String[] bathArray = bathStr.split(",");
                    Set<Integer> bathIntArray = new HashSet<>();
                    for (String oneBathStr : bathArray) {
                        oneBathStr = oneBathStr.trim();
                        if (StringUtils.isNumeric(oneBathStr)) {
                            bathIntArray.add(Integer.valueOf(oneBathStr));
                        } else if (oneBathStr.contains("+")){
                            oneBathStr = oneBathStr.replace("+", "");
                            Integer oneBathInt = Integer.valueOf(oneBathStr);
                            QueryBuilder rangFilter = QueryBuilders.rangeQuery("baths").from(oneBathInt);
                            boolFilter.should(rangFilter);
                        }
                    }
                    
                    QueryBuilder termsFilter = QueryBuilders.termsQuery("baths", bathIntArray);
                    boolFilter.should(termsFilter);
                    boolFilter.minimumNumberShouldMatch(1);
                    bathFilter = boolFilter;
                } else {
                    if (!StringUtils.isNumeric(baths[0])) {
                        throw new IllegalArgumentException("bathroom数量必须是数字");
                    }
                    bathFilter = QueryBuilders.termQuery("baths", Integer.valueOf(baths[0]));
                }


            } else {
                int bathFrom = 0;
                if (StringUtils.isNumeric(baths[0])) {
                    bathFrom = Integer.valueOf(baths[0]);
                }
                if (StringUtils.isNumeric(baths[1])) {
                    int bathTo = Integer.valueOf(baths[1]);
                    bathFilter = QueryBuilders.rangeQuery("baths").from(bathFrom).to(bathTo);
                } else {
                    bathFilter = QueryBuilders.rangeQuery("baths").from(bathFrom);
                }
            }
            filterQuery.must(bathFilter);
        }
        
        // Parking 过滤器 parking:0-any 或 parking:2 或2,3,5+
        if (filter.get("parking") != null) {
            QueryBuilder parkingFilter = null;
            String parkingStr = (String)filter.get("parking");
            String[] parkings = parkingStr.split("-");
            if (parkings.length == 1) {
                if (parkingStr.contains(",")) {
                    // 如果包含逗号则是选择数组
                    BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();
                    String[] parkingArray = parkingStr.split(",");
                    Set<Integer> parkingIntArray = new HashSet<>();
                    for (String oneParkingStr : parkingArray) {
                        oneParkingStr = oneParkingStr.trim();
                        if (StringUtils.isNumeric(oneParkingStr)) {
                            parkingIntArray.add(Integer.valueOf(oneParkingStr));
                        } else if (oneParkingStr.contains("+")){
                            oneParkingStr = oneParkingStr.replace("+", "");
                            Integer oneParkingInt = Integer.valueOf(oneParkingStr);
                            QueryBuilder rangFilter = QueryBuilders.rangeQuery("parking").from(oneParkingInt);
                            boolFilter.should(rangFilter);
                        }
                        
                    }
                    
                    QueryBuilder termsFilter = QueryBuilders.termsQuery("parking", parkingIntArray);
                    boolFilter.should(termsFilter);
                    boolFilter.minimumNumberShouldMatch(1);
                    parkingFilter = boolFilter;
                    
                } else {
                    if (!StringUtils.isNumeric(parkings[0])) {
                        throw new IllegalArgumentException("parking数量必须是数字");
                    }
                    parkingFilter = QueryBuilders.termQuery("parking", Integer.valueOf(parkings[0]));
                }

            } else {
                int parkingFrom = 0;
                if (StringUtils.isNumeric(parkings[0])) {
                    parkingFrom = Integer.valueOf(parkings[0]);
                }
                if (StringUtils.isNumeric(parkings[1])) {
                    int parkingTo = Integer.valueOf(parkings[1]);
                    parkingFilter = QueryBuilders.rangeQuery("parking").from(parkingFrom).to(parkingTo);
                } else {
                    parkingFilter = QueryBuilders.rangeQuery("parking").from(parkingFrom);
                }
            }
            filterQuery.must(parkingFilter);
        }
        
        // PropertyType 过滤器 propertyType:[xxx,yyy,zzz]
        if (filter.get("propertyType") != null) {
            @SuppressWarnings("unchecked")
            ArrayList<String> propertyTypeList = (ArrayList<String>)filter.get("propertyType");
            List<String> newList = new ArrayList<String>();
            for (String propertyType : propertyTypeList) {
                if (propertyType.contains("house")) {
                    newList.add("House");
                    newList.add("Duplex");
                    newList.add("New Home Designs");
                    newList.add("New House & Land,Villa");
                    
                } else if (propertyType.equals("apartment")) {
                    newList.add("Apartment / Unit / Flat");
                    newList.add("Block of Units");
                    newList.add("Studio");
                    newList.add("newApartments");
                    newList.add("New Apartments / Off the Plan");
                    
                } else if (propertyType.equals("townhouse")) {
                    newList.add("Townhouse");
                } else if (propertyType.equals("rural")) {
                    newList.add("Rural");
                    newList.add("Acreage / Semi-Rural");
                    newList.add("Farm");
                } else if (propertyType.equals("land")) {
                    newList.add("Development Site");
                    newList.add("New land");
                    newList.add("Vacant land");
                }
                
            }
            QueryBuilder propertyTypeFilter = QueryBuilders.termsQuery("property_type", newList);
            filterQuery.must(propertyTypeFilter);
        }
        
        // Feature 过滤器 feature:[xxx,yyy,zzz]
        if (filter.get("feature") != null) {
            @SuppressWarnings("unchecked")
            ArrayList<String> features = (ArrayList<String>)filter.get("feature");
            ArrayList<String> lowFeatures = new ArrayList<>();
            for (String feature : features) {
                lowFeatures.add(feature.toLowerCase());
            }
            QueryBuilder featureFilter = QueryBuilders.termsQuery("features", lowFeatures);
            filterQuery.must(featureFilter);
        } 
        
        // IsNew 过滤器 isNew:true 
        if (filter.get("isNew") != null) {
            boolean isNew = (Boolean)filter.get("isNew");
            if (isNew) {
                QueryBuilder isNewFilter = QueryBuilders.termQuery("tag_list.is_new", isNew);
                filterQuery.must(isNewFilter);
            }
        }
        
        // IsReduceIn24 过滤器 isReduceIn24:true
        if (filter.get("isReduceIn24") != null) {
            boolean isReduceIn24 = (Boolean)filter.get("isReduceIn24");
            if (isReduceIn24) {
                QueryBuilder reduceIn24Filter = QueryBuilders.termQuery("tag_list.price_tag", 1);
                filterQuery.must(reduceIn24Filter);
            }
        }
        
        // IsExcludeOffer 过滤器 isExcludeOffer:true
        if (filter.get("isExcludeOffer") != null) {
            boolean isExcludeOffer = (Boolean)filter.get("isExcludeOffer");
            if (isExcludeOffer) {
                QueryBuilder isExcludeOfferFilter = QueryBuilders.termQuery("tag_list.is_offered", true);
                filterQuery.mustNot(isExcludeOfferFilter);
            }
        }
        
        
        return filterQuery;
    }
    
    /**
     * 给searchBuilder增加过滤器，不仅仅是简单排序，其实还有各种
     * @param searchRequestBuilder
     * @param query
     * @param sort
     */
    private  void addSorter(SearchRequestBuilder searchRequestBuilder, QueryBuilder query, String sort,String cate) {
        if (sort.equals("newest") || sort.equals("priceDown")) {
            SortBuilder<?> sortor = buildSorter(sort);
            searchRequestBuilder.setQuery(query);
            searchRequestBuilder.addSort(sortor);
        } else if (sort.equals("priceUp")) {
            // 去掉排序，使用打分干预的方法来排序，让没价格的房源显示在后面
            Map<String, Object> params = new HashMap<>();  
            String inlineScript = "long value = -100000000000L;"
                    + "if (doc['house_price'] == null || doc['house_price'].value == null || doc['house_price'].value <= 0) value = -100000000000L;"
                    + "else value = 0 - doc['house_price'].value;"
                    + "return value;";
            Script script = new Script(ScriptType.INLINE,"painless",inlineScript, params);
            ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script); 
            searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(query,scriptBuilder));
        } else {
            // Default 排序采用脚本方法干预分数，比如有主图，有basepoint等等，脚本在config/scripts下面
            Map<String, Object> params = new HashMap<>(); 
            params.put("cate", cate);
            Script script = new Script(ScriptType.FILE,"painless",SCRIPT_SCORE_FILE, params);
            ScriptScoreFunctionBuilder scriptBuilder = ScoreFunctionBuilders.scriptFunction(script); 
            searchRequestBuilder.setQuery(QueryBuilders.functionScoreQuery(query,scriptBuilder));
        }
    }
    
    private SortBuilder<?> buildSorter(String sort) {
        SortBuilder<?> sortBuilder = null;
        
        if(sort.equals("newest")){
            sortBuilder = SortBuilders.fieldSort("created_on")
                    .order(SortOrder.DESC);
        }else if(sort.equals("priceDown")){
            sortBuilder = SortBuilders.fieldSort("house_price")
                    .order(SortOrder.DESC);
        }else if(sort.equals("priceUp")){
            sortBuilder = SortBuilders.fieldSort("house_price")
                    .order(SortOrder.ASC);
        }
        
        return sortBuilder;
    }
    
    private int getAreaLevelFromZoom(int zoom) {
        int areaLevel = 1;
        
        if (zoom < 1) {
            // 显示新西兰，澳大利亚，美国等国家级别的信息
            // 当前，我们不显示国家信息，所以更大范围的显示state信息
            areaLevel = 0;
        } else if (zoom >= 1 && zoom < 7) {
            // 显示奥克兰等区域级别信息
            areaLevel = 1;
        } else if (zoom >= 7 && zoom < 11) { // 8
            // 显示奥克兰等区域级别信息
            areaLevel = 2;
        } else if (zoom >= 11 && zoom < 13) {
            // 显示city级别信息
            areaLevel = 3;
        } else if (zoom >= 13 && zoom < 15) {
            // 显示suburb级别信息
            areaLevel = 4;
        } else if (zoom >= 15) {
            // 显示房源级别信息
            areaLevel = 5;
        }

        return areaLevel;
    }
    
//    public static void main(String[] args) {
//        ESTransportService service = new ESTransportService();
//        long startTime = System.currentTimeMillis();
//        ArrayList<Double> point = new ArrayList<>();
//        point.add(151.199295);
//        point.add(-33.9011257);
//        int price = 874000;
//        // 考虑是不是距离太远的也选进来了
//        // 速度很慢需要优化
//        
//        List<HouseSimpleInfo> list = service.searchSellingHouseByPointAndPrice(point, price);
//        long endTime = System.currentTimeMillis();
//        
//        System.out.println("找到房子:" + list.size() + "花费时间:" + (endTime - startTime));
//        for (HouseSimpleInfo houseSimpleInfo : list) {
//            System.out.println(houseSimpleInfo.toString());
//        }
//    }
    
}