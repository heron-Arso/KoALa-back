package com.arso.arsoback.domain.artist.controller;

import com.arso.arsoback.domain.artist.dto.ArtistChannelResponse;
import com.arso.arsoback.domain.artist.service.ArtistService;
import com.arso.arsoback.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    // 유튜브 채널처럼: 작가 소개 + 작품 리스트
    @GetMapping("/{artistId}/channel")
    public ApiResponse<ArtistChannelResponse> getChannel(@PathVariable Long artistId) {
        return ApiResponse.ok(artistService.getChannel(artistId));
    }
}