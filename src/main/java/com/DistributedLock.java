package com;

import org.yaml.snakeyaml.events.Event;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.UUID;

/**
 * Created by sc on 2019/1/28.
 */
public class DistributedLock extends Thread{


    public String acquireLock(String lockName, long acquireTimeout, long lockTimeout) {
        String identifier = UUID.randomUUID().toString();
        ;//保证释放锁的时候是同一个持有锁的人
        String lockKey = "lock:" + lockName;
        int lockExpire = (int) (lockTimeout / 1000);
        Jedis jedis = null;
        try {
            jedis = JedisConnectionUtils.getJedis();
            long end = System.currentTimeMillis() + acquireTimeout;
            while (System.currentTimeMillis() < end) {
                if (jedis.setnx(lockKey, identifier) == 1) {//设置成功
                    jedis.expire(lockKey, lockExpire);//设置超时时间
                    return identifier;//获得锁成功
                }
                //-1表示没有设置过期时间
                if (jedis.ttl(lockKey) == -1) {
                    jedis.expire(lockKey, lockExpire);
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return null;
    }

    //用lua脚本释放锁
    public boolean releaseLockWithLua(String lockName, String indentifier) {
        System.out.println(lockName + "开始释放锁" + indentifier);
        Jedis jedis = JedisConnectionUtils.getJedis();
        String lockKey = "lock:" + lockName;
        String lua = "if redis.call(\"get\",KEYS[1])==ARGV[1] then " +
                "return redis.call(\"del\",KEYS[1])" +
                "else return 0 end";
        Long rs = (Long) jedis.eval(lua, 1, new String[]{lockKey, indentifier});
        System.out.println("释放锁结果："+rs);
        if (rs.intValue() > 0) {
            return true;
        }
        return false;
    }

    //代码释放锁
    public boolean releaseLock(String lockName, String identifier) {
        System.out.println(lockName + "开始释放锁：" + identifier);
        String lockkey="lock:"+lockName;
        Jedis jedis=null;
        boolean isRelease=false;
        try {
            jedis=JedisConnectionUtils.getJedis();
            while (true) {
                jedis.watch(lockkey);//WATCH 命令可以为 Redis 事务提供 check-and-set （CAS）行为。开启乐观锁
                //判短是否为一把锁
                if (identifier.equals(jedis.get(lockkey))) {
                    Transaction transaction = jedis.multi();
                    transaction.del(lockkey);
                    if(transaction.exec().isEmpty()){
                        continue;
                    }
                    isRelease=true;
                }
                jedis.unwatch();
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        System.out.println(lockName + "释放锁结果：" + isRelease);
        return isRelease;


    }

    @Override
    public void run() {
        while (true) {
            DistributedLock distributedLock = new DistributedLock();
            String rs = distributedLock.acquireLock("aaa",2000,5000);
            if(rs!=null){
                System.out.println(Thread.currentThread().getName()+"->成功获得锁"+rs);
                try {
                    Thread.sleep(1000);
                    //distributedLock.releaseLock("aaa", rs);
                    distributedLock.releaseLockWithLua("aaa",rs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                }
                break;
            }
        }
    }

    public static void main(String[] args) {
        DistributedLock distributedLock=new DistributedLock();
        for (int i = 0; i < 10; i++) {
            new Thread(distributedLock, "tName" + i).start();
        }
    }
}
