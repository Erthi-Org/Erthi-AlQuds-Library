package org.alquds.library;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HeritageHomeTest {
    @Test public void launcherUsesRecoveredHomeAndNativeMenuStillOpensPdfAndCatalog() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull(launch);
        assertEquals(HeritageHomeActivity.class.getName(), launch.getComponent().getClassName());
        try (ActivityScenario<HeritageHomeActivity> scenario = ActivityScenario.launch(HeritageHomeActivity.class)) {
            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            instrumentation.waitForIdleSync();
            // Give the first Canvas traversal time to paint before recording the real screen.
            SystemClock.sleep(300);
            try (ParcelFileDescriptor descriptor = instrumentation.getUiAutomation()
                    .executeShellCommand("screencap -p /data/local/tmp/alquds-restored-home.png");
                 InputStream output = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                byte[] buffer = new byte[1024]; while (output.read(buffer) != -1) { /* Wait for capture. */ }
            }
            scenario.recreate();
            scenario.onActivity(activity -> {
                ViewGroup content = activity.findViewById(android.R.id.content);
                View home = content.getChildAt(0);
                assertTrue(home.getContentDescription().toString().contains("مكتبة القدس الجديدة"));
                assertTrue(home.performClick()); // Accessible fallback to the native menu.
            });
            clickDialog("كتبي وقراءة PDF");
            awaitWindowText("كتبي على هذا الجهاز");
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0).performClick());
            clickDialog("الفهرس المحلي");
            awaitWindowText("ابحث في الفهرس");
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        }
    }

    private static void clickDialog(String text) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            if (root != null) for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
                AccessibilityNodeInfo target = node;
                while (target != null && !target.isClickable()) target = target.getParent();
                if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    instrumentation.waitForIdleSync(); return;
                }
            }
            SystemClock.sleep(50);
        }
        fail("Missing menu action: " + text);
    }

    private static void awaitWindowText(String text) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            if (root != null && !root.findAccessibilityNodeInfosByText(text).isEmpty()) return;
            SystemClock.sleep(50);
        }
        fail("Missing destination: " + text);
    }
}
