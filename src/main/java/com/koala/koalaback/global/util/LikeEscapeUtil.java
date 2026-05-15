package com.koala.koalaback.global.util;

/**
 * JPQL / JDBC LIKE 파라미터 와일드카드 이스케이프 유틸리티
 *
 * <p>LIKE 쿼리에 사용자 입력을 그대로 바인딩하면 {@code %}, {@code _} 문자가
 * 와일드카드로 해석돼 의도치 않은 전체 매칭 및 DB 부하를 유발합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 *   String keyword = "%" + LikeEscapeUtil.escape(userInput) + "%";
 *   skuRepository.searchByKeyword(keyword, pageable);
 * }</pre>
 *
 * <p>JPQL 쿼리에는 반드시 {@code ESCAPE '\\'} 절이 있어야 합니다.
 */
public final class LikeEscapeUtil {

    private static final char ESCAPE_CHAR = '\\';

    private LikeEscapeUtil() {}

    /**
     * LIKE 와일드카드 문자를 이스케이프합니다.
     * {@code \} → {@code \\}, {@code %} → {@code \%}, {@code _} → {@code \_}
     */
    public static String escape(String value) {
        if (value == null) return null;
        return value
                .replace(String.valueOf(ESCAPE_CHAR), ESCAPE_CHAR + String.valueOf(ESCAPE_CHAR))
                .replace("%", ESCAPE_CHAR + "%")
                .replace("_", ESCAPE_CHAR + "_");
    }

    /**
     * {@code %keyword%} 형태의 양방향 포함 검색 파라미터를 만들어 반환합니다.
     */
    public static String contains(String value) {
        return "%" + escape(value) + "%";
    }
}
