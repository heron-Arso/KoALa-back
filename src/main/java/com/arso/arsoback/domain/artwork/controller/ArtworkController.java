package com.arso.arsoback.domain.artwork.controller;

import com.arso.arsoback.domain.artwork.dto.ArtworkResponse;
import com.arso.arsoback.domain.artwork.service.ArtworkService;
import com.arso.arsoback.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;

    @GetMapping("/{artworkId}")
    public ApiResponse<ArtworkResponse> get(@PathVariable Long artworkId) {
        return ApiResponse.ok(artworkService.get(artworkId));
    }
}