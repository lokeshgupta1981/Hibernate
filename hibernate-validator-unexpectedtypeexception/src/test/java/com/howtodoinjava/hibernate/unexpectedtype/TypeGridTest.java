package com.howtodoinjava.hibernate.unexpectedtype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Builds the type to constraint grid by validating every field of TypeGrid.
 * "ok" = a validator exists, "X" = UnexpectedTypeException.
 */
class TypeGridTest {

  private static String row(Validator validator, Object bean) {
    StringBuilder sb = new StringBuilder();
    for (String property : TypeGrid.COLUMNS) {
      try {
        validator.validateProperty(bean, property);
        sb.append(" ok");
      } catch (UnexpectedTypeException e) {
        sb.append(" X");
      }
    }
    return sb.toString().trim();
  }

  @Test
  void compatibilityGrid() {
    Map<String, String> grid = new LinkedHashMap<>();
    try (ValidatorFactory factory = Validators.create()) {
      Validator v = factory.getValidator();
      grid.put("String", row(v, new TypeGrid.StringField()));
      grid.put("Integer", row(v, new TypeGrid.IntegerField()));
      grid.put("BigDecimal", row(v, new TypeGrid.BigDecimalField()));
      grid.put("Double", row(v, new TypeGrid.DoubleField()));
      grid.put("LocalDate", row(v, new TypeGrid.LocalDateField()));
      grid.put("List<String>", row(v, new TypeGrid.ListField()));
      grid.put("Optional<String>", row(v, new TypeGrid.OptionalField()));
      grid.put("Recipient", row(v, new TypeGrid.RecipientField()));
    }
    grid.forEach((k, r) -> System.out.println(k + " -> " + r));

    // columns: NotNull NotBlank NotEmpty Size Email Min Positive Past
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("String", "ok ok ok ok ok ok ok X");
    expected.put("Integer", "ok X X X X ok ok X");
    expected.put("BigDecimal", "ok X X X X ok ok X");
    expected.put("Double", "ok X X X X ok ok X");
    expected.put("LocalDate", "ok X X X X X X ok");
    expected.put("List<String>", "ok X ok ok X X X X");
    expected.put("Optional<String>", "ok X X X X X X X");
    expected.put("Recipient", "ok X X X X X X X");
    assertEquals(expected, grid);
  }
}
