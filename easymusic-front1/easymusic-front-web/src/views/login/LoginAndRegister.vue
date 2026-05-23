<template>
  <Dialog
    :show="userInfoStore.showLogin"
    :width="dialogWidth"
    :showCancel="false"
    @close="userInfoStore.showLogin = false"
    :padding="0"
    class="modern-login-dialog"
   
  >
    <!-- 切换标签 -->
    <div class="auth-tabs">
      <div 
        class="tab-item" 
        :class="{ 'active': currentTab === 'login' }"
        @click="switchToLogin"
      >
        登录
      </div>
      <div 
        class="tab-item" 
        :class="{ 'active': currentTab === 'register' }"
        @click="switchToRegister"
      >
        注册
      </div>
      <div 
        class="tab-item" 
        :class="{ 'active': currentTab === 'game' }"
        @click="switchToGame"
      >
        玩游戏
      </div>
    </div>
    
    <div class="form-container">
      <!-- 登录/注册页面 -->
      <template v-if="currentTab !== 'game'">
        <!-- 错误提示 -->
        <transition name="error-fade">
          <div v-if="errorMsg" class="error-message">
            <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            {{ errorMsg }}
          </div>
        </transition>
        
        <el-form
          :model="formData"
          ref="formDataRef"
          @submit.prevent="submit"
          class="auth-form"
        >
          <!-- 邮箱输入 -->
          <div class="input-field">
            <div class="input-wrapper" :class="{ 'has-value': formData.email, 'has-error': isEmailInvalid }">
              <input
                type="email"
                v-model.trim="formData.email"
                placeholder=" "
                @input="validateEmail"
                @focus="clearError"
                maxlength="30"
                class="modern-input"
              />
              <label class="input-label">邮箱地址</label>
              <div class="input-border"></div>
              <div class="validation-icon">
                <svg v-if="isEmailValid" class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <svg v-if="isEmailInvalid" class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="15" y1="9" x2="9" y2="15"></line>
                  <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
              </div>
            </div>
          </div>
          
          <!-- 昵称输入 - 注册模式 -->
          <transition name="slide-down">
            <div v-if="!isLogin" class="input-field">
              <div class="input-wrapper" :class="{ 'has-value': formData.nickName }">
                <input
                  type="text"
                  v-model.trim="formData.nickName"
                  placeholder=" "
                  @input="validateNickName"
                  maxlength="15"
                  class="modern-input"
                />
                <label class="input-label">用户昵称</label>
                <div class="input-border"></div>
                <div class="validation-icon">
                  <svg v-if="isNickNameValid" class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
              </div>
            </div>
          </transition>
          
          <!-- 密码输入 -->
          <div class="input-field">
            <div class="input-wrapper" :class="{ 'has-value': formData.password }">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model.trim="formData.password"
                placeholder=" "
                @input="validatePassword"
                @focus="clearError"
                maxlength="18"
                class="modern-input"
              />
              <label class="input-label">密码</label>
              <div class="input-border"></div>
              <div class="password-toggle" @click="togglePassword">
                <svg v-if="!showPassword" class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </div>
            </div>
            <!-- 密码强度 -->
            <transition name="fade">
              <div v-if="!isLogin && formData.password" class="password-strength">
                <div class="strength-bars">
                  <div class="strength-bar" :class="{ 'active': passwordStrengthLevel >= 1 }"></div>
                  <div class="strength-bar" :class="{ 'active': passwordStrengthLevel >= 2 }"></div>
                  <div class="strength-bar" :class="{ 'active': passwordStrengthLevel >= 3 }"></div>
                  <div class="strength-bar" :class="{ 'active': passwordStrengthLevel >= 4 }"></div>
                </div>
                <span class="strength-label">{{ passwordStrengthText }}</span>
              </div>
            </transition>
          </div>
          
          <!-- 确认密码 - 注册模式 -->
          <transition name="slide-down">
            <div v-if="!isLogin" class="input-field">
              <div class="input-wrapper" :class="{ 'has-value': formData.rePassword, 'has-error': isRePasswordInvalid }">
                <input
                  :type="showRePassword ? 'text' : 'password'"
                  v-model.trim="formData.rePassword"
                  placeholder=" "
                  @input="validateRePassword"
                  maxlength="18"
                  class="modern-input"
                />
                <label class="input-label">确认密码</label>
                <div class="input-border"></div>
                <div class="password-toggle" @click="toggleRePassword">
                  <svg v-if="!showRePassword" class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                  <svg v-else class="eye-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                </div>
                <div class="validation-icon">
                  <svg v-if="isRePasswordValid" class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  <svg v-if="isRePasswordInvalid" class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="15" y1="9" x2="9" y2="15"></line>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                  </svg>
                </div>
              </div>
            </div>
          </transition>
          
          <!-- 验证码 -->
          <div class="input-field">
            <div class="captcha-container">
              <div class="input-wrapper captcha-input" :class="{ 'has-value': formData.checkCode }">
                <input
                  type="text"
                  v-model.trim="formData.checkCode"
                  placeholder=" "
                  @focus="clearError"
                  maxlength="6"
                  @keyup.enter="submit"
                  class="modern-input"
                />
                <label class="input-label">验证码</label>
                <div class="input-border"></div>
              </div>
              <div class="captcha-image" @click="refreshCaptcha">
                <img :src="checkCodeUrl" alt="验证码" />
                <div class="refresh-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <polyline points="23 4 23 10 17 10"></polyline>
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                  </svg>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 提交按钮 -->
          <button 
            type="submit" 
            class="submit-btn"
            :class="{ 'loading': showLoading, 'disabled': !canSubmit }"
            :disabled="!canSubmit"
          >
            <span v-if="!showLoading">{{ isLogin ? '登录' : '注册' }}</span>
            <div v-else class="loading-spinner"></div>
          </button>
          
          <!-- 切换模式 -->
          <div class="auth-switch" @click="toggleAuthMode">
            {{ isLogin ? '还没有账号？立即注册' : '已有账号？返回登录' }}
          </div>
          
          <!-- 社交登录 -->
          <div v-if="isLogin" class="social-auth">
            <div class="divider">
              <span>或者使用</span>
            </div>
            <div class="social-buttons">
              <button class="social-btn wechat" @click="socialLogin('wechat')">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a 1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a 1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.27-.027-.407-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
                </svg>
              </button>
              <button class="social-btn qq" @click="socialLogin('qq')">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21.395 15.035a39.39 39.39 0 0 0-.803-2.264l-1.079-2.695c0-.727-.5-2.058-.5-2.058-.714-1.708-1.069-2.78-1.069-3.183 0-1.186 1.378-2.493 2.116-3.183.46-.432.386-1.093.002-1.509-.426-.461-1.25-.717-2.11-.664C17.197.27 16.173.434 15.26.883c-1.714.851-2.953 2.29-2.953 3.738 0 .608.31 1.384.774 2.356.777 1.63 2.115 3.904 2.116 3.904s-.614 3.255-.782 4.267c-.18 1.087-.18 1.976-.18 1.976s.453.136.876-.074c.543-.267.647-1.218.647-1.218.424 1.218.966 1.044.966 1.044s.75-.18 1.075-1.218c.188 1.218.749 1.218.749 1.218s.902.178 1.186-1.218c.424.792.637 1.218.637 1.218s.75.136 1.074-.267c.12-.187.121-.811.121-1.218zm-7.467-2.592c-.383-1.334-1.187-2.857-1.187-2.857-1.628-3.76-1.585-6.581-.504-7.86 1.042-1.232 3.257-1.28 4.956-.876.889.21 1.648.536 2.252.956-2.476.245-4.395 1.467-4.395 2.949 0 1.582 2.272 2.868 5.075 2.868.835 0 1.628-.128 2.336-.356a29.037 29.037 0 0 1 1.446 3.536c.378 1.12.613 2.058.74 2.836.12.736-.144 1.081-.42 1.218-.304.151-.766.014-1.186-.08-.263-.057-.498-.106-.718-.106-.18 0-.318.034-.435.104-.22.134-.31.426-.325.789-.01.262.044.561.108.885.058.293.121.61.121.915 0 .167-.018.332-.056.492-.154.643-.656.799-1.126.945-.511.158-.998.307-.998.894 0 .579.448.842.824 1.058.368.212.704.403.704.86 0 .28-.268.54-.677.74-.483.237-1.167.384-1.86.384-.979 0-1.896-.292-2.461-.734-1.15-.904-1.671-2.555-1.671-4.963 0-1.653.275-2.9.55-3.87z"/>
                </svg>
              </button>
            </div>
          </div>
        </el-form>
      </template>
      
      <!-- 游戏页面 -->
      <template v-else>
        <div class="game-section">
          <div class="game-header">
            <h3>打气球小游戏</h3>
            <div class="game-info">
              <span>时间: {{ gameTime }}s</span>
              <span>得分: {{ gameScore }}</span>
            </div>
          </div>
          
          <div class="game-container" @click="startGame" v-if="!gameStarted">
            <div class="game-start-prompt">
              <p>点击开始游戏</p>
              <p>60秒内打爆气球获取分数</p>
            </div>
          </div>
          
          <div class="game-container" v-else>
            <div class="balloons-area">
              <div 
                v-for="balloon in balloons" 
                :key="balloon.id"
                class="balloon"
                :style="{
                  left: balloon.x + 'px',
                  top: balloon.y + 'px',
                  width: balloon.size + 'px',
                  height: balloon.size + 'px',
                  backgroundColor: balloon.color,
                  opacity: balloon.opacity
                }"
                @click="popBalloon(balloon)"
              ></div>
            </div>
          </div>
          
          <div class="game-controls" v-if="gameStarted">
            <button class="game-btn restart-btn" @click="restartGame">重新开始</button>
            <button class="game-btn end-btn" @click="endGame">结束游戏</button>
          </div>
        </div>
      </template>
      
      <!-- 随机游戏弹窗 -->
      <transition name="game-popup">
        <div v-if="showRandomGame && !gameStarted" class="random-game-popup">
          <div class="popup-overlay" @click="closeRandomGame"></div>
          <div class="popup-content">
            <div class="popup-header">
              <h3>🎈 发现小游戏！</h3>
              <button class="close-btn" @click="closeRandomGame">×</button>
            </div>
            <div class="popup-body">
              <p>登录成功！来玩个打气球小游戏放松一下吧！</p>
              <p>60秒内打爆气球获取分数</p>
              <div class="popup-actions">
                <button class="play-btn" @click="startGame">开始游戏</button>
                <button class="skip-btn" @click="closeRandomGame">跳过</button>
              </div>
            </div>
          </div>
        </div>
      </transition>
      
      </div>
  </Dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useUserInfoStore } from "@/stores/userInfoStore";
