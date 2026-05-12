ALTER TABLE referral_requests
    ADD COLUMN IF NOT EXISTS letter_management_document_url VARCHAR(1000);

ALTER TABLE referral_requests
    ADD COLUMN IF NOT EXISTS letter_management_sent_at VARCHAR(50);

ALTER TABLE referral_requests
    ADD COLUMN IF NOT EXISTS letter_management_sent_by_user_id VARCHAR(100);
