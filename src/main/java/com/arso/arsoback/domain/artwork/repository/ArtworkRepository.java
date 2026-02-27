package com.arso.arsoback.domain.artwork.repository;

import com.arso.arsoback.domain.artwork.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    List<Artwork> findAllByArtistIdOrderByCreatedAtDesc(Long artistId);
}