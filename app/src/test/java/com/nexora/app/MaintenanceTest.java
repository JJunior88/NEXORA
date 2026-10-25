package com.nexora.app;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MaintenanceTest {
    @Test public void automaticWorkRequiresOptInAndSixHourInterval() {
        Context c = RuntimeEnvironment.getApplication();
        long now = 100000000;
        assertFalse(Maintenance.due(c, now));
        Maintenance.preferences(c).edit().putBoolean("automatic", true).putLong("lastAttempt", now).commit();
        assertFalse(Maintenance.due(c, now + Maintenance.INTERVAL_MS - 1));
        assertTrue(Maintenance.due(c, now + Maintenance.INTERVAL_MS));
        Maintenance.preferences(c).edit().putBoolean("automatic", false).commit();
        assertFalse(Maintenance.due(c, now + Maintenance.INTERVAL_MS));
    }
}
