package com.easymusic.controller.notify;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.easymusic.entity.enums.PayOrderStatusEnum;
import com.easymusic.entity.enums.UserIntegralRecordTypeEnum;
import com.easymusic.entity.po.PayOrderInfo;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.PayOrderInfoService;
import com.easymusic.service.UserIntegralRecordService;
import com.easymusic.utils.PayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping("/notify")
@Slf4j
@RequiredArgsConstructor
public class AliPayNotifyController {
    
    private final PayOrderInfoService payOrderInfoService;
    private final PayUtils payUtils;
    private final UserIntegralRecordService userIntegralRecordService;
    private final RedissonClient redissonClient;

    private final RedisComponent redisComponent;
    /**
     * 支付宝异步通知回调
     * @param request
     * @return
     */
    @RequestMapping(value = "/alipayCallback", method = {RequestMethod.POST, RequestMethod.GET})
    public String alipayNotify(HttpServletRequest request){
        log.info("进入支付宝异步通知");
        
        try {
            // 获取支付宝POST过来反馈信息   其实解释在解析request
            Map<String, String> params = new HashMap<>();
            Map requestParams = request.getParameterMap();
            
            // 检查是否有参数
            if (requestParams.isEmpty()) {
                log.error("支付宝通知中未接收到参数");
                // 对于POST请求，尝试从输入流读取数据
                if ("POST".equalsIgnoreCase(request.getMethod())) {
                    try {
                        StringBuilder sb = new StringBuilder();
                        BufferedReader reader = request.getReader();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        log.debug("原始POST数据: {}", sb);
                    } catch (Exception e) {
                        log.error("读取POST数据时出错: {}", e.getMessage());
                    }
                }
                return "fail";
            }
            
            // 提取参数
            for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
                String name = (String) iter.next();
                String[] values = (String[]) requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }
            
            log.info("支付宝异步通知参数：{}", JSON.toJSONString(params));
            
            // 验证必要参数
            if (!validateAlipayParams(params)) {
                return "fail";
            }
            
            // 使用正确的支付宝公钥进行签名验证
            String alipayPublicKey = payUtils.getAlipayPublicKey();
            if (alipayPublicKey == null || alipayPublicKey.isEmpty()) {
                log.error("支付宝公钥为空，无法验证签名");
                return "fail";
            }
            
            boolean verify_result = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
            log.info("支付宝签名验证结果：{}", verify_result);

            if (verify_result) {
                return processAlipayNotification(request, params);
            }
            
            log.warn("支付宝签名验证失败");
            return "fail";
        } catch (Exception e) {
            log.error("处理支付宝异步通知时发生未捕获的异常: {}", e.getMessage(), e);
            return "fail";
        }
    }
    
    /**
     * 验证支付宝通知参数
     * @param params
     * @return
     */
    private boolean validateAlipayParams(Map<String, String> params) {
        // 验证sign参数
        if (!params.containsKey("sign") || params.get("sign") == null || params.get("sign").isEmpty()) {
            log.error("支付宝通知中缺少或为空的'sign'参数");
            log.error("可用参数: {}", params.keySet());
            return false;
        }
        
        // 验证订单号参数
        if (!params.containsKey("out_trade_no") || params.get("out_trade_no") == null || params.get("out_trade_no").isEmpty()) {
            log.error("支付宝通知中缺少或为空的'out_trade_no'参数");
            return false;
        }
        
        return true;
    }
    
    /**
     * 处理支付宝通知
     * @param request
     * @param params
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    String processAlipayNotification(HttpServletRequest request, Map<String, String> params) {
        try {
            // 商户订单号
            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
            // 支付宝交易号
            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");
            // 交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

            log.info("订单号：{}，支付宝交易号：{}，交易状态：{}", out_trade_no, trade_no, trade_status);

            // 验证订单号
            if (out_trade_no == null || out_trade_no.isEmpty()) {
                log.error("支付宝通知中缺少out_trade_no");
                return "fail";
            }

            if (trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_SUCCESS")) {
                // 使用分布式锁保证支付回调的幂等性（基于订单号加锁）
                String lockKey = "lock:pay:notify:" + out_trade_no;
                RLock lock = redissonClient.getLock(lockKey);
                
                try {
                    // 尝试获取锁，等待3秒，锁定30秒
                    if (lock.tryLock(3, 30, java.util.concurrent.TimeUnit.SECONDS)) {
                        try {
                            // 检查订单是否存在
                            PayOrderInfo existingOrder = payOrderInfoService.getPayOrderInfoByOrderId(out_trade_no);
                            if (existingOrder == null) {
                                log.error("未找到订单: {}", out_trade_no);
                                return "fail";
                            }

                            // 添加幂等性检查：如果订单已经是支付状态，则直接返回成功
                            if (PayOrderStatusEnum.HAVE_PAY.getStatus().equals(existingOrder.getStatus())) {
                                log.info("订单 {} 已经处理过，无需重复处理", out_trade_no);
                                return "success";
                            }

                            // 更新订单状态
                            PayOrderInfo payOrderInfo = new PayOrderInfo();
                            payOrderInfo.setStatus(PayOrderStatusEnum.HAVE_PAY.getStatus());
                            payOrderInfo.setChannelOrderId(trade_no);
                            payOrderInfo.setPayTime(new java.util.Date());

                            int result = payOrderInfoService.updatePayOrderInfoByOrderId(payOrderInfo, out_trade_no);

                            if (result > 0) {
                                log.info("订单 {} 支付成功，支付宝交易号：{}", out_trade_no, trade_no);

                                // 删除订单信息缓存，确保前端能获取到最新的订单状态（支付后的订单会缓存1天）
                                redisComponent.deleteOrderInfo(out_trade_no);

                                // 重新查询订单信息以获取完整数据
                                PayOrderInfo updatedOrder = payOrderInfoService.getPayOrderInfoByOrderId(out_trade_no);
                                if (updatedOrder != null) {
                                    try {
                                        // 更新积分
                                        userIntegralRecordService.changeUserIntegral(
                                            UserIntegralRecordTypeEnum.RECHARGE, 
                                            updatedOrder.getOrderId(), 
                                            updatedOrder.getUserId(),
                                            updatedOrder.getIntegral(), 
                                            updatedOrder.getAmount()
                                        );
                                        log.info("用户 {} 积分更新成功，增加积分：{}", updatedOrder.getUserId(), updatedOrder.getIntegral());
                                    } catch (Exception e) {
                                        log.error("更新用户积分失败，订单号：{}，用户ID：{}，错误：{}",
                                            updatedOrder.getOrderId(), updatedOrder.getUserId(), e.getMessage(), e);
                                        throw e;
                                    }
                                } else {
                                    log.warn("更新订单后无法查询到订单信息，订单号：{}", out_trade_no);
                                }
                            } else {
                                log.error("更新订单 {} 状态失败", out_trade_no);
                                return "fail";
                            }
                        } finally {
                            // 释放锁
                            if (lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                        }
                    } else {
                        log.warn("获取支付回调锁失败，订单可能正在处理中, orderId={}", out_trade_no);
                        // 如果获取锁失败，可能是其他线程正在处理，返回success避免支付宝重复回调
                        return "success";
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("获取支付回调锁被中断, orderId={}", out_trade_no, e);
                    return "fail";
                }
            } else {
                log.warn("未处理的交易状态: {}", trade_status);
            }
            
            log.info("支付宝异步通知处理完成");
            return "success";
        } catch (UnsupportedEncodingException e) {
            log.error("编码转换异常", e);
            return "fail";
        } catch (Exception e) {
            log.error("处理支付宝通知时发生异常", e);
            return "fail";
        }
    }
}