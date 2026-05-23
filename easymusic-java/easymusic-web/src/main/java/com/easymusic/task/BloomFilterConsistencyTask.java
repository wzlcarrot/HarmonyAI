package com.easymusic.task;

import com.easymusic.entity.constants.Constants;
import com.easymusic.redis.BloomFilterComponent;
import com.easymusic.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 布隆过滤器一致性校准任务
 * 每天凌晨2点扫描Redis缓存，将数据补齐到布隆过滤器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BloomFilterConsistencyTask {

    private final BloomFilterComponent bloomFilterComponent;
    private final RedisUtils<?> redisUtils;

    @Resource(name = "bloomFilterLoaderExecutor")
    private ThreadPoolExecutor executor;

    /**
     * 每天凌晨2:00执行数据一致性校准
     * 使用异步方式执行，不阻塞定时任务线程
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncAllToBloomFilter() {
        log.info("开始执行布隆过滤器一致性校准任务...");
        
        // 异步执行，不阻塞定时任务线程
        executor.execute(() -> {
            try {
                syncMusicToBloom();
                syncUserToBloom();
                log.info("布隆过滤器一致性校准任务完成");
            } catch (Exception e) {
                log.error("布隆过滤器一致性校准任务执行异常", e);
            }
        });
    }

    /**
     * 同步音乐数据到布隆过滤器
     */
    private void syncMusicToBloom() {
        try {
            String pattern = Constants.REDIS_KEY_MUSIC_INFO + "*";
            Set<String> keys = redisUtils.scan(pattern, 1000);
            
            if (keys == null || keys.isEmpty()) {
                log.info("Redis中没有音乐缓存数据");
                return;
            }

            for (String key : keys) {
                String id = key.substring(Constants.REDIS_KEY_MUSIC_INFO.length());
                if (id != null && !id.isEmpty()) {
                    try {
                        if (!bloomFilterComponent.mightContainMusic(id)) {
                            bloomFilterComponent.addMusic(id);
                        }
                    } catch (Exception e) {
                        log.warn("处理音乐数据失败，id={}", id, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描Redis音乐缓存失败", e);
        }
    }

    /**
     * 同步用户数据到布隆过滤器
     */
    private void syncUserToBloom() {
        try {
            String pattern = Constants.REDIS_KEY_USER_INFO + "*";
            Set<String> keys = redisUtils.scan(pattern, 1000);
            
            if (keys == null || keys.isEmpty()) {
                log.info("Redis中没有用户缓存数据");
                return;
            }

            for (String key : keys) {
                String id = key.substring(Constants.REDIS_KEY_USER_INFO.length());
                if (id != null && !id.isEmpty()) {
                    try {
                        if (!bloomFilterComponent.mightContainUser(id)) {
                            bloomFilterComponent.addUser(id);
                        }
                    } catch (Exception e) {
                        log.warn("处理用户数据失败，id={}", id, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描Redis用户缓存失败", e);
        }
    }
}

