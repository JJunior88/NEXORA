package com.nexora.app;

import java.util.Locale;

/** Formatting and normalization shared by all device readings. */
public final class Metrics {
    private Metrics() {}
    public static int percent(long part, long total) {
        if (total <= 0 || part < 0) return -1;
        return (int) Math.round(Math.min(1d, (double) part / total) * 100);
    }
    public static String bytes(long bytes) {
        if (bytes < 0) return "Indisponível";
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) { value /= 1024; unit++; }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f %s", value, units[unit]);
    }
    public static String percentage(int value) { return value < 0 ? "—" : value + "%"; }
}
