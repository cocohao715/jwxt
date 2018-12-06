package com.campus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConifg {
    @Value("spring.redis.pool.max-active")
    private int  maxActive;
    @Value("spring.redis.pool.max-wait")
    private int  maxWait;
    @Value("spring.redis.pool.min-idle")
    private int  minIdle;
    @Value("spring.redis.pool.max-idle")
    private int  maxIdle;
    @Value("spring.redis.pool.max-total")
    private int  maxTotal;
    @Value("spring.redis.timeout")
    private int   timeout;
    @Value("spring.redis.password")
    private String password;
    @Value("spring.redis.host")
    private String  host;
    @Value("spring.redis.port")
    private int   port;
    @Value("spring.redis.database")
    private int   database;

    @Bean
    public JedisPool redisPoolFactory()
    {
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxWaitMillis(maxWait);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
        jedisPoolConfig.setNumTestsPerEvictionRun(10);
        jedisPoolConfig.setMinEvictableIdleTimeMillis(60000);
        JedisPool jedisPool=new JedisPool(jedisPoolConfig,host,port,timeout);
        return jedisPool;
    }
}
