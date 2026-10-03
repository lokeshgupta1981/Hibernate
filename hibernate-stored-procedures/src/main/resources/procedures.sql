CREATE PROCEDURE count_courses_by_level(
    IN  p_level VARCHAR(20),
    OUT p_total INT)
BEGIN
  SELECT COUNT(*) INTO p_total FROM course WHERE level = p_level;
END
//
CREATE PROCEDURE find_courses_by_level(IN p_level VARCHAR(20))
BEGIN
  SELECT id, title, level, price FROM course WHERE level = p_level ORDER BY title;
END
//
CREATE PROCEDURE apply_discount(
    IN  p_course_id BIGINT,
    IN  p_percent   INT,
    OUT p_new_price DECIMAL(8,2))
BEGIN
  UPDATE course SET price = ROUND(price * (100 - p_percent) / 100, 2) WHERE id = p_course_id;
  SELECT price INTO p_new_price FROM course WHERE id = p_course_id;
END
//
