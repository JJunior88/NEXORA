package com.nexora.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional foreground-triggered maintenance, with one worker per process. No background service. */
final class Maintenance {
    static final long INTERVAL_MS = 6L * 60 * 60 * 1000;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    static SharedPreferences preferences(Context c) { return c.getSharedPreferences("maintenance", Context.MODE_PRIVATE); }
    static boolean running() { return RUNNING.get(); }
    static boolean enabled(Context c) { return preferences(c).getBoolean("automatic", false); }
    static boolean due(Context c, long now) {
        long last = preferences(c).getLong("lastAttempt", 0);
        return enabled(c) && (last == 0 || now < last || now - last >= INTERVAL_MS);
    }
    static boolean run(Context context, Runnable completion) {
        if (!RUNNING.compareAndSet(false, true)) return false;
        Context app = context.getApplicationContext();
        SharedPreferences prefs = preferences(app);
        prefs.edit().putLong("lastAttempt", System.currentTimeMillis()).apply();
        WORKER.execute(() -> {
            String result;
            try {
                CacheCleaner.Result cleaned = CacheCleaner.clean(app.getCacheDir().toPath());
                result = cleaned.summary();
            } catch (RuntimeException failure) {
                result = "Não foi possível concluir a limpeza local. Nenhuma correção de outros apps foi executada.";
            }
            prefs.edit().putString("lastResult", result).putLong("lastCompleted", System.currentTimeMillis()).apply();
            RUNNING.set(false);
            new Handler(Looper.getMainLooper()).post(completion);
        });
        return true;
    }
}
