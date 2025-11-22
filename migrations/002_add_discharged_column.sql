-- Migration: add discharged column to patients table to mark released patients
ALTER TABLE patients ADD COLUMN discharged BOOLEAN DEFAULT FALSE;
-- Optionally, set discharged = FALSE for existing rows
UPDATE patients SET discharged = FALSE WHERE discharged IS NULL;
