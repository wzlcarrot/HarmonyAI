<template>
  <Dialog :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="535px"
    :showCancel="false" @close="closePay">
    <div class="pay-panel">
      <div class="step-panel">
        <el-steps :space="200" :active="currentStep" finish-status="success" align-center>
          <el-step title="确认订单" />
          <el-step title="扫码支付" />
          <el-step title="购买成功" />
        </el-steps>
      </div>

      <template v-if="currentStep == 1">
        <div class="product-info-panel">
          <div class="title-info">订单详情信息</div>
          <div class="product-info">
            <div class="product-cover">
              <Cover :cover="productInfo.cover" :width="100"></Cover>
            </div>
            <div class="product-name-panel">
              <div class="product-name">{{ productInfo.productName }}</div>
              <div class="sku-name">充值积分:{{ productInfo.integral }}</div>
            </div>
            <div class="price">
              ￥<span>{{ proxy.Utils.convert2Amount(productInfo.price) }}</span>
            </div>
          </div>
        </div>
        <el-form class="pay-form" :rules="rules" :model="formData" ref="formDataRef" label-width="95px" @submit.prevent>
          <el-form-item label="支付方式：" prop="payType">
            <div class="pay-method">
              <el-radio-group v-model="formData.payType" @change="payTypeChange">
                <el-radio :value="1">支付宝支付(推荐)</el-radio>
                <el-radio :value="0">支付码支付</el-radio>
              </el-radio-group>
              <el-popover placement="right" :width="220" trigger="hover">
                <template #reference>
                  <div class="no-pay-tips">没有支付宝支付?</div>
                </template>
                <template #default>
                  <div class="show-qrcode">
                    <img :src="proxy.Utils.getLocalResource('img/qrcode.png')" :style="{ width: '200px' }" />
                    <div :style="{ 'text-align': 'left' }">
                      <div class="info">1、支付宝扫码联系管理员</div>
                      <div class="info">2、备注商品信息</div>
                      <div class="info">3、管理员会给你解决</div>
                    </div>
                  </div>
                </template>
              </el-popover>
            </div>
          </el-form-item>
          <template v-if="formData.payType == 0">
            <el-form-item label="支付码：" prop="payCode">
              <div class="form-item">
                <div class="input">
                  <el-input size="large" placeholder="输入支付码" v-model.trim="formData.payCode" :maxLength="8"></el-input>
                </div>
                <div class="input-tips">输入付款码</div>
              </div>
            </el-form-item>
            <el-form-item label="验证码：" prop="checkCode">
              <div class="form-item">
                <div class="input">
                  <el-input size="large" placeholder="输入图片验证码" v-model.trim="formData.checkCode"
                    @keyup.enter="payCodePay"></el-input>
                </div>
                <img :src="checkCodeUrl" @click="changeCheckCode" class="check-code" />
              </div>
            </el-form-item>
          </template>
        </el-form>
        <div class="pay-btn-panel">
          <div class="pay-btn" @click="getPayQrcode" v-if="formData.payType == 1">
            提交订单
          </div>
          <div class="pay-btn" @click="payCodePay" v-if="formData.payType == 0">
            立即购买
          </div>
        </div>
      </template>

      <!--获取支付信息-->
      <div class="step2" v-if="currentStep == 2">
        <div class="amount-panel">
          应付金额：￥<span class="amount">{{
            getDisplayAmount()
          }}</span>
        </div>
        <!-- 订单到期时间显示 -->
        <div class="expire-time" v-if="payInfo.expireTime">
          订单到期时间：<span class="expire-text">{{ formatExpireTime(payInfo.expireTime) }}</span>
        </div>
        <div class="qrcode">
          <div v-if="payInfo.payUrl">
            <QrcodeVue :value="payInfo.payUrl" :size="180"></QrcodeVue>
            <!-- Debug: Show the QR code content -->
            <div style="margin-top: 10px; font-size: 12px; color: #666; word-break: break-all;">
              QR内容: {{ payInfo.payUrl }}
            </div>
          </div>
          <div v-else>
            正在生成二维码，请稍候...
          </div>
          <div class="pay-remind">
            <div>
              支付完成后，页面在5秒钟后会跳转，如果未跳转，请点击下方
              "我已经支付"。
            </div>
            <div v-if="payInfo.isPendingOrder">
              检测到您有未完成的订单，请继续完成支付
            </div>
          </div>
          <div class="pay-info">
            <div class="iconfont icon-alipay">
              <span class="text">支付宝扫码支付</span>
            </div>
            <div class="refresh-qrcode" @click="refreshQrcode">刷新二维码</div>
            <div class="have-pay" @click="havePay">我已经支付？</div>
          </div>
        </div>
      </div>
      <div class="step3" v-if="currentStep == 3">
        <div class="iconfont icon-ok">恭喜你，支付成功</div>
        <div class="go-order-panel">
          <div class="go-btn" @click="showMyOrder">查看订单</div>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import QrcodeVue from 'qrcode.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

