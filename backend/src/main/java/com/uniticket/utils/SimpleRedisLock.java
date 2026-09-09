package com.uniticket.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class SimpleRedisLock implements Ilock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    // ID_PREFIX 区分不同JVM
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";
    // Lua脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    //静态代码块：只会在JVM初始化 类加载的时候执行一次
    //好处是每次订单不用重复执行脚本 加快IO流并节省内存
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String key = KEY_PREFIX + name;
        // value存入线程标识
        String value = ID_PREFIX + Thread.currentThread().getId();
        Boolean res = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(res);
    }

    @Override
    public void unlock() {
        // 判断锁和释放锁的非原子性误删锁 使用Lua脚本解决
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }
}

