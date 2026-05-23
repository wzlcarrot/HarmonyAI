# 分布式锁学习文档：从Redis到Redisson

## 目录
1. [为什么需要分布式锁](#为什么需要分布式锁)
2. [Redis分布式锁的实现](#redis分布式锁的实现)
3. [Redis分布式锁的问题](#redis分布式锁的问题)
4. [Redisson解决方案](#redisson解决方案)
5. [项目中的实际应用](#项目中的实际应用)
6. [面试常见问题](#面试常见问题)

---

## 为什么需要分布式锁

### 1.1 单机锁的局限性

在单机环境下，我们可以使用 `synchronized` 或 `ReentrantLock` 来保证线程安全：

```java
// 单机锁示例
private final Object lock = new Object();

public void updateStock() {
    synchronized (lock) {
        // 临界区代码
        int stock = getStock();
        if (stock > 0) {
            setStock(stock - 1);
        }
    }
}
```

但在分布式系统中，多个服务实例运行在不同的JVM中，单机锁无法跨进程工作。

### 1.2 分布式锁的应用场景

- **防止重复操作**：支付回调幂等性、防止重复下单
- **缓存击穿防护**：防止大量请求同时查询数据库
- **资源竞争**：控制并发数、防止超卖
- **状态更新**：保证状态更新的原子性

---

## Redis分布式锁的实现

### 2.1 基础实现（SETNX）

最简单的实现方式：

```java
// 伪代码
public boolean tryLock(String key, String value, int expireTime) {
    // SET key value NX EX expireTime
    // NX: 只有当key不存在时才设置
    // EX: 设置过期时间（秒）
    return redis.setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
}

public void unlock(String key, String value) {
    // 先判断value是否匹配，再删除
    if (redis.get(key).equals(value)) {
        redis.delete(key);
    }
}
```

### 2.2 为什么需要value？

使用唯一value（如UUID）的原因：
- **防止误删**：如果线程A获取锁后执行时间过长，锁过期了，线程B获取了锁。此时线程A执行完毕，如果直接删除key，会误删线程B的锁。
key 是锁的名称，value是锁的持有者。
```java
// 错误示例：可能误删其他线程的锁
public void unlock(String key) {
    redis.delete(key);  // ❌ 危险！
}

// 正确示例：先判断value再删除
public void unlock(String key, String value) {
    if (redis.get(key).equals(value)) {
        redis.delete(key);  // ✅ 安全
    }
}
    ```
    ### 2.3 Lua脚本保证原子性
    删除锁的操作需要保证原子性，使用Lua脚本：
    我感觉就是一条，，就是把判断key和value的操作打包成原子性。
    ```lua
    -- Lua脚本：先判断value，再删除
    if redis.call("get", KEYS[1]) == ARGV[1] then
        return redis.call("del", KEYS[1])
    else
        return 0
    end
    ```
    ---

## Redis分布式锁的问题

### 3.1 问题1：锁过期时间设置困难

**问题描述**：
- 如果过期时间设置太短，业务还没执行完，锁就过期了
- 如果过期时间设置太长，线程异常退出时，锁释放不及时

**场景示例**：
```
线程A获取锁，设置过期时间10秒
线程A执行业务逻辑（需要15秒）
10秒后，锁过期
线程B获取锁
15秒后，线程A执行完毕，释放锁（误删了线程B的锁）
```

### 3.2 问题2：主从切换导致锁丢失

**问题描述**：
在Redis主从架构中，如果主节点宕机，从节点升级为主节点，但主从同步是异步的，可能导致锁丢失。

**场景示例**：
```
1. 线程A在主节点获取锁成功
2. 主节点宕机，从节点升级为主节点
3. 主从同步延迟，新主节点上没有这个锁
4. 线程B在新主节点获取锁成功（与线程A同时持有锁）
```

### 3.3 问题3：可重入性

**问题描述**：
同一个线程需要多次获取同一把锁时，需要支持可重入。

**场景示例**：
```java
public void methodA() {
    lock.lock();
    try {
        methodB();  // 需要再次获取同一把锁
    } finally {
        lock.unlock();
    }
}

public void methodB() {
    lock.lock();  // 如果是不可重入锁，这里会死锁
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
}
```

### 3.4 问题4：锁续期（Watch Dog）

**问题描述**：
业务执行时间不确定，需要自动延长锁的过期时间。

---

## Redisson解决方案

### 4.1 Redisson简介

Redisson是一个在Redis基础上实现的Java驻内存数据网格（In-Memory Data Grid），提供了丰富的分布式对象和服务，包括：
- 分布式锁（RLock）
- 分布式集合
- 分布式对象
- 分布式服务

### 4.2 Redisson分布式锁特性

#### 4.2.1 自动续期（Watch Dog机制）

Redisson的`RLock`实现了自动续期机制：

```java
RLock lock = redissonClient.getLock("myLock");

// tryLock(waitTime, leaseTime, timeUnit)
// waitTime: 等待获取锁的时间
// leaseTime: 锁的持有时间，-1表示使用Watch Dog自动续期
lock.tryLock(3, -1, TimeUnit.SECONDS);
```

**Watch Dog工作原理**：
1. 获取锁成功后，启动一个后台线程（Watch Dog）
2. Watch Dog每隔 `leaseTime / 3` 时间检查一次
3. 如果锁还在当前线程持有，自动续期 `leaseTime` 时间
4. 线程释放锁时，停止Watch Dog

#### 4.2.2 可重入锁

Redisson的`RLock`实现了可重入：

```java
RLock lock = redissonClient.getLock("myLock");

public void methodA() {
    lock.lock();
    try {
        methodB();  // ✅ 可以再次获取同一把锁
    } finally {
        lock.unlock();
    }
}

public void methodB() {
    lock.lock();  // ✅ 可重入，不会死锁
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
}
```

**实现原理**：
- 使用Hash结构存储：`key: {field: threadId, value: count}`
- 同一线程获取锁时，count+1
- 释放锁时，count-1，count为0时删除key

#### 4.2.3 公平锁

```java
// 公平锁：按照请求顺序获取锁
RLock fairLock = redissonClient.getFairLock("myLock");
fairLock.lock();
```

#### 4.2.4 读写锁

```java
// 读锁：多个线程可以同时持有
RReadWriteLock rwLock = redissonClient.getReadWriteLock("myLock");
RLock readLock = rwLock.readLock();

// 写锁：独占锁
RLock writeLock = rwLock.writeLock();
```

#### 4.2.5 联锁（MultiLock）

```java
// 同时获取多把锁，全部成功才算成功
RLock lock1 = redissonClient.getLock("lock1");
RLock lock2 = redissonClient.getLock("lock2");
RLock lock3 = redissonClient.getLock("lock3");

RLock multiLock = redissonClient.getMultiLock(lock1, lock2, lock3);
multiLock.lock();
```

### 4.3 Redisson锁的底层实现

#### 4.3.1 Lua脚本加锁

```lua
-- Redisson加锁Lua脚本（简化版）
if (redis.call('exists', KEYS[1]) == 0) then
    redis.call('hset', KEYS[1], ARGV[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end;
if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then
    redis.call('hincrby', KEYS[1], ARGV[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end;
return redis.call('pttl', KEYS[1]);
```

**说明**：
- `KEYS[1]`：锁的key
- `ARGV[1]`：锁的过期时间（毫秒）
- `ARGV[2]`：线程ID（用于可重入）
- 使用Hash结构：`key: {threadId: count}`

#### 4.3.2 Lua脚本释放锁

```lua
-- Redisson释放锁Lua脚本（简化版）
if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then
    return nil;
end;
local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1);
if (counter > 0) then
    redis.call('pexpire', KEYS[1], ARGV[2]);
    return 0;
else
    redis.call('del', KEYS[1]);
    redis.call('publish', KEYS[2], ARGV[1]);
    return 1;
end;
```

---

## 项目中的实际应用

### 5.1 场景1：支付回调幂等性

**位置**：`AliPayNotifyController.processAlipayNotification()`

**问题**：支付宝可能多次发送支付回调，需要保证订单状态只更新一次。

**实现**：

```java
// 使用分布式锁保证支付回调的幂等性（基于订单号加锁）
String lockKey = "lock:pay:notify:" + out_trade_no;
RLock lock = redissonClient.getLock(lockKey);

try {
    // 尝试获取锁，等待3秒，锁定30秒
    if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        try {
            // 检查订单是否存在
            PayOrderInfo existingOrder = payOrderInfoService.getPayOrderInfoByOrderId(out_trade_no);
            if (existingOrder == null) {
                return "fail";
            }

            // 幂等性检查：如果订单已经是支付状态，则直接返回成功
            if (PayOrderStatusEnum.HAVE_PAY.getStatus().equals(existingOrder.getStatus())) {
                log.info("订单 {} 已经处理过，无需重复处理", out_trade_no);
                return "success";
            }

            // 更新订单状态
            PayOrderInfo payOrderInfo = new PayOrderInfo();
            payOrderInfo.setStatus(PayOrderStatusEnum.HAVE_PAY.getStatus());
            payOrderInfo.setChannelOrderId(trade_no);
            payOrderInfo.setPayTime(new Date());
            payOrderInfoService.updatePayOrderInfoByOrderId(payOrderInfo, out_trade_no);
            
            // 更新积分
            userIntegralRecordService.changeUserIntegral(...);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    } else {
        // 获取锁失败，可能是其他线程正在处理，返回success避免支付宝重复回调
        return "success";
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return "fail";
}
```

**设计要点**：
1. **锁的粒度**：基于订单号加锁，`lock:pay:notify:{orderId}`
2. **双重校验**：分布式锁 + 订单状态检查
3. **超时处理**：获取锁失败时返回success，避免支付宝重复回调
4. **异常处理**：使用finally确保锁释放

### 5.2 场景2：防止用户并发创建订单

**位置**：`PayOrderInfoServiceImpl.getPayInfo()`

**问题**：同一用户快速点击多次，可能创建多个订单。

**实现**：

```java
// 使用分布式锁防止并发创建订单（基于用户ID加锁，而不是订单ID）
String lockKey = "lock:order:create:user:" + tokenUserInfoDTO.getUserId();
RLock lock = redissonClient.getLock(lockKey);

try {
    // 尝试获取锁，等待3秒，锁定30秒
    if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        try {
            // 检查是否存在有效期内待付款的订单
            PayOrderInfo validPendingOrder = getValidPendingAlipayOrder(userId);
            if (validPendingOrder != null) {
                // 返回已有订单的二维码
                return payInfoDTO;
            }

            // 生成订单ID
            String orderId = getOrderId();
            
            // 调用支付宝API生成二维码
            String payUrl = payUtils.sendRequestToAlipay(orderId, amount, productName);
            
            // 创建订单记录
            PayOrderInfo payOrderInfo = new PayOrderInfo();
            // ... 设置订单信息
            payOrderInfoMapper.insert(payOrderInfo);
            
            // 保存二维码到Redis
            redisComponent.saveQrCode(orderId, payUrl, Constants.ORDER_TIMEOUT_MIN * 60);
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
```

**设计要点**：
1. **锁的粒度**：基于用户ID加锁，`lock:order:create:user:{userId}`
2. **先检查再创建**：先检查是否有待支付订单，避免重复创建
3. **超时处理**：获取锁失败时抛出业务异常

### 5.3 场景3：缓存击穿防护

**位置**：`MusicInfoServiceImpl.getMusicInfoByMusicId()`、`UserInfoServiceImpl.getUserInfoByUserId()`

**问题**：热点数据过期时，大量请求同时查询数据库。

**实现**：

```java
// 1. 先查Redis缓存
MusicInfo musicInfo = redisComponent.getMusicInfo(musicId);
if (musicInfo != null) {
    return musicInfo;
}

// 2. Redis未命中时，使用分布式锁防止缓存击穿
String lockKey = "lock:music:" + musicId;
RLock lock = redissonClient.getLock(lockKey);

try {
    // 尝试获取锁，等待100ms，锁定3秒
    if (lock.tryLock(100, 3000, TimeUnit.MILLISECONDS)) {
        try {
            // 双重检查：再次查询Redis（可能其他线程已经加载了）
            musicInfo = redisComponent.getMusicInfo(musicId);
            if (musicInfo != null) {
                return musicInfo;
            }
            
            // 查询数据库
            musicInfo = musicInfoMapper.selectByMusicId(musicId);
            
            if (musicInfo != null) {
                // 存入Redis（会自动使用随机过期时间）
                redisComponent.saveMusicInfo(musicId, musicInfo);
            }
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    } else {
        // 获取锁失败，等待一小段时间后重试Redis
        Thread.sleep(50);
        musicInfo = redisComponent.getMusicInfo(musicId);
        // 如果还是没拿到，降级：直接查询数据库
        if (musicInfo == null) {
            musicInfo = musicInfoMapper.selectByMusicId(musicId);
        }
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    // 降级：直接查询数据库
    musicInfo = musicInfoMapper.selectByMusicId(musicId);
}

return musicInfo;
```

**设计要点**：
1. **双重检查**：获取锁后再次检查缓存，避免重复查询数据库
2. **降级策略**：获取锁失败时，等待后重试缓存，最后降级到数据库
3. **锁的粒度**：基于资源ID加锁，`lock:music:{musicId}`

### 5.4 场景4：音乐创建状态更新

**位置**：`MusicInfoServiceImpl.musicCreated()`

**问题**：AI音乐生成完成后，需要更新音乐状态，防止重复更新。

**实现**：

```java
// 使用分布式锁防止重复更新音乐状态（基于taskId加锁）
String lockKey = "lock:music:update:" + resultDTO.getTaskId();
RLock lock = redissonClient.getLock(lockKey);

try {
    // 尝试获取锁，等待3秒，锁定30秒
    if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        try {
            // 先查询当前音乐状态，避免重复更新
            MusicInfoQuery checkQuery = new MusicInfoQuery();
            checkQuery.setTaskId(resultDTO.getTaskId());
            List<MusicInfo> existingMusic = musicInfoMapper.selectList(checkQuery);
            
            if (existingMusic != null && !existingMusic.isEmpty()) {
                MusicInfo music = existingMusic.get(0);
                // 如果已经是创建完成状态，直接返回，避免重复处理
                if (MusicStatusEnum.CREATED.getStatus().equals(music.getMusicStatus())) {
                    log.info("音乐已创建完成，跳过重复更新, taskId={}", resultDTO.getTaskId());
                    return;
                }
            }
            
            // 更新音乐信息
            MusicInfo updateInfo = new MusicInfo();
            updateInfo.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
            // ... 设置其他字段
            musicInfoMapper.updateByParam(updateInfo, query);
            
            // 更新Redis缓存
            redisComponent.saveMusicInfo(musicId, updatedMusic);
            
            // 同步到ES
            musicSearchService.saveOrUpdateMusicToES(updatedMusic);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new BusinessException("获取锁被中断");
}
```

### 5.5 场景5：积分更新

**位置**：`UserIntegralRecordServiceImpl.changeUserIntegral()`

**问题**：并发更新用户积分时，需要保证原子性。

**实现**：

```java
// 使用分布式锁保证积分更新的原子性（基于用户ID加锁）
String lockKey = "lock:integral:user:" + userId;
RLock lock = redissonClient.getLock(lockKey);

try {
    // 尝试获取锁，等待3秒，锁定30秒
    if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        try {
            // 查询当前积分
            UserInfo userInfo = userInfoMapper.selectByUserId(userId);
            Integer currentIntegral = userInfo.getIntegral();
            
            // 计算新积分
            Integer newIntegral = calculateNewIntegral(currentIntegral, type, integral);
            
            // 更新积分
            UserInfo updateInfo = new UserInfo();
            updateInfo.setIntegral(newIntegral);
            userInfoMapper.updateByUserId(updateInfo, userId);
            
            // 记录积分变更
            UserIntegralRecord record = new UserIntegralRecord();
            // ... 设置记录信息
            userIntegralRecordMapper.insert(record);
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
```

---

## 面试常见问题

### 6.1 基础问题

#### Q1: 什么是分布式锁？为什么需要分布式锁？

**答案**：
分布式锁是在分布式系统中，用于控制多个进程/线程对共享资源访问的同步机制。

**为什么需要**：
- 单机锁（synchronized、ReentrantLock）只能保证同一JVM内的线程安全
- 分布式系统中，多个服务实例运行在不同JVM，需要跨进程的锁机制
- 常见场景：防止重复操作、缓存击穿、资源竞争、状态更新

#### Q2: Redis实现分布式锁的基本原理是什么？

**答案**：
1. **加锁**：使用 `SET key value NX EX timeout` 命令
   - `NX`：只有当key不存在时才设置
   - `EX`：设置过期时间
2. **释放锁**：使用Lua脚本保证原子性
   ```lua
   if redis.call("get", KEYS[1]) == ARGV[1] then
       return redis.call("del", KEYS[1])
   else
       return 0
   end
   ```
3. **为什么需要value**：防止误删其他线程的锁

#### Q3: Redis分布式锁有哪些问题？

**答案**：
1. **锁过期时间设置困难**：太短业务未完成，太长异常退出时释放不及时
2. **主从切换导致锁丢失**：主从同步延迟，新主节点可能没有锁
3. **不支持可重入**：同一线程无法多次获取同一把锁
4. **不支持自动续期**：业务执行时间不确定时，需要手动续期

### 6.2 Redisson相关问题

#### Q4: Redisson如何解决Redis分布式锁的问题？

**答案**：
1. **Watch Dog自动续期**：
   - 获取锁成功后启动后台线程
   - 每隔 `leaseTime / 3` 检查一次
   - 如果锁还在持有，自动续期
2. **可重入锁**：
   - 使用Hash结构：`key: {threadId: count}`
   - 同一线程获取锁时count+1，释放时count-1
3. **公平锁、读写锁、联锁**：提供更多锁类型

#### Q5: Redisson的Watch Dog机制是如何工作的？

**答案**：
1. 获取锁时，如果 `leaseTime = -1`，启动Watch Dog
2. Watch Dog每隔 `leaseTime / 3`（默认10秒）检查一次
3. 如果锁还在当前线程持有，调用Lua脚本续期
4. 线程释放锁时，停止Watch Dog
5. 如果线程异常退出，锁会在过期时间后自动释放

#### Q6: Redisson锁的可重入是如何实现的？

**答案**：
- 使用Redis Hash结构：`key: {field: threadId, value: count}`
- 加锁时：如果threadId已存在，count+1；否则设置为1
- 释放锁时：count-1，count为0时删除key
- 通过threadId判断是否为同一线程

### 6.3 项目实践问题

#### Q7: 在你的项目中，分布式锁是如何保证支付回调幂等性的？

**答案**（结合项目代码）：
1. **锁的粒度**：基于订单号加锁，`lock:pay:notify:{orderId}`
2. **双重校验**：
   - 分布式锁：保证同一订单的回调串行执行
   - 订单状态检查：如果订单已是支付状态，直接返回success
3. **超时处理**：获取锁失败时返回success，避免支付宝重复回调
4. **异常处理**：使用finally确保锁释放

**代码位置**：`AliPayNotifyController.processAlipayNotification()`

#### Q8: 如何防止用户并发创建多个订单？

**答案**（结合项目代码）：
1. **锁的粒度**：基于用户ID加锁，`lock:order:create:user:{userId}`
2. **先检查再创建**：
   - 先检查是否有有效期内待支付订单
   - 如果有，直接返回已有订单的二维码
   - 如果没有，再创建新订单
3. **超时处理**：获取锁失败时抛出业务异常

**代码位置**：`PayOrderInfoServiceImpl.getPayInfo()`

#### Q9: 如何用分布式锁防止缓存击穿？

**答案**（结合项目代码）：
1. **双重检查**：
   - 先查Redis缓存
   - 缓存未命中时，获取分布式锁
   - 获取锁后，再次检查缓存（可能其他线程已加载）
2. **降级策略**：
   - 获取锁失败时，等待后重试缓存
   - 最后降级到直接查询数据库
3. **锁的粒度**：基于资源ID加锁，`lock:music:{musicId}`

**代码位置**：`MusicInfoServiceImpl.getMusicInfoByMusicId()`

#### Q10: 分布式锁的锁粒度如何选择？

**答案**：
1. **原则**：锁的粒度要尽可能小，但也要保证业务正确性
2. **项目中的实践**：
   - 支付回调：基于订单号，`lock:pay:notify:{orderId}`
   - 创建订单：基于用户ID，`lock:order:create:user:{userId}`
   - 缓存击穿：基于资源ID，`lock:music:{musicId}`
   - 积分更新：基于用户ID，`lock:integral:user:{userId}`
3. **权衡**：
   - 粒度太小：可能无法保证业务正确性
   - 粒度太大：影响并发性能

#### Q11: 如果获取锁失败，应该如何处理？

**答案**（结合项目实践）：
1. **支付回调场景**：返回success，避免支付宝重复回调
2. **创建订单场景**：抛出业务异常，提示用户稍后重试
3. **缓存击穿场景**：降级策略，等待后重试缓存，最后查询数据库
4. **通用原则**：根据业务场景选择合适的降级策略

#### Q12: 分布式锁的过期时间如何设置？

**答案**：
1. **原则**：根据业务执行时间设置，留出安全余量
2. **项目中的实践**：
   - 支付回调：30秒（业务执行时间短）
   - 创建订单：30秒（包含调用支付宝API）
   - 缓存击穿：3秒（查询数据库时间短）
   - 音乐更新：30秒（包含ES同步）
3. **使用Watch Dog**：如果业务执行时间不确定，使用 `leaseTime = -1` 启用自动续期

### 6.4 进阶问题

#### Q13: Redisson的RedLock算法是什么？解决了什么问题？

**答案**：
1. **问题**：Redis主从切换时，锁可能丢失
2. **RedLock算法**：
   - 在多个独立的Redis节点上获取锁
   - 大多数节点（N/2+1）获取成功才算成功
   - 释放锁时，在所有节点上释放
3. **注意**：RedLock也有争议，实际项目中较少使用

#### Q14: 分布式锁和数据库锁有什么区别？

**答案**：
1. **性能**：分布式锁（Redis）性能更高
2. **功能**：分布式锁支持自动续期、可重入等特性
3. **适用场景**：
   - 分布式锁：跨服务、高并发场景
   - 数据库锁：事务场景、数据一致性要求高

#### Q15: 除了Redis，还有哪些分布式锁的实现方式？

**答案**：
1. **Zookeeper**：
   - 优点：强一致性、支持临时节点
   - 缺点：性能较低、需要维护Zookeeper集群
2. **数据库**：
   - 优点：实现简单、利用现有数据库
   - 缺点：性能较低、增加数据库压力
3. **etcd**：
   - 优点：强一致性、支持租约机制
   - 缺点：需要额外维护etcd集群

---

## 总结

### 关键要点

1. **Redis分布式锁**：基础实现简单，但存在锁过期、主从切换等问题
2. **Redisson**：解决了Redis分布式锁的问题，提供了自动续期、可重入等特性
3. **项目实践**：
   - 支付回调幂等性：分布式锁 + 订单状态双重校验
   - 防止重复下单：基于用户ID加锁
   - 缓存击穿防护：双重检查 + 降级策略
   - 状态更新：防止重复更新

### 最佳实践

1. **锁的粒度**：尽可能小，但保证业务正确性
2. **过期时间**：根据业务执行时间设置，留出安全余量
3. **异常处理**：使用finally确保锁释放
4. **降级策略**：根据业务场景选择合适的降级方案
5. **双重校验**：分布式锁 + 业务状态检查，提高可靠性

---

## 参考资料

- [Redisson官方文档](https://github.com/redisson/redisson)
- [Redis分布式锁的实现](https://redis.io/topics/distlock)
- [Martin Kleppmann的分布式锁分析](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)