const dialogConfig = ref({
  show: false,
  title: '购买',
})
const currentStep = ref(1)

const productInfo = ref({
  cover: null // 初始化封面字段
})
const pay = async (data) => {
  dialogConfig.value.show = true
  currentStep.value = 1
  productInfo.value = data
  await nextTick()
  formDataRef.value.resetFields()
  formData.value = {
    payType: 1,
  }
}

// 处理待付款订单的直接跳转
const payForPendingOrder = (pendingOrderData) => {
  console.log('Pay.vue: 处理待付款订单，原始数据:', pendingOrderData);
  
  dialogConfig.value.show = true
  console.log('Pay.vue: 设置dialogConfig.show = true');
  
  currentStep.value = 2 // 直接跳转到二维码页面
  console.log('Pay.vue: 设置currentStep = 2');
  
  // 确保金额字段正确设置
  // 处理BigDecimal对象或类似对象
  let orderAmount = 0;
  if (pendingOrderData.amount !== null && pendingOrderData.amount !== undefined) {
    if (typeof pendingOrderData.amount === 'object' && typeof pendingOrderData.amount.toString === 'function') {
      orderAmount = parseFloat(pendingOrderData.amount.toString());
    } else {
      orderAmount = parseFloat(pendingOrderData.amount);
    }
    if (isNaN(orderAmount)) orderAmount = pendingOrderData.price || 0;
  } else {
    orderAmount = pendingOrderData.price || 0;
  }
  
  // 设置支付信息，确保包含金额字段
  payInfo.value = {
    ...pendingOrderData,
    isPendingOrder: true, // 标记为待付款订单
    amount: orderAmount, // 确保amount字段存在
    price: orderAmount   // 同时设置price字段作为备选
  }
  
  // 设置商品信息，使用后端返回的真实数据
  productInfo.value = {
    productId: pendingOrderData.existingOrderId,
    productName: pendingOrderData.productName || '待付款订单',
    price: orderAmount, // 使用相同的金额值
    integral: pendingOrderData.integral || 0
  }
  
  console.log('设置后的支付信息:', payInfo.value);
  console.log('设置后的商品信息:', productInfo.value);
  startTimer()
}

const formData = ref({})
const formDataRef = ref()
const rules = {
  payType: [{ required: true, message: '请选择支付方式' }],
  payCode: [{ required: true, message: '请输入支付码' }],
  checkCode: [{ required: true, message: '请输入图片验证码' }],
}

const checkCodeUrl = ref(null)
const checkCodeKey = ref()
const changeCheckCode = async () => {
  let result = await proxy.Request({
    url: proxy.Api.checkCode,
    showLoading: false,
  })
  if (!result) {
    return
  }
  checkCodeUrl.value = result.data.checkCode
  checkCodeKey.value = result.data.checkCodeKey
}

const payTypeChange = (e) => {
  if (e === 1) {
    return
  }
  changeCheckCode()
}

//刷新二维码
const refreshQrcode = async () => {
  console.log('Refreshing QR code...');
  proxy.Message.info('正在重新生成二维码...');
  await getPayQrcode();
}

