package com.koala.koalaback.domain.artist.repository;

import com.koala.koalaback.domain.artist.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByArtistCode(String artistCode);

    Optional<Artist> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Artist> findByIsActiveTrueAndDeletedAtIsNull(Pageable pageable);
}