const userInfoStore = useUserInfoStore();

import md5 from "js-md5";

// 响应式宽度
const dialogWidth = computed(() => {
  return window.innerWidth > 480 ? "420px" : "95%";
});

// 表单数据
const isLogin = ref(true);
const currentTab = ref('login');
const showPassword = ref(false);
const showRePassword = ref(false);
const formData = ref({});
const formDataRef = ref();
const errorMsg = ref("");
const showLoading = ref(false);

// 验证码
const checkCodeUrl = ref(null);
const refreshCaptcha = async () => {
  let result = await proxy.Request({
    url: proxy.Api.checkCode,
  });
  if (!result) return;
  checkCodeUrl.value = result.data.checkCode;
  localStorage.setItem("checkCodeKey", result.data.checkCodeKey);
};
refreshCaptcha();

// 验证状态
const isEmailValid = ref(false);
const isEmailInvalid = ref(false);
const isNickNameValid = ref(false);
const isPasswordValid = ref(false);
const isRePasswordValid = ref(false);
const isRePasswordInvalid = ref(false);
const passwordStrengthLevel = ref(0);
const passwordStrengthText = ref("");

// 表单提交状态
const canSubmit = computed(() => {
  if (!formData.value.email || !formData.value.password || !formData.value.checkCode) {
    return false;
  }
  if (!isLogin.value && (!formData.value.nickName || !formData.value.rePassword)) {
    return false;
  }
  if (isEmailInvalid.value || isRePasswordInvalid.value) {
    return false;
  }
  return true;
});

