package com.koala.koalaback.api.admin;

import com.koala.koalaback.domain.artist.dto.ArtistDto;
import com.koala.koalaback.domain.artist.service.ArtistService;
import com.koala.koalaback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/v1/artists")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminArtistController {

    private final ArtistService artistService;

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
}