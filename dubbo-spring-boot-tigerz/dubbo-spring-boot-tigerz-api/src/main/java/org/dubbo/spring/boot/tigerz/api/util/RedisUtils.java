package org.dubbo.spring.boot.tigerz.api.util;


/**
 * Created by Ted on 4/23/16.
 */

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class RedisUtils {
    private final static Logger LOG = Logger.getLogger(RedisUtils.class);
    private static Properties redisProp = new Properties();

    private static JedisPool shardedJedisPool = null;
    private static int defaultIndex = 0;

    static {
        if (shardedJedisPool == null) {
            shardedJedisPool = initSharedJedisPool();
            defaultIndex = Integer.valueOf(redisProp.getProperty("redis.db.index"));
        }
    }

    private RedisUtils() {

    }

    public static JedisPool getShardedJedisPool() {
        if (shardedJedisPool == null) {
            initSharedJedisPool();
        }
        return shardedJedisPool;
    }

    /**
     * Init the sharedJedisPool
     *
     * @return Success: return the shardedJedisPool Otherwise null
     */
    private static JedisPool initSharedJedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        InputStream inputStream = config.getClass().getClassLoader().getResourceAsStream("redis.properties");
        try {
            redisProp.load(inputStream);
            System.out.println("read redis property file success!");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        String host = redisProp.getProperty("redis.host");
        String port = redisProp.getProperty("redis.port");
        String pass = redisProp.getProperty("redis.pass");
        String maxIdle = redisProp.getProperty("redis.maxIdle");
        String minIdle = redisProp.getProperty("redis.minIdle");

        String maxTotal = redisProp.getProperty("redis.maxTotal");
        String maxWait = redisProp.getProperty("redis.maxWait");
        String timeOut = redisProp.getProperty("redis.timeOut");
        String testOnBorrow = redisProp.getProperty("redis.testOnBorrow");
        
        
        config.setMaxTotal(Integer.valueOf(maxTotal));
        config.setMaxIdle(Integer.valueOf(maxIdle));
        config.setMinIdle(Integer.valueOf(minIdle));
        config.setMaxWaitMillis(Integer.valueOf(maxWait));
        config.setTestOnBorrow(Boolean.valueOf(testOnBorrow));

        config.setTestOnReturn(true);
        // Idle时进行连接扫描
        config.setTestWhileIdle(true);
        // 表示idle object evitor两次扫描之间要sleep的毫秒数
        config.setTimeBetweenEvictionRunsMillis(30000);
        // 表示idle object evitor每次扫描的最多的对象数
        config.setNumTestsPerEvictionRun(10);
        // 表示一个对象至少停留在idle状态的最短时间，然后才能被idle object
        // evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        config.setMinEvictableIdleTimeMillis(60000);
        
        JedisPool pool = new JedisPool(config, host, Integer.valueOf(port), Integer.valueOf(timeOut), pass);
        shardedJedisPool = pool;
        
        return pool;
    }

    private static Jedis getJedis() {
        if (shardedJedisPool == null) {
            LOG.error("reids pool is null,please check the configuration");
            return null;
        }
        Jedis jedis = shardedJedisPool.getResource();
        jedis.select(defaultIndex);
        return jedis;
    }

    public static byte[] getKey(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.get(key.getBytes());
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static String getKeyAsString(String key) {
        Jedis jedis = getJedis();
        try {
            byte[] data = jedis.get(key.getBytes());
            if (data != null) {
                return new String(data, "UTF-8");
            } else {
                return null;
            }
        } catch (Exception ex) {
            LOG.error(String.format("Get key:%s from redis failed", key), ex);
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void publish(String channel, String msg){
        Jedis jedis = getJedis();
        try {
            jedis.publish(channel, msg);
        } catch (Exception ex) {
            LOG.error(String.format("publis msg:%s from redis failed", msg), ex);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static String getKeyAsString(String key, int expiredSeconds) {
        Jedis jedis = getJedis();
        try {
            byte[] data = jedis.get(key.getBytes());
            if (data != null) {
                jedis.expire(key, expiredSeconds);
                return new String(data, "UTF-8");
            } else {
                return null;
            }
        } catch (Exception ex) {
            LOG.error(String.format("Get key:%s from redis failed", key), ex);
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void setex(String key, String value, int seconds) {
        Jedis jedis = getJedis();
        try {
            jedis.setex(key, seconds, value);
        } catch (Exception ex) {
            LOG.error(String.format("Set key:%s with value:%s to redis failed", key, value), ex);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void setex(String key, byte[] value, int seconds) {
        Jedis jedis = getJedis();
        try {
            jedis.setex(key.getBytes("utf-8"), seconds, value);
        } catch (Exception ex) {
            LOG.error(String.format("Set key:%s with value:%s to redis failed", key, value), ex);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void set(String key, String value) {
        Jedis jedis = getJedis();
        try {
            jedis.set(key, value);
        } catch (Exception ex) {
            LOG.error(String.format("Set key:%s with value:%s to redis failed", key, value), ex);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static Long del(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.del(key);
        } catch (Exception ex) {
            LOG.error(String.format("Delete key:%s from redis failed", key), ex);
            return 0L;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static long  llen(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.llen(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static List<String> lrange(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.lrange(key,0,-1);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    public static void lpush(String key, String... values) {
        Jedis jedis = getJedis();
        try {
            jedis.lpush(key, values);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    public static String rpop(String key) {
       Jedis jedis = getJedis();;
        try {
            return jedis.rpop(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    public static boolean exists(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.exists(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static long incr(String key) {
        Jedis jedis = getJedis();
        try {
            return (Long) (jedis.incr(key));
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }



    public static long incrBy(String key,long count) {
        if (shardedJedisPool == null) {
            LOG.error("redis pool is null,please check the configuration");
            return 0;
        }
        Jedis jedis = getJedis();
        try {
            return jedis.incrBy(key,count);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static Double zincrBy(String key,int score,String member) {
        Jedis jedis =getJedis();
        try {
            return jedis.zincrby(key,(double)score,member);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static Set<String> zrevrange(String key, int topn) {
        Jedis jedis = getJedis();
        try {
            return jedis.zrevrange(key,0,topn-1);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static double zscore(String key,String member) {
        Jedis jedis = getJedis();
        try {
            return jedis.zscore(key,member);
        } catch(Exception e) {
            return 0;
        } finally{
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static long zrevrank(String key,String member) {
        Jedis jedis = getJedis();
        try {
            return jedis.zrevrank(key,member);
        } catch(Exception e) {
            return -1;
        } finally{
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static Long zunionstore(String dstKey,String[] keys) {
        Jedis jedis = getJedis();
        try {
            return jedis.zunionstore(dstKey,keys);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static Long zcount(String dstKey,int min,int max) {
        Jedis jedis = getJedis();
        try {
            return jedis.zcount(dstKey,(double)min,(double)max);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void sadd(String key,String[] members) {
        Jedis jedis = getJedis();
        try {
            jedis.sadd(key,members);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void zadd(String key,int score,String member){
        Jedis jedis = getJedis();
        try {
            jedis.zadd(key,score,member);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    public static Set<String> smembers(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.smembers(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    public static void sremove(String key,String[] members) {
        Jedis jedis = getJedis();
        try {
            jedis.srem(key,members);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void incrby(String key,int count) {
        Jedis jedis = getJedis();
        try {
            jedis.incrBy(key,(long)count);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void expire(String key,int seconds) {
        Jedis jedis = getJedis();
        try {
            jedis.expire(key,seconds);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void hset(String key,String field,String value) {
        Jedis jedis = getJedis();
        try {
            jedis.hset(key, field, value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    public static void hsetnx(String key,String field,String value) {
        Jedis jedis = getJedis();
        try {
            jedis.hsetnx(key, field, value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    public static void hmset(String key,Map<String,String> value) {
        Jedis jedis = getJedis();
        try {
            jedis.hmset(key,value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static List<String> hmget(String key,String... fields) {
        Jedis jedis = getJedis();
        try {
            return jedis.hmget(key,fields);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static Map<String,String> hgetall(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.hgetAll(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static boolean hExists(String key,String field) {
        Jedis jedis = getJedis();
        try {
            return jedis.hexists(key,field);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static long hincrBy(String key,String field,int increment) {
        Jedis jedis = getJedis();
        try {
            return jedis.hincrBy(key,field,(long)increment);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 存储数据到缓存中，并制定过期时间和当Key存在时是否覆盖。
     *
     * @param key
     * @param value
     * @param nxxx
     *            nxxx的值只能取NX或者XX，如果取NX，则只有当key不存在是才进行set，如果取XX，则只有当key已经存在时才进行set
     *
     * @param expx expx的值只能取EX或者PX，代表数据过期时间的单位，EX代表秒，PX代表毫秒。
     * @param time 过期时间，单位是expx所代表的单位。
     * @return
     */
    public static String setAndTime(String key, String value, long time) {
        Jedis jedis = getJedis();
        try {
            return jedis.set(key,value,"NX","EX",time);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
    
    public static void batchDel(String preStr, int dbIndex) {
        Jedis jedis = getJedis();
        try {
            jedis.select(dbIndex);
            Set<String> set = jedis.keys(preStr + "*");
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                String keyStr = it.next();
                System.out.println(keyStr);
                jedis.del(keyStr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 返还到连接池
            if (jedis != null) {
                jedis.close();
            }
        }

    }
    
    public static void main(String[] args) {
        RedisUtils.batchDel("searchByArea_Australia",8);
    }

}