// 切换登录/注册模式
const switchToLogin = () => {
  if (currentTab.value !== 'login') {
    currentTab.value = 'login';
    isLogin.value = true;
    resetForm();
  }
};

const switchToRegister = () => {
  if (currentTab.value !== 'register') {
    currentTab.value = 'register';
    isLogin.value = false;
    resetForm();
  }
};

const switchToGame = () => {
  if (currentTab.value !== 'game') {
    currentTab.value = 'game';
    // 不再自动开始游戏，只切换到游戏界面
  }
};

const resetForm = async () => {
  await nextTick();
  formDataRef.value?.resetFields();
  formData.value = {};
  errorMsg.value = "";
  resetValidation();
  refreshCaptcha();
};

// 重置验证状态
const resetValidation = () => {
  isEmailValid.value = false;
  isEmailInvalid.value = false;
  isNickNameValid.value = false;
  isPasswordValid.value = false;
  isRePasswordValid.value = false;
  isRePasswordInvalid.value = false;
  passwordStrengthLevel.value = 0;
  passwordStrengthText.value = "";
};

// 清除错误信息
const clearError = () => {
  errorMsg.value = "";
};

// 切换密码显示
const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const toggleRePassword = () => {
  showRePassword.value = !showRePassword.value;
};

// 邮箱验证
const validateEmail = () => {
  if (!formData.value.email) {
    isEmailValid.value = false;
    isEmailInvalid.value = false;
    return;
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isValid = emailRegex.test(formData.value.email);
  isEmailValid.value = isValid;
  isEmailInvalid.value = !isValid;
  
  if (isEmailInvalid.value) {
    errorMsg.value = "请输入正确的邮箱格式";
  } else {
    clearError();
  }
};

// 昵称验证
const validateNickName = () => {
  if (!formData.value.nickName) {
    isNickNameValid.value = false;
    return;
  }
  
  isNickNameValid.value = formData.value.nickName.length >= 2 && formData.value.nickName.length <= 15;
};

// 密码验证
const validatePassword = () => {
  if (!formData.value.password) {
    isPasswordValid.value = false;
    passwordStrengthLevel.value = 0;
    passwordStrengthText.value = "";
    return;
  }
  
  const passwordRegex = /^[a-zA-Z0-9~!@#$%^&*()_+`\-={}[\]|\\:";'<>?,./]{8,18}$/;
  isPasswordValid.value = passwordRegex.test(formData.value.password);
  
  // 计算密码强度
  if (!isLogin.value) {
    calculatePasswordStrength(formData.value.password);
  }
};

// 确认密码验证
const validateRePassword = () => {
  if (!formData.value.rePassword) {
    isRePasswordValid.value = false;
    isRePasswordInvalid.value = false;
    return;
  }
  
  const isValid = formData.value.password === formData.value.rePassword;
  isRePasswordValid.value = isValid;
  isRePasswordInvalid.value = !isValid;
  
  if (isRePasswordInvalid.value) {
    errorMsg.value = "两次输入的密码不一致";
  }
};

// 计算密码强度
const calculatePasswordStrength = (password) => {
  let strength = 0;
  
  if (password.length >= 8) strength++;
  if (/[a-z]/.test(password)) strength++;
  if (/[A-Z]/.test(password)) strength++;
  if (/[0-9]/.test(password)) strength++;
  if (/[^a-zA-Z0-9]/.test(password)) strength++;
  
  passwordStrengthLevel.value = strength;
  
  if (strength <= 2) {
    passwordStrengthText.value = "弱";
  } else if (strength === 3) {
    passwordStrengthText.value = "中等";
  } else {
    passwordStrengthText.value = "强";
  }
};

// 社交登录
const socialLogin = (platform) => {
  const platformName = platform === 'wechat' ? '微信' : 'QQ';
  proxy.Message.info(`暂不支持${platformName}登录，敬请期待！`);
};

// 游戏相关变量
const gameStarted = ref(false);
const gameTime = ref(60);
const gameScore = ref(0);
const balloons = ref([]);
const gameTimer = ref(null);
const balloonTimer = ref(null);
const balloonDisappearTimer = ref(null);
const showRandomGame = ref(false);

// 气球消失参数
const balloonLifeTime = 3000; // 气球生命周期（毫秒）
const disappearThreshold = 2000; // 2秒后开始消失
const maxDisappearSpeed = 0.5;   // 最大消失速度（秒）

// 气球颜色数组
const balloonColors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57', '#ff9ff3', '#54a0ff', '#5f27cd'];

// 随机显示游戏
const checkRandomGame = () => {
  // 不再显示随机游戏
  showRandomGame.value = false;
};

// 开始游戏
const startGame = () => {
  gameStarted.value = true;
  gameTime.value = 60;
  gameScore.value = 0;
  balloons.value = [];
  
  // 开始计时器
  gameTimer.value = setInterval(() => {
    gameTime.value--;
    if (gameTime.value <= 0) {
      endGame(true); // 传入true表示时间结束
    }
  }, 1000);
  
  // 生成气球
  generateBalloons();
  balloonTimer.value = setInterval(generateBalloons, 1000);
  
  // 开始气球消失计时器
  balloonDisappearTimer.value = setInterval(updateBalloonsDisappear, 100);
};

// 生成气球
const generateBalloons = () => {
  if (balloons.value.length < 15) {
    const gameContainer = document.querySelector('.game-container');
    if (!gameContainer) return;
    
    const containerWidth = gameContainer.clientWidth;
    const containerHeight = gameContainer.clientHeight;
    
    // 确保气球在容器内随机出现
    const balloon = {
      id: Date.now() + Math.random(),
      x: Math.random() * (containerWidth - 60), // 留出边距
      y: Math.random() * (containerHeight - 60), // 在容器内随机位置
      size: 30 + Math.random() * 30,
      color: balloonColors[Math.floor(Math.random() * balloonColors.length)],
      createdAt: Date.now(), // 记录气球创建时间
      opacity: 1, // 初始透明度
      disappearing: false // 是否正在消失
    };
    balloons.value.push(balloon);
  }
};

// 气球消失逻辑
const updateBalloonsDisappear = () => {
  const currentTime = Date.now();
  
  balloons.value.forEach((balloon, index) => {
    const timeAlive = currentTime - balloon.createdAt;
    
    // 如果气球存活时间超过生命周期，直接移除
    if (timeAlive > balloonLifeTime) {
      balloons.value.splice(index, 1);
      return;
    }
    
    // 如果气球存活时间超过消失阈值，开始消失
    if (timeAlive > disappearThreshold) {
      const remainingTime = balloonLifeTime - timeAlive;
      const timeSinceThreshold = timeAlive - disappearThreshold;
      
      // 计算透明度，剩余时间越少透明度越低
      const disappearProgress = timeSinceThreshold / (balloonLifeTime - disappearThreshold);
      balloon.opacity = Math.max(0, 1 - disappearProgress);
      balloon.disappearing = true;
    }
  });
};

// 打爆气球
const popBalloon = (balloon) => {
  balloons.value = balloons.value.filter(b => b.id !== balloon.id);
  gameScore.value += Math.floor(balloon.size / 10);
};

// 重新开始游戏
const restartGame = () => {
  endGame();
  startGame();
};

// 结束游戏
const endGame = (isTimeOut = false) => {
  if (gameTimer.value) {
    clearInterval(gameTimer.value);
    gameTimer.value = null;
  }
  if (balloonTimer.value) {
    clearInterval(balloonTimer.value);
    balloonTimer.value = null;
  }
  if (balloonDisappearTimer.value) {
    clearInterval(balloonDisappearTimer.value);
    balloonDisappearTimer.value = null;
  }
  balloons.value = [];
  gameStarted.value = false;
  showRandomGame.value = false;
  
  // 只有在时间结束时才显示提示，并且只显示2秒
  if (isTimeOut && gameScore.value > 0) {
    // 使用setTimeout来确保提示显示2秒后消失
    const message = proxy.Message.success(`游戏结束！您的得分是：${gameScore.value}`);
    setTimeout(() => {
      if (message && message.close) {
        message.close();
      }
    }, 2000);
    return;
  }
};

// 关闭随机游戏
const closeRandomGame = () => {
  showRandomGame.value = false;
};

  // 表单提交
  const submit = async () => {
    clearError();
    
    // 验证表单
    if (!formData.value.email) {
      errorMsg.value = "请输入邮箱地址";
      return;
    }
    
    if (isEmailInvalid.value) {
      errorMsg.value = "请输入正确的邮箱格式";
      return;
    }
    
    if (!isLogin.value && !formData.value.nickName) {
      errorMsg.value = "请输入昵称";
      return;
    }
    
    if (!formData.value.password) {
      errorMsg.value = "请输入密码";
      return;
    }
    
    if (!formData.value.checkCode) {
      errorMsg.value = "请输入验证码";
      return;
    }
    
    if (!isLogin.value && !formData.value.rePassword) {
      errorMsg.value = "请再次输入密码";
      return;
    }
    
    if (!isLogin.value && isRePasswordInvalid.value) {
      errorMsg.value = "两次输入的密码不一致";
      return;
    }
    
    // 检查是否显示随机游戏
    if (isLogin.value) {
      checkRandomGame();
    }
    
    // 提交表单
    if (isLogin.value) {
      showLoading.value = true;
    }
    
    try {
      let result = await proxy.Request({
        url: isLogin.value ? proxy.Api.login : proxy.Api.register,
        showLoading: false,
        showError: false,
        params: {
          email: formData.value.email,
          password: isLogin.value
            ? md5(formData.value.password)
            : formData.value.password,
          checkCode: formData.value.checkCode,
          nickName: formData.value.nickName,
          checkCodeKey: localStorage.getItem("checkCodeKey"),
          rePassword: !isLogin.value
            ? formData.value.rePassword
            : undefined,
        },
        errorCallback: (response) => {
          showLoading.value = false;
          refreshCaptcha();
          errorMsg.value = response.info;
        },
      });
      
      if (!result) return;
      
      if (isLogin.value) {
        localStorage.setItem("token", result.data.token);
        userInfoStore.setLoginInfo(result.data);
        userInfoStore.showLogin = false;
        proxy.Message.success("登录成功");
        resetForm(); // 登录成功后清除所有输入信息
        showLoading.value = false; // 重置加载状态
        
        // 触发首页数据刷新
        userInfoStore.updateLastReloadTime();
        
        // 如果当前在首页，则刷新页面
        if (router.currentRoute.value.path === '/' || router.currentRoute.value.path === '/index') {
          setTimeout(() => {
            window.location.reload();
          }, 500);
        }
      } else {
        proxy.Message.success("注册成功");
        toggleAuthMode();
        showLoading.value = false; // 重置加载状态
      }
    } catch (error) {
      showLoading.value = false;
      refreshCaptcha();
    }
  };
</script>

<style lang="scss" scoped>


// 切换标签
.auth-tabs {
  display: flex;
  border-bottom: 1px solid rgba(30, 60, 114, 0.1);
}

.tab-item {
  flex: 1;
  padding: 20px 0;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: rgba(30, 60, 114, 0.6);
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  
  &:hover {
    color: rgba(30, 60, 114, 0.8);
  }
  
  &.active {
   
    font-weight: 600;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, #1e3c72, #2a5298);
      border-radius: 3px 3px 0 0;
    }
  }
}

.form-container {
  padding: 30px 40px 40px;
  
  @media (max-width: 480px) {
    padding: 25px 20px 30px;
  }
}

// 表单样式
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

// 错误消息
.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 77, 79, 0.1);
  border-radius: 8px;
  color: #ff4d4f;
  font-size: 14px;
  margin-bottom: 8px;
  
  .error-icon {
    width: 18px;
    height: 18px;
    flex-shrink: 0;
  }
}

