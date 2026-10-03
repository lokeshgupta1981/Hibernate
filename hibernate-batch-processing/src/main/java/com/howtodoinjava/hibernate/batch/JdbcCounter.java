package com.howtodoinjava.hibernate.batch;

import java.util.ArrayList;
import java.util.List;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

/**
 * Listens to every JDBC execution that passes through the datasource-proxy.
 * One execute()/executeUpdate()/executeQuery()/executeBatch() call is one database round trip.
 */
public class JdbcCounter implements QueryExecutionListener {

  /** One call to the JDBC driver. */
  public record Execution(String sql, boolean batch, int statements) {
  }

  private final List<Execution> executions = new ArrayList<>();

  @Override
  public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
  }

  @Override
  public synchronized void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    String sql = queryInfoList.get(0).getQuery();
    int statements = execInfo.isBatch() ? execInfo.getBatchSize() : 1;
    executions.add(new Execution(sql, execInfo.isBatch(), statements));
  }

  public synchronized void reset() {
    executions.clear();
  }

  public synchronized List<Execution> executions() {
    return List.copyOf(executions);
  }

  /** Round trips for SQL that starts with the prefix ("insert", "update", "delete", "select"). */
  public synchronized long roundTrips(String prefix) {
    return executions.stream().filter(e -> e.sql().startsWith(prefix)).count();
  }

  /** Executions that were sent with executeBatch(). */
  public synchronized long batches(String prefix) {
    return executions.stream().filter(e -> e.batch() && e.sql().startsWith(prefix)).count();
  }

  /** SQL statements sent, counting every statement inside a batch. */
  public synchronized long statements(String prefix) {
    return executions.stream().filter(e -> e.sql().startsWith(prefix)).mapToLong(Execution::statements).sum();
  }

  /** Batch sizes in the order they were executed, for example [50, 50, 12]. */
  public synchronized List<Integer> batchSizes(String prefix) {
    return executions.stream().filter(e -> e.batch() && e.sql().startsWith(prefix)).map(Execution::statements).toList();
  }

  public synchronized long sequenceCalls() {
    return executions.stream().filter(e -> e.sql().contains("next value for")).count();
  }

  public synchronized long total() {
    return executions.size();
  }
}
