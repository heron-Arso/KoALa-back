package com.koala.koalaback.api.app;

import com.koala.koalaback.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 앱 버전 체크 API
 *
 * 앱 업데이트 시 변경할 상수:
 * - MIN_VERSION    : 이 버전 미만이면 강제 업데이트 다이얼로그
 * - LATEST_VERSION : 현재 최신 버전 (이보다 낮으면 권장 업데이트 다이얼로그)
 * - forceUpdate    : true로 설정 시 버전 무관하게 무조건 강제 업데이트
 */
@RestController
@RequestMapping("/api/v1/app")
public class AppVersionController {

    private static final String MIN_VERSION    = "1.0.0";
    private static final String LATEST_VERSION = "1.0.0";
    private static final String AOS_STORE_URL  = "https://play.google.com/store/apps/details?id=com.koala.app";
    private static final String IOS_STORE_URL  = "https://apps.apple.com/app/id000000000"; // 등록 후 변경

    @GetMapping("/version")
    public ApiResponse<VersionResponse> getVersion() {
        return ApiResponse.ok(new VersionResponse(
                MIN_VERSION,
                LATEST_VERSION,
                false,
                AOS_STORE_URL
        ));
    }

    public record VersionResponse(
            String minVersion,
            String latestVersion,
            boolean forceUpdate,
            String storeUrl
    ) {}
}