.error-fade-enter-active, .error-fade-leave-active {
  transition: all 0.3s ease;
}
.error-fade-enter-from, .error-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

// 输入字段
.input-field {
  position: relative;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: flex-end;
  
  .modern-input {
    width: 100%;
    padding: 12px 0 8px 0;
    background: transparent;
    border: none;
    border-radius: 0;
    font-size: 16px;
    color: #333;
    outline: none;
    transition: all 0.3s ease;
    
    &:focus + .input-label {
      color: #1e3c72;
      font-size: 14px;
      transform: translateY(-24px);
    }
    
    &:focus ~ .input-border {
      transform: scaleX(1);
    }
    
    &:not(:placeholder-shown) + .input-label {
      font-size: 14px;
      transform: translateY(-24px);
    }
  }
  
  .input-label {
    position: absolute;
    left: 0;
    top: 12px;
    color: rgba(30, 60, 114, 0.6);
    font-size: 16px;
    pointer-events: none;
    transition: all 0.3s ease;
    transform-origin: left top;
  }
  
  .input-border {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 2px;
    background: #1e3c72;
    transform: scaleX(0);
    transition: transform 0.3s ease;
  }
  
  .validation-icon {
    position: absolute;
    right: 0;
    bottom: 10px;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .success-icon {
      color: #52c41a;
    }
    
    .error-icon {
      color: #ff4d4f;
    }
  }
  
  .password-toggle {
    position: absolute;
    right: 0;
    bottom: 10px;
    width: 20px;
    height: 20px;
    cursor: pointer;
    color: rgba(30, 60, 114, 0.5);
    transition: color 0.3s ease;
    
    &:hover {
      color: #1e3c72;
    }
  }
  
  &.has-error .input-border {
    background: #ff4d4f;
  }
  
  &.has-error .input-label {
    color: #ff4d4f;
  }
  
  &.has-value .input-label {
    font-size: 14px;
    transform: translateY(-24px);
  }
}

