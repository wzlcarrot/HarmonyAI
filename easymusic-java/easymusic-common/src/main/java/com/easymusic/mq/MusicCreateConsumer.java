package com.easymusic.mq;

import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.MusicTaskDTO;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.MusicInfoService;
import com.easymusic.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.rabbitmq.client.Channel;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 音乐创建任务消息消费者
 * 
 * 职责：监听 RabbitMQ 消息，处理音乐创建任务
 * 
 * 工作流程：
 * 1. 监听 MQ 中的音乐创建任务消息
 * 2. 使用分布式信号量控制并发数
 * 3. 调用 AI API 查询音乐生成状态
 * 4. 如果音乐已生成，更新数据库
 * 5. 如果未生成或失败，重新放入延迟队列等待重试
 * 
 * @RabbitListener 注解说明：
 * - queues: 监听的队列名称
 * - concurrency: 并发消费线程数（可选）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MusicCreateConsumer {

    private final RedisComponent redisComponent;
    private final RedissonClient redissonClient;
    private final MusicInfoService musicInfoService;

    /**
     * 处理接收到的消息
     *
     * @param message JSON 格式的 MusicTaskDTO 字符串
     * @param channel RabbitMQ 通道，用于手动确认消息
     * @param deliveryTag 消息投递标签
     */
    @RabbitListener(queues = Constants.RABBITMQ_QUEUE_MUSIC_CREATE)  //Constants.RABBITMQ_QUEUE_MUSIC_CREATE是路由键
    public void onMessage(String message, 
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) {
        boolean messageAcknowledged = false;
        try {
            // 将 JSON 字符串转换为对象
            MusicTaskDTO taskDto = JsonUtils.convertJson2Obj(message, MusicTaskDTO.class);
            
            if (taskDto == null) {
                log.error("消息解析失败，message={}", message);
                // 消息格式错误，确认消息避免重复消费
                if (channel != null && deliveryTag != null) {
                    channel.basicAck(deliveryTag, false);
                    messageAcknowledged = true;
                }
                return;
            }

            log.info("收到音乐创建任务消息, taskId={}, musicId={}", 
                taskDto.getTaskId(), taskDto.getMusicId());

            // 使用分布式信号量控制并发数
            //获取名为easymusic:create:semaphore:的信号量
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(
                Constants.REDIS_KEY_MUSIC_CREATE_SEMAPHORE);
            //设置信号量的总允许数，也就是最大并发数
            semaphore.trySetPermits(Constants.MUSIC_CREATE_MAX_PERMITS);

            // 尝试获取许可，如果进程获取不到许可说明并发数已满，需要等待200ms，为什么需要等待，其实主要是为了减少排队的开销。
            // 获取许可时设置30秒过期时间，避免异常情况下许可无法释放导致资源泄漏
            String permitId = semaphore.tryAcquire(Constants.MUSIC_CREATE_SEMAPHORE_WAIT_MS, 
                                                     Constants.MUSIC_CREATE_SEMAPHORE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (permitId == null) {
                log.warn("并发数已满，任务重新入延迟队列, taskId={}", taskDto.getTaskId());
                // 并发数已满，重新放入延迟队列
                redisComponent.addMusicCreateTask(taskDto);
                // 确认消息，因为已经重新入队，避免重复处理
                if (channel != null && deliveryTag != null) {
                    channel.basicAck(deliveryTag, false);
                    messageAcknowledged = true;
                }
                return;
            }

            try {
                // 处理音乐创建任务（调用服务层方法）
                musicInfoService.processMusicCreateTask(taskDto);
                // 处理成功，确认消息
                if (channel != null && deliveryTag != null) {
                    channel.basicAck(deliveryTag, false);
                    messageAcknowledged = true;
                    log.debug("消息已确认, taskId={}, deliveryTag={}", taskDto.getTaskId(), deliveryTag);
                }
            } catch (Exception e) {
                log.error("处理音乐任务失败，重新入延迟队列, taskId={}", taskDto.getTaskId(), e);
                // 处理失败，重新放入延迟队列等待重试
                redisComponent.addMusicCreateTask(taskDto);
                // 确认消息，因为已经重新入队，避免重复处理
                if (channel != null && deliveryTag != null) {
                    channel.basicAck(deliveryTag, false);
                    messageAcknowledged = true;
                }
            } finally {
                // 释放许可
                try {
                    semaphore.release(permitId);
                } catch (Exception ex) {
                    log.error("释放分布式许可失败, taskId={}", taskDto.getTaskId(), ex);
                }
            }

        } catch (Exception e) {
            log.error("消费音乐创建任务消息失败, message={}", message, e);
            // 发生异常，确认消息避免消息堆积
            if (channel != null && deliveryTag != null && !messageAcknowledged) {
                try {
                    channel.basicAck(deliveryTag, false);
                    log.debug("异常情况下确认消息, deliveryTag={}", deliveryTag);
                } catch (Exception ackEx) {
                    log.error("确认消息失败, deliveryTag={}", deliveryTag, ackEx);
                }
            }
        }
    }
}

