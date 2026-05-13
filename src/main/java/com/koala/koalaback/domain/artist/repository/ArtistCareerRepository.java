package com.koala.koalaback.domain.artist.repository;

import com.koala.koalaback.domain.artist.entity.ArtistCareer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistCareerRepository extends JpaRepository<ArtistCareer, Long> {
    List<ArtistCareer> findByArtistIdOrderByCategoryAscSortOrderAsc(Long artistId);
    void deleteByArtistId(Long artistId);
}
