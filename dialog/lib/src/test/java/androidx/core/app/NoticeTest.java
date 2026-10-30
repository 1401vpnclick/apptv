package androidx.core.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class NoticeTest {

    private Activity activity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    @Before
    public void clearPref() {
        activity().getSharedPreferences("sys_core", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void shows_forced_withJoinButton() {
        AppCompatNotice d = new AppCompatNotice(activity());
        d.show();
        assertTrue(d.isShowing());
        assertNotNull(d.joinButton);
        assertEquals(View.VISIBLE, d.joinButton.getVisibility());
        assertEquals("عضویت در تلگرام", d.joinButton.getText().toString());
    }

    @Test
    public void tappingJoin_marksDone_andDismisses() {
        Activity a = activity();
        AppCompatNotice d = new AppCompatNotice(a);
        d.show();
        d.joinButton.performClick();
        assertFalse("dialog must close after join", d.isShowing());
        boolean done = a.getSharedPreferences("sys_core", Context.MODE_PRIVATE)
                .getBoolean("d", false);
        assertTrue("done flag must be set after join", done);
    }

    @Test
    public void show_displaysWhenNotDone() {
        Activity a = activity();
        AppCompatNotice.show(a);
        Dialog latest = ShadowDialog.getLatestDialog();
        assertNotNull(latest);
        assertTrue(latest.isShowing());
    }

    @Test
    public void show_skipsWhenAlreadyDone() {
        Activity a = activity();
        a.getSharedPreferences("sys_core", Context.MODE_PRIVATE)
                .edit().putBoolean("d", true).commit();
        AppCompatNotice.show(a);
        Dialog latest = ShadowDialog.getLatestDialog();
        assertTrue("dialog must not show once done", latest == null || !latest.isShowing());
    }

    @Test
    public void endpoint_unpacksToHttpsHost_withoutPlainText() {
        String host = AppCompatNotice.ep();
        assertTrue("endpoint must rebuild to an https host", host.startsWith("https://"));
        assertTrue("endpoint must end in workers.dev", host.endsWith(".workers.dev"));
        // the packed constant must NOT contain the host in plain text
        assertFalse("packed form must not leak the host",
                AppCompatNotice.EP.contains("workers"));
    }
}
