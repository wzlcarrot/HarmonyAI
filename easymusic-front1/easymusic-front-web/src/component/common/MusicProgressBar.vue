<template>
  <div v-if="showProgress" class="music-progress-container">
    <div class="progress-content">
      <div class="progress-title">音乐创作中...</div>
      <el-progress 
        :percentage="progressPercent" 
        :status="progressPercent === 100 ? 'success' : ''"
        :stroke-width="8"
        class="progress-bar"
      />
      <div class="progress-text">
        {{ completed }}/{{ total }} 已完成
        <span v-if="currentTime > 0" class="time-text">
          (已用时 {{ formatTime(currentTime) }})
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import { mitter } from '@/eventbus/eventBus.js'

const { proxy } = getCurrentInstance()

const showProgress = ref(false)
const trackingMusicIds = ref([])
const total = ref(0)
const completed = ref(0)

// 时间相关（类似音频播放器）
const startTime = ref(0) // 开始时间戳
const currentTime = ref(0) // 当前已用时间（秒）
const duration = ref(0) // 预估总时长（秒）
const progressPercent = ref(0) // 进度百分比

// 预估每首音乐的创建时间（秒），默认值，会根据实际完成情况动态调整
const ESTIMATED_DURATION_PER_MUSIC = 60

// 存储已完成音乐的实际创建时间（用于动态调整预估时长）
const completedMusicTimes = ref([]) // 存储已完成音乐的实际创建时间（秒）

// 格式化时间显示（秒转分:秒）
const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 时间更新处理（类似音频播放器的 onTimeUpdateHandler）
const onTimeUpdateHandler = () => {
  if (!showProgress.value || startTime.value === 0) {
    return
  }
  
  // 计算已用时间（秒）
  currentTime.value = Math.floor((Date.now() - startTime.value) / 1000)
  
  // 计算进度百分比（类似音频播放器的计算方式）
  if (duration.value > 0) {
    const newPercent = Math.round((currentTime.value / duration.value) * 100)
    
    // 确保进度只能增加，不能减少（防止回退）
    if (newPercent > progressPercent.value) {
      progressPercent.value = newPercent
    }
    
    // 如果进度超过100%，限制为100%
    if (progressPercent.value > 100) {
      progressPercent.value = 100
    }
  }
}

// 检查音乐状态
const checkMusicStatus = async () => {
  if (trackingMusicIds.value.length === 0) {
    return
  }
  
  try {
    let result = await proxy.Request({
      url: proxy.Api.loadCreatingMusic,
      showLoading: false,
      params: {
        musicIds: trackingMusicIds.value.join(','),
      },
    })
    
    if (result && result.data && result.data.length > 0) {
      let newCompleted = 0
      const remainingIds = []
      const previousCompleted = completed.value // 记录之前的完成数量
      
      result.data.forEach((music) => {
        // musicStatus: 0=生成中, 1=完成, 2=失败
        if (music.musicStatus !== 0) {
          newCompleted++
        } else {
          remainingIds.push(music.musicId)
        }
      })
      
      // 如果有新完成的音乐，记录当前已用时间
      if (newCompleted > previousCompleted && currentTime.value > 0) {
        const newlyCompletedCount = newCompleted - previousCompleted
        // 为每个新完成的音乐记录当前已用时间（所有音乐同时开始，所以使用当前时间）
        for (let i = 0; i < newlyCompletedCount; i++) {
          // 只记录合理的创建时间（大于0且小于1小时）
          if (currentTime.value > 0 && currentTime.value < 3600) {
            completedMusicTimes.value.push(currentTime.value)
          }
        }
      }
      
      completed.value = newCompleted
      trackingMusicIds.value = remainingIds
      
      // 根据已完成音乐的实际创建时间动态调整预估总时长
      if (completedMusicTimes.value.length > 0) {
        // 计算平均创建时间
        const avgDuration = completedMusicTimes.value.reduce((sum, time) => sum + time, 0) / completedMusicTimes.value.length
        // 预估剩余音乐的平均创建时间（使用已完成音乐的平均时间）
        const estimatedRemainingTime = avgDuration * (total.value - completed.value)
        // 总时长 = 已用时间 + 预估剩余时间
        const newDuration = currentTime.value + estimatedRemainingTime
        
        // 只有当新的预估时长比当前已用时间大时才更新（避免预估时长变小）
        // 同时确保不会导致进度百分比下降（进度只能增加，不能减少）
        if (newDuration > currentTime.value) {
          const oldPercent = progressPercent.value
          const newPercent = Math.round((currentTime.value / newDuration) * 100)
          // 只有当新进度不会导致回退时才更新
          if (newPercent >= oldPercent) {
            duration.value = Math.ceil(newDuration)
          }
        }
      }
      
      // 如果全部完成，设置进度为100%并延迟隐藏
      if (newCompleted >= total.value) {
        progressPercent.value = 100
        setTimeout(() => {
          showProgress.value = false
          resetProgress()
        }, 2000) // 延迟2秒，让用户看到100%完成
      }
    }
  } catch (e) {
    console.error('检查音乐状态失败', e)
  }
}

