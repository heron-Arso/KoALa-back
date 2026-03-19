/*
ALTER TABLE users
    ADD COLUMN oauth_provider VARCHAR(20)  NULL AFTER phone,
    ADD COLUMN oauth_id       VARCHAR(100) NULL AFTER oauth_provider,
    ADD INDEX idx_users_oauth (oauth_provider, oauth_id);*/
