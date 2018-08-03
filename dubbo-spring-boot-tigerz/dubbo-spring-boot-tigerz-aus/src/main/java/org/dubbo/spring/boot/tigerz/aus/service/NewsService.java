package org.dubbo.spring.boot.tigerz.aus.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.dubbo.spring.boot.tigerz.api.util.GsonUtil;
import org.dubbo.spring.boot.tigerz.api.util.RedisUtils;
import org.dubbo.spring.boot.tigerz.aus.constant.RedisKey;
import org.dubbo.spring.boot.tigerz.aus.entity.NewsList;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;
import com.tigerz.easymongo.MongoService;

@Service
public class NewsService {
    
    private MongoService mongoService = MongoService.getInstance();
    
    @SuppressWarnings("unchecked")
    public List<NewsList> getNewsList(int page, String stat,int pageCount) {
        String keyWord = RedisKey.GET_NEWS_LIST + "_" + stat + "_" + page + "_" + pageCount;
        String redisValue = RedisUtils.getKeyAsString(keyWord);
        if(redisValue != null){
            return GsonUtil.fromJson(redisValue, List.class);
        }
        
        BasicDBObject query = new BasicDBObject();
        if (!stat.equals("all")) {
            query.append("state", stat);
        }
        BasicDBObject sort = new BasicDBObject();
        sort.append("date", -1);
        try {
            List<NewsList> newsList = mongoService.find(query, sort, (page-1)*pageCount, pageCount, NewsList.class);
            if (newsList != null && newsList.size() > 0) {
                for (NewsList news : newsList) {
                    // 处理用户浏览量
                    setNewsViewCount(news);
                    
                    // 处理用户分享数量
                    setNewsShareCount(news);
                    
                    // 处理正文, 把正文去掉
                    news.setDescription(null);
                    news.setDescSource(null);
                    
                    // 把headline前面的空格去掉
                    news.setHeadline(news.getHeadline().trim());
                    
                }
                RedisUtils.setex(keyWord, GsonUtil.toJson(newsList), 60*60*24*1);
            }
            return newsList;
        } catch (InstantiationException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public NewsList getOneNews(String id) {
        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(id));
        try {
            NewsList news = mongoService.findOne(query, NewsList.class);
            setNewsViewCount(news);
            return news;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void setNewsViewCount(NewsList news) {
        Integer realView = news.getRealView();
        int realViewCount = 0;
        if (realView  != null) {
            realViewCount = realView * 10;
        }
        int randomUserView = (int)(Math.random() * 20) + 100;
        news.setUserView(randomUserView + realViewCount);
    }
    
    private void setNewsShareCount(NewsList news) {
        Integer realShare = news.getRealShare();
        int realShareCount = 0;
        if (realShare != null) {
            realShareCount = realShareCount * 2;
        }
        int randomUserShare = (int)(Math.random() * 10) + 10;
        news.setShare(randomUserShare + realShareCount);
    }

}
