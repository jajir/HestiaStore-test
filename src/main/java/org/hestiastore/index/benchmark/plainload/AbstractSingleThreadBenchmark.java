package org.hestiastore.index.benchmark.plainload;

/**
 * Common single-thread benchmark parent that centralizes the shared operation
 * implementation while allowing specialized parents to expose scenario-specific
 * JMH method names such as read/write/readSequential.
 */
abstract class AbstractSingleThreadBenchmark extends AbstractBenchmarkSupport {

    protected final String runSingleThreadOperation() throws Exception {
        return performOperation();
    }

    protected abstract String performOperation() throws Exception;
}
