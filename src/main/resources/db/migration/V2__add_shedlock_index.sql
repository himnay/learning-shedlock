-- Speeds up ShedLock's expired-lock queries which filter on lock_until
CREATE INDEX IF NOT EXISTS idx_shedlock_lock_until ON shedlock (lock_until);
