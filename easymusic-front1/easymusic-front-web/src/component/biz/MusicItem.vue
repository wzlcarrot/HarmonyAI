<template>
  <div class="music-item">
    <div class="cover">
      <template v-if="data.musicStatus == 1">
        <Cover :cover="data.cover" :width="100" @click="playMusic(false)"></Cover>
        <PlayBtn :data="data" @playList="playList"></PlayBtn>
        <div class="upload-cover" @click="uploadCover" v-if="userInfoStore.userInfo.userId == data.userId">
          上传封面
        </div>
      </template>
      <div class="createing" v-if="data.musicStatus == 0">
        <img :src="proxy.Utils.getLocalResource('img/loading.gif')" />
      </div>
    </div>
    <div class="music-info">
      <div v-if="data.musicStatus == 2">生成失败</div>
      <div v-else :class="[
          'music-title',
          data.musicStatus != 1 ? 'music-title-creating' : '',
        ]" @click="playMusic(true)">
        {{ data.musicTitle || "作品生成中......" }}
      </div>
      <div class="lyrics" v-if="data.musicType === 0">
        {{ musicLyrics || "--" }}
      </div>
      <div class="lyrics" v-if="data.musicType === 1">纯音乐，请欣赏</div>
      <!-- 进度条：只在生成中时显示 -->
      <div v-if="data.musicStatus == 0" class="progress-container">
        <el-progress 
          :percentage="progressPercent" 
          :stroke-width="4"
          :show-text="false"
          :status="progressPercent >= 99 ? 'warning' : ''"
          class="music-progress"
        />
        <div class="progress-text">
          <span v-if="progressPercent >= 99">处理中...</span>
          <span v-else>{{ progressPercent }}%</span>
        </div>
      </div>
      <div class="time">
        {{ proxy.Utils.seconds2Min(data.duration) || "--" }} ·
        {{ proxy.Utils.formatDate(data.createTime) }}
      </div>
    </div>
    <div class="op-panel" v-if="data.userId == userInfoStore.userInfo.userId">
      <template v-if="data.musicStatus === 0"> -- </template>
      <div class="op-btn" @click="renameMusic" v-if="data.musicStatus === 1">
        重命名
      </div>
      <div class="op-btn" @click="delMusic" v-if="data.musicStatus === 1 || data.musicStatus == 2">
        删除
      </div>
    </div>
  </div>

  <ImageCoverCut ref="imageCoverCutRef" :cutWidth="200" :scale="1" @cutImage="updateCover">
  </ImageCoverCut>

  <MusicTitleUpdate ref="musicTitleUpdateRef" @update="updateTitle"></MusicTitleUpdate>
</template>

<script setup>
import MusicTitleUpdate from './MusicTitleUpdate.vue'
import ImageCoverCut from '@/component/common/ImageCoverCut.vue'
import PlayBtn from '@/component/common/PlayBtn.vue'
import { ref, reactive, getCurrentInstance, nextTick, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
import { useMusicPlayStore } from '@/stores/musicPlay.js'
const musicPlayStore = useMusicPlayStore()

import { useUserInfoStore } from '@/stores/userInfoStore'
const userInfoStore = useUserInfoStore()

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
})

// 进度条相关
const progressPercent = ref(0) // 进度百分比
const startTime = ref(0) // 开始时间戳
const currentTime = ref(0) // 当前已用时间（秒）
const duration = ref(60) // 预估总时长（秒），默认60秒
const ESTIMATED_DURATION = 60 // 预估每首音乐创建时间（秒）

// 计算进度百分比
const updateProgress = () => {
  if (props.data.musicStatus !== 0) {
    // 如果音乐已完成或失败，隐藏进度条
    progressPercent.value = 0
    return
  }
  
  if (startTime.value > 0) {
    // 计算已用时间（秒）
    currentTime.value = Math.floor((Date.now() - startTime.value) / 1000)
    
    // 如果已用时间超过预估时间，动态延长预估时间
    // 这样可以避免进度条卡在100%但音乐还没完成的情况
    if (currentTime.value >= duration.value) {
      // 每次延长30秒，最多延长到300秒（5分钟）
      // 延长前先保存当前进度，确保延长后不会导致进度下降
      const oldPercent = progressPercent.value
      const oldDuration = duration.value
      
      if (duration.value < 300) {
        duration.value = Math.min(duration.value + 30, 300)
        
        // 如果延长后会导致进度下降，则保持当前进度不变
        const newPercent = Math.round((currentTime.value / duration.value) * 100)
        if (newPercent < oldPercent) {
          // 恢复原来的时长，或者调整时长使进度不下降
          duration.value = Math.max(oldDuration, Math.ceil((currentTime.value / oldPercent) * 100))
        }
      }
    }
    
    // 计算进度百分比
    if (duration.value > 0) {
      const newPercent = Math.round((currentTime.value / duration.value) * 100)
      
      // 确保进度只能增加，不能减少（防止回退）
      if (newPercent > progressPercent.value) {
        progressPercent.value = newPercent
      }
      
      // 如果进度超过100%，限制为99%（显示即将完成，但还没完成）
      if (progressPercent.value > 100) {
        progressPercent.value = 99
      }
    }
  }
}

let progressTimer = null

// 初始化开始时间
const initStartTime = () => {
  // 重置开始时间，使用音乐的创建时间
  if (props.data.createTime) {
    const createTimeMs = new Date(props.data.createTime).getTime()
    // 如果创建时间是有效的（不是NaN），使用它
    if (!isNaN(createTimeMs) && createTimeMs > 0) {
      startTime.value = createTimeMs
    } else {
      // 如果创建时间无效，使用当前时间
      startTime.value = Date.now()
    }
  } else {
    // 如果没有创建时间，使用当前时间
    startTime.value = Date.now()
  }
  
  // 重置当前时间和进度
  currentTime.value = 0
  progressPercent.value = 0
  duration.value = ESTIMATED_DURATION // 重置预估时长为默认值
}

