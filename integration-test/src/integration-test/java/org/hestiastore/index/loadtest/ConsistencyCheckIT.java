package org.hestiastore.index.loadtest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ConsistencyCheckIT {

    @Test
    void test_run() {
        ConsistencyCheck consistencyCheck = new ConsistencyCheck();
        consistencyCheck.test();
        assertTrue(true);
    }

}
