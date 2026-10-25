package com.nexora.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/** Explicit, reversible system changes. Saves recovery data before the first write. */
final class ScreenProfile {
    private final Context context;
    private final SharedPreferences state;
    ScreenProfile(Context context) {
        this.context = context.getApplicationContext();
        state = this.context.getSharedPreferences("screen_profile", Context.MODE_PRIVATE);
    }
    boolean active() { return state.getBoolean("active", false); }
    boolean permitted() { return Settings.System.canWrite(context); }
    private int read(String key, int fallback) { return Settings.System.getInt(context.getContentResolver(), key, fallback); }
    private boolean write(String key, int value) {
        try { return Settings.System.putInt(context.getContentResolver(), key, value); }
        catch (SecurityException | IllegalArgumentException e) { return false; }
    }
    String apply() {
        if (!permitted()) return "Autorize a alteração de configurações e toque novamente em Aplicar perfil econômico.";
        if (active()) return "Já existe um perfil salvo. Restaure antes de aplicar outro.";
        int brightness = read(Settings.System.SCREEN_BRIGHTNESS, 128);
        int mode = read(Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        int timeout = read(Settings.System.SCREEN_OFF_TIMEOUT, 60000);
        int targetBrightness = Math.min(77, Math.max(1, brightness));
        int targetTimeout = Math.min(30000, Math.max(1000, timeout));
        boolean saved = state.edit().putInt("brightness", brightness).putInt("mode", mode).putInt("timeout", timeout)
                .putInt("targetBrightness", targetBrightness).putInt("targetTimeout", targetTimeout)
                .putBoolean("active", true).commit();
        if (!saved) return "Não foi possível salvar os ajustes anteriores. Nenhuma configuração alterada.";
        if (!write(Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
                || !write(Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                || !write(Settings.System.SCREEN_OFF_TIMEOUT, targetTimeout)) {
            // Keep saved recovery information on partial application; user can restore each unchanged setting.
            return "O Android recusou parte dos ajustes. Use Restaurar ajustes para recuperar o que foi alterado.";
        }
        return "Perfil aplicado: brilho manual de até 30% e suspensão em até 30 segundos. Ganho de bateria não medido; não aumenta CPU ou RAM.";
    }
    String restore() {
        if (!active()) return "Nenhum perfil para restaurar.";
        if (!permitted()) return "Autorize a alteração de configurações para restaurar o perfil.";
        boolean success = true;
        boolean preserved = false;
        int currentBrightness = read(Settings.System.SCREEN_BRIGHTNESS, -1);
        int currentMode = read(Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
        int oldBrightness = state.getInt("brightness", 128);
        int oldMode = state.getInt("mode", 1);
        // Restore our changes or an interrupted application; preserve later user changes.
        if ((currentBrightness == state.getInt("targetBrightness", -2) || currentBrightness == oldBrightness)
                && (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL || currentMode == oldMode)) {
            success = write(Settings.System.SCREEN_BRIGHTNESS, oldBrightness);
            success = write(Settings.System.SCREEN_BRIGHTNESS_MODE, oldMode) && success;
        } else { preserved = true; }
        int currentTimeout = read(Settings.System.SCREEN_OFF_TIMEOUT, -1);
        if (currentTimeout == state.getInt("targetTimeout", -2) || currentTimeout == state.getInt("timeout", -2)) {
            success = write(Settings.System.SCREEN_OFF_TIMEOUT, state.getInt("timeout", 60000)) && success;
        } else { preserved = true; }
        if (!success) return "Restauração incompleta. Os ajustes anteriores continuam salvos; tente novamente após verificar a permissão.";
        if (!state.edit().clear().commit()) return "Ajustes restaurados, mas o registro não pôde ser removido. Tente novamente.";
        return preserved ? "Restauração concluída. Alterações posteriores do usuário foram preservadas." : "Brilho e tempo de tela anteriores restaurados.";
    }
}
