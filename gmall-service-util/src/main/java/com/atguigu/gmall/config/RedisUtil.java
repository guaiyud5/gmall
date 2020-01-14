package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    //创建连接池
    private JedisPool jedisPool;

    //初始化连接池
    public void initJedisPool(String host,int port,int database){
        //直接创建一个连接池的配置类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置连接池最大数
        jedisPoolConfig.setMaxTotal(200);
        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //设置最小剩余数
        jedisPoolConfig.setMaxIdle(10);
        //开启缓冲池
        jedisPoolConfig.setBlockWhenExhausted(true);
        //当用户获取到一个连接池后，自检是否可以使用
        jedisPoolConfig.setTestOnBorrow(true);
        //连接池配置类
        jedisPool = new JedisPool(jedisPoolConfig,host,port,20*1000);
    }

    //获取jedis
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }
}
