package com.easymusic.service.impl;

import com.easymusic.entity.enums.PageSize;
import com.easymusic.entity.enums.UserIntegralRecordTypeEnum;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.po.UserIntegralRecord;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.entity.query.UserInfoQuery;
import com.easymusic.entity.query.UserIntegralRecordQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.exception.BusinessException;
import com.easymusic.mappers.UserInfoMapper;
import com.easymusic.mappers.UserIntegralRecordMapper;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.UserIntegralRecordService;
import com.easymusic.utils.StringTools;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * 用户积分记录信息 业务接口实现
 */
@Service("userIntegralRecordService")
@RequiredArgsConstructor
@Slf4j
public class UserIntegralRecordServiceImpl implements UserIntegralRecordService {

    private final UserIntegralRecordMapper<UserIntegralRecord, UserIntegralRecordQuery> userIntegralRecordMapper;

    private final UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    private final RedissonClient redissonClient;

    private final RedisComponent redisComponent;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserIntegralRecord> findListByParam(UserIntegralRecordQuery param) {
        return this.userIntegralRecordMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserIntegralRecordQuery param) {
        return this.userIntegralRecordMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserIntegralRecord> findListByPage(UserIntegralRecordQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserIntegralRecord> list = this.findListByParam(param);
        PaginationResultVO<UserIntegralRecord> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserIntegralRecord bean) {
        return this.userIntegralRecordMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserIntegralRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userIntegralRecordMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserIntegralRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userIntegralRecordMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserIntegralRecord bean, UserIntegralRecordQuery param) {
        StringTools.checkParam(param);
        return this.userIntegralRecordMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserIntegralRecordQuery param) {
        StringTools.checkParam(param);
        return this.userIntegralRecordMapper.deleteByParam(param);
    }

    /**
     * 根据RecordId获取对象
     */
    @Override
    public UserIntegralRecord getUserIntegralRecordByRecordId(Integer recordId) {
        return this.userIntegralRecordMapper.selectByRecordId(recordId);
    }

    /**
     * 根据RecordId修改
     */
    @Override
    public Integer updateUserIntegralRecordByRecordId(UserIntegralRecord bean, Integer recordId) {
        return this.userIntegralRecordMapper.updateByRecordId(bean, recordId);
    }

    /**
     * 根据RecordId删除
     */
    @Override
    public Integer deleteUserIntegralRecordByRecordId(Integer recordId) {
        return this.userIntegralRecordMapper.deleteByRecordId(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserIntegral(UserIntegralRecordTypeEnum recordTypeEnum, String businessId, String userId,
                                   Integer changeIntegral, BigDecimal amount) {
        // 使用分布式锁防止并发修改用户积分（基于用户ID加锁）
        String lockKey = "lock:user:integral:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，等待3秒，锁定30秒
            if (lock.tryLock(3, 30, java.util.concurrent.TimeUnit.SECONDS)) {
                try {
                    Integer updateCount = this.userInfoMapper.changeUserIntegral(userId, changeIntegral);
                    if (updateCount == 0) {
                        throw new BusinessException("用户积分不足");
                    }
                    UserIntegralRecord records = new UserIntegralRecord();
                    records.setChangeIntegral(changeIntegral);
                    records.setUserId(userId);
                    records.setCreateTime(new Date());
                    records.setBusinessId(businessId);
                    records.setRecordType(recordTypeEnum.getType());
                    records.setAmount(amount);

                    try {
                        this.userIntegralRecordMapper.insert(records);
                    } catch (org.springframework.dao.DuplicateKeyException e) {
                        log.warn("积分记录已存在, businessId={}, userId={}", businessId, userId, e);
                        // 如果是幂等性场景（如支付回调），可以忽略重复记录
                        // 否则抛出异常
                        throw new BusinessException("积分记录已存在");
                    }
                    
                    // 清除用户信息缓存，确保前端能获取到最新的积分
                    redisComponent.deleteUserInfo(userId);
                } finally {
                    // 释放锁
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取锁被中断");
        }
    }
}