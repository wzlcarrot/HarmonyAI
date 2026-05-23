<template>
  <div class="search-body">
    <!-- 搜索框 -->
    <div class="search-header">
      <div class="search-box">
        <span class="iconfont icon-search search-icon"></span>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索音乐..."
          class="search-input"
          @keyup.enter="handleSearch"
          @input="handleInput"
        />
        <div class="search-btn" @click="handleSearch">
          <span class="iconfont icon-search"></span>
        </div>
      </div>
    </div>

    <!-- 搜索条件和排序 -->
    <div class="search-filters" v-if="hasSearched">
      <div class="filter-item">
        <span class="filter-label">音乐类型：</span>
        <div class="filter-options">
          <span
            :class="['filter-option', musicType === null ? 'active' : '']"
            @click="changeMusicType(null)"
          >
            全部
          </span>
          <span
            :class="['filter-option', musicType === 0 ? 'active' : '']"
            @click="changeMusicType(0)"
          >
            音乐
          </span>
          <span
            :class="['filter-option', musicType === 1 ? 'active' : '']"
            @click="changeMusicType(1)"
          >
            纯音乐
          </span>
        </div>
      </div>
      <div class="filter-item">
        <span class="filter-label">排序：</span>
        <div class="filter-options">
          <span
            :class="['filter-option', sortType === 'playCount' ? 'active' : '']"
            @click="changeSortType('playCount')"
          >
            播放量
          </span>
          <span
            :class="['filter-option', sortType === 'time' ? 'active' : '']"
            @click="changeSortType('time')"
          >
            最新发布
          </span>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results" v-if="hasSearched">
      <div class="result-header" v-if="searchResult">
        <span class="result-count">
          找到 {{ searchResult.totalCount || 0 }} 首音乐
        </span>
      </div>

      <!-- 加载状态 -->
      <div class="loading-container" v-if="loading">
        <img :src="proxy.Utils.getLocalResource('img/loading.gif')" />
        <div>搜索中...</div>
      </div>

      <!-- 结果列表 -->
      <div class="result-list" v-else-if="searchResult && searchResult.musicList && searchResult.musicList.length > 0">
        <div
          v-for="(music, index) in searchResult.musicList"
          :key="music.musicId"
          class="result-item"
          @click="goToMusicDetail(music.musicId)"
        >
          <div class="music-cover">
            <Cover :cover="music.cover" :width="80"></Cover>
            <PlayBtn :data="music" @playList="playList"></PlayBtn>
          </div>
          <div class="music-info">
            <div class="music-title">{{ music.musicTitle }}</div>
            <div class="music-author">作者：{{ music.nickName || '未知' }}</div>
            <div class="music-stats">
              <span>播放 {{ music.playCount || 0 }}</span>
              <span>点赞 {{ music.goodCount || 0 }}</span>
              <span>{{ proxy.Utils.formatDate(music.createTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="searchResult.pageTotal > 1">
          <div
            :class="['page-btn', searchResult.pageNo <= 1 ? 'disabled' : '']"
            @click="changePage(searchResult.pageNo - 1)"
          >
            上一页
          </div>
          <div class="page-info">
            第 {{ searchResult.pageNo }} / {{ searchResult.pageTotal }} 页
          </div>
          <div
            :class="['page-btn', searchResult.pageNo >= searchResult.pageTotal ? 'disabled' : '']"
            @click="changePage(searchResult.pageNo + 1)"
          >
            下一页
          </div>
        </div>
      </div>

      <!-- 空结果 -->
      <div class="no-result" v-else>
        <NoData :text="'没有找到相关音乐，试试其他关键词吧~'"></NoData>
      </div>
    </div>

    <!-- 未搜索状态 -->
    <div class="search-tips" v-else>
      <div class="tips-icon">
        <span class="iconfont icon-search" style="font-size: 60px; color: #999;"></span>
      </div>
      <div class="tips-text">输入关键词搜索音乐</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import Cover from "@/component/common/Cover.vue";
import PlayBtn from "@/component/common/PlayBtn.vue";
import NoData from "@/component/common/NoData.vue";
import { useMusicPlayStore } from "@/stores/musicPlay.js";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const musicPlayStore = useMusicPlayStore();

// 搜索关键词
const keyword = ref("");
// 音乐类型筛选 0:音乐 1:纯音乐 null:全部
const musicType = ref(null);
// 排序类型 playCount:播放量 time:最新发布
const sortType = ref("playCount");
// 当前页码
const pageNo = ref(1);
// 每页数量
const pageSize = ref(20);
// 是否已搜索
const hasSearched = ref(false);
// 加载状态
const loading = ref(false);
// 搜索结果
const searchResult = ref(null);

// 搜索
const handleSearch = async () => {
  if (!keyword.value || !keyword.value.trim()) {
    proxy.Message.warning("请输入搜索关键词");
    return;
  }

  hasSearched.value = true;
  loading.value = true;
  pageNo.value = 1;

  await doSearch();
};

// 执行搜索
const doSearch = async () => {
  loading.value = true;
  try {
    const result = await proxy.Request({
      url: proxy.Api.searchMusic,
      params: {
        keyword: keyword.value.trim(),
        musicType: musicType.value,
        sortType: sortType.value,
        pageNo: pageNo.value,
        pageSize: pageSize.value,
      },
      showLoading: false,
    });

    loading.value = false;

    if (result && result.data) {
      searchResult.value = result.data;
    } else {
      searchResult.value = null;
    }
  } catch (error) {
    loading.value = false;
    searchResult.value = null;
  }
};

// 输入框输入事件
const handleInput = () => {
  // 输入时不清空结果，等用户点击搜索或按回车
};

// 改变音乐类型
const changeMusicType = (type) => {
  if (musicType.value === type) {
    return;
  }
  musicType.value = type;
  pageNo.value = 1;
  doSearch();
};

// 改变排序类型
const changeSortType = (type) => {
  if (sortType.value === type) {
    return;
  }
  sortType.value = type;
  pageNo.value = 1;
  doSearch();
};

// 改变页码
const changePage = (newPageNo) => {
  if (newPageNo < 1 || (searchResult.value && newPageNo > searchResult.value.pageTotal)) {
    return;
  }
  pageNo.value = newPageNo;
  doSearch();
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: "smooth" });
};

// 跳转到音乐详情
const goToMusicDetail = (musicId) => {
  router.push(`/play/${musicId}`);
};

// 播放列表
const playList = (music) => {
  if (searchResult.value && searchResult.value.musicList) {
    musicPlayStore.savePlayList(searchResult.value.musicList);
  }
};

// 页面加载时，如果有查询参数，自动搜索
onMounted(() => {
  const queryKeyword = route.query.keyword;
  if (queryKeyword) {
    keyword.value = queryKeyword;
    handleSearch();
  }
});
</script>

<style lang="scss" scoped>
.search-body {
  padding: 20px;
  background: var(--cardBg);
  min-height: calc(100vh - 100px);

  .search-header {
    margin-bottom: 20px;

    .search-box {
      display: flex;
      align-items: center;
      background: var(--bg);
      border-radius: 25px;
      padding: 5px;
      border: 1px solid var(--borderColor);
      max-width: 600px;
      margin: 0 auto;

      .search-icon {
        color: #999;
        font-size: 18px;
        margin-left: 15px;
        flex-shrink: 0;
      }

      .search-input {
        flex: 1;
        border: none;
        outline: none;
        background: transparent;
        padding: 10px 15px;
        font-size: 16px;
        color: var(--text);

        &::placeholder {
          color: #999;
        }
      }

      .search-btn {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--primary);
        border-radius: 20px;
        cursor: pointer;
        color: #fff;
        transition: all 0.3s;

        &:hover {
          background: var(--primaryHover);
        }

        .iconfont {
          font-size: 20px;
        }
      }
    }
  }

  .search-filters {
    display: flex;
    gap: 30px;
    margin-bottom: 20px;
    padding: 15px;
    background: var(--bg);
    border-radius: 8px;
    flex-wrap: wrap;

    .filter-item {
      display: flex;
      align-items: center;
      gap: 10px;

      .filter-label {
        color: var(--text);
        font-size: 14px;
        white-space: nowrap;
      }

      .filter-options {
        display: flex;
        gap: 10px;

        .filter-option {
          padding: 5px 15px;
          border-radius: 15px;
          background: var(--cardBg);
          color: var(--text);
          cursor: pointer;
          font-size: 14px;
          transition: all 0.3s;
          border: 1px solid var(--borderColor);

          &:hover {
            background: var(--bg);
          }

          &.active {
            background: var(--primary);
            color: #fff;
            border-color: var(--primary);
          }
        }
      }
    }
  }

  .search-results {
    .result-header {
      margin-bottom: 20px;
      padding: 10px 0;
      border-bottom: 1px solid var(--borderColor);

      .result-count {
        color: var(--text);
        font-size: 14px;
      }
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 0;
      color: var(--text);

      img {
        width: 50px;
        height: 50px;
        margin-bottom: 10px;
      }
    }

    .result-list {
      .result-item {
        display: flex;
        gap: 15px;
        padding: 15px;
        margin-bottom: 15px;
        background: var(--bg);
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.3s;
        border: 1px solid transparent;

        &:hover {
          background: var(--cardBg);
          border-color: var(--borderColor);
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }

        .music-cover {
          position: relative;
          flex-shrink: 0;
        }

        .music-info {
          flex: 1;
          display: flex;
          flex-direction: column;
          justify-content: center;
          gap: 8px;

          .music-title {
            font-size: 18px;
            font-weight: 500;
            color: var(--text);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .music-author {
            font-size: 14px;
            color: #999;
          }

          .music-stats {
            display: flex;
            gap: 15px;
            font-size: 12px;
            color: #999;

            span {
              display: flex;
              align-items: center;
            }
          }
        }
      }

      .pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 20px;
        margin-top: 30px;
        padding: 20px 0;

        .page-btn {
          padding: 8px 20px;
          background: var(--primary);
          color: #fff;
          border-radius: 5px;
          cursor: pointer;
          transition: all 0.3s;

          &:hover:not(.disabled) {
            background: var(--primaryHover);
          }

          &.disabled {
            background: #ccc;
            cursor: not-allowed;
          }
        }

        .page-info {
          color: var(--text);
          font-size: 14px;
        }
      }
    }

    .no-result {
      padding: 60px 0;
    }
  }

  .search-tips {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 100px 0;
    color: #999;

    .tips-icon {
      margin-bottom: 20px;
    }

    .tips-text {
      font-size: 16px;
    }
  }
}

@media (max-width: 768px) {
  .search-body {
    padding: 15px;

    .search-filters {
      flex-direction: column;
      gap: 15px;

      .filter-item {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
      }
    }

    .result-list {
      .result-item {
        .music-info {
          .music-stats {
            flex-direction: column;
            gap: 5px;
          }
        }
      }
    }
  }
}
</style>

