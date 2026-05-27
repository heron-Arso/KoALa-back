-- 포장 섹션 설명 컬럼 추가
ALTER TABLE skus
    ADD COLUMN packaging_description LONGTEXT NULL COMMENT '포장 섹션 설명';
