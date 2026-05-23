<template>
  <div class="buy-title">充值</div>
  <div class="integral">
    积分：<span class="record-btn iconfont icon-narrow-right"
      @click="showIntegralRecord">{{ userInfoStore.userInfo.integral || 0 }}</span>
  </div>
  <div class="product-list">
    <ProductItem v-for="item in productList" :data="item" @pay="pay"></ProductItem>
  </div>
  <Pay ref="payRef" @showMyOrder="showIntegralRecord"></Pay>

  <IntegralRecord ref="integralRecordRef"></IntegralRecord>
</template>

<script setup>
import IntegralRecord from '@/views/my/IntegralRecord.vue'
import Pay from './Pay.vue'
import ProductItem from './ProductItem.vue'
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

const productList = ref([])

const loadProduct = async () => {
  console.log('Buy.vue: 加载商品列表');
  let result = await proxy.Request({
    url: proxy.Api.loadProduct,
    params: {},
  })
  if (!result) {
    console.log('Buy.vue: 加载商品列表失败');
    return
  }
  productList.value = result.data
  console.log('Buy.vue: 商品列表加载完成，数量:', productList.value.length);
}

const payRef = ref()
const pay = async (data) => {
  console.log('Buy.vue: 用户点击购买商品，商品信息:', data);
  
  // 先检查是否有有效期内待付款订单，如果有则直接跳转到支付页面
  try {
    let result = await proxy.Request({
      url: proxy.Api.checkPendingOrder,
      params: {},
      showLoading: false,
    });
    
    if (result && result.data && result.data.hasValidPendingOrder) {
      console.log('Buy.vue: 检测到有效期内待付款订单，直接跳转到支付页面');
      // 显示提示信息
      proxy.Message.success('检测到您有未支付的订单，已自动恢复支付流程');
      // 直接显示支付弹窗并跳转到二维码页面
      payRef.value.payForPendingOrder(result.data);
      return;
    }
  } catch (error) {
    console.log('Buy.vue: 检查待付款订单时出错:', error);
  }
  
  // 没有待付款订单，正常调用pay方法创建新订单
  payRef.value.pay({ ...data });
}

const integralRecordRef = ref()
const showIntegralRecord = () => {
  integralRecordRef.value.show()
}

// 检查是否有有效期内待付款订单
const checkPendingOrder = async () => {
  console.log('Buy.vue: 开始检查待付款订单');
  if (!userInfoStore.checkLogin()) {
    console.log('Buy.vue: 用户未登录，不检查待付款订单');
    return;
  }
  
  try {
    // 直接调用检查待付款订单接口
    let result = await proxy.Request({
      url: proxy.Api.checkPendingOrder,
      params: {},
      showLoading: false,
    })
    
    console.log('Buy.vue: 待付款订单检查结果:', result);
    
    if (result && result.data && result.data.hasValidPendingOrder) {
      console.log('Buy.vue: 检测到有效期内待付款订单，自动跳转到支付页面');
      
      // 确保页面加载完成后再显示弹窗
      await nextTick();
      
      // 直接显示支付弹窗并跳转到二维码页面
      if (payRef.value && payRef.value.payForPendingOrder) {
        console.log('Buy.vue: 调用payForPendingOrder');
        payRef.value.payForPendingOrder(result.data);
      } else {
        console.log('Buy.vue: payRef或payForPendingOrder方法不存在');
      }
    } else {
      console.log('Buy.vue: 没有有效期内待付款订单');
    }
  } catch (error) {
    console.log('Buy.vue: 检查待付款订单时出错:', error);
  }
}

onMounted(async () => {
  await loadProduct()
  // 页面加载时不检查待付款订单，只在用户点击购买时检查
  // await checkPendingOrder()
})
</script>

<style lang="scss" scoped>
.buy-title {
  margin-top: 20px;
  text-align: center;
  color: var(--text);
  font-size: 30px;
  font-weight: bold;
}
.integral {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  color: var(--text);
  .record-btn {
    cursor: pointer;
    display: inline-block;
    text-decoration: underline;
    &::before {
      float: right;
      margin-top: 7px;
      font-size: 13px;
    }
  }
}
.product-list {
  margin-top: 10px;
  display: flex;
  padding: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 500px) {
  .product-list {
    flex-direction: column;
  }
}
</style>
