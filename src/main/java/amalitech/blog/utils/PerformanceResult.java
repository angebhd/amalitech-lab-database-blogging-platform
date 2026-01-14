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
  private final long totalTimeNs;

  public PerformanceResult(String operationName, int runCount, double averageMs,
                           double minMs, double maxMs, long totalTimeNs) {
    this.operationName = operationName;
    this.runCount = runCount;
    this.averageMs = averageMs;
    this.minMs = minMs;
    this.maxMs = maxMs;
    this.totalTimeNs = totalTimeNs;
  }

  // Getters
  public String getOperationName() { return operationName; }
  public int getRunCount() { return runCount; }
  public double getAverageMs() { return averageMs; }
  public double getMinMs() { return minMs; }
  public double getMaxMs() { return maxMs; }
  public long getTotalTimeNs() { return totalTimeNs; }

  @Override
  public String toString() {
    return String.format("%s (%d runs): avg = %.2f ms | min = %.2f ms | max = %.2f ms",
            operationName, runCount, averageMs, minMs, maxMs);
  }

  /**
   * Simple CSV line — copy-paste into your report table
   */
  public String toCsvLine() {
    return String.format("%s,%d,%.2f,%.2f,%.2f",
            operationName, runCount, averageMs, minMs, maxMs);
  }
}