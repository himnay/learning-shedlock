-- V2's index on lock_until never helps ShedLock: its insert-on-conflict, update, extend and
-- unlock statements all find the row by name, the primary key. lock_until also changes on every
-- lock and unlock, and an index on a changed column rules out PostgreSQL's HOT (heap-only tuple)
-- updates, so the index only added write work and table bloat. V2 itself is left untouched so
-- Flyway's checksum still matches on databases that already applied it.
DROP INDEX IF EXISTS idx_shedlock_lock_until;
