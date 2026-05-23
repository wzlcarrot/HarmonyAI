package com.easymusic.service.impl;

import com.easymusic.entity.es.MusicInfoDocument;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.query.MusicSearchQuery;
import com.easymusic.entity.vo.SearchResultVO;
import com.easymusic.mappers.MusicInfoMapper;
import com.easymusic.mappers.UserInfoMapper;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.query.UserInfoQuery;
import com.easymusic.redis.RedisComponent;
import com.easymusic.repository.MusicInfoRepository;
import com.easymusic.service.MusicSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 音乐搜索服务实现类
 * 
 * @author EasyMusic Team
 * @date 2024
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MusicSearchServiceImpl implements MusicSearchService {

    private final MusicInfoRepository musicInfoRepository;
    
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;
    
    private final MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;
    
    private final UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    
    private final RedisComponent redisComponent;

    /**
     * 根据音乐标题搜索音乐
     * 
     * @param query 搜索查询参数
     * @return 搜索结果
     */
    @Override
    public SearchResultVO search(MusicSearchQuery query) {
        try {
            // 构建查询条件
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            
            // 1. 音乐标题搜索（支持中文分词）
            if (StringUtils.hasText(query.getKeyword())) {
                boolQuery.must(QueryBuilders.matchQuery("musicTitle", query.getKeyword()));
            }
            
            // 2. 只搜索已完成的音乐（musicStatus=1）
            boolQuery.must(QueryBuilders.termQuery("musicStatus", 1));
            
            // 3. 音乐类型筛选（可选）
            if (query.getMusicType() != null) {
                boolQuery.must(QueryBuilders.termQuery("musicType", query.getMusicType()));
            }
            
            // 4. 构建排序
            String sortType = StringUtils.hasText(query.getSortType()) 
                ? query.getSortType() : "playCount";
            
            NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery);
            
            // 根据排序类型设置排序
            if ("time".equals(sortType)) {
                // 最新发布排序：按创建时间降序
                searchQueryBuilder.withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
            } else {
                // 播放量排序（默认）：按播放数降序
                searchQueryBuilder.withSort(SortBuilders.fieldSort("playCount").order(SortOrder.DESC));
            }
            
            // 5. 分页设置
            int pageNo = query.getPageNo() != null && query.getPageNo() > 0 ? query.getPageNo() : 1;
            int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20;
            Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
            searchQueryBuilder.withPageable(pageable);
            
            // 6. 执行搜索
            NativeSearchQuery searchQuery = searchQueryBuilder.build();
            SearchHits<MusicInfoDocument> searchHits = elasticsearchRestTemplate.search(searchQuery, MusicInfoDocument.class);
            
            // 7. 转换结果
            long totalCount = searchHits.getTotalHits();
            List<MusicInfoDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
            
            // 8. 转换为MusicInfo对象（从MySQL获取完整信息，确保数据一致性）
            // 直接使用Mapper和RedisComponent，避免循环依赖
            List<MusicInfo> musicList = new ArrayList<>();
            for (MusicInfoDocument doc : documents) {
                MusicInfo musicInfo = getMusicInfoByMusicId(doc.getMusicId());
                if (musicInfo != null) {
                    // 确保播放量有值（如果为null，使用ES中的值或默认为0）
                    if (musicInfo.getPlayCount() == null) {
                        musicInfo.setPlayCount(doc.getPlayCount() != null ? doc.getPlayCount() : 0);
                    }
                    // 去掉点赞量（设置为null，不返回给前端）
                    musicInfo.setGoodCount(null);
                    musicList.add(musicInfo);
                }
            }
            
            // 9. 构建返回结果
            int pageTotal = (int) Math.ceil((double) totalCount / pageSize);
            return new SearchResultVO(pageNo, pageSize, totalCount, musicList);
            
        } catch (Exception e) {
            log.error("ES搜索失败, keyword={}, pageNo={}, pageSize={}", 
                query.getKeyword(), query.getPageNo(), query.getPageSize(), e);
            // 搜索失败时返回空结果，不抛出异常，保证服务可用性
            return new SearchResultVO(query.getPageNo(), query.getPageSize(), 0L, new ArrayList<>());
        }
    }

    /**
     * 保存或更新音乐到ES
     * 
     * @param musicInfo 音乐信息
     */
    @Override
    public void saveOrUpdateMusicToES(MusicInfo musicInfo) {
        if (musicInfo == null || musicInfo.getMusicId() == null) {
            log.warn("音乐信息为空，跳过ES同步");
            return;
        }
        
        try {
            // 只同步已完成的音乐（musicStatus=1）
            if (musicInfo.getMusicStatus() == null || musicInfo.getMusicStatus() != 1) {
                log.debug("音乐未完成，跳过ES同步, musicId={}, musicStatus={}", 
                    musicInfo.getMusicId(), musicInfo.getMusicStatus());
                return;
            }
            
            // 转换为ES文档
            MusicInfoDocument document = convertToDocument(musicInfo);
            
            // 保存或更新到ES（幂等操作）
            musicInfoRepository.save(document);
            
            log.debug("音乐同步到ES成功, musicId={}, musicTitle={}", 
                musicInfo.getMusicId(), musicInfo.getMusicTitle());
                
        } catch (Exception e) {
            log.error("同步音乐到ES失败, musicId={}", musicInfo.getMusicId(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 批量保存音乐到ES（用于全量同步）
     * 
     * @param musicInfoList 音乐信息列表
     */
    @Override
    public void batchSaveMusicToES(List<MusicInfo> musicInfoList) {
        if (musicInfoList == null || musicInfoList.isEmpty()) {
            return;
        }
        
        try {
            List<MusicInfoDocument> documents = new ArrayList<>();
            for (MusicInfo musicInfo : musicInfoList) {
                // 只同步已完成的音乐
                if (musicInfo.getMusicStatus() != null && musicInfo.getMusicStatus() == 1) {
                    MusicInfoDocument document = convertToDocument(musicInfo);
                    documents.add(document);
                }
            }
            
            if (!documents.isEmpty()) {
                musicInfoRepository.saveAll(documents);
                log.info("批量同步音乐到ES成功, 数量={}", documents.size());
            }
            
        } catch (Exception e) {
            log.error("批量同步音乐到ES失败, 数量={}", musicInfoList.size(), e);
            throw new RuntimeException("批量同步音乐到ES失败", e);
        }
    }

    /**
     * 从ES删除音乐
     * 
     * @param musicId 音乐ID
     */
    @Override
    public void deleteMusicFromES(String musicId) {
        if (musicId == null) {
            return;
        }
        
        try {
            musicInfoRepository.deleteById(musicId);
            log.debug("从ES删除音乐成功, musicId={}", musicId);
        } catch (Exception e) {
            log.error("从ES删除音乐失败, musicId={}", musicId, e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 全量同步MySQL数据到ES（管理员功能）
     * 
     * @return 同步数量
     */
    @Override
    public Integer syncAllMusicToES() {
        try {
            log.info("开始全量同步音乐数据到ES");
            
            // 查询所有已完成的音乐（musicStatus=1）
            MusicInfoQuery query = new MusicInfoQuery();
            query.setMusicStatus(1);
            
            // 分批查询，每批5000条
            int batchSize = 5000;
            int pageNo = 1;
            int totalCount = 0;
            
            while (true) {
                query.setPageNo(pageNo);
                query.setPageSize(batchSize);
                
                // 直接使用Mapper查询，避免循环依赖
                List<MusicInfo> musicList = musicInfoMapper.selectList(query);
                if (musicList == null || musicList.isEmpty()) {
                    break;
                }
                
                // 批量保存到ES
                batchSaveMusicToES(musicList);
                totalCount += musicList.size();
                
                log.info("全量同步进度: 已同步{}条, 当前批次{}条", totalCount, musicList.size());
                
                // 如果当前批次数量小于batchSize，说明已经是最后一批
                if (musicList.size() < batchSize) {
                    break;
                }
                
                pageNo++;
            }
            
            log.info("全量同步音乐数据到ES完成, 总数量={}", totalCount);
            return totalCount;
            
        } catch (Exception e) {
            log.error("全量同步音乐数据到ES失败", e);
            throw new RuntimeException("全量同步失败", e);
        }
    }

    /**
     * 将MusicInfo转换为MusicInfoDocument
     * 
     * @param musicInfo 音乐信息
     * @return ES文档
     */
    private MusicInfoDocument convertToDocument(MusicInfo musicInfo) {
        MusicInfoDocument document = new MusicInfoDocument();
        document.setMusicId(musicInfo.getMusicId());
        document.setUserId(musicInfo.getUserId());
        document.setMusicTitle(musicInfo.getMusicTitle());
        document.setNickName(musicInfo.getNickName());
        document.setCover(musicInfo.getCover());
        document.setAudioPath(musicInfo.getAudioPath());
        document.setDuration(musicInfo.getDuration());
        document.setPlayCount(musicInfo.getPlayCount() != null ? musicInfo.getPlayCount() : 0);
        document.setGoodCount(musicInfo.getGoodCount() != null ? musicInfo.getGoodCount() : 0);
        document.setCommendType(musicInfo.getCommendType());
        document.setCreateTime(musicInfo.getCreateTime());
        document.setMusicStatus(musicInfo.getMusicStatus());
        document.setMusicType(musicInfo.getMusicType());
        return document;
    }

    /**
     * 根据MusicId获取音乐信息（避免循环依赖，直接使用Mapper和Redis）
     * 
     * @param musicId 音乐ID
     * @return 音乐信息
     */
    private MusicInfo getMusicInfoByMusicId(String musicId) {
        if (musicId == null) {
            return null;
        }
        
        // 1. 先查 Redis 缓存
        MusicInfo musicInfo = redisComponent.getMusicInfo(musicId);
        if (musicInfo != null) {
            // 补充用户信息
            if (musicInfo.getUserId() != null) {
                UserInfo userInfo = userInfoMapper.selectByUserId(musicInfo.getUserId());
                if (userInfo != null) {
                    musicInfo.setNickName(userInfo.getNickName());
                }
            }
            return musicInfo;
        }
        
        // 2. Redis 未命中时，查数据库
        musicInfo = musicInfoMapper.selectByMusicId(musicId);
        
        if (musicInfo != null) {
            // 补充用户信息
            if (musicInfo.getUserId() != null) {
                UserInfo userInfo = userInfoMapper.selectByUserId(musicInfo.getUserId());
                if (userInfo != null) {
                    musicInfo.setNickName(userInfo.getNickName());
                }
            }
            // 存入 Redis 缓存
            redisComponent.saveMusicInfo(musicId, musicInfo);
        }
        
        return musicInfo;
    }
}

