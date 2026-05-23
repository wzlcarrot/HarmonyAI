package com.easymusic.config;

import com.easymusic.entity.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置：声明交换机、队列与绑定关系。
 * 
 * 优化点：
 * 1. 添加死信队列配置，处理失败消息
 * 2. 支持消息重试机制
 * 3. 消息持久化，防止消息丢失
 * 4. 消息发送确认和返回回调，确保消息可靠投递
 */
@Configuration
@Slf4j
public class RabbitConfig {

    // ================== 音乐创建队列（主队列） ==================
    
    /**
     * 创建直连交换机（主交换机）
     * durable: true - 持久化，服务重启后交换机仍然存在
     * autoDelete: false - 不自动删除，需要手动删除
     */
    @Bean
    public DirectExchange musicCreateExchange() {
        return new DirectExchange(Constants.RABBITMQ_EXCHANGE_MUSIC_CREATE, true, false);
    }
    
    /**
     * 创建死信交换机
     * 用于接收处理失败的消息
     */
    @Bean
    public DirectExchange musicCreateDlxExchange() {
        return new DirectExchange(Constants.RABBITMQ_EXCHANGE_MUSIC_CREATE_DLX, true, false);
    }
    
    /**
     * 创建死信队列
     * 用于存储处理失败的消息，方便后续分析和重试
     */
    @Bean
    public Queue musicCreateDlxQueue() {
        return QueueBuilder.durable(Constants.RABBITMQ_QUEUE_MUSIC_CREATE_DLX).build();
    }
    
    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding musicCreateDlxBinding() {
        return BindingBuilder.bind(musicCreateDlxQueue())
            .to(musicCreateDlxExchange())
            .with(Constants.RABBITMQ_ROUTING_KEY_MUSIC_CREATE_DLX);
    }
    
    /**
     * 创建持久化队列（主队列）
     * durable: true - 持久化，服务重启后队列仍然存在
     * 
     * 死信队列配置：
     * - x-dead-letter-exchange: 指定死信交换机
     * - x-dead-letter-routing-key: 指定死信路由键
     * - x-message-ttl: 可选，消息过期时间（毫秒）
     * 
     * 当消息满足以下条件时，会被发送到死信队列：
     * 1. 消息被拒绝（basic.reject 或 basic.nack）且 requeue=false
     * 2. 消息过期（TTL到期）
     * 3. 队列达到最大长度
     */
    @Bean
    public Queue musicCreateQueue() {
        return QueueBuilder.durable(Constants.RABBITMQ_QUEUE_MUSIC_CREATE)
            .withArgument("x-dead-letter-exchange", Constants.RABBITMQ_EXCHANGE_MUSIC_CREATE_DLX)
            .withArgument("x-dead-letter-routing-key", Constants.RABBITMQ_ROUTING_KEY_MUSIC_CREATE_DLX)
            .build();
    }

    /**
     * 创建绑定关系（把队列绑定到交换机）
     */
    @Bean
    public Binding musicCreateBinding(Queue musicCreateQueue, DirectExchange musicCreateExchange) {
        return BindingBuilder.bind(musicCreateQueue)
            .to(musicCreateExchange)
            .with(Constants.RABBITMQ_ROUTING_KEY_MUSIC_CREATE);
    }
    
    /**
     * 配置消息恢复器
     * 当消息处理失败时，自动发送到死信队列
     * 
     * 注意：需要在 application.yml 中配置重试机制
     * spring:
     *   rabbitmq:
     *     listener:
     *       simple:
     *         retry:
     *           enabled: true
     *           max-attempts: 3
     *           initial-interval: 1000
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(
            rabbitTemplate,
            Constants.RABBITMQ_EXCHANGE_MUSIC_CREATE_DLX,
            Constants.RABBITMQ_ROUTING_KEY_MUSIC_CREATE_DLX
        );
    }

    /**
     * 配置消息发送确认回调
     * 确保消息成功到达 Exchange
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 消息发送到 Exchange 成功的回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息成功发送到Exchange, correlationData={}", correlationData);
            } else {
                log.error("消息发送到Exchange失败, correlationData={}, cause={}", correlationData, cause);
                // 这里可以添加重试逻辑或告警
            }
        });

        // 消息从 Exchange 路由到 Queue 失败的回调
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error("消息路由到Queue失败, exchange={}, routingKey={}, message={}",
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getMessage());
            // 这里可以添加重试逻辑或告警
        });

        // 设置消息发布前的后处理器，确保消息持久化（delivery_mode=2）
        // 这样即使 RabbitMQ 服务宕机或重启，消息仍会从磁盘恢复
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });

        return rabbitTemplate;
    }
}

