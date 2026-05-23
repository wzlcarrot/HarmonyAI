package com.easymusic.redis;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

//Redis 操作工具类
@Component("redisUtils")
@RequiredArgsConstructor
public class RedisUtils<V> {

    private final RedisTemplate<String, V> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    /**
     * 随机数生成器（用于缓存过期时间随机化，防止缓存雪崩）
     */
    private static final Random RANDOM = new Random();

    //删除缓存
    public void delete(String ...key) {
        if (key != null && key.length > 0) {
            redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
        }
    }

    //取出对应键的值
    public V get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    //缓存放入
    public boolean set(String key, V value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     * 
     * 过期时间随机化：基础时间 ± 20%，防止缓存雪崩
     * 例如：86400秒（1天）→ 实际过期时间在 69120-103680秒 之间随机
     *
     * @param key   键
     * @param value 值
     * @param time  基础时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean setex(String key, V value, long time) {
        try {
            if (time > 0) {
                // 过期时间随机化：基础时间 ± 20%，防止大量缓存同时过期导致缓存雪崩
                long randomRange = (long) (time * 0.2);
                long offset = RANDOM.nextInt((int) (randomRange * 2)) - randomRange; // [-range, +range]
                long finalExpire = Math.max(time + offset, time / 2); // 确保不会太短
                
                redisTemplate.opsForValue().set(key, value, finalExpire, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * hash 相关操作
     键是key,值是hashKey,值是value
     */
    //缓存hash数据
    public void hset(String key, String hashKey, V value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    //取出对应键的值
    public V hget(String key, String hashKey) {
        return (V) redisTemplate.opsForHash().get(key, hashKey);
    }

    //取出对应键的值，也就是去除哈希表
    public Map<String, V> entries(String key) {
        return (Map) redisTemplate.opsForHash().entries(key);
    }

    /**
     * set 关操作
     */

    //添加带优先级或时间戳的任务到队列,score试过用来排序的依据
    public void zsetAdd(String key, V value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }
    //任务完成后，取出队列中的任务
    public Set<V> zsetRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    public Long zsetAddRemove(String key, V v) {
        return redisTemplate.opsForZSet().remove(key, v);
    }

    /**
     * 扫描匹配pattern的key（分页）
     * 
     * @param pattern key模式，如 "easymusic:music:info:*"
     * @param count 每次扫描的数量
     * @return 匹配的key集合
     */
    @SuppressWarnings("unchecked")
    public Set<String> scan(String pattern, long count) {
        Set<String> keys = new HashSet<>();
        try {
            Set<String> result = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Set<String>>) connection -> {
                Set<String> scanResult = new HashSet<>();
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern)
                        .count(count)
                        .build()
                );
                while (cursor.hasNext()) {
                    scanResult.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                }
                cursor.close();
                return scanResult;
            });
            if (result != null) {
                keys = result;
            }
        } catch (Exception e) {
            logger.error("扫描Redis key失败，pattern={}", pattern, e);
        }
        return keys;
    }

}