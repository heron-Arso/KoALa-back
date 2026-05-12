package com.koala.koalaback.global.util;

/**
 * 로그/응답 출력 시 개인정보(PII) 마스킹 유틸.
 *
 * 개인정보보호법 / GDPR 대응:
 *  - 로그에 평문 이메일·전화번호가 그대로 남으면 안 됨
 *  - 분석/디버깅을 위한 식별성은 유지하되 원본은 복구 불가하게 처리
 */
public final class PiiMasker {

    private PiiMasker() {}

    /**
     * 이메일 마스킹.
     *   honggildong@example.com  →  h********@example.com
     *   ab@example.com           →  a*@example.com
     *   a@example.com            →  *@example.com
     */
    public static String email(String email) {
        if (email == null || email.isBlank()) return "(blank)";
        int at = email.indexOf('@');
        if (at <= 0) return "***";

        String local = email.substring(0, at);
        String domain = email.substring(at);

        if (local.length() == 1) return "*" + domain;
        return local.charAt(0) + "*".repeat(Math.max(1, local.length() - 1)) + domain;
    }

    /**
     * 전화번호 마스킹.
     *   01012345678  →  010****5678
     *   0212345678   →  02****5678
     */
    public static String phone(String phone) {
        if (phone == null || phone.isBlank()) return "(blank)";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) return "***";

        String prefix = digits.substring(0, 3);
        String suffix = digits.substring(digits.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * 이름 마스킹 (한글 기준 가운데 글자 마스킹).
     *   홍길동  →  홍*동
     *   김철수  →  김*수
     *   김수  →  김*
     *   김  →  *
     */
    public static String name(String name) {
        if (name == null || name.isBlank()) return "(blank)";
        if (name.length() == 1) return "*";
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}
