package com.nexora.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetricsTest {
    @Test public void batteryScaleIsNormalized() {
        assertEquals(75, Metrics.percent(150, 200));
        assertEquals(0, Metrics.percent(0, 100));
        assertEquals(100, Metrics.percent(120, 100));
    }
    @Test public void unavailableReadingsStayUnavailable() {
        assertEquals(-1, Metrics.percent(-1, 100));
        assertEquals(-1, Metrics.percent(20, 0));
        assertEquals("—", Metrics.percentage(-1));
        assertEquals("Indisponível", Metrics.bytes(-1));
    }
    @Test public void largeStorageValuesDoNotOverflow() {
        assertEquals(50, Metrics.percent(256L * 1024 * 1024 * 1024, 512L * 1024 * 1024 * 1024));
        assertEquals("1,0 GB", Metrics.bytes(1024L * 1024 * 1024));
        assertEquals("0,0 B", Metrics.bytes(0));
    }
}
