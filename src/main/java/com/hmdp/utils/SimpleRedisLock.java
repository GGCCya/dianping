package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.apache.ibatis.io.Resources;
//import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    //模块名，与锁前缀拼接后就作为redis中的key
    private String name;
    //要注意的是不能把锁的名字给写死，因为我们想要的是不同业务有不同的名字的锁


    private StringRedisTemplate stringRedisTemplate;


    //这里写一个构造函数 ，用于实现不同的锁
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //redis中锁的前缀
    private static final String KEY_PREFIX = "lock:";

    //锁前缀
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    //Redis脚本执行器 RedisScript 这里使用它的实现类
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    //脚本初始化
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    @Override
    public boolean tryLock(long timeoutSec) {
        //要过去线程的id来作为标识。
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);

    //这样做，避免了空指针的可能性。
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }


//    @Override
//    public void unlock() {
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        if (threadId.equals(id)){
//            stringRedisTemplate.delete(KEY_PREFIX+name);
//        }
//    }
}
