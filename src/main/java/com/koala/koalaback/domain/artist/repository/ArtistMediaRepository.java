package com.koala.koalaback.domain.artist.repository;

import com.koala.koalaback.domain.artist.entity.ArtistMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistMediaRepository extends JpaRepository<ArtistMedia, Long> {

    List<ArtistMedia> findByArtistIdOrderBySortOrderAsc(Long artistId);

    void deleteByArtistId(Long artistId);
}