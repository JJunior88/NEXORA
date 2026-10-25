package com.nexora.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiagnosticsTest {
    private DeviceSnapshot normal() {
        DeviceSnapshot s = new DeviceSnapshot();
        s.battery = 70; s.temperature = 300; s.totalMemory = 4096; s.freeMemory = 1024;
        s.totalStorage = 100; s.freeStorage = 50;
        return s;
    }
    @Test public void occupiedRamAloneIsNotDiagnosedAsAnError() {
        DeviceSnapshot s = normal(); s.freeMemory = 0;
        assertTrue(Diagnostics.inspect(s).isEmpty());
    }
    @Test public void heatAndLowStorageProduceSpecificActions() {
        DeviceSnapshot s = normal(); s.thermalStatus = 2; s.freeStorage = 9;
        assertEquals(2, Diagnostics.inspect(s).size());
        assertNull(Diagnostics.inspect(s).get(0).action);
        assertEquals(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS, Diagnostics.inspect(s).get(1).action);
    }
    @Test public void unknownReadingsAreNotClaimedHealthy() {
        assertEquals("Algumas leituras estão indisponíveis", Diagnostics.inspect(new DeviceSnapshot()).get(0).title);
    }
    @Test public void lowBatteryDoesNotSuggestSavingWhileCharging() {
        DeviceSnapshot s = normal(); s.battery = 10;
        s.batteryStatus = android.os.BatteryManager.BATTERY_STATUS_CHARGING;
        assertTrue(Diagnostics.inspect(s).isEmpty());
        s.batteryStatus = android.os.BatteryManager.BATTERY_STATUS_DISCHARGING;
        assertEquals(1, Diagnostics.inspect(s).size());
        s.powerSave = true;
        assertTrue(Diagnostics.inspect(s).isEmpty());
    }
}
