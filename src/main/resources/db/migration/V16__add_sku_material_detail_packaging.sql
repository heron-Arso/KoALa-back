-- 재질/소재 상세 설명 및 포장 섹션 제목 컬럼 추가
ALTER TABLE skus
    ADD COLUMN material_description LONGTEXT NULL COMMENT '재질/소재 상세 설명 (상세 페이지 표시용)',
    ADD COLUMN packaging_title VARCHAR(200) NULL COMMENT '포장 섹션 제목';
