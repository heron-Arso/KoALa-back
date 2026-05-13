package com.koala.koalaback.api.admin;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.service.ArtistService;
import com.koala.koalaback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/api/v1/artists")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminArtistController {

    private final ArtistService artistService;

    // ── 기본 CRUD ─────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArtistDto.SummaryResponse> createArtist(
            @Valid @RequestBody ArtistDto.CreateRequest req) {
        return ApiResponse.ok(artistService.createArtist(req));
    }

    @PutMapping("/{artistCode}")
    public ApiResponse<ArtistDto.SummaryResponse> updateArtist(
            @PathVariable String artistCode,
            @Valid @RequestBody ArtistDto.UpdateRequest req) {
        return ApiResponse.ok(artistService.updateArtist(artistCode, req));
    }

    @DeleteMapping("/{artistCode}")
    public ApiResponse<Void> deleteArtist(@PathVariable String artistCode) {
        artistService.deleteArtist(artistCode);
        return ApiResponse.ok();
    }

    // ── 미디어 관리 ───────────────────────────────────────

    @GetMapping("/{artistCode}/media")
    public ApiResponse<List<ArtistDto.MediaResponse>> getMedia(
            @PathVariable String artistCode) {
        return ApiResponse.ok(artistService.getMediaList(artistCode));
    }

    @PostMapping(value = "/{artistCode}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArtistDto.MediaResponse> addMedia(
            @PathVariable String artistCode,
            @RequestPart("file") MultipartFile file,
            @RequestPart("meta") @Valid ArtistDto.MediaAddRequest req) {
        return ApiResponse.ok(artistService.addMedia(artistCode, file, req));
    }

    @PostMapping("/{artistCode}/media-url")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArtistDto.MediaResponse> addMediaUrl(
            @PathVariable String artistCode,
            @Valid @RequestBody ArtistDto.MediaUrlRequest req) {
        return ApiResponse.ok(artistService.addMediaUrl(artistCode, req));
    }

    @DeleteMapping("/{artistCode}/media/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            @PathVariable String artistCode,
            @PathVariable Long mediaId) {
        artistService.deleteMedia(artistCode, mediaId);
        return ApiResponse.ok();
    }

    // ── 약력 관리 ─────────────────────────────────────────

    @PostMapping("/{artistCode}/careers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArtistDto.CareerResponse> addCareer(
            @PathVariable String artistCode,
            @Valid @RequestBody ArtistDto.CareerAddRequest req) {
        return ApiResponse.ok(artistService.addCareer(artistCode, req));
    }

    @PutMapping("/{artistCode}/careers/{careerId}")
    public ApiResponse<ArtistDto.CareerResponse> updateCareer(
            @PathVariable String artistCode,
            @PathVariable Long careerId,
            @Valid @RequestBody ArtistDto.CareerUpdateRequest req) {
        return ApiResponse.ok(artistService.updateCareer(artistCode, careerId, req));
    }

    @DeleteMapping("/{artistCode}/careers/{careerId}")
    public ApiResponse<Void> deleteCareer(
            @PathVariable String artistCode,
            @PathVariable Long careerId) {
        artistService.deleteCareer(artistCode, careerId);
        return ApiResponse.ok();
    }
}