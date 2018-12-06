package com.campus.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * redicache 工具类
 *
 */
@SuppressWarnings("unchecked")
@Component
public class RedisUtil {
    @Autowired
    JedisPool jedisPool;
   public Jedis getJedis()
   {
        return jedisPool.getResource();
   }

   public String set(String key,String value,long time)
   {
       jedisPool
   }
}