// 验证码容器
.captcha-container {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  
  .captcha-input {
    flex: 1;
  }
  
  .captcha-image {
    position: relative;
    width: 100px;
    height: 40px;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s ease;
    
    &:hover {
      transform: scale(1.02);
    }
    
    &:active {
      transform: scale(0.98);
    }
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .refresh-icon {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.5);
      opacity: 0;
      transition: opacity 0.2s ease;
      
      svg {
        width: 24px;
        height: 24px;
        color: white;
      }
    }
    
    &:hover .refresh-icon {
      opacity: 1;
    }
  }
}

// 密码强度
.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  
  .strength-bars {
    display: flex;
    gap: 4px;
    
    .strength-bar {
      width: 30px;
      height: 4px;
      background: #e6e6e6;
      border-radius: 2px;
      transition: background 0.3s ease;
      
      &.active {
        background: #1e3c72;
        
        &:nth-child(1) {
          background: #ff4d4f;
        }
        
        &:nth-child(2) {
          background: #faad14;
        }
        
        &:nth-child(3) {
          background: #1890ff;
        }
        
        &:nth-child(4) {
          background: #52c41a;
        }
      }
    }
  }
  
  .strength-label {
    font-size: 12px;
    color: rgba(30, 60, 114, 0.6);
  }
}

