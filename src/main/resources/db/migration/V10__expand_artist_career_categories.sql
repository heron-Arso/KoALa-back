-- ============================================================
-- V10: artist_careers content 길이 1000자 + year NULL 허용
-- ============================================================

-- content 컬럼 길이 500 → 1000
ALTER TABLE artist_careers
    MODIFY COLUMN content VARCHAR(1000) NOT NULL;

-- year 컬럼 NULL 허용 (연도 미공개 약력 지원)
ALTER TABLE artist_careers
    MODIFY COLUMN year INT NULL;

-- category CHECK 제약은 애플리케이션 레이어에서 검증
-- (기존 ck_artist_careers_category 제약은 로컬 수동 작업으로 제거됨)
