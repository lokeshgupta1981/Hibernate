package com.howtodoinjava.hibernate.namedprocedure;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_item")

// 1. OUT parameter
@NamedStoredProcedureQuery(
    name = "StockItem.countUnits",
    procedureName = "count_units",
    parameters = {
        @StoredProcedureParameter(name = "p_warehouse", type = String.class, mode = ParameterMode.IN),
        @StoredProcedureParameter(name = "p_total", type = Integer.class, mode = ParameterMode.OUT)
    })

// 2. Result set mapped to the entity with resultClasses
@NamedStoredProcedureQuery(
    name = "StockItem.findLowStock",
    procedureName = "find_low_stock",
    resultClasses = StockItem.class,
    parameters = {
        @StoredProcedureParameter(name = "p_warehouse", type = String.class),
        @StoredProcedureParameter(name = "p_below", type = Integer.class)
    })

// 3. Result set mapped to a record with resultSetMappings
@NamedStoredProcedureQuery(
    name = "StockItem.summary",
    procedureName = "stock_summary",
    resultSetMappings = "WarehouseSummaryMapping")
@SqlResultSetMapping(
    name = "WarehouseSummaryMapping",
    classes = @ConstructorResult(
        targetClass = WarehouseSummary.class,
        columns = {
            @ColumnResult(name = "warehouse"),
            @ColumnResult(name = "items"),
            @ColumnResult(name = "units", type = Long.class)
        }))

// 4. INOUT parameter, procedure changes data
@NamedStoredProcedureQuery(
    name = "StockItem.restock",
    procedureName = "restock",
    parameters = {
        @StoredProcedureParameter(name = "p_name", type = String.class),
        @StoredProcedureParameter(name = "p_warehouse", type = String.class),
        @StoredProcedureParameter(name = "p_quantity", type = Integer.class, mode = ParameterMode.INOUT)
    })

// 5. Positional parameters: no name attribute
@NamedStoredProcedureQuery(
    name = "StockItem.countUnitsByPosition",
    procedureName = "count_units",
    parameters = {
        @StoredProcedureParameter(type = String.class),
        @StoredProcedureParameter(type = Integer.class, mode = ParameterMode.OUT)
    })

// 6. Query timeout hint in milliseconds (Hibernate 7.4.11 stores it, but does not apply it to the call)
@NamedStoredProcedureQuery(
    name = "StockItem.audit",
    procedureName = "stock_audit",
    parameters = @StoredProcedureParameter(name = "p_rounds", type = Integer.class),
    hints = @QueryHint(name = "jakarta.persistence.query.timeout", value = "1000"))

// Declarations used by the FAQ examples
// 7. Wrong procedure name: the factory starts, the call fails
@NamedStoredProcedureQuery(
    name = "StockItem.missingProcedure",
    procedureName = "count_unit")

// 8. A record in resultClasses: fails, records need resultSetMappings
@NamedStoredProcedureQuery(
    name = "StockItem.summaryAsRecord",
    procedureName = "stock_summary",
    resultClasses = WarehouseSummary.class)

// 9. Names that differ from the procedure: works, the call is positional
@NamedStoredProcedureQuery(
    name = "StockItem.countUnitsRenamed",
    procedureName = "count_units",
    parameters = {
        @StoredProcedureParameter(name = "warehouse", type = String.class),
        @StoredProcedureParameter(name = "total", type = Integer.class, mode = ParameterMode.OUT)
    })

// 10. Parameters in the wrong order: fails
@NamedStoredProcedureQuery(
    name = "StockItem.countUnitsSwapped",
    procedureName = "count_units",
    parameters = {
        @StoredProcedureParameter(name = "p_total", type = Integer.class, mode = ParameterMode.OUT),
        @StoredProcedureParameter(name = "p_warehouse", type = String.class)
    })
public class StockItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String warehouse;

  private int quantity;

  protected StockItem() {
  }

  public StockItem(String name, String warehouse, int quantity) {
    this.name = name;
    this.warehouse = warehouse;
    this.quantity = quantity;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getWarehouse() {
    return warehouse;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  @Override
  public String toString() {
    return name + " (" + warehouse + ", " + quantity + ")";
  }
}
