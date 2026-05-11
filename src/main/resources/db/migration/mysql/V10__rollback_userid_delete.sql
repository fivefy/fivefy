-- subscriptions 테이블에서 user_id 컬럼 제거
-- 롤백
ALTER TABLE subscriptions DROP COLUMN user_id;