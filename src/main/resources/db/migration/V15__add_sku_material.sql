-- 작품 재질/소재 컬럼 추가
ALTER TABLE skus
    ADD COLUMN material VARCHAR(300) NULL COMMENT '재질/소재 (예: 레진, 아크릴, 캔버스에 유화)';
