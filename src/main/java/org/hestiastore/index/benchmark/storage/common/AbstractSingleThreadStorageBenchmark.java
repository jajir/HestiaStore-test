package org.hestiastore.index.benchmark.storage.common;

import org.hestiastore.index.benchmark.common.AbstractBenchmarkSupport;

/**
 * Common single-thread benchmark parent that centralizes the shared operation
 * implementation while allowing specialized parents to expose scenario-specific
 * JMH method names such as read/write/sequentialRead.
 */
public abstract class AbstractSingleThreadStorageBenchmark
        extends AbstractBenchmarkSupport {

    protected final String runSingleThreadOperation() throws Exception {
        return performOperation();
    }

    protected abstract String performOperation() throws Exception;
}
