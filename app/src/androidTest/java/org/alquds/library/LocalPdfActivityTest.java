package org.alquds.library;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LocalPdfActivityTest {
    @Test public void readsNavigatesRecreatesAndResumesTheSavedPage() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (LocalPdfStore store = new LocalPdfStore(context)) {
            String[] importedId = new String[1];
            try {
                try (ActivityScenario<LocalPdfActivity> scenario = ActivityScenario.launch(LocalPdfActivity.class)) {
                    // Exercise the real ContentResolver/import result path using a test-only provider.
                    scenario.onActivity(activity -> activity.onActivityResult(41, android.app.Activity.RESULT_OK,
                            new android.content.Intent().setData(PdfFixtureProvider.URI)));
                    await(scenario, "الصفحة 1 من 2");
                    importedId[0] = store.list().stream().filter(item -> item.title.equals("اختبار قارئ محلي.pdf")).findFirst().get().id;
                    assertEquals(1, context.getContentResolver().delete(PdfFixtureProvider.URI, null, null));
                    click(scenario, "الصفحة التالية");
                    await(scenario, "الصفحة 2 من 2");
                    scenario.recreate();
                    await(scenario, "الصفحة 2 من 2");
                    android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
                    try (android.os.ParcelFileDescriptor descriptor = instrumentation.getUiAutomation()
                            .executeShellCommand("screencap -p /data/local/tmp/alquds-local-pdf-page.png");
                         java.io.InputStream output = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                        byte[] buffer = new byte[1024]; while (output.read(buffer) != -1) { /* Wait for capture. */ }
                    }
                    click(scenario, "الرجوع إلى كتبي");
                    await(scenario, "كتبي على هذا الجهاز");
                }
                try (ActivityScenario<LocalPdfActivity> scenario = ActivityScenario.launch(LocalPdfActivity.class)) {
                    await(scenario, "اختبار قارئ محلي.pdf — 2 صفحة");
                    click(scenario, "اختبار قارئ محلي.pdf — 2 صفحة");
                    await(scenario, "الصفحة 2 من 2");
                }
            } finally {
                if (importedId[0] != null) store.delete(importedId[0]);
                context.getContentResolver().delete(PdfFixtureProvider.URI, null, null);
            }
        }
    }

    private static TextView find(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = find(group.getChildAt(i), text); if (result != null) return result;
            }
        }
        return null;
    }

    private static void await(ActivityScenario<LocalPdfActivity> scenario, String text) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        AtomicBoolean found = new AtomicBoolean();
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> found.set(find(activity.findViewById(android.R.id.content), text) != null));
            if (found.get()) return;
            SystemClock.sleep(50);
        }
        fail("Missing UI text: " + text);
    }

    private static void click(ActivityScenario<LocalPdfActivity> scenario, String text) {
        scenario.onActivity(activity -> {
            TextView view = find(activity.findViewById(android.R.id.content), text);
            assertTrue(view instanceof Button); assertTrue(view.isEnabled()); view.performClick();
        });
    }
}