// 提交按钮
.submit-btn {
  position: relative;
  padding: 14px 20px;
  background: linear-gradient(90deg, #1e3c72, #2a5298);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  margin-top: 10px;
  
  &:hover:not(.disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(30, 60, 114, 0.3);
  }
  
  &:active:not(.disabled) {
    transform: translateY(0);
  }
  
  &.disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none !important;
  }
  
  &.loading {
    pointer-events: none;
  }
  
  .loading-spinner {
    width: 20px;
    height: 20px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top: 2px solid white;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
}

// 切换模式
.auth-switch {
  text-align: center;
  margin-top: 20px;
  color: #1e3c72;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    color: #2a5298;
    text-decoration: underline;
  }
}

// 社交登录
.social-auth {
  margin-top: -5px;
  
  .divider {
    display: flex;
    align-items: center;
    margin: 20px 0;
    
    &::before, &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: rgba(30, 60, 114, 0.1);
    }
    
    span {
      padding: 0 15px;
      color: rgba(30, 60, 114, 0.6);
      font-size: 12px;
    }
  }
  
  .social-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;
    
    .social-btn {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      border: 1px solid rgba(30, 60, 114, 0.2);
      background: white;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
      }
      
      svg {
        width: 24px;
        height: 24px;
      }
      
      &.wechat {
        color: #07c160;
        
        &:hover {
          border-color: #07c160;
          background: rgba(7, 193, 96, 0.05);
        }
      }
      
      &.qq {
        color: #1890ff;
        
        &:hover {
          border-color: #1890ff;
          background: rgba(24, 144, 255, 0.05);
        }
      }
    }
  }
}

