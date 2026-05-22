-- FCM 푸시 알림 토큰 컬럼 추가
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(512) NULL;
