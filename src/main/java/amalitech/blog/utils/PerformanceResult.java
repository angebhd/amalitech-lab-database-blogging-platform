package amalitech.blog.utils;

/**
 * Simple immutable object that holds the result of one performance test run.
 * Contains only the information for this specific measurement.
 * Use multiple instances to compare results yourself (e.g. before vs after index).
 */
public class PerformanceResult {

  private final String operationName;
  private final int runCount;
  private final double averageMs;
  private final double minMs;
  private final double maxMs;
  private final double totalTimeNs;

  public PerformanceResult(String operationName, int runCount, double averageMs,
                           double minMs, double maxMs, double totalTimeNs) {
    this.operationName = operationName;
    this.runCount = runCount;
    this.averageMs = averageMs;
    this.minMs = minMs;
    this.maxMs = maxMs;
    this.totalTimeNs = totalTimeNs;
  }


  @Override
  public String toString() {
    return String.format("%s (%d runs): avg = %.2f ms | min = %.2f ms | max = %.2f ms | total time = %.2f ms",
            operationName, runCount, averageMs, minMs, maxMs, totalTimeNs);
  }

}