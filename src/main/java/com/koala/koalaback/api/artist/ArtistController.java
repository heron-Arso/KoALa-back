package com.koala.koalaback.api.artist;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.service.ArtistService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping
    public ApiResponse<PageResponse<ArtistDto.SummaryResponse>> getArtists(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(artistService.getArtists(pageable));
    }

    @GetMapping("/{artistCode}")
    public ApiResponse<ArtistDto.DetailResponse> getArtist(
            @PathVariable String artistCode) {
        return ApiResponse.ok(artistService.getArtist(artistCode));
    }
}