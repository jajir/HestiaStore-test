package org.hestiastore.index.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hestiastore.index.integration.ConsistencyCheck;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * It's just helping test class that allows verify consistency test itself.
 */
public class ConsistencyCheckIT {

    @Test
    @Disabled
    void test_run() {
        ConsistencyCheck consistencyCheck = new ConsistencyCheck();
        consistencyCheck.test();
        assertTrue(true);
    }

}