// 过渡动画
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.slide-down-enter-from, .slide-down-leave-to {
  max-height: 0;
  opacity: 0;
  margin-bottom: 0;
}
.slide-down-enter-to, .slide-down-leave-from {
  max-height: 100px;
  opacity: 1;
  margin-bottom: 24px;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

// 响应式设计
@media (max-width: 480px) {
  .form-container {
    padding: 25px 20px 30px;
  }
  
  .auth-form {
    gap: 20px;
  }
  
  .captcha-container {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
    
    .captcha-image {
      width: 100%;
      height: 50px;
      align-self: center;
      max-width: 150px;
    }
  }
  
  .submit-btn {
    padding: 16px 20px;
    font-size: 17px;
  }
}

// 游戏样式
.game-section {
  width: 100%;
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.game-header {
  text-align: center;
  margin-bottom: 20px;
  
  h3 {
    color: #1e3c72;
    margin-bottom: 10px;
    font-size: 18px;
  }
  
  .game-info {
    display: flex;
    gap: 20px;
    justify-content: center;
    font-size: 14px;
    color: #666;
  }
}

.game-container {
  width: 100%;
  height: 280px;
  border: 2px dashed #e6e6e6;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  background: #f8f9fa;
  cursor: pointer;
  
  &.v-else {
    cursor: default;
  }
}

.game-start-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  
  p {
    margin: 5px 0;
    color: #666;
    font-size: 16px;
  }
  
  p:first-child {
    font-size: 18px;
    font-weight: 600;
    color: #1e3c72;
  }
}

.balloons-area {
  position: relative;
  width: 100%;
  height: 100%;
}

.balloon {
  position: absolute;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  
  // 气球上升动画
  animation: balloonFloat 8s ease-in-out infinite;
  
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);
  }
  
  &:active {
    transform: scale(0.9);
  }
}

