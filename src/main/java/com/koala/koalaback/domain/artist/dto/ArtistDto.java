package com.koala.koalaback.domain.artist.dto;

import com.koala.koalaback.domain.artist.entity.Artist;
import com.koala.koalaback.domain.artist.entity.ArtistCareer;
import com.koala.koalaback.domain.artist.entity.ArtistMedia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class ArtistDto {

    // ── Requests ──────────────────────────────────────────

    @Getter
    public static class CreateRequest {
        @NotBlank @Size(max = 150)
        private String name;

        @NotBlank @Size(max = 180)
        private String slug;

        private String description;
        private String artistNote;
        private String profileImageUrl;
    }

    @Getter
    public static class UpdateRequest {
        @NotBlank @Size(max = 150)
        private String name;

        @NotBlank @Size(max = 180)
        private String slug;

        private String description;
        private String artistNote;
        private String profileImageUrl;
    }

    // ── Responses ─────────────────────────────────────────

    @Getter
    @Builder
    public static class SummaryResponse {
        private Long id;
        private String artistCode;
        private String name;
        private String slug;
        private String profileImageUrl;
        private Boolean isActive;

        public static SummaryResponse from(Artist a) {
            return SummaryResponse.builder()
                    .id(a.getId())
                    .artistCode(a.getArtistCode())
                    .name(a.getName())
                    .slug(a.getSlug())
                    .profileImageUrl(a.getProfileImageUrl())
                    .isActive(a.getIsActive())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long id;
        private String artistCode;
        private String name;
        private String slug;
        private String description;
        private String artistNote;
        private String profileImageUrl;
        private Boolean isActive;
        private List<MediaResponse>  mediaList;
        private List<CareerResponse> careerList;
        private long followCount;
        private boolean isFollowing;

        public static DetailResponse from(Artist a, List<ArtistMedia> media,
                                          List<ArtistCareer> careers,
                                          long followCount, boolean isFollowing) {
            return DetailResponse.builder()
                    .id(a.getId())
                    .artistCode(a.getArtistCode())
                    .name(a.getName())
                    .slug(a.getSlug())
                    .description(a.getDescription())
                    .artistNote(a.getArtistNote())
                    .profileImageUrl(a.getProfileImageUrl())
                    .isActive(a.getIsActive())
                    .mediaList(media.stream().map(MediaResponse::from).toList())
                    .careerList(careers.stream().map(CareerResponse::from).toList())
                    .followCount(followCount)
                    .isFollowing(isFollowing)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CareerResponse {
        private Long id;
        private String category;
        private Integer year;
        private String content;
        private Integer sortOrder;

        public static CareerResponse from(ArtistCareer c) {
            return CareerResponse.builder()
                    .id(c.getId())
                    .category(c.getCategory())
                    .year(c.getYear())
                    .content(c.getContent())
                    .sortOrder(c.getSortOrder())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MediaResponse {
        private Long id;
        private String mediaType;
        private String mediaRole;
        private String fileUrl;
        private String thumbnailUrl;
        private String title;
        private Integer sortOrder;

        public static MediaResponse from(ArtistMedia m) {
            return MediaResponse.builder()
                    .id(m.getId())
                    .mediaType(m.getMediaType())
                    .mediaRole(m.getMediaRole())
                    .fileUrl(m.getFileUrl())
                    .thumbnailUrl(m.getThumbnailUrl())
                    .title(m.getTitle())
                    .sortOrder(m.getSortOrder())
                    .build();
        }
    }
}