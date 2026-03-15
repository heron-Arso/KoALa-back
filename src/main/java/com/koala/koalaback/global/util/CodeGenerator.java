package com.koala.koalaback.global.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class CodeGenerator {

    private static final DateTimeFormatter ORDER_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** user_code, artist_code, sku_code 등 범용 코드 생성 */
    public String generateCode() {
        return UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }

    /** 주문번호: KL-20240115123045-ABCD */
    public String generateOrderNo() {
        String time = LocalDateTime.now().format(ORDER_FMT);
        String suffix = generateCode().substring(0, 4);
        return "KL-" + time + "-" + suffix;
    }

    /** 결제번호: PAY-{uuid 앞 12자리} */
    public String generatePaymentNo() {
        return "PAY-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }

    /** 리뷰코드: REV-{uuid 앞 12자리} */
    public String generateReviewCode() {
        return "REV-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }
}