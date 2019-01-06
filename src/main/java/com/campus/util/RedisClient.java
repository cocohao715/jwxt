package com.campus.util;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;


@Component
public class RedisClient<T> {

    @Autowired
    private JedisPool jedisPool;
    private Logger logger = LoggerFactory.getLogger(RedisClient.class);
    public void set(String key, String value) throws Exception {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.set(key, value);
        } finally {
            //返还到连接池
            jedis.close();
        }
    }
    public void setEx(String key,int time,T value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.setex(key.getBytes(),time,SerializeUtil.serialize(value));
        }
        catch (Exception e)
        {
            logger.info(e.toString());
        }
        finally {
            //返还到连接池
            jedis.close();
        }
    }
    public Object get(String key)  {

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[] value=jedis.get(key.getBytes());
            return SerializeUtil.unserizlize(value);
        }
        catch (Exception e)
        {
            logger.info(e.toString());
            return null;
        }
        finally {
            //返还到连接池
            jedis.close();
        }
    }

    public void setobj(String key, T value) throws Exception {
        Jedis jedis = null;
        try {
            Set<T> set = new HashSet<T>();
            set.add(value);
            jedis = jedisPool.getResource();
            jedis.sadd(key, String.valueOf(set));
        } finally {
            //返还到连接池
            jedis.close();
        }
    }

    /**
     * Push(存入 数据到队列中)
     * @param key
     * @param value
     * @param <V>
     */
    public <V> void Push(String key, V value) {
        String json = JSON.toJSONString(value);
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.lpush(key, json);
        } catch (Exception e) {
            if (jedis != null) {
                //jedisPool.returnBrokenResource(jedis);
                jedis.close();
            }
        } finally {
            //返还到连接池
            jedis.close();
        }
    }

    /**
     * pop(从队列中取值)
     * @param key
     * @param <V>
     * @return
     */
    public  <V> V Pop(String key) {
        String value = "";
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            value = jedis.rpop(key);
        }
        finally {
            //返还到连接池
            jedis.close();
        }
        return (V) value;
    }

    /**
     * 模糊查找键
     * @param key
     * @return
     */
    public  Set<String> Keys(String key) {
        Set<String> value;
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            value = jedis.keys(key);
        }
        finally {
            //返还到连接池
            jedis.close();
        }
        return value;
    }



    public void delKey(String key)  {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
             jedis.del(key.getBytes());
        } finally {
            //返还到连接池
            jedis.close();
        }
    }
}