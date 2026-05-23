package com.easymusic.mq;

import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.MusicTaskDTO;
import com.easymusic.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 音乐创建任务消息生产者
 * 
 * 职责：将音乐创建任务发送到 RabbitMQ 消息队列
 * 
 * 使用场景：
 * 1. 当延迟队列（Redis）中的任务到期后，将任务发送到 MQ 进行实时处理
 * 2. 任务处理失败需要重试时，也可以发送到 MQ
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MusicCreateProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送音乐创建任务到消息队列
     * 
     * @param musicTaskDTO 音乐任务DTO
     */
    public void sendMusicCreateTask(MusicTaskDTO musicTaskDTO) {
        try {
            // 将对象转换为 JSON 字符串
            String messageBody = JsonUtils.convertObj2Json(musicTaskDTO);

            // 发送消息到 RabbitMQ（Direct 交换机）
            rabbitTemplate.convertAndSend(
                Constants.RABBITMQ_EXCHANGE_MUSIC_CREATE,
                Constants.RABBITMQ_ROUTING_KEY_MUSIC_CREATE,
                messageBody
            );

            log.info("音乐创建任务已发送到MQ, taskId={}, musicId={}", 
                musicTaskDTO.getTaskId(), musicTaskDTO.getMusicId());
        } catch (Exception e) {
            log.error("发送音乐创建任务到MQ失败, taskId={}", musicTaskDTO.getTaskId(), e);
            throw new RuntimeException("发送消息到MQ失败", e);
        }
    }
}

