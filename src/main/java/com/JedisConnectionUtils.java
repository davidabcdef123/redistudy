package com;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by sc on 2019/1/26.
 */
public class JedisConnectionUtils {

    private static JedisPool pool=null;

    static {
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(100);
        pool = new JedisPool(jedisPoolConfig,"192.168.80.135",6379);
    }

    public static Jedis getJedis(){return pool.getResource();}

    public static void main(String[] args) {
        System.out.println(JedisConnectionUtils.getJedis().get("aaa"));
    }
}
