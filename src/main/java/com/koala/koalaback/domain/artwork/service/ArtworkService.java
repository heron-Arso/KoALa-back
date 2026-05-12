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

/**
 * TODO: 현재 ArtworkController 및 외부 참조 없음.
 * SKU 도메인으로 통합하거나 별도 API 엔드포인트 추가 필요.
 * artworks 테이블 사용 여부 확인 후 정리 예정.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkService {

    private final ArtworkRepository artworkRepository;

    public ArtworkResponse get(Long artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTWORK_NOT_FOUND));
        return ArtworkResponse.from(artwork);
    }
    public List<ArtworkListResponse> getByArtist(Long artistId) {
        return artworkRepository.findAllByArtistIdOrderByCreatedAtDesc(artistId)
                .stream()
                .map(ArtworkListResponse::from)
                .toList();
    }
}