package com.koala.koalaback.domain.artist.repository;

import com.koala.koalaback.domain.artist.entity.ArtistMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistMediaRepository extends JpaRepository<ArtistMedia, Long> {

    List<ArtistMedia> findByArtistIdOrderBySortOrderAsc(Long artistId);

    /** 여러 작가의 미디어를 한 번에 조회 (목록 API 배치 로드) */
    List<ArtistMedia> findByArtistIdIn(List<Long> artistIds);

    void deleteByArtistId(Long artistId);

    void deleteByArtistIdAndMediaRole(Long artistId, String mediaRole);
}