package amalitech.blog.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Very simple utility to create PerformanceResult objects.
 * Just measures average/min/max time for a given operation.
 */
public class PerformanceUtil {

  private PerformanceUtil(){ }

  private static final Logger log = LoggerFactory.getLogger(PerformanceUtil.class);

  /**
   * Measures the given operation and returns a PerformanceResult object.
   *
   * @param operationName   name for the report (e.g. "Get recent posts - no index")
   * @param operation       the code to measure (e.g. () -> dao.getAll(1, 100))
   * @param runCount        how many times to execute (recommended: 10–30)
   * @return PerformanceResult object with the results
   */
  public static PerformanceResult measure(String operationName, Supplier<?> operation, int runCount) {
    long minNs = Long.MAX_VALUE;
    long maxNs = Long.MIN_VALUE;
    double totalNs = 0;
    int successfulRuns = 0;

    for (int i = 0; i < runCount; i++) {
      long start = System.nanoTime();
      try {
        operation.get();
        long durationNs = System.nanoTime() - start;
        totalNs += durationNs;
        minNs = Math.min(minNs, durationNs);
        maxNs = Math.max(maxNs, durationNs);
        successfulRuns++;
      } catch (Exception e) {
        log.warn("Run {} failed for '{}': {}", i + 1, operationName, e.getMessage());
      }
    }

    if (successfulRuns == 0) {
      log.error("No successful runs for '{}'", operationName);
      return new PerformanceResult(operationName, 0, -1, -1, -1, 0);
    }

    double avgMs = (totalNs / (double) successfulRuns) / 1_000_000.0;
    double minMs = minNs / 1_000_000.0;
    double maxMs = maxNs / 1_000_000.0;
    double totalMs = totalNs / 1_000_000.0;

    return new PerformanceResult(
            operationName, successfulRuns, avgMs, minMs, maxMs, totalMs
    );


  }

}