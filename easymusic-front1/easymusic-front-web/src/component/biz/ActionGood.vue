<template>
  <div class="action-good-container" @click="goodMusic">
    <div
      :class="['iconfont', data.doGood ? 'icon-good-solid' : 'icon-good']"
    ></div>
    <div class="good-count" v-if="data.goodCount !== undefined && data.goodCount !== null">
      {{ data.goodCount }}
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  computed,
  watch,
} from "vue";
import { useRouter, useRoute } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
import { useUserInfoStore } from "@/stores/userInfoStore";
const userInfoStore = useUserInfoStore();

import { useMusicPlayStore } from "@/stores/musicPlay.js";
const musicPlayStore = useMusicPlayStore();

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
});

const emits = defineEmits(['goodCountUpdated']);

const goodMusic = async () => {
  if (!userInfoStore.checkLogin()) {
    return;
  }
  let result = await proxy.Request({
    url: proxy.Api.doGood,
    showLoading: false,
    params: {
      musicId: props.data.musicId,
    },
  });
  if (!result) {
    return;
  }
  const oldDoGood = props.data.doGood;
  props.data.doGood = !props.data.doGood;
  
  // 更新点赞数（本地更新，如果后端返回了最新值则使用后端值）
  if (props.data.goodCount !== undefined && props.data.goodCount !== null) {
    // 如果后端返回了最新的点赞数，使用后端值；否则本地更新
    if (result.data && typeof result.data === 'number') {
      props.data.goodCount = result.data;
    } else {
      // 本地更新：点赞+1，取消点赞-1
      props.data.goodCount = props.data.doGood ? (props.data.goodCount + 1) : Math.max(0, props.data.goodCount - 1);
    }
  } else if (props.data.goodCount === undefined || props.data.goodCount === null) {
    // 如果之前没有点赞数，设置为1或0
    props.data.goodCount = props.data.doGood ? 1 : 0;
  }
  
  //判断当前正在播放的是不是该音乐，播放器和列表保持同步
  if (musicPlayStore.currentMusic?.musicId == props.data.musicId) {
    musicPlayStore.currentMusic.doGood = props.data.doGood;
    if (musicPlayStore.currentMusic.goodCount !== undefined && musicPlayStore.currentMusic.goodCount !== null) {
      musicPlayStore.currentMusic.goodCount = props.data.goodCount;
    }
  }
  
  // 触发自定义事件，通知父组件点赞数已更新（用于列表刷新）
  emits('goodCountUpdated', {
    musicId: props.data.musicId,
    goodCount: props.data.goodCount,
    doGood: props.data.doGood
  });
};

watch(
  () => musicPlayStore.currentMusic.doGood,
  (newVal, oldVal) => {
    if (
      newVal != null &&
      props.data.musicId === musicPlayStore.currentMusic.musicId
    ) {
      props.data.doGood = newVal;
    }
  },
  { immediate: true, deep: true }
);
</script>

<style lang="scss" scoped>
.action-good-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  .iconfont {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    -webkit-backdrop-filter: blur(12px);
    backdrop-filter: blur(12px);
    border-radius: 50%;
    &:hover {
      background: #3e3450;
    }
  }
  .icon-good-solid {
    color: var(--activeText);
  }
  .good-count {
    margin-top: 4px;
    font-size: 12px;
    color: var(--text);
    opacity: 0.8;
    min-height: 16px;
    line-height: 16px;
  }
}
</style>
