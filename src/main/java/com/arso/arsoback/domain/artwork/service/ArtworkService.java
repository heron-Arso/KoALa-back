package com.arso.arsoback.domain.artwork.service;

import com.arso.arsoback.domain.artwork.dto.ArtworkResponse;
import com.arso.arsoback.domain.artwork.entity.Artwork;
import com.arso.arsoback.domain.artwork.repository.ArtworkRepository;
import com.arso.arsoback.global.exception.BusinessException;
import com.arso.arsoback.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}