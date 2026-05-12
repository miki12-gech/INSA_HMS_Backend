-- Add the height column required for doctor triage BMI display.

ALTER TABLE patient_triage
ADD COLUMN IF NOT EXISTS height VARCHAR(100);
