package com.easymusic.utils;

import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.easymusic.exception.BusinessException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class PayUtils implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    
    //appid
    private final String APP_ID = "9021000155671807";
    //应用私钥 - 从文件读取
    private final String APP_PRIVATE_KEY = readKeyFromFile("alipay_app_private_key.txt");
    private final String CHARSET = "UTF-8";
    // 支付宝公钥 - 从文件读取
    private final String ALIPAY_PUBLIC_KEY = readKeyFromFile("alipay_public_key.txt");
    private final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private final String FORMAT = "JSON";
    //签名方式
    private final String SIGN_TYPE = "RSA2";
    //支付宝异步通知路径,付款完毕后会异步调用本项目的回调方法,必须为公网地址
    private final String NOTIFY_URL = "http://n98978ad.natappfree.cc/api/notify/alipayCallback";
    //支付宝同步通知路径,也就是当付款完毕后跳转本项目的页面,可以不是公网地址
    private final String RETURN_URL = "http://localhost:8090/#/buy";
    private AlipayClient alipayClient = null;
    
    // 提供获取支付宝公钥的方法
    public String getAlipayPublicKey() {
        return ALIPAY_PUBLIC_KEY;
    }
    
    // 提供获取应用ID的方法
    public String getAppId() {
        return APP_ID;
    }


    /**
     * 发起支付宝支付请求，返回二维码内容
     * @param outTradeNo 订单号
     * @param totalAmount 总金额
     * @param subject 商品标题
     * @return 用于生成二维码的字符串
     * @throws AlipayApiException
     */
    public String sendRequestToAlipay(String outTradeNo, Float totalAmount, String subject) throws AlipayApiException {
        //获得初始化的AlipayClient
        alipayClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        //设置请求参数
        AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
        alipayRequest.setNotifyUrl(NOTIFY_URL);

        //商品描述（可空）
        String body = "";
        // 设置二维码有效期为5分钟，避免过早失效
        String timeoutExpress = "5m";
        
        // 构建请求参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", String.format("%.2f", totalAmount));
        bizContent.put("subject", subject);
        bizContent.put("body", body);
        bizContent.put("timeout_express", timeoutExpress);
        
        alipayRequest.setBizContent(bizContent.toJSONString());
        
        log.info("Sending request to Alipay with bizContent: {}", bizContent.toJSONString());
        log.info("Timeout express set to: {}", timeoutExpress);

        //请求并返回响应内容，OrCode是二维码内容
        AlipayTradePrecreateResponse response = alipayClient.execute(alipayRequest);
        
        // Log the response for debugging
        log.info("Alipay response success: {}", response.isSuccess());
        log.info("Alipay response code: {}", response.getCode());
        log.info("Alipay response message: {}", response.getMsg());
        log.info("Alipay response subCode: {}", response.getSubCode());
        log.info("Alipay response subMsg: {}", response.getSubMsg());
        
        if(response.isSuccess()){
            String qrCodeContent = response.getQrCode();
            log.info("QR Code Content: {}", qrCodeContent);
            
            // Validate that we have a proper QR code
            if (qrCodeContent == null || qrCodeContent.isEmpty()) {
                throw new AlipayApiException("支付宝返回的二维码内容为空");
            }
            
            // Additional validation - check if it looks like a valid QR code
            if (!qrCodeContent.startsWith("https://")) {
                log.warn("Warning: QR code doesn't start with https:// - might be invalid");
            }

            return qrCodeContent;
        } else {
            // Log more detailed error information
            String errorMsg = "支付宝预下单失败: " + response.getSubMsg();
            if (response.getSubCode() != null) {
                errorMsg += " (SubCode: " + response.getSubCode() + ")";
            }
            log.error("Alipay error: {}", errorMsg);
            throw new AlipayApiException(errorMsg);
        }
    }

    //    通过订单编号查询
    public String query(String id){
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", id);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        String body=null;
        try {
            response = alipayClient.execute(request);
            body = response.getBody();
            log.info("Query response: {}", body);
        } catch (AlipayApiException e) {
            log.error("Query error: {}", e.getMessage(), e);
        }
        if(response != null && response.isSuccess()){
            log.info("调用成功");
        } else {
            log.info("调用失败");
        }
        return body;
    }

    
    // 添加一个新的测试方法来验证支付宝配置
    public String testAlipayConfiguration() {
        try {
            log.info("=== 开始测试支付宝配置 ===");
            
            // 1. 测试应用ID
            log.info("App ID: {}", APP_ID);
            
            // 2. 测试应用私钥
            if (APP_PRIVATE_KEY != null && APP_PRIVATE_KEY.length() > 50) {
                log.info("App Private Key loaded, length: {}", APP_PRIVATE_KEY.length());
            } else {
                log.error("App Private Key loading failed");
                return "应用私钥加载失败";
            }
            
            // 3. 测试支付宝公钥
            if (ALIPAY_PUBLIC_KEY != null && ALIPAY_PUBLIC_KEY.length() > 50) {
                log.info("Alipay Public Key loaded, length: {}", ALIPAY_PUBLIC_KEY.length());
            } else {
                log.error("Alipay Public Key loading failed");
                return "支付宝公钥加载失败";
            }
            
            // 4. 测试网关URL
            log.info("Gateway URL: {}", GATEWAY_URL);
            
            // 5. 测试通知URL
            log.info("Notify URL: {}", NOTIFY_URL);
            
            // 6. 尝试创建客户端
            AlipayClient testClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
            log.info("Alipay Client created successfully");
            
            return "支付宝配置测试通过";
        } catch (Exception e) {
            log.error("支付宝配置测试失败: {}", e.getMessage(), e);
            return "支付宝配置测试失败: " + e.getMessage();
        }
    }

    
    // 测试生成二维码的功能
    public String testGenerateQRCode() {
        try {
            log.info("=== 开始测试生成支付宝二维码 ===");
            
            // 生成一个测试订单号
            String testOrderId = "TEST_" + System.currentTimeMillis();
            float testAmount = 0.01f; // 一分钱用于测试
            String testSubject = "测试商品";
            
            log.info("测试订单信息: ");
            log.info("  订单号: {}", testOrderId);
            log.info("  金额: {}", testAmount);
            log.info("  商品名称: {}", testSubject);
            
            // 调用生成二维码的方法
            String qrCodeUrl = sendRequestToAlipay(testOrderId, testAmount, testSubject);
            
            log.info("生成的二维码URL: {}", qrCodeUrl);
            
            if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                return "二维码生成成功: " + qrCodeUrl;
            } else {
                return "二维码生成失败";
            }
        } catch (Exception e) {
            log.error("生成二维码测试失败: {}", e.getMessage(), e);
            return "生成二维码测试失败: " + e.getMessage();
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 从文件读取密钥
     * @param fileName 文件名
     * @return 密钥字符串
     */
    public String readKeyFromFile(String fileName) {
        try {
            log.info("Attempting to read key file: {}", fileName);
            
            // 使用classpath资源加载
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
            if (inputStream == null) {
                log.info("Key file not found in classpath: {}", fileName);
                // 如果在classpath中找不到，则尝试从文件系统读取
                throw new BusinessException("Key file not found");
            }
            
            StringBuilder keyContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Reading line: {}", line);
                    // 对于没有PEM标记的密钥文件，只过滤空行
                    if (!line.trim().isEmpty()) {
                        keyContent.append(line);
                    }
                }
            }
            
            String result = keyContent.toString().trim();
            log.info("Successfully read key file: {}, length: {}", fileName, result.length());
            
            // 返回处理后的密钥内容
            return result;
        } catch (IOException e) {
            log.error("读取密钥文件失败: {}, 错误: {}", fileName, e.getMessage(), e);
            return "";
        } catch (Exception e) {
            log.error("读取密钥文件时发生未知错误: {}, 错误: {}", fileName, e.getMessage(), e);
            return "";
        }
    }

}