//获取支付二维码
const payInfo = ref({})
const getPayQrcode = async () => {
  console.log('检查支付状态并创建/恢复订单...');
  // 清理之前的定时器
  cleanTimer();
  
  // 首先检查是否有有效期内待付款订单
  // 这样即使用户点击不同的商品，如果有未完成的订单，也会继续使用该订单
  let pendingOrderResult = await proxy.Request({
    url: proxy.Api.checkPendingOrder,
    params: {},
    showLoading: false,
  })
  
  // 如果存在有效期内待付款订单，直接使用该订单的二维码
  if (pendingOrderResult && pendingOrderResult.data && pendingOrderResult.data.hasValidPendingOrder) {
    console.log('检测到有效期内待付款订单，恢复到原有订单');
    
    // 确保金额字段正确设置
    // 处理BigDecimal对象或类似对象
    let orderAmount = 0;
    if (pendingOrderResult.data.amount !== null && pendingOrderResult.data.amount !== undefined) {
      if (typeof pendingOrderResult.data.amount === 'object' && typeof pendingOrderResult.data.amount.toString === 'function') {
        orderAmount = parseFloat(pendingOrderResult.data.amount.toString());
      } else {
        orderAmount = parseFloat(pendingOrderResult.data.amount);
      }
      if (isNaN(orderAmount)) orderAmount = pendingOrderResult.data.price || 0;
    } else {
      orderAmount = pendingOrderResult.data.price || 0;
    }
    
    currentStep.value = 2
    
    // 设置支付信息，确保包含金额字段
    payInfo.value = {
      ...pendingOrderResult.data,
      amount: orderAmount, // 确保amount字段存在
      price: orderAmount   // 同时设置price字段作为备选
    }
    
    // 确保商品信息正确设置，使用相同的金额
    productInfo.value = {
      ...productInfo.value, // 保留原有字段（如cover）
      productId: pendingOrderResult.data.existingOrderId,
      price: orderAmount, // 使用相同的金额值
      productName: pendingOrderResult.data.productName || '待付款订单',
      integral: pendingOrderResult.data.integral || 0,
      cover: productInfo.value.cover // 保留原有的封面图
    }
    
    console.log('恢复到待付款订单，金额:', orderAmount);
    // 显示提示信息
    proxy.Message.success('检测到您有未支付的订单，已自动恢复支付流程');
    
    startTimer()
    return
  }
  
  // 如果没有待付款订单，则创建新的支付订单
  console.log('没有待付款订单，创建新订单');
  let result = await proxy.Request({
    url: proxy.Api.getPayInfo,
    params: {
      productId: productInfo.value.productId,
      payType: formData.value.payType,
    },
  })
  if (!result) {
    console.log('Failed to get payment info');
    proxy.Message.error('获取支付信息失败，请重试');
    return
  }
  console.log('Pay info result:', result)
  console.log('Pay URL:', result.data.payUrl)
  
  // Check if payUrl is valid
  if (!result.data.payUrl || result.data.payUrl.length < 10) {
    console.log('Invalid pay URL received');
    proxy.Message.error('支付二维码生成失败，请重试')
    return
  }
  
  currentStep.value = 2
  payInfo.value = result.data
  startTimer()
}



//校验支付结果
const checkPayInfo = async () => {
  const orderId = payInfo.value.orderId
  if (!orderId) {
    return
  }
  console.log('Checking payment status for order:', orderId)
  let result = await proxy.Request({
    showLoading: false,
    url: proxy.Api.checkPayOrder,
    params: {
      orderId: orderId,
    },
  })
  if (!result) {
    console.log('Failed to check payment status')
    return
  }
  console.log('Payment status check result:', result)
  if (result.data != null && result.data) {
    console.log('Payment successful for order:', orderId)
    cleanTimer()
    currentStep.value = 3
    //重新加载积分
    userInfoStore.updateLastReloadTime()
  } else {
    console.log('Payment not yet completed for order:', orderId)
  }
}

//已经支付
const havePay = async () => {
  const orderId = payInfo.value.orderId
  if (!orderId) {
    proxy.Message.error('未获取到支付信息')
    return
  }
  let result = await proxy.Request({
    url: proxy.Api.havePay,
    params: {
      orderId: orderId,
    },
  })
  if (!result) {
    return
  }
  if (result.data != null && result.data) {
    cleanTimer()
    currentStep.value = 3
    //重新加载积分
    userInfoStore.updateLastReloadTime()
  } else {
    proxy.Message.error('未查询到已支付订单,请等30秒后再试')
  }
}

