package com.nexora.app;

import android.content.Context;
import android.provider.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import android.app.AppOpsManager;
import org.robolectric.Shadows;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ScreenProfileTest {
    Context context;
    @Before public void initialize() {
        context = RuntimeEnvironment.getApplication();
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 200);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 60000);
        allowSettings(true);
    }
    @Test public void refusedPermissionDoesNotChangeSettingsOrRecordProfile() {
        allowSettings(false);
        ScreenProfile profile = new ScreenProfile(context);
        profile.apply();
        assertFalse(profile.active());
        assertEquals(200, read(Settings.System.SCREEN_BRIGHTNESS));
    }
    @Test public void profileCanBeRestoredAcrossControllerInstances() {
        ScreenProfile profile = new ScreenProfile(context);
        profile.apply();
        assertTrue(profile.active());
        assertEquals(77, read(Settings.System.SCREEN_BRIGHTNESS));
        assertEquals(0, read(Settings.System.SCREEN_BRIGHTNESS_MODE));
        assertEquals(30000, read(Settings.System.SCREEN_OFF_TIMEOUT));
        new ScreenProfile(context).restore();
        assertFalse(profile.active());
        assertEquals(200, read(Settings.System.SCREEN_BRIGHTNESS));
        assertEquals(1, read(Settings.System.SCREEN_BRIGHTNESS_MODE));
        assertEquals(60000, read(Settings.System.SCREEN_OFF_TIMEOUT));
    }
    @Test public void restoringDoesNotOverwriteSubsequentUserChanges() {
        ScreenProfile profile = new ScreenProfile(context); profile.apply();
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 180);
        profile.restore();
        assertEquals(180, read(Settings.System.SCREEN_BRIGHTNESS));
        assertEquals(60000, read(Settings.System.SCREEN_OFF_TIMEOUT));
        assertFalse(profile.active());
    }
    @Test public void applyingTwiceDoesNotLoseOriginalSettings() {
        ScreenProfile profile = new ScreenProfile(context); profile.apply(); profile.apply(); profile.restore();
        assertEquals(200, read(Settings.System.SCREEN_BRIGHTNESS));
    }
    @Test public void smallerBrightnessAndTimeoutAreNotIncreased() {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 20);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 15000);
        new ScreenProfile(context).apply();
        assertEquals(20, read(Settings.System.SCREEN_BRIGHTNESS));
        assertEquals(15000, read(Settings.System.SCREEN_OFF_TIMEOUT));
    }
    @Test public void revokingPermissionKeepsRecoveryInformation() {
        ScreenProfile profile = new ScreenProfile(context); profile.apply();
        allowSettings(false); profile.restore();
        assertTrue(profile.active());
        allowSettings(true); profile.restore();
        assertFalse(profile.active());
    }
    private void allowSettings(boolean allowed) {
        AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        Shadows.shadowOf(manager).setMode(AppOpsManager.OPSTR_WRITE_SETTINGS, android.os.Process.myUid(),
                context.getPackageName(), allowed ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }
    private int read(String key) { return Settings.System.getInt(context.getContentResolver(), key, -1); }
}
