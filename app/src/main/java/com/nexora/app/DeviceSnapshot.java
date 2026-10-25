package com.nexora.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;

/** Only public, permission-free Android APIs. No estimates of CPU usage or battery capacity. */
final class DeviceSnapshot {
    int thermalStatus = -1;
    int battery = -1, temperature = Integer.MIN_VALUE, voltage = -1;
    int batteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    int health = BatteryManager.BATTERY_HEALTH_UNKNOWN;
    long totalStorage = -1, freeStorage = -1, totalMemory = -1, freeMemory = -1;
    boolean powerSave, lowMemory;
    String thermal = "Indisponível nesta versão do Android";

    static DeviceSnapshot read(Context context) {
        DeviceSnapshot s = new DeviceSnapshot();
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            s.battery = Metrics.percent(battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                    battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
            s.temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            s.voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            s.batteryStatus = battery.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            s.health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        }
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            s.totalStorage = fs.getTotalBytes();
            s.freeStorage = fs.getAvailableBytes();
        } catch (IllegalArgumentException ignored) { /* UI displays unavailable. */ }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            s.totalMemory = info.totalMem;
            s.freeMemory = info.availMem;
            s.lowMemory = info.lowMemory;
        }
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power != null) {
            s.powerSave = power.isPowerSaveMode();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String[] states = {"Normal", "Leve", "Moderado", "Severo", "Crítico", "Emergência", "Desligamento"};
                int status = power.getCurrentThermalStatus();
                s.thermalStatus = status;
                s.thermal = status >= 0 && status < states.length ? states[status] : "Indisponível";
            }
        }
        return s;
    }

    String charging() {
        switch (batteryStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Carregando";
            case BatteryManager.BATTERY_STATUS_FULL: return "Carga completa";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Em uso";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Conectado, sem carregar";
            default: return "Estado indisponível";
        }
    }
    String health() {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Boa";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Superaquecimento";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Falha";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Tensão elevada";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Temperatura baixa";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Falha não especificada";
            default: return "Indisponível";
        }
    }
}