let timmer = ref(null)
const startTimer = () => {
  timmer.value = setInterval(() => {
    checkPayInfo()
  }, 5000)
}

const cleanTimer = () => {
  if (timmer.value !== null) {
    clearInterval(timmer.value)
    timmer.value = null
  }
}
const closePay = () => {
  dialogConfig.value.show = false
  cleanTimer()
}

//支付码支付
const payCodePay = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let result = await proxy.Request({
      url: proxy.Api.buyByPayCode,
      params: {
        checkCodeKey: checkCodeKey.value,
        checkCode: formData.value.checkCode,
        payCode: formData.value.payCode,
        productId: productInfo.value.productId,
      },
      errorCallback: () => {
        changeCheckCode()
      },
    })
    if (!result) {
      return
    }
    currentStep.value = 3
    //重新加载积分
    userInfoStore.updateLastReloadTime()
  })
}

const emits = defineEmits(['showMyOrder'])
const showMyOrder = () => {
  dialogConfig.value.show = false
  emits('showMyOrder')
}

// 获取显示的金额
const getDisplayAmount = () => {
  console.log('支付信息对象:', JSON.parse(JSON.stringify(payInfo.value)));
  console.log('商品信息对象:', JSON.parse(JSON.stringify(productInfo.value)));
  
  // 强制类型转换辅助函数
  const toNumber = (value) => {
    if (value === null || value === undefined || value === '') return null;
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
      const parsed = parseFloat(value);
      return isNaN(parsed) ? null : parsed;
    }
    // 处理BigDecimal对象或类似对象
    if (typeof value === 'object' && value !== null) {
      // 如果有toString方法且能转换为数字
      if (typeof value.toString === 'function') {
        const parsed = parseFloat(value.toString());
        return isNaN(parsed) ? null : parsed;
      }
    }
    return null;
  };
  
  // 优先使用 payInfo.value 中的 amount 字段
  if (payInfo.value && payInfo.value.amount !== undefined && payInfo.value.amount !== null) {
    const amount = toNumber(payInfo.value.amount);
    if (amount !== null && amount > 0) {
      console.log('使用 payInfo.value.amount:', amount);
      return proxy.Utils.convert2Amount(amount);
    }
    console.log('payInfo.value.amount 值无效:', payInfo.value.amount);
  }
  
  // 备选：使用 payInfo.value 中的 price 字段
  if (payInfo.value && payInfo.value.price !== undefined && payInfo.value.price !== null) {
    const price = toNumber(payInfo.value.price);
    if (price !== null && price > 0) {
      console.log('使用 payInfo.value.price:', price);
      return proxy.Utils.convert2Amount(price);
    }
    console.log('payInfo.value.price 值无效:', payInfo.value.price);
  }
  
  // 最后备选：使用 productInfo.value 中的 price 字段
  if (productInfo.value && productInfo.value.price !== undefined && productInfo.value.price !== null) {
    const price = toNumber(productInfo.value.price);
    if (price !== null) {
      console.log('使用 productInfo.value.price:', price);
      return proxy.Utils.convert2Amount(price);
    }
    console.log('productInfo.value.price 值无效:', productInfo.value.price);
  }
  
  console.log('没有找到有效的金额，使用默认值 0');
  return proxy.Utils.convert2Amount(0);
}

