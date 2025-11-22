-- Create BED database
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
    allocated_days INT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    allocated_on DATE NULL,
    discharged BOOLEAN DEFAULT FALSE
);

-- Create beds table
CREATE TABLE IF NOT EXISTS beds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bed_number VARCHAR(20),
    is_occupied BOOLEAN DEFAULT FALSE
);

-- Insert 100 beds
DELIMITER $$

CREATE PROCEDURE insert_beds()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 100 DO
        INSERT INTO beds (bed_number, is_occupied)
        VALUES (CONCAT('BED-', LPAD(i, 3, '0')), FALSE);
        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL insert_beds();


-- Optional checks
SELECT * FROM beds;
SELECT * FROM patients;
