package com.koala.koalaback.api.artist;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.service.ArtistService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    // ── Public ────────────────────────────────────────────

    @GetMapping("/api/v1/artists")
    public ApiResponse<PageResponse<ArtistDto.SummaryResponse>> getArtists(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(artistService.getArtists(pageable));
    }

    @GetMapping("/api/v1/artists/{slug}")
    public ApiResponse<ArtistDto.DetailResponse> getArtist(@PathVariable String slug) {
        return ApiResponse.ok(artistService.getArtistBySlug(slug));
    }

    // ── Admin ─────────────────────────────────────────────

    @PostMapping("/admin/api/v1/artists")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArtistDto.SummaryResponse> createArtist(
            @Valid @RequestBody ArtistDto.CreateRequest req) {
        return ApiResponse.ok(artistService.createArtist(req));
    }

    @PutMapping("/admin/api/v1/artists/{artistCode}")
    public ApiResponse<ArtistDto.SummaryResponse> updateArtist(
            @PathVariable String artistCode,
            @Valid @RequestBody ArtistDto.UpdateRequest req) {
        return ApiResponse.ok(artistService.updateArtist(artistCode, req));
    }

    @DeleteMapping("/admin/api/v1/artists/{artistCode}")
    public ApiResponse<Void> deleteArtist(@PathVariable String artistCode) {
        artistService.deleteArtist(artistCode);
        return ApiResponse.ok();
    }
}