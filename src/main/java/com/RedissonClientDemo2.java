package com;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

/**
 * Created by sc on 2019/1/26.
 * //必须有集群
 * 有两个进程同时启动来测试这个demo
 */
public class RedissonClientDemo2 {

    public static String lockKey = "testLockKey1";

    public static void main(String[] args) throws InterruptedException {
        Jedis jedis=JedisConnectionUtils.getJedis();
        Config config = new Config();
        //config.useClusterServers().addNodeAddress("redis://192.168.80.135:6379");集群方式
        config.useSingleServer().setAddress("redis://192.168.80.135:6379");
        RedissonClient redissonClient = Redisson.create(config);
        RLock rlock=null;
        for (int i = 0; i < 100; i++) {
            rlock=redissonClient.getLock(lockKey);
            rlock.lock(60, TimeUnit.SECONDS);
            int stock= Integer.parseInt(jedis.get("aaa").toString());
            if (stock > 0) {
                jedis.set("aaa", (stock - 1) + "");
                System.out.println("RedissonClientDemo:lockkey:" + lockKey + ",stock:" + (stock - 1) + "");
            }
            rlock.unlock();//释放锁
        }

    }
}
