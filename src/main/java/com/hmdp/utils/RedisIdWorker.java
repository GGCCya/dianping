package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {     //12.5基于redis的   id生成器

    //开始时间戳
    private static final long BEGIN_TIMESTAMP=1711843200L; //初始的时间戳 //

    private StringRedisTemplate stringRedisTemplate;

    //序列号的位数
    private static final int COUNT_BITS = 32;   //定义一个常量就是方面以后要进行改动


    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //获取id
    public long nextId(String keyPrefix){
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        //生成序列号
        //获取当前日期，精准到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增长  这样达到的效果就是同一天下单，采用同一个key,不同的天的采用不同的Key
        long count = stringRedisTemplate.opsForValue().increment("incr:" + keyPrefix + ":" + date);
        //拼接并返回
        return timeStamp << COUNT_BITS | count;  //
    }

}
