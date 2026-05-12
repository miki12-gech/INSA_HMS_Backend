ALTER TABLE referral_requests
    ADD COLUMN IF NOT EXISTS approved_by_user_id VARCHAR(100);

ALTER TABLE referral_requests
    ADD COLUMN IF NOT EXISTS approved_by_name VARCHAR(255);
