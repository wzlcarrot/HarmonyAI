<template>
  <div class="index-body">
    <div class="part-title">
      <div class="title">推荐歌曲</div>
      <div class="title-op">
        <div
          :class="[
            'iconfont icon-narrow-left',
            disableType == 1 ? 'disable' : '',
          ]"
          @click="changeCommend(1)"
        ></div>
        <div
          :class="[
            'iconfont icon-narrow-right',
            disableType == -1 ? 'disable' : '',
          ]"
          @click="changeCommend(-1)"
        ></div>
      </div>
    </div>
    <CommendList
      ref="commendListRef"
      @disableType="hotChangeTypeHandler"
    ></CommendList>
    <div class="part-title latest-title">
      <div>最新发布歌曲</div>
      <router-link to="latest" class="more"
        >更多<span class="iconfont iconfont icon-narrow-right"></span>
      </router-link>
    </div>
    <div class="latest-list">
      <LatestList ref="latestListRef" :indexType="1"></LatestList>
    </div>
  </div>
</template>

<script setup>
import LatestList from "./LatestList.vue";
import CommendList from "./CommendList.vue";
import { ref, reactive, getCurrentInstance, nextTick, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

import { useUserInfoStore } from "@/stores/userInfoStore";
const userInfoStore = useUserInfoStore();

const commendListRef = ref();
const latestListRef = ref();
const disableType = ref(1);
const hotChangeTypeHandler = (type) => {
  disableType.value = type;
};
const changeCommend = (type) => {
  commendListRef.value.change(type);
};

// 监听登录状态变化
watch(() => userInfoStore.lastReloadTime, (newVal, oldVal) => {
  if (newVal !== oldVal && newVal > 0) {
    // 登录状态变化，重新加载数据
    if (commendListRef.value) {
      commendListRef.value.loadCommend && commendListRef.value.loadCommend();
    }
    
    // 重新加载最新音乐列表
    if (latestListRef.value) {
      latestListRef.value.loadLatestMusic && latestListRef.value.loadLatestMusic();
    }
  }
});

// 监听用户信息变化
watch(() => userInfoStore.userInfo, (newVal, oldVal) => {
  if (newVal && Object.keys(newVal).length > 0 && (!oldVal || Object.keys(oldVal).length === 0)) {
    // 从未登录到登录状态，重新加载数据
    if (commendListRef.value) {
      commendListRef.value.loadCommend && commendListRef.value.loadCommend();
    }
    
    // 重新加载最新音乐列表
    if (latestListRef.value) {
      latestListRef.value.loadLatestMusic && latestListRef.value.loadLatestMusic();
    }
  }
});
</script>

<style lang="scss" scoped>
.index-body {
  padding: 10px 0px 0px 10px;
  background: #ced4da; /* 首页使用更深一点的背景色 */
  .part-title {
    font-size: 25px;
    color: var(--text);
    display: flex;
    align-items: center;
    justify-content: space-between;
    .title {
      color: var(--text); /* 确保标题颜色与最新歌曲区域一致 */
    }
    .title-op {
      display: flex;
      .iconfont {
        background: var(--cardBg);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 10px;
        margin-right: 10px;
        cursor: pointer;
        color: var(--text);
        &:hover {
          opacity: 0.8;
        }
      }
      .disable {
        opacity: 0.5;
        cursor: not-allowed;
        &:hover {
          opacity: 0.5;
        }
      }
    }
  }
  .latest-title {
    margin-top: 10px;
    .more {
      margin-left: 20px;
      text-decoration: none;
      margin-right: 20px;
      color: var(--text);
      font-size: 14px;
      padding: 5px 10px;
      display: flex;
      align-items: center;
      .iconfont {
        font-size: 12px;
        margin-left: 3px;
      }
      &:hover {
        color: var(--activeText);
        background: var(--cardBg);
        border-radius: 15px;
      }
    }
  }
  .latest-list {
    padding-right: 10px;
  }
}
</style>
