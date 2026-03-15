package com.koala.koalaback.domain.artwork.service;

import com.koala.koalaback.domain.artwork.dto.ArtworkListResponse;
import com.koala.koalaback.domain.artwork.dto.ArtworkResponse;
import com.koala.koalaback.domain.artwork.entity.Artwork;
import com.koala.koalaback.domain.artwork.repository.ArtworkRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkService {

    private final ArtworkRepository artworkRepository;

    public ArtworkResponse get(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "작품을 찾을 수 없습니다. artworkId=" + artworkId));

        return ArtworkResponse.from(artwork);
    }
    public List<ArtworkListResponse> getByArtist(Long artistId) {
        return artworkRepository.findAllByArtistIdOrderByCreatedAtDesc(artistId)
                .stream()
                .map(ArtworkListResponse::from)
                .toList();
    }
}