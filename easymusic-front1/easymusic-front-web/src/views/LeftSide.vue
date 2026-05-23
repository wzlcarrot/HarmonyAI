<template>
  <div class="left-side-panel">
    <div class="bg"></div>
    <div class="left-side">
      <div class="logo">EasyMusic</div>
      <div class="menu-list">
        <template v-for="item in menuList">
          <div :class="[
              'menu-item',
              item.codes.includes(route.meta.code) ? 'active' : '',
            ]" @click="jump(item)">
            <!-- 搜索菜单项使用 SVG 图标 -->
            <div v-if="item.icon === 'search'" class="search-icon-svg">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/>
                <path d="m20 20-4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <!-- 其他菜单项使用字体图标 -->
            <div v-else :class="['iconfont', 'icon-' + item.icon]"></div>
            <div class="menu-name">{{ item.name }}</div>
          </div>
        </template>
      </div>
      <div class="integral-panel">
        <div class="integra">
          积分：{{ userInfoStore.userInfo.integral || 0 }}
        </div>
        <div class="record-btn" @click="showIntegralRecord">积分记录</div>
      </div>
      <div class="user-info-panel">
        <div class="login-btn" @click="login" v-if="Object.keys(userInfoStore.userInfo).length == 0">
          登录
        </div>
        <el-popover popper-class="user-info-popper" placement="top" trigger="click" :show-arrow="false" :offset="5"
          :width="150" ref="userInfoPopoverRef">
          <template #reference>
            <div class="user-info" v-if="Object.keys(userInfoStore.userInfo).length > 0">
              <Avatar :avatar="userInfoStore.userInfo.avatar" :width="30"></Avatar>
              <div class="user-name">{{ userInfoStore.userInfo.nickName }}</div>
            </div>
          </template>
          <div class="menu-item" @click="updatePassword">修改密码</div>
          <div class="menu-item" @click="editUserInfo">编辑个人资料</div>
          <div class="menu-item" @click="logout">退出登录</div>
        </el-popover>
      </div>
    </div>
  </div>
  <EditUser ref="editUserRef"></EditUser>

  <UpdatePassword ref="updatePasswordRef"></UpdatePassword>

  <IntegralRecord ref="integralRecordRef"></IntegralRecord>
</template>

<script setup>
import IntegralRecord from '@/views/my/IntegralRecord.vue'
import UpdatePassword from '@/views/my/UpdatePassword.vue'
import EditUser from '@/views/my/EditUser.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

const menuList = ref([
  {
    name: '首页',
    icon: 'home',
    codes: ['index'],
    path: '/',
  },
  {
    name: '搜索',
    icon: 'search',
    path: '/search',
    codes: ['search'],
  },
  {
    name: '创作音乐',
    icon: 'music',
    path: '/idea',
    codes: ['idea', 'pure'],
  },
  {
    name: '我的',
    icon: 'user',
    path: '/my',
    codes: ['my'],
  },
  {
    name: '充值',
    icon: 'buy',
    path: '/buy',
    codes: ['buy'],
  },
])

const jump = (item) => {
  if (item.path == '/my' && !userInfoStore.checkLogin()) {
    return
  }
  router.push(item.path)
}

const login = () => {
  userInfoStore.showLogin = true
}

const userInfoPopoverRef = ref()
const logout = async () => {
  userInfoPopoverRef.value.hide()
  proxy.Confirm({
    message: '确定要退出吗?',
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.logout,
      })
      if (!result) {
        return
      }
      userInfoStore.userInfo = {}
      userInfoStore.showLogin = false
      localStorage.removeItem('token')
    },
  })
}

const editUserRef = ref()
const editUserInfo = () => {
  userInfoPopoverRef.value.hide()
  editUserRef.value.show()
}

const updatePasswordRef = ref()
const updatePassword = () => {
  userInfoPopoverRef.value.hide()
  updatePasswordRef.value.show()
}

const integralRecordRef = ref()
const showIntegralRecord = () => {
  integralRecordRef.value.show()
}
</script>

<style lang="scss" scoped>
.left-side-panel {
  width: 200px;
  height: 100%;
  color: var(--text);
  overflow: hidden;
  background: var(--cardBg);
  border-right: 1px solid var(--borderColor);
  .bg {
    display: none; // 隐藏原有背景图
  }
  .left-side {
    position: absolute;
    z-index: 1;
    width: 200px;
    height: calc(100vh);
    display: flex;
    flex-direction: column;
    .logo {
      font-size: 20px;
      font-weight: bold;
      padding: 20px;
    }
    .menu-list {
      flex: 1;
      .menu-item {
        padding: 10px 0px 10px 20px;
        color: var(--text);
        display: flex;
        align-items: center;
        cursor: pointer;
        &:hover {
          color: var(--hiText);
        }
        .iconfont {
          font-size: 20px;
        }
        .search-icon-svg {
          width: 20px;
          height: 20px;
          display: flex;
          align-items: center;
          justify-content: center;
          svg {
            width: 100%;
            height: 100%;
            color: currentColor;
          }
        }
        .menu-name {
          margin-left: 10px;
        }
      }
      .active {
        color: var(--activeText);
        position: relative;
        &::before {
          content: '';
          left: 0px;
          top: 12px;
          bottom: 12px;
          width: 3px;
          background: var(--activeText);
          position: absolute;
          border-radius: 2px;
          font-weight: bold;
        }
        &:hover {
          color: var(--activeText);
        }
      }
    }
  }

  .integral-panel {
    color: var(--text);
    display: flex;
    justify-content: space-between;
    justify-items: center;
    padding: 10px;
    .integral {
      flex: 1;
    }
    .record-btn {
      display: flex;
      align-items: center;
      font-size: 13px;
      cursor: pointer;
      color: var(--activeText);
    }
  }

  .user-info-panel {
    padding: 10px;
    .login-btn {
      cursor: pointer;
      padding: 10px;
      text-align: center;
      border-radius: 50px;
      background: var(--btnBg);
      color: var(--text);
    }
    .user-info {
      display: flex;
      align-items: center;
      background: var(--bgColor);
      border: 1px solid var(--borderColor);
      border-radius: 20px;
      padding: 5px 10px;
      cursor: pointer;
      .user-name {
        flex: 1;
        width: 0;
        margin-left: 10px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: var(--text);
      }
      .icon-logout {
        margin-left: 5px;
        color: var(--text);
      }
    }
  }

  @media (max-width: 500px) {
    .bg,
    .logo {
      display: none;
    }

    .left-side {
      height: calc(100vh - 50px);
    }
  }
}
</style>
