CREATE TABLE Employee (
  ID INT AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  firstName VARCHAR(255),
  lastName VARCHAR(255),
  PRIMARY KEY (ID),
  UNIQUE KEY (email)
);

CREATE PROCEDURE get_employee_by_id(
    IN employeeId INT,
    OUT email VARCHAR(100),
    OUT firstName VARCHAR(100),
    OUT lastName VARCHAR(100))
BEGIN
SELECT e.email, e.firstName, e.lastName
INTO email, firstName, lastName
FROM Employee e
WHERE e.ID = employeeId;
END;

CREATE PROCEDURE get_employee_details_by_id(IN employeeId INT)
BEGIN
SELECT *
FROM Employee e
WHERE e.ID = employeeId;
END;