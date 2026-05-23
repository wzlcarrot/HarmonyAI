package com.easymusic.redis;

import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.MusicTaskDTO;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.PayOrderInfo;
import com.easymusic.entity.po.SysDict;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.vo.PaginationResultVO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisComponent {

    private final RedisUtils redisUtils;

    //把验证码保存到redis中
    public String saveCode(String code){
        String checkCodeKey = UUID.randomUUID().toString();

        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey, code,Constants.REDIS_KEY_EXPIRES_ONE_MIN);

        return checkCodeKey;
    }

    //获取code
    public String getCheckCode(String checkCodeKey){
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    //删除code
    public void cleanCheckCode(String checkCodeKey){
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public void saveTokenUserInfoDTO(TokenUserInfoDTO tokenUserInfoDTO) {
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB_USER+tokenUserInfoDTO.getToken(), tokenUserInfoDTO,Constants.REDIS_KEY_EXPIRES_DAY);
    }

    public TokenUserInfoDTO getTokenUserInfoDTO(String token) {
        return (TokenUserInfoDTO) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB_USER+token);
    }


    public TokenUserInfoDTO getTokenInfo(String token){
        return (TokenUserInfoDTO) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB_USER+token);
    }


    public void cleanTokenInfo(String token){
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB_USER+token);
    }


    public void addOrder2DelayQueue(Integer delayMin, String orderId) {
        long executeTime = System.currentTimeMillis() + delayMin * 60 * 1000;
        redisUtils.zsetAdd(Constants.REDIS_KEY_ORDER_DELAY_QUEUE, orderId, executeTime);
    }

    public Set<String> getTimeOutOrder() {
        return redisUtils.zsetRangeByScore(Constants.REDIS_KEY_ORDER_DELAY_QUEUE, 0, System.currentTimeMillis());
    }

    public Long removeTimeOutOrder(String orderId) {
        return redisUtils.zsetAddRemove(Constants.REDIS_KEY_ORDER_DELAY_QUEUE, orderId);
    }

    /**
     * 添加音乐创建任务到延迟队列
     * 设置30秒后执行查询（给AI API一些时间开始生成音乐）
     */
    public void addMusicCreateTask(MusicTaskDTO musicTaskDto) {
        long executeTime = System.currentTimeMillis() + 30 * 1000;  // 30秒后的时间戳
        //key是一样的，通过score来排序值。
        redisUtils.zsetAdd(Constants.REDIS_KEY_MUSIC_CREATE_QUEUE, musicTaskDto, executeTime);
    }

    /**
     * 获取到期任务（已经等待了30秒的任务）
     * 查询条件：任务的执行时间 <= 当前时间（即已经到期了）
     */
    public Set<MusicTaskDTO> getMusicTaskDto() {
        return redisUtils.zsetRangeByScore(Constants.REDIS_KEY_MUSIC_CREATE_QUEUE, 0, System.currentTimeMillis());
    }

    //从队列中移除已经完成的任务
    public Long removeMusicTaskDto(MusicTaskDTO taskDto) {
        return redisUtils.zsetAddRemove(Constants.REDIS_KEY_MUSIC_CREATE_QUEUE, taskDto);
    }


    /**
     * 获取某个父编码下的字典子项列表
     *
     * 读取顺序：Redis -> （DB 由上层 SysDictServiceImpl 回源并调用 saveDict）
     */
    public List<SysDict> getDictSubList(String dictPcode) {
        if (dictPcode == null) {
            return Collections.emptyList();
        }

        // 从 Redis 读取
        List<SysDict> list = (List<SysDict>) redisUtils.hget(Constants.REDIS_KEY_SYS_DICT, dictPcode);
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 将某个父编码下的字典列表写入 Redis
     */
    public void saveDict(String dictPcode, List<SysDict> sysDictList) {
        redisUtils.hset(Constants.REDIS_KEY_SYS_DICT, dictPcode, sysDictList);
    }

    /**
     * ================== 音乐列表缓存（Redis） ==================
     */

    /**
     * 获取推荐音乐列表：Redis -> 空集合
     */
    @SuppressWarnings("unchecked")
    public List<MusicInfo> getCommendMusicList(String cacheKey) {
        if (cacheKey == null) {
            return Collections.emptyList();
        }
        // 从 Redis 读取
        Object redisVal = redisUtils.get(cacheKey);
        if (redisVal instanceof List) {
            List<MusicInfo> list = (List<MusicInfo>) redisVal;
            if (list != null && !list.isEmpty()) {
                return list;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 存入Redis中
     */
    public void saveCommendMusicList(String cacheKey, List<MusicInfo> list, Integer expireSeconds) {
        if (cacheKey == null || list == null) {
            return;
        }
        int ttl = expireSeconds != null ? expireSeconds : Constants.REDIS_KEY_EXPIRES_ONE_MIN;
        redisUtils.setex(cacheKey, list, ttl);
    }

    /**
     * 获取最新音乐分页数据：Redis -> null
     */
    @SuppressWarnings("unchecked")
    public PaginationResultVO<MusicInfo> getLatestMusicPage(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        // 从 Redis 读取
        Object redisVal = redisUtils.get(cacheKey);
        if (redisVal instanceof PaginationResultVO) {
            PaginationResultVO<MusicInfo> page = (PaginationResultVO<MusicInfo>) redisVal;
            if (page != null && page.getList() != null && !page.getList().isEmpty()) {
                return page;
            }
        }
        return null;
    }

    /**
     * 存入Redis中
     */
    public void saveLatestMusicPage(String cacheKey, PaginationResultVO<MusicInfo> page, Integer expireSeconds) {
        if (cacheKey == null || page == null) {
            return;
        }
        int ttl = expireSeconds != null ? expireSeconds : Constants.REDIS_KEY_EXPIRES_ONE_MIN;
        redisUtils.setex(cacheKey, page, ttl);
    }

    /**
     * 保存支付二维码到Redis
     * @param orderId 订单ID
     * @param qrCodeUrl 二维码URL
     * @param expireSeconds 过期时间（秒）
     */
    public void saveQrCode(String orderId, String qrCodeUrl, Integer expireSeconds) {
        String key = Constants.REDIS_KEY_QR_CODE + orderId;
        redisUtils.setex(key, qrCodeUrl, expireSeconds);
    }

    /**
     * 获取支付二维码
     * @param orderId 订单ID
     * @return 二维码URL
     */
    public String getQrCode(String orderId) {
        String key = Constants.REDIS_KEY_QR_CODE + orderId;
        return (String) redisUtils.get(key);
    }

    /**
     * 删除支付二维码
     * @param orderId 订单ID
     */
    public void deleteQrCode(String orderId) {
        String key = Constants.REDIS_KEY_QR_CODE + orderId;
        redisUtils.delete(key);
    }



    /**
     * ================== 歌曲细节缓存（Redis + MySQL） ==================
     */

    /**
     * 获取歌曲细节信息
     *
     * 读取顺序：Redis -> （DB 由上层 MusicInfoServiceImpl 回源并调用 saveMusicInfo）
     *
     * @param musicId 音乐ID
     * @return 音乐信息
     */
    public MusicInfo getMusicInfo(String musicId) {
        if (musicId == null) {
            return null;
        }

        // 从 Redis 读取
        MusicInfo info = (MusicInfo) redisUtils.get(Constants.REDIS_KEY_MUSIC_INFO + musicId);
        return info; // 如果 Redis 也没有，返回 null，由上层从数据库查询
    }

    /**
     * 保存歌曲细节信息到 Redis
     *
     * @param musicId 音乐ID
     * @param musicInfo 音乐信息
     */
    public void saveMusicInfo(String musicId, MusicInfo musicInfo) {
        if (musicId == null || musicInfo == null) {
            return;
        }
        // 写入 Redis
        redisUtils.setex(Constants.REDIS_KEY_MUSIC_INFO + musicId, musicInfo,
            Constants.REDIS_KEY_EXPIRES_DAY);
    }

    /**
     * 删除歌曲细节信息缓存
     *
     * @param musicId 音乐ID
     */
    public void deleteMusicInfo(String musicId) {
        if (musicId == null) {
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_MUSIC_INFO + musicId);
    }

    /**
     * ================== 用户信息缓存（Redis + MySQL） ==================
     */

    /**
     * 获取用户信息
     *
     * 读取顺序：Redis -> （DB 由上层 UserInfoServiceImpl 回源并调用 saveUserInfo）
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfo getUserInfo(String userId) {
        if (userId == null) {
            return null;
        }

        // 从 Redis 读取
        UserInfo info = (UserInfo) redisUtils.get(Constants.REDIS_KEY_USER_INFO + userId);
        return info; // 如果 Redis 也没有，返回 null，由上层从数据库查询
    }

    /**
     * 保存用户信息到 Redis
     *
     * @param userId 用户ID
     * @param userInfo 用户信息
     */
    public void saveUserInfo(String userId, UserInfo userInfo) {
        if (userId == null || userInfo == null) {
            return;
        }
        // 写入 Redis
        redisUtils.setex(Constants.REDIS_KEY_USER_INFO + userId, userInfo,
            Constants.REDIS_KEY_EXPIRES_DAY);
    }

    /**
     * 删除用户信息缓存
     * @param userId 用户ID
     */
    public void deleteUserInfo(String userId) {
        if (userId == null) {
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_USER_INFO + userId);
    }

    /**
     * 缓存订单信息
     * @param orderId 订单ID
     * @param orderInfo 订单信息
     */
    public void saveOrderInfo(String orderId, PayOrderInfo orderInfo) {
        saveOrderInfo(orderId, orderInfo, Constants.REDIS_KEY_EXPIRES_DAY);
    }

    /**
     * 缓存订单信息（支持自定义过期时间）
     * @param orderId 订单ID
     * @param orderInfo 订单信息
     * @param expireSeconds 过期时间（秒）
     */
    public void saveOrderInfo(String orderId, PayOrderInfo orderInfo, Integer expireSeconds) {
        if (orderId != null && orderInfo != null) {
            redisUtils.setex(Constants.REDIS_KEY_ORDER_INFO + orderId, orderInfo, expireSeconds);
        }
    }

    /**
     * 获取缓存的订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    public PayOrderInfo getOrderInfo(String orderId) {
        if (orderId == null) {
            return null;
        }
        try {
            return (PayOrderInfo) redisUtils.get(Constants.REDIS_KEY_ORDER_INFO + orderId);
        } catch (Exception e) {
            // Redis异常时返回null，让调用方回源到数据库
            return null;
        }
    }

    /**
     * 删除订单信息缓存
     * @param orderId 订单ID
     */
    public void deleteOrderInfo(String orderId) {
        if (orderId == null) {
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_ORDER_INFO + orderId);
    }

    // ================== 用户待支付订单缓存 ==================

    /**
     * 用户待支付订单 Redis key 前缀
     * 这里直接写死完整前缀，格式为：easymusic:order:pending:user:{userId}
     */
    private static final String USER_PENDING_ORDER_KEY_PREFIX = "easymusic:order:pending:user:";

    /**
     * 缓存用户当前待支付订单 ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param expireSeconds 过期时间（秒）
     */
    public void saveUserPendingOrder(String userId, String orderId, int expireSeconds) {
        if (userId == null || orderId == null) {
            return;
        }
        String key = USER_PENDING_ORDER_KEY_PREFIX + userId;
        redisUtils.setex(key, orderId, expireSeconds);
    }

    /**
     * 获取用户当前待支付订单 ID
     * @param userId 用户ID
     * @return 订单ID，如果不存在则返回 null
     */
    public String getUserPendingOrder(String userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_PENDING_ORDER_KEY_PREFIX + userId;
        Object val = redisUtils.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    /**
     * 删除用户当前待支付订单缓存
     * @param userId 用户ID
     */
    public void deleteUserPendingOrder(String userId) {
        if (userId == null) {
            return;
        }
        String key = USER_PENDING_ORDER_KEY_PREFIX + userId;
        redisUtils.delete(key);
    }

    /**
     * 删除订单过期标记（支付成功时调用）
     * @param orderId 订单ID
     */
    public void deleteOrderExpireMark(String orderId) {
        String key = "easymusic:order:expire:" + orderId;
        redisUtils.delete(key);
    }

}
