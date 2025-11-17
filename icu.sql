-- Create ICU database
CREATE DATABASE IF NOT EXISTS icu;

USE icu;

-- Create patients table
CREATE TABLE IF NOT EXISTS patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    age INT,
    condition_priority INT,
    bed_allocated BOOLEAN DEFAULT FALSE,
    allocated_bed_id INT NULL
);

-- Create beds table
CREATE TABLE IF NOT EXISTS beds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bed_number VARCHAR(10),
    is_occupied BOOLEAN DEFAULT FALSE
);

-- Insert ICU beds
INSERT INTO beds (bed_number, is_occupied) VALUES
('ICU-101', FALSE),
('ICU-102', FALSE),
('ICU-103', FALSE),
('ICU-104', FALSE);

-- Optional: check tables
SELECT * FROM beds;
SELECT * FROM patients;