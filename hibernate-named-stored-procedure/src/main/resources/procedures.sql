CREATE PROCEDURE count_units(
    IN  p_warehouse VARCHAR(20),
    OUT p_total     INT)
BEGIN
  SELECT COALESCE(SUM(quantity), 0) INTO p_total FROM stock_item WHERE warehouse = p_warehouse;
END
//
CREATE PROCEDURE find_low_stock(
    IN p_warehouse VARCHAR(20),
    IN p_below     INT)
BEGIN
  SELECT id, name, warehouse, quantity FROM stock_item
  WHERE warehouse = p_warehouse AND quantity < p_below
  ORDER BY quantity;
END
//
CREATE PROCEDURE stock_summary()
BEGIN
  SELECT warehouse, COUNT(*) AS items, SUM(quantity) AS units
  FROM stock_item GROUP BY warehouse ORDER BY warehouse;
END
//
CREATE PROCEDURE restock(
    IN    p_name      VARCHAR(20),
    IN    p_warehouse VARCHAR(20),
    INOUT p_quantity  INT)
BEGIN
  UPDATE stock_item SET quantity = quantity + p_quantity
  WHERE name = p_name AND warehouse = p_warehouse;
  SELECT quantity INTO p_quantity FROM stock_item
  WHERE name = p_name AND warehouse = p_warehouse;
END
//
CREATE PROCEDURE stock_audit(IN p_rounds INT)
BEGIN
  SELECT BENCHMARK(p_rounds, SHA2('bolt', 256)) AS done;
END
//
