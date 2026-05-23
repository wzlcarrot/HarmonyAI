<template>
  <el-config-provider :locale="zhCn" :message="{ max: 1 }">
    <router-view></router-view>
  </el-config-provider>
</template>

<script setup>
import { ElConfigProvider } from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import { onMounted } from 'vue';
import { useUserInfoStore } from '@/stores/userInfoStore';
import { getCurrentInstance } from 'vue';

const { proxy } = getCurrentInstance();
const userInfoStore = useUserInfoStore();

// 页面加载时自动检查登录状态
onMounted(() => {
  autoLogin();
});

// 自动登录功能
const autoLogin = async () => {
  const token = localStorage.getItem('token');
  if (!token) {
    return; // 没有token，不需要自动登录
  }
  
  try {
    // 调用获取登录信息的接口验证token有效性
    const result = await proxy.Request({
      url: proxy.Api.getLoginInfo,
      params: {},
      showLoading: false
    });
    
    if (result && result.data) {
      // token有效，恢复登录状态
      userInfoStore.setLoginInfo(result.data);
      console.log('自动登录成功');
      
      // 登录成功后，检查是否有有效期内待付款订单
      checkPendingOrder();
    } else {
      // token无效，清除本地存储
      localStorage.removeItem('token');
    }
  } catch (error) {
    console.log('自动登录失败:', error);
    // token无效或过期，清除本地存储
    localStorage.removeItem('token');
  }
};

// 检查是否有有效期内待付款订单
const checkPendingOrder = async () => {
  try {
    const result = await proxy.Request({
      url: proxy.Api.checkPendingOrder,
      params: {},
      showLoading: false,
    });
    
    if (result && result.data && result.data.hasValidPendingOrder) {
      console.log('检测到有效期内待付款订单');
      // 提示用户有未支付订单，下次点击充值时会自动恢复
      proxy.Message({
        message: '检测到您有未支付的订单，下次点击充值时将自动恢复支付流程',
        type: 'info',
        duration: 5000 // 5秒后自动关闭
      });
    }
  } catch (error) {
    console.log('检查待付款订单时出错:', error);
  }
};
</script>

<style lang="scss" scoped>
</style>