// 启动进度更新
const startProgressUpdate = () => {
  // 先停止之前的定时器（如果存在）
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  
  // 重新初始化开始时间（确保每次开始生成时都重置）
  initStartTime()
  
  // 立即更新一次
  updateProgress()
  
  // 启动定时器，每秒更新一次
  progressTimer = setInterval(() => {
    updateProgress()
  }, 1000)
}

// 停止进度更新
const stopProgressUpdate = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  progressPercent.value = 0
  startTime.value = 0
  currentTime.value = 0
}

// 监听音乐状态变化
watch(() => props.data.musicStatus, (newStatus, oldStatus) => {
  if (newStatus !== 0) {
    // 音乐已完成或失败，停止进度更新
    stopProgressUpdate()
  } else {
    // 音乐开始生成，启动进度更新
    // 如果之前不是生成中状态，或者状态从其他状态变为生成中，重新初始化
    if (oldStatus !== 0 || startTime.value === 0) {
      startProgressUpdate()
    }
  }
}, { immediate: true })

// 监听创建时间变化（处理新创建的音乐）
watch(() => props.data.createTime, (newCreateTime, oldCreateTime) => {
  // 如果音乐正在生成中，且创建时间变化了，重新初始化开始时间
  // 但只有在创建时间真正变化且之前没有初始化过时才重新初始化
  if (props.data.musicStatus === 0 && newCreateTime && newCreateTime !== oldCreateTime) {
    // 如果之前已经有进度了，说明已经初始化过，不要重置
    if (startTime.value === 0 || progressPercent.value === 0) {
      initStartTime()
      // 立即更新一次进度
      updateProgress()
    }
  }
})

onMounted(() => {
  // 如果音乐正在生成中，启动进度更新
  if (props.data.musicStatus === 0) {
    startProgressUpdate()
  }
})

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
})

const emits = defineEmits(['playList', 'reload'])
const playMusic = (jumpDetail) => {
  if (props.data.musicStatus != 1) {
    return
  }
  emits('playList')
  musicPlayStore.play({ ...props.data })
  if (!jumpDetail) {
    return
  }
  router.push(`/play/${props.data.musicId}`)
}

const playList = () => {
  emits('playList')
}
const musicLyrics = computed(() => {
  if (!props.data.lyrics) {
    return ''
  }
  const lyricsArray = JSON.parse(props.data.lyrics)
  const lyricsTextArray = lyricsArray.map((item) => {
    return item.text
  })
  return lyricsTextArray.join(' ')
})

//上传封面
const imageCoverCutRef = ref()
const uploadCover = () => {
  imageCoverCutRef.value.show()
}

const updateCover = async (file) => {
  let result = await proxy.Request({
    url: proxy.Api.uploadMusicCover,
    params: {
      cover: file,
      musicId: props.data.musicId,
    },
  })
  if (!result) {
    return
  }
  props.data.cover = result.data
}

const delMusic = () => {
  proxy.Confirm({
    message: `确定要删除歌曲[${props.data.musicTitle}]吗?`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.delMusic,
        params: {
          musicId: props.data.musicId,
        },
      })
      if (!result) {
        return
      }
      emits('reload')
    },
  })
}

const musicTitleUpdateRef = ref()
const renameMusic = () => {
  musicTitleUpdateRef.value.show(props.data)
}
const updateTitle = (title) => {
  props.data.musicTitle = title
}
</script>

<style lang="scss" scoped>
.music-item {
  margin: 10px;
  padding: 10px;
  margin-bottom: 10px;
  border-width: 1px;
  border-style: solid;
  border-color: hsla(0, 0%, 100%, 0.2);
  border-radius: 10px;
  color: var(--text);
  display: flex;
  align-items: center;
  overflow: hidden;
  .cover {
    position: relative;
    .upload-cover {
      width: 100%;
      position: absolute;
      left: 0px;
      bottom: 0px;
      background: rgba(0, 0, 0, 0.5);
      z-index: 1;
      color: var(--text);
      text-align: center;
      cursor: pointer;
      padding: 3px 0px;
      font-size: 13px;
    }
    .createing {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100px;
      height: 100px;
      background: #1f212d;
      border-radius: 5px;
      img {
        width: 20px;
      }
    }
  }
  .music-info {
    flex: 1;
    width: 0;
    margin-left: 10px;
    .music-title {
      display: inline-block;
      font-size: 20px;
      cursor: pointer;
      &:hover {
        color: var(--activeText);
      }
    }
    .music-title-creating {
      cursor: not-allowed;
      color: var(--text);
      &:hover {
        color: var(--text);
      }
    }
    .lyrics {
      margin-top: 10px;
      color: var(--text);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 12px;
    }
    .time {
      margin-top: 5px;
      font-size: 13px;
      color: var(--text);
    }
    .progress-container {
      margin-top: 10px;
      display: flex;
      align-items: center;
      gap: 10px;
      .music-progress {
        flex: 1;
        :deep(.el-progress-bar__outer) {
          background-color: #3f3a60;
          border-radius: 10px;
        }
        :deep(.el-progress-bar__inner) {
          background: linear-gradient(90deg, var(--purple), #8b5cf6);
          border-radius: 10px;
        }
      }
      .progress-text {
        font-size: 12px;
        color: var(--text);
        min-width: 35px;
        text-align: right;
      }
    }
  }
  .op-panel {
    width: 80px;
    margin-left: 10px;
    display: flex;
    font-size: 14px;
    justify-content: space-between;
    align-items: center;
    .op-btn {
      cursor: pointer;
    }
  }
}
</style>