@keyframes balloonFloat {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-10px) scale(1.05);
  }
}

.game-controls {
  display: flex;
  gap: 15px;
  margin-top: 20px;
  
  .game-btn {
    padding: 10px 20px;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &.restart-btn {
      background: #1e3c72;
      color: white;
      
      &:hover {
        background: #2a5298;
        transform: translateY(-2px);
      }
    }
    
    &.end-btn {
      background: #ff6b6b;
      color: white;
      
      &:hover {
        background: #ff5252;
        transform: translateY(-2px);
      }
    }
  }
}

// 随机游戏弹窗样式
.random-game-popup {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.popup-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}

.popup-content {
  position: relative;
  background: white;
  border-radius: 12px;
  padding: 0;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 10px;
  border-bottom: 1px solid #e6e6e6;
  
  h3 {
    margin: 0;
    color: #1e3c72;
  }
  
  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: #999;
    
    &:hover {
      color: #666;
    }
  }
}

.popup-body {
  padding: 20px;
  text-align: center;
  
  p {
    margin: 10px 0;
    color: #666;
  }
}

.popup-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 20px;
  
  button {
    padding: 10px 25px;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &.play-btn {
      background: #1e3c72;
      color: white;
      
      &:hover {
        background: #2a5298;
      }
    }
    
    &.skip-btn {
      background: #e6e6e6;
      color: #666;
      
      &:hover {
        background: #d4d4d4;
      }
    }
  }
}

.game-popup-enter-active, .game-popup-leave-active {
  transition: all 0.3s ease;
}

.game-popup-enter-from, .game-popup-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

// Dialog组件圆角样式
.modern-login-dialog {
  border-radius: 12px !important;
  overflow: hidden !important;
}

// 确保Dialog内容的圆角
.modern-login-dialog :deep(.el-dialog) {
  border-radius: 12px !important;
}

.modern-login-dialog :deep(.el-dialog__header) {
  border-top-left-radius: 12px !important;
  border-top-right-radius: 12px !important;
}

.modern-login-dialog :deep(.el-dialog__body) {
  border-bottom-left-radius: 12px !important;
  border-bottom-right-radius: 12px !important;
}
</style>
