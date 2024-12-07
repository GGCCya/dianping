package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    //线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR=
            Executors.newFixedThreadPool(10);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient client;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = queryWithPassThrough(id);
        Shop shop = client.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class,
                this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //逻辑过期解决缓存击穿
//        Shop shop = client.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("商户不存在");
        }
        //返回信息
        return Result.ok(shop);
    }
//    //缓存击穿
//    public Shop queryWithLogicalExpire(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //在redis中查询商户
//        String redisDataJson = stringRedisTemplate.opsForValue().get(key);
//        //未命中则返回null
//        if (StrUtil.isBlank(redisDataJson)){
//            return null;
//        }
//        //命中,将json反序列为对象
//        RedisData redisData = JSONUtil.toBean(redisDataJson, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        //判断缓存逻辑是否过期
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //未过期,直接返回shop
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            return shop;
//        }
//        //已过期，缓存重建
//        String lockKey = LOCK_SHOP_KEY+id;
//        //获取锁
//        boolean isLock = trylock(lockKey);
//        //获取到锁，开启独立线程，实现缓存重建
//        if (isLock) {
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    saveShopToRedis(id,20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    //释放锁
//                    unlock(lockKey);
//                }
//            });
//        }
//
//        //返回信息
//        return shop;
//    }
    //互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //在redis中查询商户
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //存在，则返回
        if (StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //如果redis中为""空字符串,返回失败
        if (shopJson != null){
            return null;
        }
        //缓存重建
        //获取锁
        String lockKey = "lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLocked = trylock(lockKey);
            //判断是否获取
            if (!isLocked) {
                //没获取，重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //获取，开始重建
            //获取锁后判断缓存是否能命中，即之前获取锁的进程是否修改redis
            String s = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(s)){
                shop = JSONUtil.toBean(s, Shop.class);
                return shop;
            }
            //如果redis中为""空字符串,返回失败
            if (s != null){
                return null;
            }
            //不存在,在mysql数据库中查询
            shop = getById(id);
            //模拟延迟
//            Thread.sleep(200);
            //不存在，返回错误信息
            if (shop == null){
                //把空值写到redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //存在，将查询的数据放到redis中
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),
                    CACHE_SHOP_TTL,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //释放锁
            unlock(lockKey);
        }

        //返回信息
        return shop;
    }
    //缓存穿透
/*
    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        //在redis中查询商户
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //存在，则返回
        if (StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //如果redis中为""空字符串,返回失败
        if (shopJson != null){
            return null;
        }
        //不存在,在mysql数据库中查询
        Shop shop = getById(id);
        //不存在，返回错误信息
        if (shop == null){
            //把空值写到redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        //存在，将查询的数据放到redis中
        stringRedisTemplate.opsForValue()
        .set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //返回信息
        return shop;
    }
*/

    //将数据库查询到的shop放到redisData中并放到redis
    public void saveShopToRedis(Long id,Long expireSeconds) throws InterruptedException {
        //数据库中查询商户
        Shop shop = getById(id);
        Thread.sleep(200);
        //将商户信息和逻辑过期时间封装
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写到redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    //获取锁
    private boolean trylock(String key){
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    //释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null)return Result.fail("商户id为空");
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //如果x或y为空，直接在数据库中查询
        if (x == null||y==null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //查询的开始与结束
        int from = (current - 1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current*SystemConstants.DEFAULT_PAGE_SIZE;
        //到redis中获取商户的geo信息
        //GEOSEARCH key FROMLONLAT x y BYRADIUS 5 M WITHDIST
        String key = SHOP_GEO_KEY+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs
                                .newGeoSearchArgs()
                                .includeDistance()
                                .limit(end)
                );
        if (results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size()<=from){
            return Result.ok(Collections.emptyList());
        }
        //获取商户id列表
        List<Long> ids = new ArrayList<>(list.size());
        //获取商户id对应的距离
        Map<String,Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result->{
            String idStr = result.getContent().getName();
            ids.add(Long.valueOf(idStr));
            Distance distance = result.getDistance();
            distanceMap.put(idStr,distance);
        });
        //数据库中查询商户
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")").list();
        //为每个shop添加距离
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }
}
