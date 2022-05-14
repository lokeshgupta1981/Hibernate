DELIMITER //
CREATE PROCEDURE get_employee_by_id(IN employeeId INT, OUT email VARCHAR(100), OUT firstName VARCHAR(100), OUT lastName VARCHAR(100))
BEGIN
 SELECT e.email, e.firstName, e.lastName
 INTO email, firstName, lastName
 FROM Employee e
 WHERE e.ID = employeeId;
END //