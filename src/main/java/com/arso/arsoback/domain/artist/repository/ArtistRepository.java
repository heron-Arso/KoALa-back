package com.arso.arsoback.domain.artist.repository;

import com.arso.arsoback.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}