package com.nexora.app;

import android.app.Application;
import android.content.Intent;
import android.os.BatteryManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MainActivityTest {
    @Test public void dashboardReadsBatteryAndNavigatesToEverySection() {
        Application app = RuntimeEnvironment.getApplication();
        app.sendStickyBroadcast(new Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_LEVEL, 75)
                .putExtra(BatteryManager.EXTRA_SCALE, 100)
                .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
                .putExtra(BatteryManager.EXTRA_TEMPERATURE, 325));
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertNotNull(find(activity.getWindow().getDecorView(), "Carregando"));
            click(activity, "Bateria");
            assertNotNull(find(activity.getWindow().getDecorView(), "32,5 °C"));
            click(activity, "Espaço");
            assertNotNull(find(activity.getWindow().getDecorView(), "Gerenciar armazenamento"));
            click(activity, "Desempenho");
            assertNotNull(find(activity.getWindow().getDecorView(), "MEMÓRIA RAM EM USO"));
            assertNotNull(find(activity.getWindow().getDecorView(), "Indisponível nesta versão do Android"));
            click(activity, "Visão geral");
            assertNotNull(find(activity.getWindow().getDecorView(), "Um olhar no sistema"));
        }
    }

    @Test public void batteryActionOpensSystemSettings() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            click(activity, "Bateria");
            click(activity, "Configurar economia de bateria");
            Intent intent = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS, intent.getAction());
        }
    }

    @Test public void selectedTabSurvivesActivityRecreation() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            click(controller.get(), "Espaço");
            controller.recreate();
            assertNotNull(find(controller.get().getWindow().getDecorView(), "Gerenciar armazenamento"));
        }
    }

    @Test @Config(sdk = 26) public void minimumAndroidVersionCanOpenDashboard() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNotNull(find(controller.get().getWindow().getDecorView(), "Um olhar no sistema"));
        }
    }

    @Test @Config(sdk = 35) public void targetAndroidVersionCanReadPerformanceScreen() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            click(controller.get(), "Desempenho");
            assertNotNull(find(controller.get().getWindow().getDecorView(), "Estado térmico"));
        }
    }

    private static void click(MainActivity activity, String text) {
        View view = findButton(activity.getWindow().getDecorView(), text);
        assertTrue("Missing button: " + text, view instanceof Button);
        assertTrue(view.performClick());
    }
    private static View findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findButton(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static View find(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
