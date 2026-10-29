package com.sing.check;

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
public class OfflineDialogTest {

    private Activity activity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    @Before
    public void clearPref() {
        activity().getSharedPreferences("offline_dlg", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void shows_forced_withJoinButton() {
        OfflineDialog d = new OfflineDialog(activity());
        d.show();
        assertTrue(d.isShowing());
        assertNotNull(d.joinButton);
        assertEquals(View.VISIBLE, d.joinButton.getVisibility());
        assertEquals("عضویت در تلگرام", d.joinButton.getText().toString());
    }

    @Test
    public void tappingJoin_marksDone_andDismisses() {
        Activity a = activity();
        OfflineDialog d = new OfflineDialog(a);
        d.show();
        d.joinButton.performClick();
        assertFalse("dialog must close after join", d.isShowing());
        boolean done = a.getSharedPreferences("offline_dlg", Context.MODE_PRIVATE)
                .getBoolean("done", false);
        assertTrue("done flag must be set after join", done);
    }

    @Test
    public void show_displaysWhenNotDone() {
        Activity a = activity();
        OfflineDialog.show(a);
        Dialog latest = ShadowDialog.getLatestDialog();
        assertNotNull(latest);
        assertTrue(latest.isShowing());
    }

    @Test
    public void show_skipsWhenAlreadyDone() {
        Activity a = activity();
        a.getSharedPreferences("offline_dlg", Context.MODE_PRIVATE)
                .edit().putBoolean("done", true).commit();
        OfflineDialog.show(a);
        Dialog latest = ShadowDialog.getLatestDialog();
        assertTrue("dialog must not show once done", latest == null || !latest.isShowing());
    }
}
