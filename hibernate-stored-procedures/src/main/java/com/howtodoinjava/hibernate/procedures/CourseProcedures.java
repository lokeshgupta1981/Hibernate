package com.howtodoinjava.hibernate.procedures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.procedure.ProcedureCall;

// Every way of calling the three procedures shown in the article
public final class CourseProcedures {

  private CourseProcedures() {
  }

  // OUT parameter
  public static int countByLevel(EntityManager em, String level) {
    StoredProcedureQuery query = em.createStoredProcedureQuery("count_courses_by_level")
        .registerStoredProcedureParameter("p_level", String.class, ParameterMode.IN)
        .registerStoredProcedureParameter("p_total", Integer.class, ParameterMode.OUT)
        .setParameter("p_level", level);
    query.execute();
    return (Integer) query.getOutputParameterValue("p_total");
  }

  // Result set mapped to entities
  @SuppressWarnings("unchecked")
  public static List<Course> findByLevel(EntityManager em, String level) {
    return em.createStoredProcedureQuery("find_courses_by_level", Course.class)
        .registerStoredProcedureParameter("p_level", String.class, ParameterMode.IN)
        .setParameter("p_level", level)
        .getResultList();
  }

  // Result set as Object[] rows, without an entity
  @SuppressWarnings("unchecked")
  public static List<Object[]> findRowsByLevel(EntityManager em, String level) {
    return em.createStoredProcedureQuery("find_courses_by_level")
        .registerStoredProcedureParameter("p_level", String.class, ParameterMode.IN)
        .setParameter("p_level", level)
        .getResultList();
  }

  // Positional parameters instead of names
  public static int countByLevelPositional(EntityManager em, String level) {
    StoredProcedureQuery query = em.createStoredProcedureQuery("count_courses_by_level")
        .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
        .registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT)
        .setParameter(1, level);
    query.execute();
    return (Integer) query.getOutputParameterValue(2);
  }

  // Procedure that changes data: IN + OUT parameters, needs a transaction
  public static BigDecimal applyDiscount(EntityManager em, long courseId, int percent) {
    StoredProcedureQuery query = em.createStoredProcedureQuery("apply_discount")
        .registerStoredProcedureParameter("p_course_id", Long.class, ParameterMode.IN)
        .registerStoredProcedureParameter("p_percent", Integer.class, ParameterMode.IN)
        .registerStoredProcedureParameter("p_new_price", BigDecimal.class, ParameterMode.OUT)
        .setParameter("p_course_id", courseId)
        .setParameter("p_percent", percent);
    query.execute();
    return (BigDecimal) query.getOutputParameterValue("p_new_price");
  }

  // @NamedStoredProcedureQuery declared on Course
  @SuppressWarnings("unchecked")
  public static List<Course> findByLevelNamed(EntityManager em, String level) {
    return em.createNamedStoredProcedureQuery("Course.findByLevel")
        .setParameter("p_level", level)
        .getResultList();
  }

  // Hibernate's own ProcedureCall API
  public static int countByLevelHibernate(EntityManager em, String level) {
    ProcedureCall call = em.unwrap(Session.class).createStoredProcedureCall("count_courses_by_level");
    call.registerParameter("p_level", String.class, ParameterMode.IN);
    call.registerParameter("p_total", Integer.class, ParameterMode.OUT);
    call.setParameter("p_level", level);
    return (Integer) call.getOutputs().getOutputParameterValue("p_total");
  }
}
