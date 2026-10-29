package com.sing.check;

import static org.junit.Assert.*;

import android.app.Activity;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DialogTest {

    private Activity activity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    @Before
    public void reset() {
        WebService.joined = false;
    }

    @Test
    public void forced_showsOnlyJoinButton() {
        CustomDialogClass d = new CustomDialogClass(activity(), "عنوان", "پیام",
                false, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertTrue("dialog must be showing", d.isShowing());
        assertNotNull("join button must exist", d.positiveButton);
        assertEquals("join button must be visible", View.VISIBLE, d.positiveButton.getVisibility());
        assertEquals("عضویت", d.positiveButton.getText().toString());
        assertNull("no dismiss button when forced", d.negativeButton);
    }

    @Test
    public void optional_showsBothButtons() {
        CustomDialogClass d = new CustomDialogClass(activity(), "عنوان", "پیام",
                true, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertNotNull(d.positiveButton);
        assertEquals(View.VISIBLE, d.positiveButton.getVisibility());
        assertNotNull("dismiss button must exist when optional", d.negativeButton);
        assertEquals(View.VISIBLE, d.negativeButton.getVisibility());
        assertEquals("رد", d.negativeButton.getText().toString());
    }

    @Test
    public void tappingJoin_setsSessionFlag() {
        CustomDialogClass d = new CustomDialogClass(activity(), "t", "m",
                false, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertFalse(WebService.joined);
        d.positiveButton.performClick();
        assertTrue("joined flag set after tapping join", WebService.joined);
    }

    @Test
    public void showOnce_logic_forForced() {
        // forced + not yet joined -> show
        assertTrue(WebService.shouldShow(false));
        WebService.joined = true;
        // forced + already joined -> skip
        assertFalse(WebService.shouldShow(false));
        // optional always shows regardless of joined
        assertTrue(WebService.shouldShow(true));
    }

    @Test
    public void setTypeeee_safeWithoutFontAsset() {
        CustomDialogClass d = new CustomDialogClass(activity(), "t", "m",
                false, "رد", "عضویت", "https://t.me/x");
        d.show();
        d.setTypeeee(); // must not throw when the font asset is missing
        assertEquals("button still visible after setTypeeee",
                View.VISIBLE, d.positiveButton.getVisibility());
    }
}
