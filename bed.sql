-- Create Bed database
CREATE DATABASE IF NOT EXISTS bed;

USE bed;

-- Create patients table
CREATE TABLE IF NOT EXISTS patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    age INT,
    condition_priority VARCHAR(10),
    bed_allocated BOOLEAN DEFAULT FALSE,
    allocated_bed_id INT NULL,
    allocated_days INT NULL
);

-- Create beds table
CREATE TABLE IF NOT EXISTS beds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bed_number VARCHAR(10),
    is_occupied BOOLEAN DEFAULT FALSE
);

-- Insert BED beds
INSERT INTO beds (bed_number, is_occupied) VALUES
('BED-101', FALSE),
('BED-102', FALSE),
('BED-103', FALSE),
('BED-104', FALSE);

-- Optional: check tables
SELECT * FROM beds;
SELECT * FROM patients;
