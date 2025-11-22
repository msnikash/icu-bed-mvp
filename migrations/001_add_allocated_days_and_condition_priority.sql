-- Migration: add allocated_days column and convert condition_priority to VARCHAR
-- Run this against the `bed` database.

-- 1) Add allocated_days if missing
ALTER TABLE patients
  ADD COLUMN IF NOT EXISTS allocated_days INT NULL;

-- 2) Convert condition_priority to VARCHAR to hold values 'High','Medium','Low'
-- If column is numeric, this will convert values to strings; otherwise it's a no-op.
ALTER TABLE patients
  MODIFY condition_priority VARCHAR(10);

-- 3) OPTIONAL: If you previously used numeric priorities (1..5), map them to textual ones.
-- Adjust mapping as you prefer. This is safe to run multiple times.
UPDATE patients
SET condition_priority = CASE
  WHEN condition_priority IN ('1','2',1,2) THEN 'High'
  WHEN condition_priority IN ('3',3) THEN 'Medium'
  WHEN condition_priority IN ('4','5',4,5) THEN 'Low'
  ELSE COALESCE(condition_priority, 'Medium')
END
WHERE condition_priority NOT IN ('High','Medium','Low');

-- 4) OPTIONAL: set existing NULL allocated_days to 0
UPDATE patients SET allocated_days = 0 WHERE allocated_days IS NULL;

-- 5) Show final schema for verification
SHOW COLUMNS FROM patients;
