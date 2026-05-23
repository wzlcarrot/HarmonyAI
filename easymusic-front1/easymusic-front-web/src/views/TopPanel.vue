<template>
  <div class="top-panel">
    <div class="iconfont icon-list" @click="showDrawerHandler"></div>
    <div class="title">EasyMusic</div>
    <div class="iconfont icon-search" @click="goToSearch"></div>
  </div>

  <el-drawer v-model="showDrawer" direction="ltr" header-class="left-side-header" body-class="left-side-body"
    size="200px">
    <LeftSide></LeftSide>
  </el-drawer>
</template>

<script setup>
import LeftSide from './LeftSide.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const showDrawer = ref(false)
const showDrawerHandler = () => {
  showDrawer.value = true
}

const goToSearch = () => {
  router.push('/search')
}
</script>

<style lang="scss" scoped>
.top-panel {
  display: none;
}
@media (max-width: 500px) {
  .top-panel {
    display: flex;
    background: var(--cardBg);
    border-radius: 5px;
    position: fixed;
    top: 10px;
    left: 10px;
    right: 10px;
    display: flex;
    align-items: center;
    padding: 12px;
    z-index: 10;
    border: 1px solid var(--borderColor);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    .icon-list {
      color: var(--text);
      font-size: 25px;
    }
    .title {
      color: var(--text);
      margin-left: 15px;
      flex: 1;
    }
    .icon-search {
      color: var(--text);
      font-size: 25px;
      cursor: pointer;
      margin-left: 10px;
      transition: all 0.3s;
      &:hover {
        color: var(--primary);
      }
    }
  }
}
</style>
