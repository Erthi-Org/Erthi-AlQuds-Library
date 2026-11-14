package org.alquds.library;

import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.os.ParcelFileDescriptor;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiNavigationExitTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();

    @Test public void homeSearchOpensAndCancels() {
        try (ActivityScenario<HeritageHomeActivity> scenario = ActivityScenario.launch(HeritageHomeActivity.class)) {
            screen(scenario,"مكتبة القدس الجديدة");
            click("البحث في المكتبة");
            windowText("بحث مكتبة القدس الجديدة");
            click("إلغاء");
            windowText("البحث في المكتبة");
        }
    }

    @Test public void homeFiltersTabsAndLibraryDestinationsRemainUsable() throws Exception {
        try(ActivityScenario<HeritageHomeActivity> scenario=ActivityScenario.launch(HeritageHomeActivity.class)) {
            screen(scenario,"مكتبة القدس الجديدة");
            click("تصفية النتائج");
            screen(scenario,"تصفية النتائج");
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            screen(scenario,"مكتبة القدس الجديدة");
            click("المفضلة");screen(scenario,"المفضلة");capture("favorites");
            click("المصادر");screen(scenario,"المصادر");capture("sources");
            click("المرصد");screen(scenario,"مرصد التراث");capture("discover");
            click("الرئيسية");screen(scenario,"مكتبة القدس الجديدة");
            click("القرآن وعلومه");screen(scenario,"القرآن وعلومه");
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);screen(scenario,"مكتبة القدس الجديدة");
            click("المكتبة");click("كتبي وقراءة PDF");windowText("كتبي على هذا الجهاز");
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);screen(scenario,"مكتبة القدس الجديدة");
            click("المكتبة");click("الفهرس المحلي");windowText("ابحث في الفهرس");
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);screen(scenario,"مكتبة القدس الجديدة");
        }
    }

    @Test public void cancelAndConfirmedExitPreserveBooksPositionAndFavorites() throws Exception {
        Context context=instrumentation.getTargetContext();
        try(LocalPdfStore pdfs=new LocalPdfStore(context);CatalogDatabase catalog=new CatalogDatabase(context)) {
            Book book=catalog.search("").get(0);boolean originalFavorite=book.favorite;
            LocalPdfStore.Document document=pdfs.importPdf(new ByteArrayInputStream(PdfFixtures.twoPages()),"اختبار سلامة الخروج.pdf");
            try {
                pdfs.savePage(document.id,1);catalog.setFavorite(book.id,true);
                try(ActivityScenario<HeritageHomeActivity> scenario=ActivityScenario.launch(HeritageHomeActivity.class)) {
                    screen(scenario,"مكتبة القدس الجديدة");
                    click("القائمة");click("الخروج من التطبيق");windowText("هل تريد الخروج؟");click("إلغاء");
                    screen(scenario,"مكتبة القدس الجديدة");
                    click("القائمة");click("الخروج من التطبيق");windowText("هل تريد الخروج؟");click("خروج");
                    long deadline=SystemClock.uptimeMillis()+15000;
                    while(scenario.getState()!=androidx.lifecycle.Lifecycle.State.DESTROYED && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(50);
                    assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED,scenario.getState());
                }
                assertEquals(1,pdfs.get(document.id).lastPage);assertTrue(pdfs.file(document.id).isFile());
                assertTrue(catalog.search("",true).stream().anyMatch(saved->saved.id==book.id));
                try(ActivityScenario<HeritageHomeActivity> scenario=ActivityScenario.launch(HeritageHomeActivity.class)) {
                    screen(scenario,"مكتبة القدس الجديدة");
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);windowText("هل تريد الخروج؟");click("إلغاء");
                    screen(scenario,"مكتبة القدس الجديدة");
                }
            } finally {catalog.setFavorite(book.id,originalFavorite);pdfs.delete(document.id);}
        }
    }

    private void click(String text) {
        long deadline=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<deadline) {
            AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
            if(root!=null) for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text)) {
                if(!text.contentEquals(node.getText()==null?"":node.getText())) continue;
                AccessibilityNodeInfo target=node;
                while(target!=null && !target.isClickable()) target=target.getParent();
                if(target!=null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {instrumentation.waitForIdleSync();return;}
            }
            SystemClock.sleep(50);
        }
        fail("Missing action: "+text);
    }
    private void windowText(String text) {
        long deadline=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<deadline) {
            AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
            if(root!=null && !root.findAccessibilityNodeInfosByText(text).isEmpty()) return;
            SystemClock.sleep(50);
        }
        fail("Missing window: "+text);
    }
    private void screen(ActivityScenario<HeritageHomeActivity> scenario,String title) {
        long deadline=SystemClock.uptimeMillis()+15000;AtomicBoolean found=new AtomicBoolean();
        while(SystemClock.uptimeMillis()<deadline) {
            scenario.onActivity(activity->{
                ViewGroup root=activity.findViewById(android.R.id.content);
                CharSequence description=root.getChildAt(0).getContentDescription();
                found.set(description!=null && description.toString().startsWith(title));
            });
            if(found.get()) return;
            SystemClock.sleep(50);
        }
        fail("Missing screen: "+title);
    }
    private void capture(String name) throws Exception {
        instrumentation.waitForIdleSync();
        try(ParcelFileDescriptor descriptor=instrumentation.getUiAutomation().executeShellCommand(
                "screencap -p /data/local/tmp/alquds-polished-"+name+".png");
            InputStream output=new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            byte[] buffer=new byte[1024];while(output.read(buffer)!=-1){ /* Wait for capture. */ }
        }
    }
}
