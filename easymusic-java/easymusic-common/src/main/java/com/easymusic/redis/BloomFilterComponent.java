package com.easymusic.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 布隆过滤器工具类
 * 用于防止缓存穿透
 * 
 * 工作原理：
 * 1. 布隆过滤器是一个概率型数据结构，用于快速判断元素是否"可能存在"
 * 2. 如果布隆过滤器返回false，说明元素一定不存在，可以直接返回，避免查数据库
 * 3. 如果布隆过滤器返回true，说明元素可能存在，需要进一步查询缓存或数据库
 * 4. 布隆过滤器有误判率（false positive），但不会漏判（false negative）
 * 
 * 使用场景：
 * - 防止恶意请求不存在的ID导致频繁查询数据库
 * - 配合Redis缓存使用，形成：布隆过滤器 -> 缓存 -> 数据库 的三层防护
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BloomFilterComponent {

    private final RedissonClient redissonClient;

    private RBloomFilter<String> musicBloomFilter;
    private RBloomFilter<String> userBloomFilter;

    /**
     * 初始化布隆过滤器
     * expectedInsertions: 预期插入数量（根据业务估算）
     * falseProbability: 误判率（0.01表示1%的误判率）
     * 
     * 注意：布隆过滤器一旦初始化，容量和误判率就固定了，不能修改
     */
    //布隆过滤器就是就查一次的要么存在要么不存在，多次查的可能存在。
    @PostConstruct
    public void initBloomFilters() {
        // 音乐布隆过滤器：预期100万首音乐，误判率1%
        musicBloomFilter = redissonClient.getBloomFilter("bloomfilter:music");
        if (!musicBloomFilter.isExists()) {
            musicBloomFilter.tryInit(1000000L, 0.01);
            log.info("音乐布隆过滤器初始化成功，容量：100万，误判率：1%");
        } else {
            log.info("音乐布隆过滤器已存在，直接使用");
        }

        // 用户布隆过滤器：预期10万用户，误判率1%
        userBloomFilter = redissonClient.getBloomFilter("bloomfilter:user");
        if (!userBloomFilter.isExists()) {
            userBloomFilter.tryInit(100000L, 0.01);
            log.info("用户布隆过滤器初始化成功，容量：10万，误判率：1%");
        } else {
            log.info("用户布隆过滤器已存在，直接使用");
        }
    }

    /**
     * 判断音乐ID是否可能存在
     * @param musicId 音乐ID
     * @return true-可能存在，false-一定不存在
     */
    public boolean mightContainMusic(String musicId) {
        if (musicId == null) {
            return false;
        }
        return musicBloomFilter.contains(musicId);
    }

    /**
     * 将音乐ID添加到布隆过滤器
     * 注意：即使数据不存在，也会加入布隆过滤器，防止重复查询
     */
    public void addMusic(String musicId) {
        if (musicId != null) {
            musicBloomFilter.add(musicId);
        }
    }

    /**
     * 判断用户ID是否可能存在
     */
    public boolean mightContainUser(String userId) {
        if (userId == null) {
            return false;
        }
        return userBloomFilter.contains(userId);
    }

    /**
     * 将用户ID添加到布隆过滤器
     */
    public void addUser(String userId) {
        if (userId != null) {
            userBloomFilter.add(userId);
        }
    }
}