// 格式化订单到期时间
const formatExpireTime = (timestamp) => {
  if (!timestamp) return '';
  
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

defineExpose({
  pay,
  payForPendingOrder,
  currentStep,
  payInfo,
})
</script>

<style lang="scss" scoped>
.pay-panel {
  color: var(--text) !important;
  background: #1a0b2e;
  border-radius: 8px;
  padding: 20px;
  border: 2px solid #4a3f5f;
  .step-panel {
    margin-bottom: 20px;
    :deep(.el-step__title.is-process) {
      color: var(--text);
      font-weight: bold;
    }
    :deep(.el-step__title) {
      color: var(--text);
    }
    :deep(.el-step__head.is-process) {
      color: var(--activeText);
      border-color: var(--activeText);
    }
    :deep(.el-step__description) {
      color: var(--text);
    }
  }
  .product-info-panel {
    border: 2px solid #4a3f5f;
    border-radius: 8px;
    overflow: hidden;
    background: #120a31;
    .title-info {
      padding: 12px;
      background: #2d1b69;
      color: var(--text) !important;
      font-weight: bold;
    }
    .product-info {
      display: flex;
      align-items: center;
      padding: 15px;
      .product-cover {
        border-radius: 5px;
        overflow: hidden;
      }
      .product-name-panel {
        margin: 0px 15px;
        flex: 1;
        width: 0;
        .product-name {
          font-weight: bold;
          color: var(--text) !important;
          font-size: 16px;
        }
        .sku-name {
          margin-top: 8px;
          font-size: 14px;
          color: var(--text) !important;
        }
      }
      .price {
        color: #ff6b35;
        font-weight: bold;
        span {
          font-size: 22px;
        }
      }
    }
  }
  .pay-form {
    margin-top: 20px;
    :deep(.el-form-item) {
      align-items: center;
      margin-bottom: 15px;
      .el-form-item__label {
        color: var(--text) !important;
        font-weight: 500;
      }
      .el-radio__label {
        color: var(--text) !important;
      }
      .el-input__wrapper {
        background-color: #220a48 !important;
        box-shadow: none !important;
        border: 2px solid #4a3f5f !important;
        border-radius: 8px;
        .el-input__inner {
          color: var(--text) !important;
        }
      }
    }
    .form-item {
      display: flex;
      align-content: center;
      align-items: center;
      .input {
        flex: 1;
        margin-right: 10px;
      }
      .input-tips {
        color: var(--text) !important;
        font-size: 12px;
      }
      .check-code {
        cursor: pointer;
        border-radius: 5px;
        border: 1px solid #4a3f5f;
      }
    }
    .pay-method {
      width: 100%;
      display: flex;
      align-items: center;
      .no-pay-tips {
        cursor: pointer;
        font-size: 13px;
        margin-left: 15px;
        color: var(--text) !important;
        &:hover {
          color: #94adff !important;
        }
      }
    }
  }
  .pay-btn-panel {
    margin-top: 10px;
    display: flex;
    justify-content: center;
    .pay-btn {
      width: 150px;
      text-align: center;
      padding: 10px;
      background: var(--btnBg);
      border-radius: 20px;
      cursor: pointer;
      opacity: 0.8;
      &:hover {
        opacity: 1;
      }
    }
  }

  .step2 {
    text-align: center;
    .amount-panel {
      color: #ffd700;
      font-size: 18px;
      margin-top: 10px;
      .amount {
        font-size: 25px;
      }
    }
    .expire-time {
      color: #ff6b35;
      font-size: 14px;
      margin-top: 10px;
      margin-bottom: 10px;
      .expire-text {
        font-weight: bold;
        color: #ffd700;
      }
    }
    .qrcode {
      margin: 20px auto;
      background: #120a31;
      border-radius: 8px;
      padding: 20px;
      border: 1px solid #4a3f5f;
      .pay-remind {
        margin-top: 5px;
        text-align: center;
        color: var(--text) !important;
      }
      .pay-info {
        margin-top: 10px;
        display: flex;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .icon-alipay {
        display: flex;
        align-items: center;
        justify-content: center;
        color: #1777ff;
        font-size: 20px;
        .text {
          color: var(--text);
          margin-left: 5px;
          font-size: 14px;
        }
      }
      .refresh-qrcode {
        margin-left: 10px;
        cursor: pointer;
        color: var(--text);
      }
      .have-pay {
        margin-left: 10px;
        cursor: pointer;
        color: var(--text);
      }
    }
  }

  .step3 {
    .icon-ok {
      text-align: center;
      color: #22ac38;
      font-size: 16px;
      margin: 40px 0px;
    }
    .icon-ok::before {
      margin-right: 10px;
    }
    .go-order-panel {
      margin: 30px 0px 20px 0px;
      text-align: center;
      .go-btn {
        background: #22ac38;
        color: var(--text);
        line-height: 45px;
        text-align: center;
        margin: 0px auto;
        display: inline-block;
        padding: 0px 60px;
        cursor: pointer;
        border-radius: 20px;
        &:hover {
          opacity: 0.8;
        }
      }
    }
  }
}
</style>