// 重置进度
const resetProgress = () => {
  trackingMusicIds.value = []
  total.value = 0
  completed.value = 0
  currentTime.value = 0
  startTime.value = 0
  duration.value = 0
  progressPercent.value = 0
  completedMusicTimes.value = []
}

// 开始跟踪音乐进度
const startTracking = (musicIds) => {
  if (!musicIds || musicIds.length === 0) {
    return
  }
  
  trackingMusicIds.value = [...musicIds]
  total.value = musicIds.length
  completed.value = 0
  
  // 初始化时间（类似音频播放器的 onLoadedMetadataHandler）
  startTime.value = Date.now()
  currentTime.value = 0
  duration.value = ESTIMATED_DURATION_PER_MUSIC * total.value // 预估总时长
  progressPercent.value = 0
  
  showProgress.value = true
  
  // 立即检查一次状态
  checkMusicStatus()
  
  // 启动状态检查定时器（每3秒检查一次）
  if (statusTimer) {
    clearInterval(statusTimer)
  }
  statusTimer = setInterval(() => {
    if (showProgress.value && trackingMusicIds.value.length > 0) {
      checkMusicStatus()
    } else {
      clearInterval(statusTimer)
      statusTimer = null
    }
  }, 3000)
}

// 监听新音乐创建事件
const handleNewMusic = (musicIds) => {
  if (musicIds && musicIds.length > 0) {
    if (trackingMusicIds.value.length > 0) {
      // 如果有正在跟踪的音乐，合并列表
      const allIds = [...new Set([...trackingMusicIds.value, ...musicIds])]
      total.value = allIds.length
      trackingMusicIds.value = allIds
      // 重新计算预估总时长
      duration.value = ESTIMATED_DURATION_PER_MUSIC * total.value
    } else {
      startTracking(musicIds)
    }
  }
}

let statusTimer = null // 状态检查定时器
let timeUpdateTimer = null // 时间更新定时器（类似音频播放器的 timeupdate 事件）

onMounted(() => {
  mitter.on('newMusic', handleNewMusic)
  
  // 启动时间更新定时器（每秒更新一次，类似音频播放器的 timeupdate 事件）
  timeUpdateTimer = setInterval(() => {
    onTimeUpdateHandler()
  }, 1000)
})

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
  if (timeUpdateTimer) {
    clearInterval(timeUpdateTimer)
    timeUpdateTimer = null
  }
  mitter.off('newMusic', handleNewMusic)
})
</script>

<style lang="scss" scoped>
.music-progress-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  background: rgba(41, 36, 78, 0.95);
  border-radius: 10px;
  padding: 20px 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  min-width: 300px;
  backdrop-filter: blur(10px);
  animation: slideDown 0.3s ease-out;
  
  @keyframes slideDown {
    from {
      opacity: 0;
      transform: translateX(-50%) translateY(-20px);
    }
    to {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
  }
  
  .progress-content {
    .progress-title {
      color: var(--text);
      font-size: 16px;
      font-weight: bold;
      margin-bottom: 15px;
      text-align: center;
    }
    
    .progress-bar {
      margin-bottom: 10px;
      :deep(.el-progress-bar__outer) {
        background-color: #3f3a60;
      }
      :deep(.el-progress-bar__inner) {
        background: linear-gradient(90deg, var(--purple), #8b5cf6);
        transition: width 0.3s ease;
      }
    }
    
    .progress-text {
      color: var(--text);
      font-size: 14px;
      text-align: center;
      opacity: 0.8;
      
      .time-text {
        margin-left: 8px;
        opacity: 0.7;
      }
    }
  }
}
</style>

