package com.easymusic.repository;

import com.easymusic.entity.es.MusicInfoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 音乐信息 ES Repository
 * 
 * @author EasyMusic Team
 * @date 2024
 */
@Repository
public interface MusicInfoRepository extends ElasticsearchRepository<MusicInfoDocument, String> {
}

