package com.koala.koalaback.infra.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[KoALa] 비밀번호 재설정 인증 코드");
            helper.setText(buildEmailTemplate(token), true);

            mailSender.send(message);
            log.info("비밀번호 재설정 이메일 발송 성공: {}", toEmail);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
        }
    }

    private String buildEmailTemplate(String token) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px; background-color: #FAFAFA;">
                    <div style="text-align: center; margin-bottom: 40px;">
                        <h1 style="font-size: 28px; font-weight: bold; color: #000000; letter-spacing: -1px;">KoALa</h1>
                        <p style="color: #6B7280; font-size: 14px;">Korean Art Laboratory</p>
                    </div>
                    <div style="background: #FFFFFF; border-radius: 16px; padding: 40px; border: 1px solid #E5E7EB;">
                        <h2 style="font-size: 20px; font-weight: bold; color: #111111; margin-bottom: 16px;">비밀번호 재설정</h2>
                        <p style="color: #6B7280; font-size: 14px; line-height: 1.6; margin-bottom: 32px;">
                            아래 인증 코드를 입력하여 비밀번호를 재설정하세요.<br>
                            인증 코드는 <strong>5분간</strong> 유효합니다.
                        </p>
                        <div style="background: #F3F4F6; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 32px;">
                            <span style="font-size: 40px; font-weight: bold; letter-spacing: 8px; color: #000000;">%s</span>
                        </div>
                        <p style="color: #9CA3AF; font-size: 12px; text-align: center;">
                            본인이 요청하지 않은 경우 이 이메일을 무시하세요.
                        </p>
                    </div>
                    <p style="color: #9CA3AF; font-size: 12px; text-align: center; margin-top: 24px;">
                        © 2024 KoALa. All rights reserved.
                    </p>
                </div>
                """.formatted(token);
    }
}